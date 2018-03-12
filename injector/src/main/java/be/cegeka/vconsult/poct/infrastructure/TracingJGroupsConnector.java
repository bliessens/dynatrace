package be.cegeka.vconsult.poct.infrastructure;

import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.OutgoingRemoteCallTracer;
import com.dynatrace.oneagent.sdk.api.enums.ChannelType;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.callbacks.NoOpCallback;
import org.axonframework.commandhandling.distributed.CommandBusConnector;
import org.axonframework.commandhandling.distributed.Member;
import org.axonframework.common.Registration;
import org.axonframework.jgroups.commandhandling.JGroupsConnector;
import org.axonframework.messaging.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.UUID;

@Component
class TracingJGroupsConnector implements CommandBusConnector {

    public static final String TRACE_ID = "traceId";

    private static final Logger LOG = LoggerFactory.getLogger(TracingJGroupsConnector.class);

    private final OneAgentSDK agent;
    private final JGroupsConnector delegate;

    @Autowired
    public TracingJGroupsConnector(OneAgentSDK oneAgent, JGroupsConnector delegate) {
        LOG.info("Using " + TracingJGroupsConnector.class.getSimpleName());
        this.delegate = delegate;
        this.agent = oneAgent;
    }

    @Override
    public <C> void send(Member destination, CommandMessage<? extends C> command) throws Exception {

        this.send(destination, command, NoOpCallback.INSTANCE);

//        final OutgoingRemoteCallTracer tracer = agent.traceOutgoingRemoteCall("dispatchCommand", delegate.getNodeName(), "DistributedCommandBus", ChannelType.TCP_IP, "jgroups://" + destination.name());
//        String tag = tracer.getDynatraceStringTag();
//        if (StringUtils.isEmpty(tag)) {
//            tag = "fake-" + UUID.randomUUID().toString();
//        }
//        try {
//            LOG.info("Adding trace {} to command {}", tag, command.getPayloadType().getSimpleName());
//            delegate.send(destination, traceableCommand(command, tag));
//        } catch (Exception e) {
//            LOG.error("Reporting trace {} as failure", tag);
//            tracer.error(e);
//        } finally {
//            LOG.info("Completed trace {} ", tag);
//            tracer.end();
//        }
    }

    @Override
    public <C, R> void send(Member destination, CommandMessage<C> command, CommandCallback<? super C, R> callback) throws Exception {
        if (destination.local()) {
            delegate.send(destination, command);
        } else {
            final OutgoingRemoteCallTracer tracer = agent.traceOutgoingRemoteCall("dispatchCommand", delegate.getNodeName(), "DistributedCommandBus", ChannelType.TCP_IP, "jgroups://" + destination.name());
            tracer.setProtocolName("jgroups/custom");
            tracer.start();
            String tag = tracer.getDynatraceStringTag();
            if (StringUtils.isEmpty(tag)) {
                tag = "fake-" + UUID.randomUUID().toString();
            }
            try {
                LOG.info("Adding trace {} to command {}", tag, command.getPayloadType().getSimpleName());
                delegate.send(destination, destination.local() ? command : traceableCommand(command, tag), callback);
            } catch (Exception e) {
                LOG.error("Reporting trace {} as failure", tag);
                tracer.error(e);
                throw e;
            } finally {
                LOG.info("Completed trace {} ", tag);
                tracer.end();
            }
        }
    }

    private <C> CommandMessage<C> traceableCommand(CommandMessage<C> command, String tag) {
        return command.andMetaData(Collections.singletonMap(TRACE_ID, tag));
    }

    @Override
    public Registration subscribe(String commandName, MessageHandler<? super CommandMessage<?>> handler) {
        return delegate.subscribe(commandName, handler);
    }

}

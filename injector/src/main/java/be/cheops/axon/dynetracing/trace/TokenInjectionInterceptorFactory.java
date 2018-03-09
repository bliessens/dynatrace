package be.cheops.axon.dynetracing.trace;

import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.OutgoingRemoteCallTracer;
import com.dynatrace.oneagent.sdk.api.enums.ChannelType;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;

// not used
@Deprecated
public class TokenInjectionInterceptorFactory {

    private static final String TRACE_ID = "traceId";

    private final OneAgentSDK factory;
    private final String name;

    public TokenInjectionInterceptorFactory(@Value("${distributed.clustername}") String clusterName) {
        this.factory = OneAgentSDKFactory.createInstance();
        this.name = clusterName;
    }

    public MessageHandlerInterceptor<CommandMessage<?>> newInjectingInterceptor() {
        final OutgoingRemoteCallTracer tracer = this.factory.traceOutgoingRemoteCall("dispatchCommand", name, "DistributedCommandBus", ChannelType.TCP_IP, "jgroups://");
        return new TokenInjectionHandlerInterceptor(tracer);
    }

    protected static class TokenInjectionHandlerInterceptor implements MessageHandlerInterceptor<CommandMessage<?>> {

        private static final Logger LOG = LoggerFactory.getLogger(TokenInjectionHandlerInterceptor.class);

        private final OutgoingRemoteCallTracer tracer;

        public TokenInjectionHandlerInterceptor(OutgoingRemoteCallTracer tracer) {
            this.tracer = tracer;
        }

        @Override
        public Object handle(UnitOfWork<? extends CommandMessage<?>> unitOfWork, InterceptorChain interceptorChain) throws Exception {
            unitOfWork.transformMessage(command -> {
                final String tag = tracer.getDynatraceStringTag();
                LOG.info("Injecting tag {}", tag);
                return command.andMetaData(Collections.singletonMap(TRACE_ID, tag));
            });
            unitOfWork.onCommit(uow -> tracer.end());
            unitOfWork.onRollback(uow -> {
                tracer.error(uow.getExecutionResult().getExceptionResult());
                tracer.end();
            });

            tracer.start();
            return interceptorChain.proceed();
        }
    }
}

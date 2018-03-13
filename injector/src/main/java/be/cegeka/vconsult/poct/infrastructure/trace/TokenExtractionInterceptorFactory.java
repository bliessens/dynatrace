package be.cegeka.vconsult.poct.infrastructure.trace;

import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.IncomingRemoteCallTracer;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;

public class TokenExtractionInterceptorFactory {

    public static final String TRACE_ID = "traceId";

    private final OneAgentSDK factory;
    private final String clusterName;

    public TokenExtractionInterceptorFactory(@Value("${distributed.clustername}") String clusterName) {
        this.clusterName = clusterName;
        this.factory = OneAgentSDKFactory.createInstance();
    }

    public MessageHandlerInterceptor<CommandMessage<?>> newExtractingInterceptor() {
        return new TokenExtractionHandlerInterceptor(factory, clusterName);
    }


    private static class TokenExtractionHandlerInterceptor implements MessageHandlerInterceptor<CommandMessage<?>> {
        private static final Logger LOG = LoggerFactory.getLogger(TokenExtractionHandlerInterceptor.class);


        private final OneAgentSDK factory;
        private final String clusterName;

        public TokenExtractionHandlerInterceptor(OneAgentSDK factory, String clusterName) {
            this.factory = factory;
            this.clusterName = clusterName;
        }

        @Override
        public Object handle(UnitOfWork<? extends CommandMessage<?>> unitOfWork, InterceptorChain interceptorChain) throws Exception {
            if (unitOfWork.getMessage().getMetaData().containsKey(TRACE_ID)) {

                IncomingRemoteCallTracer tracer = this.factory.traceIncomingRemoteCall(unitOfWork.getMessage().getCommandName(), "Command Bus[" + clusterName + "]", "DistributedCommandBus");
                final String tag = (String) unitOfWork.getMessage().getMetaData().get(TRACE_ID);
                LOG.info("Extracting tag {}", tag);
                unitOfWork.transformMessage(cmd -> cmd.withMetaData(Collections.emptyMap()));
                unitOfWork.onCommit(uow -> tracer.end());
                unitOfWork.onRollback(uow -> {
                    tracer.error(uow.getExecutionResult().getExceptionResult());
                    tracer.end();
                });

                tracer.setDynatraceStringTag(tag);
                tracer.start();
                tracer.setProtocolName("jgroups/custom");
            }
            return interceptorChain.proceed();
        }

    }
}

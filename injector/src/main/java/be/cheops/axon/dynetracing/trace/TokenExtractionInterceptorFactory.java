package be.cheops.axon.dynetracing.trace;

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
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class TokenExtractionInterceptorFactory {

    public static final String TRACE_ID = "traceId";

    private final OneAgentSDK factory;
    private final String name;

    public TokenExtractionInterceptorFactory(@Value("${distributed.clustername}") String clusterName) {
        this.name = clusterName;
        this.factory = OneAgentSDKFactory.createInstance();
    }

    public MessageHandlerInterceptor<CommandMessage<?>> newExtractingInterceptor() {
        return new TokenExtractionHandlerInterceptor(this.factory.traceIncomingRemoteCall("dispatchCommand", name, "DistributedCommandBus"));
    }


    private static class TokenExtractionHandlerInterceptor implements MessageHandlerInterceptor<CommandMessage<?>> {
        private static final Logger LOG = LoggerFactory.getLogger(TokenExtractionHandlerInterceptor.class);

        private final IncomingRemoteCallTracer tracer;

        public TokenExtractionHandlerInterceptor(IncomingRemoteCallTracer tracer) {
            this.tracer = tracer;
        }

        @Override
        public Object handle(UnitOfWork<? extends CommandMessage<?>> unitOfWork, InterceptorChain interceptorChain) throws Exception {
            if (unitOfWork.getMessage().getMetaData().containsKey(TRACE_ID)) {
                final String tag = (String) unitOfWork.getMessage().getMetaData().get(TRACE_ID);
                LOG.info("Extracting tag {}", tag);
                unitOfWork.transformMessage(cmd -> cmd.withMetaData(Collections.emptyMap()));
                tracer.setDynatraceStringTag(tag);
                unitOfWork.onCommit(uow -> tracer.end());
                unitOfWork.onRollback(uow -> {
                    tracer.error(uow.getExecutionResult().getExceptionResult());
                    tracer.end();
                });

                tracer.start();
            }
            return interceptorChain.proceed();
        }

    }
}

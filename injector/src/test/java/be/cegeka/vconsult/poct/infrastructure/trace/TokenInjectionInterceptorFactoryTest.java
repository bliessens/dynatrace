package be.cegeka.vconsult.poct.infrastructure.trace;

import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.OutgoingRemoteCallTracer;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.DefaultUnitOfWork;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class TokenInjectionInterceptorFactoryTest {

    private UnitOfWork<CommandMessage<?>> unitOfWork;
    private InterceptorChain chain = Mockito.mock(InterceptorChain.class);
    private OneAgentSDK sdk = Mockito.mock(OneAgentSDK.class);
    private OutgoingRemoteCallTracer tracer = Mockito.mock(OutgoingRemoteCallTracer.class);

    private MessageHandlerInterceptor<CommandMessage<?>> interceptor;


    @Before
    public void setUp() throws Exception {
//        while (CurrentUnitOfWork.isStarted()) {
//            CurrentUnitOfWork.get().rollback();
//        }

        Mockito.when(sdk.traceOutgoingRemoteCall(Matchers.anyString(), Matchers.anyString(), Matchers.anyString(), Matchers.any(), Matchers.anyString())).thenReturn(tracer);
        Mockito.when(tracer.getDynatraceStringTag()).thenReturn("token");
        interceptor = new TokenInjectionInterceptorFactory.TokenInjectionHandlerInterceptor(tracer);
        unitOfWork = DefaultUnitOfWork.startAndGet(GenericCommandMessage.asCommandMessage(new Object()));
    }

    @Test
    public void testSuccessfulTransaction() throws Exception {
        interceptor.handle(this.unitOfWork, chain);
        this.unitOfWork.commit();

        InOrder inOrder = Mockito.inOrder(tracer, chain);
        inOrder.verify(tracer).getDynatraceStringTag();
        inOrder.verify(tracer).start();
        inOrder.verify(chain).proceed();
        inOrder.verify(tracer).end();

    }

    @Test
    public void testFailingTransaction() throws Exception {
        final Throwable throwable = new RuntimeException();

        interceptor.handle(this.unitOfWork, chain);
        this.unitOfWork.rollback(throwable);

        InOrder inOrder = Mockito.inOrder(tracer, chain);
        inOrder.verify(tracer).getDynatraceStringTag();
        inOrder.verify(tracer).start();
        inOrder.verify(chain).proceed();
        inOrder.verify(tracer).error(throwable);
        inOrder.verify(tracer).end();

    }
}
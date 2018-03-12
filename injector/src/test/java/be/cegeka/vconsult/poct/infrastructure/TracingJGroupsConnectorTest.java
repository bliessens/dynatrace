package be.cegeka.vconsult.poct.infrastructure;

import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.OutgoingRemoteCallTracer;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.distributed.Member;
import org.axonframework.commandhandling.distributed.SimpleMember;
import org.axonframework.jgroups.commandhandling.JGroupsConnector;
import org.junit.Test;
import org.mockito.InOrder;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TracingJGroupsConnectorTest {

    private final OneAgentSDK agentMock = mock(OneAgentSDK.class);
    private final OutgoingRemoteCallTracer tracerMock = mock(OutgoingRemoteCallTracer.class);
    private final JGroupsConnector delegateConnector = mock(JGroupsConnector.class);
    private final Member remoteMember = new SimpleMember<>("abc", new Object(), false, objectSimpleMember -> {
    });

    private final TracingJGroupsConnector connector = new TracingJGroupsConnector(agentMock, delegateConnector);

    @Test
    public void testEndTracerWhenConnectorThrowsException() throws Exception {
        final Exception expected = new RuntimeException();

        doThrow(expected).when(delegateConnector).send(any(), any(), any());
        when(agentMock.traceOutgoingRemoteCall(any(), any(), any(), any(), any())).thenReturn(tracerMock);

        try {
            connector.send(remoteMember, new GenericCommandMessage<>(new Object()));
            fail("");
        } catch (Exception e) {
            assertNotNull(e);
        }

        InOrder ordered = inOrder(tracerMock, delegateConnector);
        ordered.verify(tracerMock).setProtocolName(anyString());
        ordered.verify(tracerMock).start();
        ordered.verify(tracerMock).getDynatraceStringTag();
        ordered.verify(delegateConnector).send(any(), any(), any());
        ordered.verify(tracerMock).error(expected);
        ordered.verify(tracerMock).end();
    }
}
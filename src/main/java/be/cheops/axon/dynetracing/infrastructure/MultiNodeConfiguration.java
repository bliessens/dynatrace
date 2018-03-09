package be.cheops.axon.dynetracing.infrastructure;

import be.cheops.axon.dynetracing.trace.TokenExtractionInterceptorFactory;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.thoughtworks.xstream.XStream;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.commandhandling.distributed.CommandBusConnector;
import org.axonframework.commandhandling.distributed.DistributedCommandBus;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.jgroups.commandhandling.JGroupsConnector;
import org.axonframework.monitoring.NoOpMessageMonitor;
import org.axonframework.serialization.xml.CompactDriver;
import org.axonframework.serialization.xml.XStreamSerializer;
import org.axonframework.spring.commandhandling.distributed.jgroups.JGroupsConnectorFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"!single", "cluster"})
class MultiNodeConfiguration {

    static class CommandSerializerFactory {
        public XStream newXStream() {
            return new XStream(new CompactDriver());
        }
    }

    @Bean("localCommandBus")
    public SimpleCommandBus commandBus(TransactionManager axonTransactionManager, TokenExtractionInterceptorFactory interceptorFactory) {
        final SimpleCommandBus bus = new SimpleCommandBus(axonTransactionManager, NoOpMessageMonitor.INSTANCE);
        bus.registerHandlerInterceptor(interceptorFactory.newExtractingInterceptor());
        return bus;
    }

    @Bean("distributedCommandBus")
    @Primary
    public DistributedCommandBus distributedCommandBus(JGroupsConnector router, CommandBusConnector connector) {
        return new DistributedCommandBus(router, connector);
    }

    @Bean
    @Primary
    public CommandBusConnector interceptingConnector(JGroupsConnector connector, OneAgentSDK oneAgent) {
        return new TracingJGroupsConnector(oneAgent, connector);
    }

    @Bean
    public JGroupsConnectorFactoryBean jGroupsConnectorFactoryBean(@Qualifier("localCommandBus") CommandBus localCommandBus,
                                                                   @Value("${distributed.clustername}") String clusterName) {

        JGroupsConnectorFactoryBean jGroupsConnectorFactoryBean = new JGroupsConnectorFactoryBean();
        jGroupsConnectorFactoryBean.setConfiguration("jgroups-config.xml");
        jGroupsConnectorFactoryBean.setLocalSegment(localCommandBus);
        jGroupsConnectorFactoryBean.setClusterName(clusterName);
        jGroupsConnectorFactoryBean.setSerializer(new XStreamSerializer(new CommandSerializerFactory().newXStream()));
        return jGroupsConnectorFactoryBean;
    }


}


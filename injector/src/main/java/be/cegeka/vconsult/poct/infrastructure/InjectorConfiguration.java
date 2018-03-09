package be.cegeka.vconsult.poct.infrastructure;

import be.cegeka.vconsult.poct.infrastructure.trace.TokenExtractionInterceptorFactory;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.commandhandling.distributed.CommandBusConnector;
import org.axonframework.commandhandling.distributed.CommandRouter;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
@Profile("tracing")
public class InjectorConfiguration {

    @Bean("distributedCommandBus")
    @Primary
    public DistributedCommandBus distributedCommandBus(CommandRouter router, CommandBusConnector connector) {
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

    @Bean("localCommandBus")
    public SimpleCommandBus commandBus(TransactionManager axonTransactionManager, TokenExtractionInterceptorFactory interceptorFactory) {
        final SimpleCommandBus bus = new SimpleCommandBus(axonTransactionManager, NoOpMessageMonitor.INSTANCE);
        bus.registerHandlerInterceptor(interceptorFactory.newExtractingInterceptor());
        return bus;
    }

    static class CommandSerializerFactory {
        public XStream newXStream() {
            final XStream xStream = new XStream(new CompactDriver());
            xStream.registerConverter(new AbstractSingleValueConverter() {
                private final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE;

                @Override
                public boolean canConvert(Class type) {
                    return type.equals(LocalDate.class);
                }

                @Override
                public LocalDate fromString(String str) {
                    return LocalDate.parse(str, FORMATTER);
                }

                @Override
                public String toString(Object obj) {
                    final LocalDate localDate = (LocalDate) obj;

                    return localDate.format(FORMATTER);
                }
            });
            xStream.registerConverter(new AbstractSingleValueConverter() {
                private final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

                @Override
                public boolean canConvert(Class type) {
                    return type.equals(LocalDateTime.class);
                }

                @Override
                public LocalDateTime fromString(String str) {
                    return LocalDateTime.parse(str, FORMATTER);
                }

                @Override
                public String toString(Object obj) {
                    final LocalDateTime localDateTime = (LocalDateTime) obj;

                    return localDateTime.format(FORMATTER);
                }
            });
            return xStream;
        }
    }
}

package be.cheops.axon.dynetracing.infrastructure;

//@Configuration
//@Profile({"!single", "cluster"})
class MultiNodeConfiguration {

//    static class CommandSerializerFactory {
//        public XStream newXStream() {
//            return new XStream(new CompactDriver());
//        }
//    }
//
//    @Bean("localCommandBus")
//    public SimpleCommandBus commandBus(TransactionManager axonTransactionManager, TokenExtractionInterceptorFactory interceptorFactory) {
//        final SimpleCommandBus bus = new SimpleCommandBus(axonTransactionManager, NoOpMessageMonitor.INSTANCE);
//        bus.registerHandlerInterceptor(interceptorFactory.newExtractingInterceptor());
//        return bus;
//    }
//
//    @Bean("distributedCommandBus")
//    @Primary
//    public DistributedCommandBus distributedCommandBus(JGroupsConnector router, CommandBusConnector connector) {
//        return new DistributedCommandBus(router, connector);
//    }
//
//    @Bean
//    @Primary
//    public CommandBusConnector interceptingConnector(JGroupsConnector connector, OneAgentSDK oneAgent) {
//        return new TracingJGroupsConnector(oneAgent, connector);
//    }
//
//    @Bean
//    public JGroupsConnectorFactoryBean jGroupsConnectorFactoryBean(@Qualifier("localCommandBus") CommandBus localCommandBus,
//                                                                   @Value("${distributed.clustername}") String clusterName) {
//
//        JGroupsConnectorFactoryBean jGroupsConnectorFactoryBean = new JGroupsConnectorFactoryBean();
//        jGroupsConnectorFactoryBean.setConfiguration("jgroups-config.xml");
//        jGroupsConnectorFactoryBean.setLocalSegment(localCommandBus);
//        jGroupsConnectorFactoryBean.setClusterName(clusterName);
//        jGroupsConnectorFactoryBean.setSerializer(new XStreamSerializer(new CommandSerializerFactory().newXStream()));
//        return jGroupsConnectorFactoryBean;
//    }
//
//
}


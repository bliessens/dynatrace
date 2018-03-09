package be.cheops.axon.dynetracing;

import be.cheops.axon.dynetracing.api.CreateCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.jgroups.commandhandling.JGroupsConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EntityScan(basePackages = {
        "org.axonframework.eventsourcing.eventstore.jpa",
        "org.axonframework.eventhandling.saga.repository.jpa"
})
public class DynetracingApplication implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(DynetracingApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DynetracingApplication.class, args);
    }

    @Autowired
    private CommandGateway gateway;
    @Autowired
    private JGroupsConnector connector;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Random r = new Random(Long.parseLong(System.getProperty("PID")));

        try {
            if (connector.awaitJoined()) {
                LOG.info("Joined cluster");
                for (int i = 0; i < 5; i++) {
                    gateway.send(new CreateCommand("aggId-" + Math.abs(r.nextInt())));
                    TimeUnit.SECONDS.sleep(3);

                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

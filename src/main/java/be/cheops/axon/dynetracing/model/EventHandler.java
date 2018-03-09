package be.cheops.axon.dynetracing.model;

import be.cheops.axon.dynetracing.api.CreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(EventHandler.class);

    @org.axonframework.eventhandling.EventHandler
    public void onEvent(CreatedEvent event) {
        LOG.debug("Received event for " + event.getId());
    }
}

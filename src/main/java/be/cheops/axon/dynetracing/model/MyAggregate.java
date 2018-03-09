package be.cheops.axon.dynetracing.model;

import be.cheops.axon.dynetracing.api.CreateCommand;
import be.cheops.axon.dynetracing.api.CreatedEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.messaging.annotation.MetaDataValue;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static be.cegeka.vconsult.poct.infrastructure.trace.TokenExtractionInterceptorFactory.TRACE_ID;
import static org.axonframework.commandhandling.model.AggregateLifecycle.apply;

@Aggregate
class MyAggregate {

    private final static Logger LOG = LoggerFactory.getLogger(MyAggregate.class);

    @AggregateIdentifier
    private String id;

    @CommandHandler
    public MyAggregate(CreateCommand command, @MetaDataValue(value = TRACE_ID, required = false) String traceId) {
        if (traceId != null) {
            LOG.error("Command still has traceId {}", traceId);
        }
        apply(new CreatedEvent(command.getId()));
    }

    @EventSourcingHandler
    public void onEvent(CreatedEvent event) {
        this.id = event.getId();
    }
}

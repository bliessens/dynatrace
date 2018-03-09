package be.cheops.axon.dynetracing.api;

import org.axonframework.commandhandling.TargetAggregateIdentifier;

public class CreateCommand {

    @TargetAggregateIdentifier
    private final String id;

    public CreateCommand(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}

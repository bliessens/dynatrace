package be.cheops.axon.dynetracing.api;

public class CreatedEvent {

    private final String id;

    public CreatedEvent(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}

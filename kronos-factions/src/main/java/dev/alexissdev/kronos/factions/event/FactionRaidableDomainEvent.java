package dev.alexissdev.kronos.factions.event;

public class FactionRaidableDomainEvent {

    private final String factionId;
    private final String factionName;

    public FactionRaidableDomainEvent(String factionId, String factionName) {
        this.factionId   = factionId;
        this.factionName = factionName;
    }

    public String getFactionId()   { return factionId; }
    public String getFactionName() { return factionName; }
}

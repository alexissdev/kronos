package dev.alexissdev.kronos.factions.event;

import java.util.UUID;

public final class FactionCreatedDomainEvent {

    private final String factionId;
    private final String factionName;
    private final UUID leaderId;

    public FactionCreatedDomainEvent(String factionId, String factionName, UUID leaderId) {
        this.factionId = factionId;
        this.factionName = factionName;
        this.leaderId = leaderId;
    }

    public String getFactionId() { return factionId; }

    public String getFactionName() { return factionName; }

    public UUID getLeaderId() { return leaderId; }
}

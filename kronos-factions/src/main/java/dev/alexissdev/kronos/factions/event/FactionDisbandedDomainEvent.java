package dev.alexissdev.kronos.factions.event;

import java.util.UUID;

public final class FactionDisbandedDomainEvent {

    private final String factionId;
    private final String factionName;
    private final UUID actorUuid;

    public FactionDisbandedDomainEvent(String factionId, String factionName, UUID actorUuid) {
        this.factionId = factionId;
        this.factionName = factionName;
        this.actorUuid = actorUuid;
    }

    public String getFactionId() { return factionId; }

    public String getFactionName() { return factionName; }

    public UUID getActorUuid() { return actorUuid; }
}

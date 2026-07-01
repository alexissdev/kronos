package dev.alexissdev.kronos.factions.event;

import java.util.UUID;

public final class PlayerLeftFactionDomainEvent {

    private final UUID playerUuid;
    private final String factionId;
    private final boolean wasKicked;

    public PlayerLeftFactionDomainEvent(UUID playerUuid, String factionId, boolean wasKicked) {
        this.playerUuid = playerUuid;
        this.factionId = factionId;
        this.wasKicked = wasKicked;
    }

    public UUID getPlayerUuid() { return playerUuid; }

    public String getFactionId() { return factionId; }

    public boolean wasKicked() { return wasKicked; }
}

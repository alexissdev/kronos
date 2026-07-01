package dev.alexissdev.kronos.core.event;

import java.util.UUID;

public final class PlayerJoinedFactionDomainEvent {

    private final UUID playerUuid;
    private final String factionId;

    public PlayerJoinedFactionDomainEvent(UUID playerUuid, String factionId) {
        this.playerUuid = playerUuid;
        this.factionId = factionId;
    }

    public UUID getPlayerUuid() { return playerUuid; }

    public String getFactionId() { return factionId; }
}

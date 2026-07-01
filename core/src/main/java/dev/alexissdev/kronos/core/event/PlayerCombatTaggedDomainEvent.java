package dev.alexissdev.kronos.core.event;

import java.util.UUID;

public final class PlayerCombatTaggedDomainEvent {

    private final UUID taggedUuid;
    private final UUID taggerUuid;
    private final long durationMillis;

    public PlayerCombatTaggedDomainEvent(UUID taggedUuid, UUID taggerUuid, long durationMillis) {
        this.taggedUuid = taggedUuid;
        this.taggerUuid = taggerUuid;
        this.durationMillis = durationMillis;
    }

    public UUID getTaggedUuid() { return taggedUuid; }

    public UUID getTaggerUuid() { return taggerUuid; }

    public long getDurationMillis() { return durationMillis; }
}

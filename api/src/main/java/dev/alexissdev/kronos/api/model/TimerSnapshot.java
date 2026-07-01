package dev.alexissdev.kronos.api.model;

import dev.alexissdev.kronos.core.domain.TimerType;

import java.time.Instant;
import java.util.UUID;

/** Immutable read-only view of a Timer for external plugin use. */
public final class TimerSnapshot {

    private final UUID playerUuid;
    private final TimerType type;
    private final Instant expiresAt;
    private final long remainingMillis;

    public TimerSnapshot(UUID playerUuid, TimerType type, Instant expiresAt, long remainingMillis) {
        this.playerUuid = playerUuid;
        this.type = type;
        this.expiresAt = expiresAt;
        this.remainingMillis = remainingMillis;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public TimerType getType() { return type; }
    public Instant getExpiresAt() { return expiresAt; }
    public long getRemainingMillis() { return remainingMillis; }
}

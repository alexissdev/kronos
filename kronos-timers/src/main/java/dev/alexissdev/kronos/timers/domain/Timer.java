package dev.alexissdev.kronos.timers.domain;

import java.time.Instant;
import java.util.UUID;

public final class Timer {

    private final UUID playerUuid;
    private final TimerType type;
    private final Instant expiresAt;

    public Timer(UUID playerUuid, TimerType type, Instant expiresAt) {
        this.playerUuid = playerUuid;
        this.type = type;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public long getRemainingMillis() {
        long remaining = expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
        return Math.max(0L, remaining);
    }

    public UUID getPlayerUuid() { return playerUuid; }

    public TimerType getType() { return type; }

    public Instant getExpiresAt() { return expiresAt; }
}

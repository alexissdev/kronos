package dev.alexissdev.kronos.timers.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity representing an active timer associated with an HCF player.
 *
 * <p>A timer is a temporary restriction applied to a player for a fixed period.
 * It can be of various types ({@link TimerType}): PvP protection on first join,
 * combat tag when entering combat, deathban after running out of lives, enderpearl
 * or golden apple cooldowns, logout timer, and so on.</p>
 *
 * <p>The timer stores the exact expiration moment as an {@link Instant} and exposes
 * methods to check whether it has already expired and how much time remains.
 * Primary persistence is handled by Redis (using native TTL), with MongoDB acting
 * as a durable backup layer.</p>
 */
public final class Timer {

    private final UUID playerUuid;
    private final TimerType type;
    private final Instant expiresAt;

    /**
     * Creates a new timer for the specified player and type, with the given expiration time.
     *
     * @param playerUuid UUID of the player this timer belongs to
     * @param type       timer type that determines the behaviour and restrictions imposed
     * @param expiresAt  exact instant at which the timer will expire and become inactive
     */
    public Timer(UUID playerUuid, TimerType type, Instant expiresAt) {
        this.playerUuid = playerUuid;
        this.type = type;
        this.expiresAt = expiresAt;
    }

    /**
     * Checks whether this timer has already expired by comparing the current instant
     * against {@link #expiresAt}.
     *
     * @return {@code true} if the current moment is after the expiration instant,
     *         {@code false} if the timer is still active
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Calculates the number of milliseconds remaining until this timer expires.
     * If the timer has already expired, returns zero instead of a negative value.
     *
     * @return milliseconds remaining until expiration, minimum {@code 0}
     */
    public long getRemainingMillis() {
        long remaining = expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
        return Math.max(0L, remaining);
    }

    /**
     * Returns the UUID of the player this timer belongs to.
     *
     * @return UUID of the player that owns this timer
     */
    public UUID getPlayerUuid() { return playerUuid; }

    /**
     * Returns the type of this timer, which determines the restrictions it imposes on the player.
     *
     * @return the timer type
     */
    public TimerType getType() { return type; }

    /**
     * Returns the exact instant at which this timer will expire.
     *
     * @return UTC instant of expiration
     */
    public Instant getExpiresAt() { return expiresAt; }
}

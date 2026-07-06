package dev.alexissdev.kronos.timers.service;

import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Domain service interface for managing the full lifecycle of player timers in the HCF system.
 *
 * <p>Defines the fundamental operations to start, cancel, and query player timers.
 * Each timer represents a temporary restriction (combat tag, PvP protection, cooldowns, etc.)
 * that limits the actions available to a player for as long as it remains active.</p>
 *
 * <p>The generic type parameter {@code T} represents the identifier of the timer's subject
 * (typically {@link java.util.UUID}). The primary implementation is
 * {@code TimerApplicationService}.</p>
 *
 * @param <T> type of the timer subject's identifier
 */
public interface TimerService<T> {

    /**
     * Starts a new timer for the player with the specified type and duration.
     *
     * <p>Persists the timer in Redis with a TTL, updates the in-memory cache, and posts
     * a {@code PlayerTimerStartedDomainEvent} on the {@code EventBus} to notify
     * interested listeners.</p>
     *
     * @param playerUuid     UUID of the player for whom the timer is being started
     * @param type           timer type that determines which restrictions to apply
     * @param durationMillis duration of the timer in milliseconds from the moment it starts
     * @return future that resolves once the timer has been persisted in Redis
     */
    CompletableFuture<Void> startTimer(UUID playerUuid, TimerType type, long durationMillis);

    /**
     * Cancels and removes the active timer for the specified player and type.
     *
     * <p>Deletes the timer from Redis, marks it as inactive in the in-memory cache, and
     * posts a {@code PlayerTimerExpiredDomainEvent} if the timer was active in the cache.</p>
     *
     * @param playerUuid UUID of the player whose timer should be cancelled
     * @param type       type of the timer to cancel
     * @return future that resolves once the timer has been deleted from Redis
     */
    CompletableFuture<Void> cancelTimer(UUID playerUuid, TimerType type);

    /**
     * Asynchronously checks whether a player currently has an active timer of the specified type
     * by querying the real state in Redis.
     *
     * <p>Updates the in-memory cache with the query result. If the timer was present in the
     * cache but has since expired in Redis, a {@code PlayerTimerExpiredDomainEvent} is posted.</p>
     *
     * @param playerUuid UUID of the player to check
     * @param type       type of the timer whose activity should be verified
     * @return future that resolves with {@code true} if the timer is active and has not expired,
     *         {@code false} if the timer does not exist or has already expired
     */
    CompletableFuture<Boolean> hasActiveTimer(UUID playerUuid, TimerType type);

    /**
     * Asynchronously retrieves the remaining time in milliseconds for a player's active timer.
     *
     * @param playerUuid UUID of the player whose remaining time should be queried
     * @param type       type of the timer to query
     * @return future that resolves with an {@link OptionalLong} containing the remaining
     *         milliseconds if the timer is active, or empty if the timer does not exist or has expired
     */
    CompletableFuture<OptionalLong> getRemainingMillis(UUID playerUuid, TimerType type);
}

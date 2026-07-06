package dev.alexissdev.kronos.timers.repository;

import dev.alexissdev.kronos.timers.domain.Timer;
import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous access contract for active player timers, backed by Redis with native TTL.
 *
 * <p>Defines read, write, and delete operations on {@link Timer} entities.
 * The default implementation ({@code RedisTimerRepository}) uses Redis as the primary store,
 * relying on the native TTL so timers expire automatically without any scheduled cleanup.
 * Each timer is stored under the key {@code timer:{uuid}:{timerType}}.</p>
 *
 * <p>All operations are non-blocking and return a {@link CompletableFuture} to integrate
 * with the plugin's asynchronous execution model.</p>
 */
public interface TimerRepository {

    /**
     * Looks up the active timer for a player and the specified timer type.
     * If the timer exists but has already passed its expiration instant, an empty result is returned.
     *
     * @param playerUuid UUID of the player whose timer should be queried
     * @param type       type of the timer to look up
     * @return future that resolves with an {@link Optional} containing the active timer
     *         if it exists and has not expired, or empty if absent or already expired
     */
    CompletableFuture<Optional<Timer>> findTimer(UUID playerUuid, TimerType type);

    /**
     * Retrieves all active timers for a player by querying every known timer type.
     * Only timers that have not yet expired are included in the result.
     *
     * @param playerUuid UUID of the player whose active timers should be retrieved
     * @return future that resolves with the list of active timers for the player;
     *         the list will be empty if the player has no active timers
     */
    CompletableFuture<List<Timer>> findAllTimers(UUID playerUuid);

    /**
     * Saves a timer in Redis with its remaining lifetime as the TTL.
     * The TTL is computed in seconds from the timer's remaining milliseconds.
     * Redis will automatically delete the key once the TTL reaches zero.
     *
     * @param timer {@link Timer} entity holding the information to persist
     * @return future that resolves once the timer has been stored in Redis
     */
    CompletableFuture<Void> saveTimer(Timer timer);

    /**
     * Deletes a player's timer for the specified type before its TTL expires.
     * Called when cancelling a timer manually or restarting it with a new duration.
     *
     * @param playerUuid UUID of the player whose timer should be deleted
     * @param type       type of the timer to remove from Redis
     * @return future that resolves once the timer has been deleted
     */
    CompletableFuture<Void> deleteTimer(UUID playerUuid, TimerType type);

    /**
     * Quickly checks whether a Redis key exists for the specified player's timer.
     * Unlike {@link #findTimer}, this does not deserialize the full timer — it simply
     * checks for the key's presence using the Redis {@code EXISTS} command.
     *
     * @param playerUuid UUID of the player to check
     * @param type       type of the timer whose existence should be verified
     * @return future that resolves with {@code true} if the key exists in Redis,
     *         {@code false} if the key does not exist or its TTL has expired
     */
    CompletableFuture<Boolean> hasTimer(UUID playerUuid, TimerType type);
}

package dev.alexissdev.kronos.timers.persistence;

import dev.alexissdev.kronos.common.database.RedisConnectionFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.timers.domain.Timer;
import dev.alexissdev.kronos.timers.domain.TimerType;
import dev.alexissdev.kronos.timers.repository.TimerRepository;
import io.lettuce.core.api.async.RedisAsyncCommands;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * {@link TimerRepository} implementation that persists active timers in Redis,
 * leveraging Redis's native TTL as the automatic expiration mechanism.
 *
 * <p>Each timer is stored under the key {@code timer:{uuid}:{timerType}}, with the
 * expiration timestamp in milliseconds (epoch) as its value. The Redis TTL is derived
 * from the timer's remaining milliseconds, so Redis automatically deletes the key when
 * the timer expires — no scheduled cleanup tasks are needed.</p>
 *
 * <p>All operations are non-blocking thanks to Lettuce's asynchronous API.</p>
 */
@Singleton
public class RedisTimerRepository implements TimerRepository {

    private static final String KEY_PREFIX = "timer:";

    private final RedisAsyncCommands<String, String> redis;

    /**
     * Creates the repository by obtaining the asynchronous Redis commands
     * through the connection factory.
     *
     * @param factory factory that provides the asynchronous Redis connection
     */
    @Inject
    public RedisTimerRepository(RedisConnectionFactory factory) {
        this.redis = factory.async();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Fetches the timer key's value from Redis, which holds the expiration timestamp
     * in milliseconds. If the key does not exist (TTL expired or never created), an empty
     * {@link Optional} is returned. If the key exists but the timestamp is already in the
     * past, an empty {@link Optional} is returned to avoid serving stale data.</p>
     */
    @Override
    public CompletableFuture<Optional<Timer>> findTimer(UUID playerUuid, TimerType type) {
        String key = buildKey(playerUuid, type);
        return redis.get(key).toCompletableFuture().thenApply(value -> {
            if (value == null) return Optional.empty();
            long expiresAt = Long.parseLong(value);
            Timer timer = new Timer(playerUuid, type, Instant.ofEpochMilli(expiresAt));
            return timer.isExpired() ? Optional.empty() : Optional.of(timer);
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Queries all timer types defined in {@link TimerType} in parallel and collects
     * the results into a list, filtering out timers that do not exist or have expired.</p>
     */
    @Override
    public CompletableFuture<List<Timer>> findAllTimers(UUID playerUuid) {
        List<CompletableFuture<Optional<Timer>>> futures = new ArrayList<>();
        for (TimerType type : TimerType.values()) {
            futures.add(findTimer(playerUuid, type));
        }

        CompletableFuture<Void> all = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        return all.thenApply(v -> {
            List<Timer> result = new ArrayList<>();
            for (CompletableFuture<Optional<Timer>> future : futures) {
                future.join().ifPresent(result::add);
            }
            return result;
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stores the timer's expiration timestamp as a string value in Redis with a TTL
     * computed in seconds. If the remaining time is less than one second, a minimum TTL
     * of 1 second is used to avoid an invalid zero or negative TTL.</p>
     */
    @Override
    public CompletableFuture<Void> saveTimer(Timer timer) {
        String key = buildKey(timer.getPlayerUuid(), timer.getType());
        long ttlSeconds = Math.max(1L, timer.getRemainingMillis() / 1000L);
        return redis.setex(key, ttlSeconds, String.valueOf(timer.getExpiresAt().toEpochMilli()))
                .toCompletableFuture()
                .thenApply(r -> null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Deletes the timer key from Redis immediately, regardless of any remaining TTL.</p>
     */
    @Override
    public CompletableFuture<Void> deleteTimer(UUID playerUuid, TimerType type) {
        return redis.del(buildKey(playerUuid, type))
                .toCompletableFuture()
                .thenApply(r -> null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses the Redis {@code EXISTS} command to check for the key's existence without
     * reading its value, which is more efficient when only presence needs to be verified.</p>
     */
    @Override
    public CompletableFuture<Boolean> hasTimer(UUID playerUuid, TimerType type) {
        return redis.exists(buildKey(playerUuid, type))
                .toCompletableFuture()
                .thenApply(count -> count > 0);
    }

    /**
     * Construye la clave de Redis para el timer de un jugador según su UUID y tipo.
     * El formato es {@code timer:{uuid}:{timerType}}.
     *
     * @param playerUuid UUID del jugador propietario del timer
     * @param type       tipo del timer
     * @return clave de Redis en el formato esperado
     */
    private String buildKey(UUID playerUuid, TimerType type) {
        return KEY_PREFIX + playerUuid.toString() + ":" + type.name();
    }
}

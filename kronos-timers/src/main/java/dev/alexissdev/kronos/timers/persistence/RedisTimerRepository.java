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

@Singleton
public class RedisTimerRepository implements TimerRepository {

    private static final String KEY_PREFIX = "timer:";

    private final RedisAsyncCommands<String, String> redis;

    @Inject
    public RedisTimerRepository(RedisConnectionFactory factory) {
        this.redis = factory.async();
    }

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

    @Override
    public CompletableFuture<Void> saveTimer(Timer timer) {
        String key = buildKey(timer.getPlayerUuid(), timer.getType());
        long ttlSeconds = Math.max(1L, timer.getRemainingMillis() / 1000L);
        return redis.setex(key, ttlSeconds, String.valueOf(timer.getExpiresAt().toEpochMilli()))
                .toCompletableFuture()
                .thenApply(r -> null);
    }

    @Override
    public CompletableFuture<Void> deleteTimer(UUID playerUuid, TimerType type) {
        return redis.del(buildKey(playerUuid, type))
                .toCompletableFuture()
                .thenApply(r -> null);
    }

    @Override
    public CompletableFuture<Boolean> hasTimer(UUID playerUuid, TimerType type) {
        return redis.exists(buildKey(playerUuid, type))
                .toCompletableFuture()
                .thenApply(count -> count > 0);
    }

    private String buildKey(UUID playerUuid, TimerType type) {
        return KEY_PREFIX + playerUuid.toString() + ":" + type.name();
    }
}

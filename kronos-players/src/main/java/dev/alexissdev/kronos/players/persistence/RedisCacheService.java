package dev.alexissdev.kronos.players.persistence;

import dev.alexissdev.kronos.common.database.RedisConnectionFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lettuce.core.api.async.RedisAsyncCommands;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class RedisCacheService {

    private final RedisAsyncCommands<String, String> redis;

    @Inject
    public RedisCacheService(RedisConnectionFactory factory) {
        this.redis = factory.async();
    }

    public CompletableFuture<Void> set(String key, String value, long ttlSeconds) {
        return redis.setex(key, ttlSeconds, value)
                .toCompletableFuture()
                .thenApply(r -> null);
    }

    public CompletableFuture<Optional<String>> get(String key) {
        return redis.get(key)
                .toCompletableFuture()
                .thenApply(Optional::ofNullable);
    }

    public CompletableFuture<Void> delete(String key) {
        return redis.del(key)
                .toCompletableFuture()
                .thenApply(r -> null);
    }

    public CompletableFuture<Boolean> exists(String key) {
        return redis.exists(key)
                .toCompletableFuture()
                .thenApply(count -> count > 0);
    }

    public CompletableFuture<Void> addToSet(String key, String... members) {
        return redis.sadd(key, members)
                .toCompletableFuture()
                .thenApply(r -> null);
    }

    public CompletableFuture<Boolean> isMemberOfSet(String key, String member) {
        return redis.sismember(key, member)
                .toCompletableFuture();
    }

    public CompletableFuture<Void> removeFromSet(String key, String member) {
        return redis.srem(key, member)
                .toCompletableFuture()
                .thenApply(r -> null);
    }
}

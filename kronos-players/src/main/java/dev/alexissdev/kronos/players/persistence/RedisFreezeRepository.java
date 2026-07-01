package dev.alexissdev.kronos.players.persistence;

import dev.alexissdev.kronos.common.database.RedisConnectionFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.players.repository.FreezeRepository;
import io.lettuce.core.api.async.RedisAsyncCommands;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class RedisFreezeRepository implements FreezeRepository {

    private static final String PREFIX = "freeze:";
    private static final long TTL_SECONDS = 86_400L;

    private final RedisAsyncCommands<String, String> redis;

    @Inject
    public RedisFreezeRepository(RedisConnectionFactory factory) {
        this.redis = factory.async();
    }

    @Override
    public CompletableFuture<Void> freeze(UUID staffUuid, UUID targetUuid) {
        return redis.setex(PREFIX + targetUuid, TTL_SECONDS, staffUuid.toString())
                .toCompletableFuture()
                .thenApply(r -> null);
    }

    @Override
    public CompletableFuture<Void> unfreeze(UUID targetUuid) {
        return redis.del(PREFIX + targetUuid)
                .toCompletableFuture()
                .thenApply(r -> null);
    }

    @Override
    public CompletableFuture<Boolean> isFrozen(UUID targetUuid) {
        return redis.exists(PREFIX + targetUuid)
                .toCompletableFuture()
                .thenApply(count -> count > 0);
    }
}

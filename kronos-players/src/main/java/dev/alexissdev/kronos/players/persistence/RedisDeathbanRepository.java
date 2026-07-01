package dev.alexissdev.kronos.players.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.database.RedisConnectionFactory;
import dev.alexissdev.kronos.players.repository.DeathbanRepository;
import io.lettuce.core.api.async.RedisAsyncCommands;

import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class RedisDeathbanRepository implements DeathbanRepository {

    private static final String KEY_PREFIX = "deathban:";

    private final RedisAsyncCommands<String, String> redis;

    @Inject
    public RedisDeathbanRepository(RedisConnectionFactory factory) {
        this.redis = factory.async();
    }

    @Override
    public CompletableFuture<Void> setDeathban(UUID uuid, long durationSeconds) {
        return redis.setex(KEY_PREFIX + uuid, durationSeconds, "1")
                .toCompletableFuture()
                .thenApply(r -> null);
    }

    @Override
    public CompletableFuture<OptionalLong> getRemainingSeconds(UUID uuid) {
        return redis.ttl(KEY_PREFIX + uuid)
                .toCompletableFuture()
                .thenApply(ttl -> ttl > 0 ? OptionalLong.of(ttl) : OptionalLong.empty());
    }
}

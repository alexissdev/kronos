package dev.alexissdev.kronos.infrastructure.redis;

import com.google.inject.Inject;
import com.google.inject.Named;
import com.google.inject.Singleton;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

@Singleton
public class RedisConnectionFactory {

    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;

    @Inject
    public RedisConnectionFactory(
            @Named("redis.host") String host,
            @Named("redis.port") int port,
            @Named("redis.password") String password
    ) {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(host)
                .withPort(port);

        if (password != null && !password.isEmpty()) {
            builder.withPassword(password.toCharArray());
        }

        this.redisClient = RedisClient.create(builder.build());
        this.connection = redisClient.connect();
    }

    public RedisAsyncCommands<String, String> async() {
        return connection.async();
    }

    public void close() {
        connection.close();
        redisClient.shutdown();
    }
}

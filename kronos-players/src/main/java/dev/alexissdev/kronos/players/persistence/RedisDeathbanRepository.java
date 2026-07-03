package dev.alexissdev.kronos.players.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.database.RedisConnectionFactory;
import dev.alexissdev.kronos.players.repository.DeathbanRepository;
import io.lettuce.core.api.async.RedisAsyncCommands;

import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación de {@link DeathbanRepository} que gestiona los baneos por muerte (Deathban)
 * usando Redis como almacenamiento con expiración automática por TTL.
 *
 * <p>Cada Deathban se representa como una clave con prefijo {@code deathban:} seguido del UUID
 * del jugador. El valor de la clave es {@code "1"} y el TTL de Redis actúa como el contador
 * regresivo del baneo: cuando el TTL expira, el jugador queda libre automáticamente sin
 * necesidad de ninguna tarea programada adicional.</p>
 *
 * <p>Se consulta el TTL restante mediante el comando Redis {@code TTL} para saber cuántos
 * segundos le quedan al baneo de un jugador.</p>
 */
@Singleton
public class RedisDeathbanRepository implements DeathbanRepository {

    private static final String KEY_PREFIX = "deathban:";

    private final RedisAsyncCommands<String, String> redis;

    /**
     * Crea el repositorio obteniendo los comandos asíncronos de Redis
     * a través de la fábrica de conexiones.
     *
     * @param factory fábrica que provee la conexión asíncrona a Redis
     */
    @Inject
    public RedisDeathbanRepository(RedisConnectionFactory factory) {
        this.redis = factory.async();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Almacena la clave {@code deathban:{uuid}} con TTL igual a {@code durationSeconds}.
     * Redis elimina la clave automáticamente al vencer el TTL, finalizando así el baneo.</p>
     */
    @Override
    public CompletableFuture<Void> setDeathban(UUID uuid, long durationSeconds) {
        return redis.setex(KEY_PREFIX + uuid, durationSeconds, "1")
                .toCompletableFuture()
                .thenApply(r -> null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Utiliza el comando Redis {@code TTL} para obtener los segundos restantes del baneo.
     * Un TTL de {@code -2} significa que la clave no existe (baneo expirado o no aplicado)
     * y se devuelve un {@link OptionalLong} vacío.</p>
     */
    @Override
    public CompletableFuture<OptionalLong> getRemainingSeconds(UUID uuid) {
        return redis.ttl(KEY_PREFIX + uuid)
                .toCompletableFuture()
                .thenApply(ttl -> ttl > 0 ? OptionalLong.of(ttl) : OptionalLong.empty());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Elimina la clave {@code deathban:{uuid}} de Redis de forma inmediata,
     * liberando al jugador del baneo sin esperar a que el TTL expire.</p>
     */
    @Override
    public CompletableFuture<Void> removeDeathban(UUID uuid) {
        return redis.del(KEY_PREFIX + uuid)
                .toCompletableFuture()
                .thenApply(r -> null);
    }
}

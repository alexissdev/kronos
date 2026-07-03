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
 * Implementación de {@link TimerRepository} que persiste los timers activos en Redis
 * usando el TTL nativo de Redis como mecanismo de expiración automática.
 *
 * <p>Cada timer se almacena con la clave {@code timer:{uuid}:{timerType}} y como valor
 * se guarda el timestamp de expiración en milisegundos (epoch). El TTL de la clave en Redis
 * se calcula a partir de los milisegundos restantes del timer, de modo que Redis elimina
 * automáticamente la clave cuando el timer expira, sin necesidad de tareas programadas.</p>
 *
 * <p>Todas las operaciones son no bloqueantes gracias a la API asíncrona de Lettuce.</p>
 */
@Singleton
public class RedisTimerRepository implements TimerRepository {

    private static final String KEY_PREFIX = "timer:";

    private final RedisAsyncCommands<String, String> redis;

    /**
     * Crea el repositorio obteniendo los comandos asíncronos de Redis
     * a través de la fábrica de conexiones.
     *
     * @param factory fábrica que provee la conexión asíncrona a Redis
     */
    @Inject
    public RedisTimerRepository(RedisConnectionFactory factory) {
        this.redis = factory.async();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Obtiene el valor de la clave del timer en Redis, que contiene el timestamp
     * de expiración en milisegundos. Si la clave no existe (TTL expirado o nunca creada)
     * devuelve un {@link Optional} vacío. Si existe pero el timestamp ya pasó,
     * también devuelve vacío para evitar datos obsoletos.</p>
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
     * <p>Consulta todos los tipos de timer definidos en {@link TimerType} en paralelo
     * y reúne los resultados en una lista, filtrando los timers inexistentes o expirados.</p>
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
     * <p>Almacena el timestamp de expiración del timer como valor de cadena en Redis
     * con un TTL calculado en segundos. Si el tiempo restante es inferior a un segundo,
     * se usa un TTL mínimo de 1 segundo para evitar TTLs no válidos.</p>
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
     * <p>Elimina la clave del timer de Redis de forma inmediata, independientemente
     * del TTL restante.</p>
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
     * <p>Usa el comando {@code EXISTS} de Redis para verificar la existencia de la clave
     * sin leer su valor, lo cual es más eficiente cuando solo se necesita saber si existe.</p>
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

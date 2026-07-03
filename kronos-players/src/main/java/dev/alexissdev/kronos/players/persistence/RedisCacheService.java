package dev.alexissdev.kronos.players.persistence;

import dev.alexissdev.kronos.common.database.RedisConnectionFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lettuce.core.api.async.RedisAsyncCommands;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio genérico de caché sobre Redis que encapsula las operaciones básicas
 * de clave-valor, conjuntos y TTL (Time To Live).
 *
 * <p>Provee una abstracción de bajo nivel reutilizable para otros servicios del módulo
 * que necesiten interactuar con Redis. Todas las operaciones son no bloqueantes y
 * devuelven {@link CompletableFuture} gracias a la API asíncrona de Lettuce.</p>
 *
 * <p>Se usa principalmente para cachear datos de sesión de jugadores en línea,
 * como el estado del Deathban o la presencia de un jugador en el servidor.</p>
 */
@Singleton
public class RedisCacheService {

    private final RedisAsyncCommands<String, String> redis;

    /**
     * Crea el servicio de caché obteniendo los comandos asíncronos de Redis
     * a través de la fábrica de conexiones.
     *
     * @param factory fábrica que provee la conexión asíncrona a Redis
     */
    @Inject
    public RedisCacheService(RedisConnectionFactory factory) {
        this.redis = factory.async();
    }

    /**
     * Almacena un valor en Redis asociado a la clave indicada con un tiempo de expiración.
     * Si ya existe un valor con esa clave, lo sobreescribe y reinicia el TTL.
     *
     * @param key        clave única bajo la que se almacena el valor
     * @param value      valor de texto a almacenar
     * @param ttlSeconds tiempo de vida en segundos; tras ese tiempo Redis elimina la clave automáticamente
     * @return future que se resuelve cuando el valor ha sido almacenado en Redis
     */
    public CompletableFuture<Void> set(String key, String value, long ttlSeconds) {
        return redis.setex(key, ttlSeconds, value)
                .toCompletableFuture()
                .thenApply(r -> null);
    }

    /**
     * Obtiene el valor almacenado en Redis bajo la clave indicada.
     *
     * @param key clave de la que se quiere obtener el valor
     * @return future que se resuelve con un {@link Optional} que contiene el valor
     *         si la clave existe y no ha expirado, o vacío si la clave no existe
     */
    public CompletableFuture<Optional<String>> get(String key) {
        return redis.get(key)
                .toCompletableFuture()
                .thenApply(Optional::ofNullable);
    }

    /**
     * Elimina una clave y su valor de Redis de forma inmediata, antes de que expire el TTL.
     *
     * @param key clave a eliminar de Redis
     * @return future que se resuelve cuando la clave ha sido eliminada
     */
    public CompletableFuture<Void> delete(String key) {
        return redis.del(key)
                .toCompletableFuture()
                .thenApply(r -> null);
    }

    /**
     * Verifica si una clave existe actualmente en Redis (no ha expirado ni fue eliminada).
     *
     * @param key clave cuya existencia se quiere verificar
     * @return future que se resuelve con {@code true} si la clave existe, {@code false} si no
     */
    public CompletableFuture<Boolean> exists(String key) {
        return redis.exists(key)
                .toCompletableFuture()
                .thenApply(count -> count > 0);
    }

    /**
     * Agrega uno o más miembros a un conjunto (Set) de Redis identificado por la clave dada.
     * Si el conjunto no existe, lo crea automáticamente.
     *
     * @param key     clave del conjunto en Redis
     * @param members valores a agregar al conjunto
     * @return future que se resuelve cuando los miembros han sido agregados al conjunto
     */
    public CompletableFuture<Void> addToSet(String key, String... members) {
        return redis.sadd(key, members)
                .toCompletableFuture()
                .thenApply(r -> null);
    }

    /**
     * Verifica si un valor es miembro de un conjunto (Set) de Redis.
     * Útil para comprobar membresía sin recuperar todos los elementos del conjunto.
     *
     * @param key    clave del conjunto en Redis
     * @param member valor cuya membresía se quiere verificar
     * @return future que se resuelve con {@code true} si el miembro pertenece al conjunto,
     *         {@code false} si no pertenece o si el conjunto no existe
     */
    public CompletableFuture<Boolean> isMemberOfSet(String key, String member) {
        return redis.sismember(key, member)
                .toCompletableFuture();
    }

    /**
     * Elimina un miembro específico de un conjunto (Set) de Redis.
     *
     * @param key    clave del conjunto en Redis
     * @param member valor a eliminar del conjunto
     * @return future que se resuelve cuando el miembro ha sido eliminado del conjunto
     */
    public CompletableFuture<Void> removeFromSet(String key, String member) {
        return redis.srem(key, member)
                .toCompletableFuture()
                .thenApply(r -> null);
    }
}

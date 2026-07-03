package dev.alexissdev.kronos.common.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

/**
 * Fábrica singleton gestionada por Guice que establece y provee la conexión a Redis
 * para el plugin Kronos HCF mediante la librería Lettuce.
 *
 * <p>Redis se utiliza en el plugin principalmente para almacenamiento temporal de alta velocidad,
 * comunicación entre servidores (pub/sub) y caché de datos que deben estar disponibles
 * con baja latencia (p. ej. estado del SOTW, cooldowns, sesiones de jugadores).</p>
 *
 * <p>La conexión se configura con host, puerto y, opcionalmente, contraseña. Si la contraseña
 * proporcionada es nula o vacía, se conecta sin autenticación. Los parámetros son inyectados
 * por Guice mediante {@link com.google.inject.name.Named}.</p>
 *
 * <p>La instancia es {@code @Singleton}: existe una única conexión durante toda la vida del
 * plugin. Al apagar el servidor debe llamarse {@link #close()} para cerrar la conexión
 * y apagar el cliente correctamente.</p>
 */
@Singleton
public class RedisConnectionFactory {

    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> connection;

    /**
     * Construye la fábrica de conexión a Redis e inicializa el cliente Lettuce.
     *
     * <p>Si se proporciona contraseña, se incluye en la URI de conexión usando
     * un arreglo de chars (en lugar de {@code String}) para minimizar el tiempo
     * que la contraseña permanece en memoria.</p>
     *
     * @param host     dirección del servidor Redis; inyectado con la clave {@code redis.host}
     * @param port     puerto del servidor Redis; inyectado con la clave {@code redis.port}
     * @param password contraseña de autenticación, o cadena vacía/nula si no se requiere;
     *                 inyectada con la clave {@code redis.password}
     */
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

    /**
     * Devuelve la interfaz de comandos asíncronos de Redis sobre la conexión activa.
     *
     * <p>Usar la API asíncrona evita bloquear el hilo principal del servidor Bukkit
     * mientras se espera respuesta de Redis. Las operaciones devuelven
     * {@code RedisFuture} que pueden encadenarse sin bloquear.</p>
     *
     * @return comandos asíncronos de Redis listos para ejecutar operaciones de lectura y escritura
     */
    public RedisAsyncCommands<String, String> async() {
        return connection.async();
    }

    /**
     * Cierra la conexión activa con Redis y apaga el cliente Lettuce liberando todos
     * los recursos de red y los hilos asociados.
     * Debe invocarse durante el apagado del plugin (p. ej. en {@code onDisable()}).
     */
    public void close() {
        connection.close();
        redisClient.shutdown();
    }
}

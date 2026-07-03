package dev.alexissdev.kronos.plugin.lifecycle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.database.MongoConnectionFactory;
import dev.alexissdev.kronos.common.database.RedisConnectionFactory;
import dev.alexissdev.kronos.koth.KothApplicationService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manejador dedicado a la lógica de apagado del plugin KronosHCF.
 *
 * <p>Separa la responsabilidad de {@code onDisable()} de la clase principal {@link dev.alexissdev.kronos.plugin.HCFPlugin}
 * hacia esta clase inyectable, facilitando las pruebas y el mantenimiento. Se encarga de
 * desactivar todos los KOTHs activos y de cerrar ordenadamente las conexiones a MongoDB y Redis.
 */
@Singleton
public class PluginDisableHandler {

    private final MongoConnectionFactory mongoFactory;
    private final RedisConnectionFactory redisFactory;
    private final KothApplicationService kothService;
    private final JavaPlugin plugin;

    /**
     * Crea una nueva instancia del manejador de apagado con todas sus dependencias inyectadas.
     *
     * @param mongoFactory fábrica de conexión a MongoDB que debe cerrarse al apagar el plugin
     * @param redisFactory fábrica de conexión a Redis que debe cerrarse al apagar el plugin
     * @param kothService  servicio de aplicación de KOTH usado para desactivar eventos activos
     * @param plugin       instancia del plugin principal, utilizada para el logger
     */
    @Inject
    public PluginDisableHandler(MongoConnectionFactory mongoFactory,
                                RedisConnectionFactory redisFactory,
                                KothApplicationService kothService,
                                JavaPlugin plugin) {
        this.mongoFactory = mongoFactory;
        this.redisFactory = redisFactory;
        this.kothService  = kothService;
        this.plugin       = plugin;
    }

    /**
     * Ejecuta el proceso de apagado ordenado del plugin.
     *
     * <p>Realiza las siguientes operaciones en orden:
     * <ol>
     *   <li>Desregistra todos los servicios del plugin del {@code ServicesManager} de Bukkit.</li>
     *   <li>Desactiva todos los KOTHs activos de forma sincrónica (bloqueante).</li>
     *   <li>Cierra la conexión a MongoDB.</li>
     *   <li>Cierra la conexión a Redis.</li>
     * </ol>
     * Cada paso se ejecuta dentro de un bloque {@code try-catch} individual para que un fallo
     * en uno no impida la ejecución de los siguientes.
     */
    public void disable() {
        Bukkit.getServicesManager().unregisterAll(plugin);

        try {
            kothService.deactivateAll().join();
            plugin.getLogger().info("KOTHs desactivados.");
        } catch (Exception e) {
            plugin.getLogger().warning("Error desactivando KOTHs: " + e.getMessage());
        }

        try {
            mongoFactory.close();
            plugin.getLogger().info("Conexión MongoDB cerrada.");
        } catch (Exception e) {
            plugin.getLogger().warning("Error cerrando MongoDB: " + e.getMessage());
        }

        try {
            redisFactory.close();
            plugin.getLogger().info("Conexión Redis cerrada.");
        } catch (Exception e) {
            plugin.getLogger().warning("Error cerrando Redis: " + e.getMessage());
        }

        plugin.getLogger().info("KronosHCF deshabilitado correctamente.");
    }
}

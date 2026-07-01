package dev.alexissdev.kronos.plugin.lifecycle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.database.MongoConnectionFactory;
import dev.alexissdev.kronos.common.database.RedisConnectionFactory;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class PluginDisableHandler {

    private final MongoConnectionFactory mongoFactory;
    private final RedisConnectionFactory redisFactory;
    private final JavaPlugin plugin;

    @Inject
    public PluginDisableHandler(MongoConnectionFactory mongoFactory,
                                RedisConnectionFactory redisFactory,
                                JavaPlugin plugin) {
        this.mongoFactory = mongoFactory;
        this.redisFactory = redisFactory;
        this.plugin = plugin;
    }

    public void disable() {
        Bukkit.getServicesManager().unregisterAll(plugin);

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

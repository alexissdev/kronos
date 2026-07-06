package dev.alexissdev.kronos.plugin;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.plugin.guice.RootModule;
import dev.alexissdev.kronos.plugin.lifecycle.PluginDisableHandler;
import dev.alexissdev.kronos.plugin.lifecycle.PluginEnableHandler;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main class of the KronosHCF plugin.
 *
 * <p>Extends {@link JavaPlugin} and serves as the plugin's entry point in the Bukkit lifecycle.
 * It initialises the Guice dependency-injection container through {@link RootModule} and
 * delegates startup and shutdown logic to {@link PluginEnableHandler} and
 * {@link PluginDisableHandler} respectively.
 */
public final class HCFPlugin extends JavaPlugin {

    private Injector injector;

    /**
     * Called before the server registers the plugin as active.
     *
     * <p>Writes the default configuration ({@code config.yml}) and the messages file
     * ({@code messages.yml}) to the plugin's data folder if they do not already exist.
     */
    @Override
    public void onLoad() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
    }

    /**
     * Main plugin entry point called when the plugin is enabled.
     *
     * <p>Loads the messages file, builds the Guice injector with the root {@link RootModule}, and
     * delegates all initialisation logic (command registration, listeners, services, etc.) to
     * {@link PluginEnableHandler#enable()}. If any exception occurs during this process, the plugin
     * is automatically disabled to prevent an inconsistent state.
     */
    @Override
    public void onEnable() {
        try {
            MessagesConfig messagesConfig = loadMessages();
            injector = Guice.createInjector(new RootModule(this, messagesConfig));
            PluginEnableHandler enableHandler = injector.getInstance(PluginEnableHandler.class);
            enableHandler.enable();
        } catch (Exception e) {
            getLogger().severe("Error al inicializar KronosHCF: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Called when the plugin is disabled by the server or due to an error.
     *
     * <p>If the injector was initialised successfully, delegates the shutdown process
     * (closing database connections, deactivating KOTHs, etc.) to
     * {@link PluginDisableHandler#disable()}.
     */
    @Override
    public void onDisable() {
        if (injector != null) {
            try {
                PluginDisableHandler disableHandler = injector.getInstance(PluginDisableHandler.class);
                disableHandler.disable();
            } catch (Exception e) {
                getLogger().severe("Error al deshabilitar KronosHCF: " + e.getMessage());
            }
        }
    }

    /**
     * Returns the Guice injector created during {@link #onEnable()}.
     *
     * <p>May be used by external components (for example, other plugins or admin commands)
     * that need to retrieve Guice-managed instances.
     *
     * @return the Guice {@link Injector}, or {@code null} if the plugin did not initialise correctly
     */
    public Injector getInjector() {
        return injector;
    }

    /**
     * Reads {@code messages.yml} from the plugin's data folder and constructs a
     * {@link MessagesConfig} populated with all keys and values found in that file.
     *
     * <p>Only keys that represent scalar values (not configuration sections) are processed,
     * ensuring the resulting map contains plain strings exclusively.
     *
     * @return a {@link MessagesConfig} populated with the messages defined in {@code messages.yml}
     */
    private MessagesConfig loadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : yaml.getKeys(true)) {
            if (!yaml.isConfigurationSection(key)) {
                map.put(key, yaml.getString(key, ""));
            }
        }
        return new MessagesConfig(map);
    }
}

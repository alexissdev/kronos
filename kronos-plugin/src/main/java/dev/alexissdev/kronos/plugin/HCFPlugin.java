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

public final class HCFPlugin extends JavaPlugin {

    private Injector injector;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
    }

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

    public Injector getInjector() {
        return injector;
    }

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

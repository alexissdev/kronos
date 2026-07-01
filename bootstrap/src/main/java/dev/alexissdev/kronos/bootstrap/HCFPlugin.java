package dev.alexissdev.kronos.bootstrap;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.alexissdev.kronos.bootstrap.guice.RootModule;
import dev.alexissdev.kronos.bootstrap.lifecycle.PluginDisableHandler;
import dev.alexissdev.kronos.bootstrap.lifecycle.PluginEnableHandler;
import org.bukkit.plugin.java.JavaPlugin;

public final class HCFPlugin extends JavaPlugin {

    private Injector injector;

    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        try {
            injector = Guice.createInjector(new RootModule(this));
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
}

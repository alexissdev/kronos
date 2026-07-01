package dev.alexissdev.kronos.bootstrap.guice;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import dev.alexissdev.kronos.api.guice.ApiModule;
import dev.alexissdev.kronos.application.guice.ApplicationModule;
import dev.alexissdev.kronos.infrastructure.guice.InfrastructureModule;
import dev.alexissdev.kronos.presentation.guice.PresentationModule;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class RootModule extends AbstractModule {

    private final JavaPlugin plugin;

    public RootModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(JavaPlugin.class).toInstance(plugin);
        bind(org.bukkit.plugin.Plugin.class).toInstance(plugin);

        bindConfig();

        install(new InfrastructureModule());
        install(new ApplicationModule());
        install(new ApiModule());
        install(new PresentationModule());
    }

    private void bindConfig() {
        FileConfiguration config = plugin.getConfig();

        bindString("mongo.uri", config.getString("database.mongo.uri", "mongodb://localhost:27017"));
        bindString("mongo.database", config.getString("database.mongo.database", "kronoshcf"));
        bindString("redis.host", config.getString("database.redis.host", "localhost"));
        bindInt("redis.port", config.getInt("database.redis.port", 6379));
        bindString("redis.password", config.getString("database.redis.password", ""));
    }

    private void bindString(String key, String value) {
        bind(String.class).annotatedWith(Names.named(key)).toInstance(value);
    }

    private void bindInt(String key, int value) {
        bind(Integer.class).annotatedWith(Names.named(key)).toInstance(value);
    }

}

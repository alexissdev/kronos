package dev.alexissdev.kronos.plugin.guice;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import dev.alexissdev.kronos.api.guice.ApiModule;
import dev.alexissdev.kronos.classes.ClassesModule;
import dev.alexissdev.kronos.claims.ClaimsModule;
import dev.alexissdev.kronos.economy.EconomyModule;
import dev.alexissdev.kronos.factions.FactionsModule;
import dev.alexissdev.kronos.koth.KothModule;
import dev.alexissdev.kronos.players.PlayersModule;
import dev.alexissdev.kronos.plugin.command.FactionCommand;
import dev.alexissdev.kronos.plugin.listener.FactionEventListener;
import dev.alexissdev.kronos.plugin.listener.PlayerDataListener;
import dev.alexissdev.kronos.plugin.listener.PvpListener;
import dev.alexissdev.kronos.plugin.listener.KothListener;
import dev.alexissdev.kronos.plugin.listener.TimerListener;
import dev.alexissdev.kronos.timers.TimersModule;
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
        bind(EventBus.class).in(Singleton.class);
        bind(PlayerDataListener.class).in(Singleton.class);
        bind(PvpListener.class).in(Singleton.class);
        bind(TimerListener.class).in(Singleton.class);
        bind(FactionEventListener.class).in(Singleton.class);
        bind(FactionCommand.class).in(Singleton.class);
        bind(KothListener.class).in(Singleton.class);

        bindConfig();

        install(new DatabaseModule());
        install(new EconomyModule());
        install(new PlayersModule());
        install(new TimersModule());
        install(new FactionsModule());
        install(new ClaimsModule());
        install(new KothModule());
        install(new ClassesModule());
        install(new ApiModule());
    }

    private void bindConfig() {
        FileConfiguration config = plugin.getConfig();

        bindString("mongo.uri",      config.getString("database.mongo.uri",      "mongodb://localhost:27017"));
        bindString("mongo.database", config.getString("database.mongo.database", "kronoshcf"));
        bindString("redis.host",     config.getString("database.redis.host",     "localhost"));
        bindInt   ("redis.port",     config.getInt   ("database.redis.port",     6379));
        bindString("redis.password", config.getString("database.redis.password", ""));
    }

    private void bindString(String key, String value) {
        bind(String.class).annotatedWith(Names.named(key)).toInstance(value);
    }

    private void bindInt(String key, int value) {
        bind(Integer.class).annotatedWith(Names.named(key)).toInstance(value);
    }
}

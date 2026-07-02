package dev.alexissdev.kronos.plugin.guice;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import dev.alexissdev.kronos.api.guice.ApiModule;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.SotwService;
import dev.alexissdev.kronos.plugin.sotw.SotwManager;
import dev.alexissdev.kronos.scoreboard.ScoreboardModule;
import dev.alexissdev.kronos.spawn.SpawnModule;
import dev.alexissdev.kronos.classes.ClassesModule;
import dev.alexissdev.kronos.claims.ClaimsModule;
import dev.alexissdev.kronos.economy.EconomyModule;
import dev.alexissdev.kronos.factions.FactionsModule;
import dev.alexissdev.kronos.koth.KothModule;
import dev.alexissdev.kronos.players.PlayersModule;
import dev.alexissdev.kronos.plugin.command.FactionCommand;
import dev.alexissdev.kronos.plugin.chat.ChatManager;
import dev.alexissdev.kronos.plugin.listener.CrateListener;
import dev.alexissdev.kronos.plugin.listener.DeathbanListener;
import dev.alexissdev.kronos.plugin.listener.FactionEventListener;
import dev.alexissdev.kronos.plugin.listener.PlayerDataListener;
import dev.alexissdev.kronos.plugin.listener.PvpListener;
import dev.alexissdev.kronos.plugin.listener.KothListener;
import dev.alexissdev.kronos.plugin.listener.TimerListener;
import dev.alexissdev.kronos.plugin.tablist.TabListManager;
import dev.alexissdev.kronos.timers.TimersModule;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class RootModule extends AbstractModule {

    private final JavaPlugin plugin;
    private final MessagesConfig messagesConfig;

    public RootModule(JavaPlugin plugin, MessagesConfig messagesConfig) {
        this.plugin = plugin;
        this.messagesConfig = messagesConfig;
    }

    @Override
    protected void configure() {
        bind(JavaPlugin.class).toInstance(plugin);
        bind(org.bukkit.plugin.Plugin.class).toInstance(plugin);
        bind(MessagesConfig.class).toInstance(messagesConfig);
        bind(EventBus.class).in(Singleton.class);
        bind(ChatManager.class).in(Singleton.class);
        bind(SotwService.class).to(SotwManager.class).in(Singleton.class);
        bind(PlayerDataListener.class).in(Singleton.class);
        bind(PvpListener.class).in(Singleton.class);
        bind(TimerListener.class).in(Singleton.class);
        bind(FactionEventListener.class).in(Singleton.class);
        bind(FactionCommand.class).in(Singleton.class);
        bind(KothListener.class).in(Singleton.class);
        bind(DeathbanListener.class).in(Singleton.class);
        bind(CrateListener.class).in(Singleton.class);
        bind(TabListManager.class).in(Singleton.class);

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
        install(new ScoreboardModule());
        install(new SpawnModule());
    }

    private void bindConfig() {
        FileConfiguration config = plugin.getConfig();

        bindString("mongo.uri",      config.getString("database.mongo.uri",      "mongodb://localhost:27017"));
        bindString("mongo.database", config.getString("database.mongo.database", "kronoshcf"));
        bindString("redis.host",     config.getString("database.redis.host",     "localhost"));
        bindInt   ("redis.port",     config.getInt   ("database.redis.port",     6379));
        bindString("redis.password", config.getString("database.redis.password", ""));

        bindInt ("hcf.lives",              config.getInt("hcf.lives",                              3));
        bindLong("hcf.deathban-seconds",   config.getInt("hcf.deathban-hours",                    24) * 3600L);
        bindLong("enderpearl.cooldown-ms", config.getInt("timers.enderpearl-cooldown-seconds",    15) * 1000L);
        bindLong("gapple.cooldown-ms",     config.getInt("timers.gapple-cooldown-seconds",        30) * 1000L);
        bindLong("home.delay-ms",          config.getInt("timers.home-delay-seconds",              5) * 1000L);
        bindInt ("faction.max-members",          config.getInt("faction.max-members",                 15));
        bindLong("faction.reinvite-cooldown-ms", config.getInt("faction.reinvite-cooldown-hours",    24) * 3600L * 1000L);
        bindInt ("faction.max-claims-per-member",config.getInt("faction.max-claims-per-member",       5));
        bindLong("faction.invite-expiry-ms",     config.getInt("faction.invite-expiry-seconds",      120) * 1000L);
        bindInt ("lives.max-lives",              config.getInt("lives.max-lives",                     5));
        bindLong("lives.regen-interval-ms",      config.getInt("lives.regen-interval-hours",         24) * 3600L * 1000L);
    }

    private void bindString(String key, String value) {
        bind(String.class).annotatedWith(Names.named(key)).toInstance(value);
    }

    private void bindInt(String key, int value) {
        bind(Integer.class).annotatedWith(Names.named(key)).toInstance(value);
    }

    private void bindLong(String key, long value) {
        bind(Long.class).annotatedWith(Names.named(key)).toInstance(value);
    }
}

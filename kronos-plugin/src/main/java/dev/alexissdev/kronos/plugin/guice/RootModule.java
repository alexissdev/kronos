package dev.alexissdev.kronos.plugin.guice;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import dev.alexissdev.kronos.api.guice.ApiModule;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.SotwService;
import dev.alexissdev.kronos.plugin.command.crate.ListCrateSub;
import dev.alexissdev.kronos.plugin.command.crate.RemoveCrateSub;
import dev.alexissdev.kronos.plugin.command.crate.SetCrateSub;
import dev.alexissdev.kronos.plugin.command.faction.AcceptFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.AllyFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.ChatFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.ClaimFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.CreateFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.DepositFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.DisbandFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.EnemyFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.FreezeFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.HomeFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.InfoFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.InviteFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.KickFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.LeaveFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.MapFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.NeutralFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.OverclaimFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.RenameFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.SetHomeFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.SetLeaderFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.StrikeFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.TopFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.UnclaimFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.UnfreezeFactionSub;
import dev.alexissdev.kronos.plugin.command.faction.WithdrawFactionSub;
import dev.alexissdev.kronos.plugin.command.hcf.EotwSub;
import dev.alexissdev.kronos.plugin.command.hcf.GiveKeySub;
import dev.alexissdev.kronos.plugin.command.hcf.GiveMoneySub;
import dev.alexissdev.kronos.plugin.command.hcf.ReloadSub;
import dev.alexissdev.kronos.plugin.command.hcf.SetMoneySub;
import dev.alexissdev.kronos.plugin.command.hcf.SotwSub;
import dev.alexissdev.kronos.plugin.command.hcf.UnbanSub;
import dev.alexissdev.kronos.plugin.command.pvptimer.GivePvpTimerSub;
import dev.alexissdev.kronos.plugin.command.pvptimer.RemovePvpTimerSub;
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
import org.bukkit.plugin.Plugin;
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
        bind(Plugin.class).toInstance(plugin);
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

        bindFactionSubCommands();
        bindHcfSubCommands();
        bindPvpTimerSubCommands();
        bindCrateSubCommands();

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

    private void bindFactionSubCommands() {
        Multibinder<SubCommand> binder = Multibinder.newSetBinder(binder(), SubCommand.class, Names.named("faction"));
        binder.addBinding().to(CreateFactionSub.class).in(Singleton.class);
        binder.addBinding().to(DisbandFactionSub.class).in(Singleton.class);
        binder.addBinding().to(InviteFactionSub.class).in(Singleton.class);
        binder.addBinding().to(AcceptFactionSub.class).in(Singleton.class);
        binder.addBinding().to(LeaveFactionSub.class).in(Singleton.class);
        binder.addBinding().to(KickFactionSub.class).in(Singleton.class);
        binder.addBinding().to(InfoFactionSub.class).in(Singleton.class);
        binder.addBinding().to(ChatFactionSub.class).in(Singleton.class);
        binder.addBinding().to(TopFactionSub.class).in(Singleton.class);
        binder.addBinding().to(AllyFactionSub.class).in(Singleton.class);
        binder.addBinding().to(EnemyFactionSub.class).in(Singleton.class);
        binder.addBinding().to(NeutralFactionSub.class).in(Singleton.class);
        binder.addBinding().to(DepositFactionSub.class).in(Singleton.class);
        binder.addBinding().to(WithdrawFactionSub.class).in(Singleton.class);
        binder.addBinding().to(ClaimFactionSub.class).in(Singleton.class);
        binder.addBinding().to(UnclaimFactionSub.class).in(Singleton.class);
        binder.addBinding().to(OverclaimFactionSub.class).in(Singleton.class);
        binder.addBinding().to(MapFactionSub.class).in(Singleton.class);
        binder.addBinding().to(SetHomeFactionSub.class).in(Singleton.class);
        binder.addBinding().to(HomeFactionSub.class).in(Singleton.class);
        binder.addBinding().to(RenameFactionSub.class).in(Singleton.class);
        binder.addBinding().to(StrikeFactionSub.class).in(Singleton.class);
        binder.addBinding().to(FreezeFactionSub.class).in(Singleton.class);
        binder.addBinding().to(UnfreezeFactionSub.class).in(Singleton.class);
        binder.addBinding().to(SetLeaderFactionSub.class).in(Singleton.class);
    }

    private void bindHcfSubCommands() {
        Multibinder<SubCommand> binder = Multibinder.newSetBinder(binder(), SubCommand.class, Names.named("hcf"));
        binder.addBinding().to(ReloadSub.class).in(Singleton.class);
        binder.addBinding().to(GiveMoneySub.class).in(Singleton.class);
        binder.addBinding().to(SetMoneySub.class).in(Singleton.class);
        binder.addBinding().to(GiveKeySub.class).in(Singleton.class);
        binder.addBinding().to(SotwSub.class).in(Singleton.class);
        binder.addBinding().to(EotwSub.class).in(Singleton.class);
        binder.addBinding().to(UnbanSub.class).in(Singleton.class);
    }

    private void bindPvpTimerSubCommands() {
        Multibinder<SubCommand> binder = Multibinder.newSetBinder(binder(), SubCommand.class, Names.named("pvptimer"));
        binder.addBinding().to(GivePvpTimerSub.class).in(Singleton.class);
        binder.addBinding().to(RemovePvpTimerSub.class).in(Singleton.class);
    }

    private void bindCrateSubCommands() {
        Multibinder<SubCommand> binder = Multibinder.newSetBinder(binder(), SubCommand.class, Names.named("crate"));
        binder.addBinding().to(SetCrateSub.class).in(Singleton.class);
        binder.addBinding().to(RemoveCrateSub.class).in(Singleton.class);
        binder.addBinding().to(ListCrateSub.class).in(Singleton.class);
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

package dev.alexissdev.kronos.plugin.lifecycle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.HCFApi;
import dev.alexissdev.kronos.api.HCFApiImpl;
import dev.alexissdev.kronos.claims.listener.ClaimListener;
import dev.alexissdev.kronos.classes.listener.ClassListener;
import dev.alexissdev.kronos.economy.command.MoneyCommand;
import dev.alexissdev.kronos.koth.command.KothCommand;
import dev.alexissdev.kronos.koth.listener.KothWandListener;
import dev.alexissdev.kronos.plugin.chat.ChatListener;
import dev.alexissdev.kronos.plugin.listener.CrateListener;
import dev.alexissdev.kronos.plugin.listener.DeathbanListener;
import dev.alexissdev.kronos.plugin.listener.KothListener;
import dev.alexissdev.kronos.plugin.command.BaltopCommand;
import dev.alexissdev.kronos.plugin.command.FactionCommand;
import dev.alexissdev.kronos.plugin.command.FixCommand;
import dev.alexissdev.kronos.plugin.command.HCFCommand;
import dev.alexissdev.kronos.plugin.command.NearCommand;
import dev.alexissdev.kronos.plugin.command.PvpTimerCommand;
import dev.alexissdev.kronos.plugin.command.StatsCommand;
import dev.alexissdev.kronos.plugin.listener.FactionEventListener;
import dev.alexissdev.kronos.plugin.listener.PlayerDataListener;
import dev.alexissdev.kronos.plugin.listener.PvpListener;
import dev.alexissdev.kronos.plugin.listener.TimerListener;
import dev.alexissdev.kronos.scoreboard.ScoreboardListener;
import dev.alexissdev.kronos.scoreboard.ScoreboardManager;
import dev.alexissdev.kronos.scoreboard.ScoreboardTask;
import dev.alexissdev.kronos.spawn.SpawnApplicationService;
import dev.alexissdev.kronos.spawn.command.SpawnCommand;
import dev.alexissdev.kronos.spawn.listener.SpawnListener;
import dev.alexissdev.kronos.spawn.listener.SpawnWandListener;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class PluginEnableHandler {

    private final Injector injector;
    private final JavaPlugin plugin;

    @Inject
    public PluginEnableHandler(Injector injector, JavaPlugin plugin) {
        this.injector = injector;
        this.plugin = plugin;
    }

    public void enable() {
        registerCommands();
        registerListeners();
        registerApiService();

        injector.getInstance(SpawnApplicationService.class).loadZone();
        injector.getInstance(ClaimListener.class).preloadCache();
        injector.getInstance(TimerApplicationService.class).scheduleExpiryChecks(plugin);
        injector.getInstance(ScoreboardTask.class); // schedules periodic tasks

        ScoreboardManager scoreboardManager = injector.getInstance(ScoreboardManager.class);
        for (org.bukkit.entity.Player online : Bukkit.getOnlinePlayers()) {
            scoreboardManager.createBoard(online);
        }

        plugin.getLogger().info("KronosHCF habilitado correctamente.");
    }

    private void registerCommands() {
        registerCommand("f",       injector.getInstance(FactionCommand.class));
        registerCommand("faction", injector.getInstance(FactionCommand.class));
        registerCommand("koth",    injector.getInstance(KothCommand.class));
        registerCommand("money",   injector.getInstance(MoneyCommand.class));
        registerCommand("balance", injector.getInstance(MoneyCommand.class));
        registerCommand("hcf",      injector.getInstance(HCFCommand.class));
        registerCommand("pvptimer", injector.getInstance(PvpTimerCommand.class));
        registerCommand("spawn",    injector.getInstance(SpawnCommand.class));
        registerCommand("stats",    injector.getInstance(StatsCommand.class));
        registerCommand("near",     injector.getInstance(NearCommand.class));
        registerCommand("fix",      injector.getInstance(FixCommand.class));
        registerCommand("baltop",   injector.getInstance(BaltopCommand.class));
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        org.bukkit.command.PluginCommand cmd = plugin.getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter) {
                cmd.setTabCompleter((org.bukkit.command.TabCompleter) executor);
            }
        }
    }

    private void registerListeners() {
        org.bukkit.plugin.PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(injector.getInstance(ChatListener.class), plugin);
        pm.registerEvents(injector.getInstance(DeathbanListener.class), plugin);
        pm.registerEvents(injector.getInstance(PlayerDataListener.class), plugin);
        pm.registerEvents(injector.getInstance(PvpListener.class), plugin);
        pm.registerEvents(injector.getInstance(ClaimListener.class), plugin);
        pm.registerEvents(injector.getInstance(ClassListener.class), plugin);
        pm.registerEvents(injector.getInstance(TimerListener.class), plugin);
        pm.registerEvents(injector.getInstance(KothListener.class), plugin);
        pm.registerEvents(injector.getInstance(FactionEventListener.class), plugin);
        pm.registerEvents(injector.getInstance(ScoreboardListener.class), plugin);
        pm.registerEvents(injector.getInstance(KothWandListener.class), plugin);
        pm.registerEvents(injector.getInstance(SpawnListener.class), plugin);
        pm.registerEvents(injector.getInstance(CrateListener.class), plugin);
        pm.registerEvents(injector.getInstance(SpawnWandListener.class), plugin);
    }

    private void registerApiService() {
        HCFApi apiImpl = injector.getInstance(HCFApiImpl.class);
        Bukkit.getServicesManager().register(HCFApi.class, apiImpl, plugin, ServicePriority.Normal);
        plugin.getLogger().info("HCFApi registrada en ServicesManager.");
    }
}

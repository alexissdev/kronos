package dev.alexissdev.kronos.bootstrap.lifecycle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.HCFApi;
import dev.alexissdev.kronos.api.HCFApiImpl;
import dev.alexissdev.kronos.application.timer.TimerApplicationService;
import dev.alexissdev.kronos.presentation.command.*;
import dev.alexissdev.kronos.presentation.listener.*;
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

        injector.getInstance(ClaimListener.class).preloadCache();
        injector.getInstance(TimerApplicationService.class).scheduleExpiryChecks(plugin);

        plugin.getLogger().info("KronosHCF habilitado correctamente.");
    }

    private void registerCommands() {
        registerCommand("f", injector.getInstance(FactionCommand.class));
        registerCommand("faction", injector.getInstance(FactionCommand.class));
        registerCommand("koth", injector.getInstance(KothCommand.class));
        registerCommand("money", injector.getInstance(MoneyCommand.class));
        registerCommand("balance", injector.getInstance(MoneyCommand.class));
        registerCommand("staff", injector.getInstance(StaffCommand.class));
        registerCommand("hcf", injector.getInstance(HCFCommand.class));
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        org.bukkit.command.PluginCommand cmd = plugin.getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
        }
    }

    private void registerListeners() {
        org.bukkit.plugin.PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(injector.getInstance(PlayerDataListener.class), plugin);
        pm.registerEvents(injector.getInstance(PvpListener.class), plugin);
        pm.registerEvents(injector.getInstance(ClaimListener.class), plugin);
        pm.registerEvents(injector.getInstance(ClassListener.class), plugin);
        pm.registerEvents(injector.getInstance(TimerListener.class), plugin);
        pm.registerEvents(injector.getInstance(KothListener.class), plugin);
        pm.registerEvents(injector.getInstance(FactionEventListener.class), plugin);
    }

    private void registerApiService() {
        HCFApi apiImpl = injector.getInstance(HCFApiImpl.class);
        Bukkit.getServicesManager().register(HCFApi.class, apiImpl, plugin, ServicePriority.Normal);
        plugin.getLogger().info("HCFApi registrada en ServicesManager.");
    }
}

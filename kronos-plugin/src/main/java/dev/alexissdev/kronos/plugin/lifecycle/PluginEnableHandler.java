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
import dev.alexissdev.kronos.plugin.command.CrateCommand;
import dev.alexissdev.kronos.plugin.command.FactionCommand;
import dev.alexissdev.kronos.plugin.command.FixCommand;
import dev.alexissdev.kronos.plugin.command.HCFCommand;
import dev.alexissdev.kronos.plugin.command.KitCommand;
import dev.alexissdev.kronos.plugin.command.NearCommand;
import dev.alexissdev.kronos.plugin.command.PvpTimerCommand;
import dev.alexissdev.kronos.plugin.command.StatsCommand;
import dev.alexissdev.kronos.plugin.command.StuckCommand;
import dev.alexissdev.kronos.plugin.listener.StuckListener;
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
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manejador dedicado a la lógica de arranque del plugin KronosHCF.
 *
 * <p>Separa la responsabilidad de {@code onEnable()} de la clase principal
 * {@link dev.alexissdev.kronos.plugin.HCFPlugin} hacia esta clase inyectable. Se encarga de
 * registrar todos los comandos y listeners del plugin, exponer la API pública en el
 * {@code ServicesManager} de Bukkit, precargar cachés y lanzar las tareas recurrentes.
 */
@Singleton
public class PluginEnableHandler {

    private final Injector injector;
    private final JavaPlugin plugin;

    /**
     * Crea el manejador de arranque con sus dependencias inyectadas por Guice.
     *
     * @param injector inyector Guice del que se obtienen todas las instancias del plugin
     * @param plugin   instancia del plugin principal de Bukkit
     */
    @Inject
    public PluginEnableHandler(Injector injector, JavaPlugin plugin) {
        this.injector = injector;
        this.plugin = plugin;
    }

    /**
     * Ejecuta el proceso completo de arranque del plugin.
     *
     * <p>Realiza en orden:
     * <ol>
     *   <li>Registro de comandos con sus tab-completers asociados.</li>
     *   <li>Registro de todos los listeners de eventos de Bukkit.</li>
     *   <li>Exposición de {@link HCFApi} en el {@code ServicesManager} de Bukkit.</li>
     *   <li>Carga de la zona de spawn y los crates desde la base de datos.</li>
     *   <li>Precarga de la caché de claims.</li>
     *   <li>Inicio de las tareas programadas de timers y scoreboard.</li>
     *   <li>Creación del scoreboard para todos los jugadores ya conectados.</li>
     * </ol>
     */
    public void enable() {
        registerCommands();
        registerListeners();
        registerApiService();

        injector.getInstance(SpawnApplicationService.class).loadZone();
        injector.getInstance(ClaimListener.class).preloadCache();
        injector.getInstance(CrateListener.class).loadCrates();
        injector.getInstance(TimerApplicationService.class).scheduleExpiryChecks(plugin);
        injector.getInstance(ScoreboardTask.class); // schedules periodic tasks

        ScoreboardManager scoreboardManager = injector.getInstance(ScoreboardManager.class);
        for (Player online : Bukkit.getOnlinePlayers()) {
            scoreboardManager.createBoard(online);
        }

        plugin.getLogger().info("KronosHCF habilitado correctamente.");
    }

    /**
     * Registra todos los comandos del plugin en el servidor de Bukkit.
     *
     * <p>Cada comando declarado en {@code plugin.yml} recibe su {@link org.bukkit.command.CommandExecutor}.
     * Si el ejecutor también implementa {@link org.bukkit.command.TabCompleter}, se asigna
     * automáticamente para ofrecer autocompletado en la consola y en el chat de los jugadores.
     */
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
        registerCommand("stuck",    injector.getInstance(StuckCommand.class));
        registerCommand("kit",      injector.getInstance(KitCommand.class));
        registerCommand("crate",    injector.getInstance(CrateCommand.class));
    }

    /**
     * Asocia un ejecutor (y, opcionalmente, un tab-completer) a un comando registrado en {@code plugin.yml}.
     *
     * <p>Si el comando no está declarado en {@code plugin.yml}, la operación se omite silenciosamente.
     *
     * @param name     nombre del comando tal como aparece en {@code plugin.yml} (p. ej. {@code "f"})
     * @param executor ejecutor que procesará las invocaciones del comando; si también implementa
     *                 {@link org.bukkit.command.TabCompleter}, se registra como tal de forma automática
     */
    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand cmd = plugin.getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            if (executor instanceof TabCompleter) {
                cmd.setTabCompleter((TabCompleter) executor);
            }
        }
    }

    /**
     * Registra todos los listeners de eventos del plugin en el {@code PluginManager} de Bukkit.
     *
     * <p>Incluye listeners de chat, deathban, datos de jugador, PvP, claims, clases de HCF,
     * timers, KOTH, facciones, scoreboard, spawn, crates y el comando stuck.
     */
    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
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
        pm.registerEvents(injector.getInstance(StuckListener.class), plugin);
    }

    /**
     * Registra la implementación de {@link HCFApi} en el {@code ServicesManager} de Bukkit.
     *
     * <p>Esto permite que otros plugins externos consulten la API pública del sistema HCF
     * mediante {@code Bukkit.getServicesManager().getRegistration(HCFApi.class)}.
     */
    private void registerApiService() {
        HCFApi apiImpl = injector.getInstance(HCFApiImpl.class);
        Bukkit.getServicesManager().register(HCFApi.class, apiImpl, plugin, ServicePriority.Normal);
        plugin.getLogger().info("HCFApi registrada en ServicesManager.");
    }
}

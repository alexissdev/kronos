package dev.alexissdev.kronos.plugin.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.players.domain.HCFPlayer;
import dev.alexissdev.kronos.players.service.PlayerService;
import dev.alexissdev.kronos.plugin.tablist.TabListManager;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Listener que gestiona la carga y persistencia de datos de los jugadores en el ciclo
 * de conexión/desconexión del servidor.
 *
 * <p>Al conectarse un jugador, este listener coordina de forma asíncrona y secuencial:
 * <ol>
 *   <li>La carga o creación del perfil {@link dev.alexissdev.kronos.players.domain.HCFPlayer}.</li>
 *   <li>La carga de todos sus timers activos en la caché del {@link dev.alexissdev.kronos.timers.TimerApplicationService}.</li>
 *   <li>La asignación automática del PvP timer para jugadores que se unen por primera vez.</li>
 *   <li>La regeneración de una vida si ha transcurrido el intervalo configurado.</li>
 *   <li>La ejecución del timer de logout (kill on reconnect) si estaba activo al desconectarse.</li>
 * </ol>
 *
 * <p>Al desconectarse, si el jugador tiene un combat tag activo, se inicia un timer de logout
 * que lo matará si vuelve a conectarse antes de que expire (protección anti-combat-log).
 */
@Singleton
public class PlayerDataListener implements Listener {

    private static final long PVP_TIMER_DURATION_MS   = 60 * 60 * 1000L;
    private static final long LOGOUT_TIMER_DURATION_MS = 30_000L;

    private final PlayerService playerService;
    private final TimerApplicationService timerService;
    private final TabListManager tabListManager;
    private final Plugin plugin;
    private final MessagesConfig messages;
    private final int maxLives;
    private final long lifeRegenIntervalMs;

    /**
     * Crea el listener con todas sus dependencias inyectadas, incluyendo parámetros de configuración
     * anotados con {@code @Named}.
     *
     * @param playerService       servicio para cargar y persistir datos de jugadores
     * @param timerService        servicio de timers para cargar, cancelar e iniciar temporizadores
     * @param tabListManager      gestor del TabList para actualizarlo al conectarse el jugador
     * @param plugin              instancia del plugin principal para programar tareas en el scheduler
     * @param messages            configuración de mensajes localizada
     * @param maxLives            número máximo de vidas que puede tener un jugador (de {@code config.yml})
     * @param lifeRegenIntervalMs intervalo en milisegundos para la regeneración automática de vidas
     */
    @Inject
    public PlayerDataListener(PlayerService playerService,
                              TimerApplicationService timerService,
                              TabListManager tabListManager,
                              Plugin plugin,
                              MessagesConfig messages,
                              @Named("lives.max-lives") int maxLives,
                              @Named("lives.regen-interval-ms") long lifeRegenIntervalMs) {
        this.playerService = playerService;
        this.timerService = timerService;
        this.tabListManager = tabListManager;
        this.plugin = plugin;
        this.messages = messages;
        this.maxLives = maxLives;
        this.lifeRegenIntervalMs = lifeRegenIntervalMs;
    }

    /**
     * Maneja la conexión de un jugador al servidor.
     *
     * <p>Actualiza el TabList inmediatamente y, en el siguiente tick del servidor (para garantizar
     * que el scoreboard ya esté creado), carga el perfil del jugador, sus timers, aplica el
     * PvP timer de primer ingreso, verifica la regeneración de vidas y ejecuta el kill de logout
     * si corresponde.
     *
     * @param event evento de unión del jugador al servidor
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        tabListManager.refresh(event.getPlayer());

        // Delay by one tick so ScoreboardManager.createBoard() (MONITOR) has already run
        // before loadTimersIntoCache fires PlayerTimerStartedDomainEvent.
        Bukkit.getScheduler().runTask(plugin, () -> {
            UUID uuid = event.getPlayer().getUniqueId();
            String name = event.getPlayer().getName();

            playerService.getOrCreate(uuid, name)
                    .thenCompose(player ->
                            timerService.loadTimersIntoCache(uuid)
                                    .thenCompose(v -> giveFirstJoinPvpTimer(uuid, player))
                                    .thenCompose(v -> checkLifeRegen(uuid, player, event.getPlayer()))
                                    .thenRun(() -> checkLogoutTimer(event, uuid)))
                    .exceptionally(ex -> {
                        plugin.getLogger().warning("Error en carga de datos para "
                                + name + ": " + ex.getMessage());
                        return null;
                    });
        });
    }

    private CompletableFuture<Void> giveFirstJoinPvpTimer(UUID uuid, HCFPlayer player) {
        // Only auto-give PvP timer on first join and not when a logout kill is pending
        if (!player.isPvpTimerGiven()
                && !timerService.hasActiveTimerSync(uuid, TimerType.PVP_TIMER)
                && !timerService.hasActiveTimerSync(uuid, TimerType.LOGOUT)) {
            player.setPvpTimerGiven(true);
            playerService.savePlayer(player);
            return timerService.startPvpTimer(uuid, PVP_TIMER_DURATION_MS);
        }
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> checkLifeRegen(UUID uuid, HCFPlayer player, Player bukkit) {
        if (!player.tryRegenLife(maxLives, lifeRegenIntervalMs)) {
            return CompletableFuture.completedFuture(null);
        }
        return playerService.savePlayer(player).thenRun(() ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (bukkit.isOnline()) {
                        bukkit.sendMessage(messages.format("lives.regen",
                                "lives", String.valueOf(player.getLives())));
                    }
                }));
    }

    private void checkLogoutTimer(PlayerJoinEvent event, UUID uuid) {
        if (timerService.hasActiveTimerSync(uuid, TimerType.LOGOUT)) {
            timerService.cancelTimer(uuid, TimerType.LOGOUT);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (event.getPlayer().isOnline()) {
                    event.getPlayer().setHealth(0);
                    event.getPlayer().sendMessage(messages.get("timers.logout-death"));
                }
            });
        }
    }

    /**
     * Maneja la desconexión de un jugador del servidor.
     *
     * <p>Si el jugador tenía un combat tag activo al desconectarse, se inicia un timer de logout
     * que lo matará automáticamente si vuelve a conectarse antes de que expire (mecanismo anti-combat-log).
     * Independientemente del estado del combat tag, se limpia la caché de timers del jugador en memoria.
     *
     * @param event evento de desconexión del jugador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // Use sync cache check — combat tag may not be in Redis yet if tagged just before disconnect
        if (timerService.hasActiveTimerSync(uuid, TimerType.COMBAT_TAG)) {
            timerService.startLogoutTimer(uuid, LOGOUT_TIMER_DURATION_MS);
        }
        timerService.clearCache(uuid);
    }
}

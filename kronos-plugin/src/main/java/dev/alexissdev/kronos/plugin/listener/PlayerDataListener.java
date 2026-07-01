package dev.alexissdev.kronos.plugin.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.players.service.PlayerService;
import dev.alexissdev.kronos.plugin.tablist.TabListManager;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PlayerDataListener implements Listener {

    private static final long PVP_TIMER_DURATION_MS = 60 * 60 * 1000L;
    private static final long LOGOUT_TIMER_DURATION_MS = 30_000L;

    private final PlayerService playerService;
    private final TimerApplicationService timerService;
    private final TabListManager tabListManager;
    private final Plugin plugin;
    private final MessagesConfig messages;

    @Inject
    public PlayerDataListener(PlayerService playerService,
                              TimerApplicationService timerService,
                              TabListManager tabListManager,
                              Plugin plugin,
                              MessagesConfig messages) {
        this.playerService = playerService;
        this.timerService = timerService;
        this.tabListManager = tabListManager;
        this.plugin = plugin;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        playerService.getOrCreate(event.getPlayer().getUniqueId(), event.getPlayer().getName())
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Error al cargar datos de "
                            + event.getPlayer().getName() + ": " + ex.getMessage());
                    return null;
                });

        tabListManager.refresh(event.getPlayer());

        // Delay by one tick so ScoreboardManager.createBoard() has already run (MONITOR priority)
        // before loadTimersIntoCache fires PlayerTimerStartedDomainEvent.
        Bukkit.getScheduler().runTask(plugin, () ->
                timerService.loadTimersIntoCache(event.getPlayer().getUniqueId())
                        .thenCompose(ignored -> {
                            UUID uuid = event.getPlayer().getUniqueId();
                            if (!timerService.hasActiveTimerSync(uuid, TimerType.PVP_TIMER)) {
                                return timerService.startPvpTimer(uuid, PVP_TIMER_DURATION_MS);
                            }
                            return CompletableFuture.completedFuture(null);
                        })
                        .thenRun(() -> {
                            UUID uuid = event.getPlayer().getUniqueId();
                            if (timerService.hasActiveTimerSync(uuid, TimerType.LOGOUT)) {
                                timerService.cancelTimer(uuid, TimerType.LOGOUT);
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    if (event.getPlayer().isOnline()) {
                                        event.getPlayer().setHealth(0);
                                        event.getPlayer().sendMessage(messages.get("timers.logout-death"));
                                    }
                                });
                            }
                        })
                        .exceptionally(ex -> {
                            plugin.getLogger().warning("Error en carga de timers para "
                                    + event.getPlayer().getName() + ": " + ex.getMessage());
                            return null;
                        }));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        timerService.hasActiveTimer(uuid, TimerType.COMBAT_TAG)
                .thenAccept(hasCombatTag -> {
                    if (hasCombatTag) {
                        timerService.startLogoutTimer(uuid, LOGOUT_TIMER_DURATION_MS);
                    }
                    // Clear in-memory cache so next login starts fresh from Redis
                    timerService.clearCache(uuid);
                });
    }
}

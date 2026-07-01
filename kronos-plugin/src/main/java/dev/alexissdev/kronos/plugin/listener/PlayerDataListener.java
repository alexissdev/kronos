package dev.alexissdev.kronos.plugin.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import dev.alexissdev.kronos.players.service.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;

@Singleton
public class PlayerDataListener implements Listener {

    private static final long PVP_TIMER_DURATION_MS = 30 * 60 * 1000L;
    private static final long LOGOUT_TIMER_DURATION_MS = 30_000L;

    private final PlayerService playerService;
    private final TimerApplicationService timerService;
    private final Plugin plugin;
    private final MessagesConfig messages;

    @Inject
    public PlayerDataListener(PlayerService playerService,
                              TimerApplicationService timerService,
                              Plugin plugin,
                              MessagesConfig messages) {
        this.playerService = playerService;
        this.timerService = timerService;
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

        timerService.loadTimersIntoCache(event.getPlayer().getUniqueId())
                .thenCompose(ignored ->
                        timerService.hasActiveTimer(event.getPlayer().getUniqueId(), TimerType.PVP_TIMER))
                .thenCompose(hasPvpTimer -> {
                    if (!hasPvpTimer) {
                        return timerService.startPvpTimer(event.getPlayer().getUniqueId(), PVP_TIMER_DURATION_MS);
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenCompose(ignored ->
                        timerService.hasActiveTimer(event.getPlayer().getUniqueId(), TimerType.LOGOUT))
                .thenAccept(hasLogoutTimer -> {
                    if (hasLogoutTimer) {
                        timerService.cancelTimer(event.getPlayer().getUniqueId(), TimerType.LOGOUT);
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
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        timerService.hasActiveTimer(event.getPlayer().getUniqueId(), TimerType.COMBAT_TAG)
                .thenAccept(hasCombatTag -> {
                    if (hasCombatTag) {
                        timerService.startLogoutTimer(event.getPlayer().getUniqueId(), LOGOUT_TIMER_DURATION_MS);
                    }
                });
    }
}

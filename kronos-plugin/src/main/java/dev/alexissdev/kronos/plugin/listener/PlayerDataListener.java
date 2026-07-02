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

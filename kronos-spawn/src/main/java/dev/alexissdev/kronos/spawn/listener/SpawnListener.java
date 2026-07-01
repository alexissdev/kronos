package dev.alexissdev.kronos.spawn.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.spawn.domain.SpawnZone;
import dev.alexissdev.kronos.spawn.service.SpawnService;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class SpawnListener implements Listener {

    // Remaining PvP timer millis paused while the player is inside spawn.
    private final ConcurrentHashMap<UUID, Long> pausedPvpMs = new ConcurrentHashMap<>();

    private final SpawnService spawnService;
    private final TimerApplicationService timerService;
    private final JavaPlugin plugin;
    private final MessagesConfig messages;

    @Inject
    public SpawnListener(SpawnService spawnService,
                         TimerApplicationService timerService,
                         JavaPlugin plugin,
                         MessagesConfig messages) {
        this.spawnService = spawnService;
        this.timerService = timerService;
        this.plugin       = plugin;
        this.messages     = messages;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to   = event.getTo();

        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) return;

        Optional<SpawnZone> zoneOpt = spawnService.getZone();
        if (!zoneOpt.isPresent()) return;

        SpawnZone zone    = zoneOpt.get();
        boolean wasInside = zone.contains(from);
        boolean isInside  = zone.contains(to);

        if (!wasInside && isInside) {
            onEnterSpawn(event, event.getPlayer());
        } else if (wasInside && !isInside) {
            onLeaveSpawn(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Restore the paused timer so it continues counting while offline.
        restorePausedTimer(event.getPlayer());
    }

    // ── enter ──────────────────────────────────────────────────────────────

    private void onEnterSpawn(PlayerMoveEvent event, Player player) {
        UUID uuid = player.getUniqueId();

        if (timerService.hasActiveTimerSync(uuid, TimerType.COMBAT_TAG)) {
            event.setCancelled(true);
            player.sendMessage(messages.get("spawn.blocked-combat-tag"));
            return;
        }

        if (timerService.hasActiveTimerSync(uuid, TimerType.PVP_TIMER)) {
            timerService.getRemainingMillis(uuid, TimerType.PVP_TIMER).thenAccept(opt ->
                    opt.ifPresent(remaining -> {
                        pausedPvpMs.put(uuid, remaining);
                        timerService.cancelTimer(uuid, TimerType.PVP_TIMER);
                        Bukkit.getScheduler().runTask(plugin, () ->
                                player.sendMessage(messages.format("spawn.pvp-timer-paused",
                                        "remaining", fmtMs(remaining))));
                    })
            );
        }
    }

    // ── leave ──────────────────────────────────────────────────────────────

    private void onLeaveSpawn(Player player) {
        Long remaining = pausedPvpMs.remove(player.getUniqueId());
        if (remaining == null || remaining <= 0) return;

        timerService.startTimer(player.getUniqueId(), TimerType.PVP_TIMER, remaining);
        player.sendMessage(messages.format("spawn.pvp-timer-resumed", "remaining", fmtMs(remaining)));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private void restorePausedTimer(Player player) {
        Long remaining = pausedPvpMs.remove(player.getUniqueId());
        if (remaining != null && remaining > 0) {
            timerService.startTimer(player.getUniqueId(), TimerType.PVP_TIMER, remaining);
        }
    }

    private static String fmtMs(long ms) {
        long s = ms / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}

package dev.alexissdev.kronos.spawn.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.spawn.domain.SpawnZone;
import dev.alexissdev.kronos.spawn.service.SpawnService;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;
import java.util.UUID;

@Singleton
public class SpawnListener implements Listener {

    private static final String PREFIX = ChatColor.AQUA + "[Spawn] " + ChatColor.RESET;

    private final SpawnService spawnService;
    private final TimerApplicationService timerService;

    @Inject
    public SpawnListener(SpawnService spawnService, TimerApplicationService timerService) {
        this.spawnService = spawnService;
        this.timerService = timerService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to   = event.getTo();

        // Skip if block position didn't change (reduces overhead to ~1/20 the calls)
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) return;

        Optional<SpawnZone> zoneOpt = spawnService.getZone();
        if (!zoneOpt.isPresent()) return;

        SpawnZone zone = zoneOpt.get();
        boolean wasInside = zone.contains(from);
        boolean isInside  = zone.contains(to);

        if (!wasInside && isInside) {
            onEnterSpawn(event, event.getPlayer());
        }
    }

    private void onEnterSpawn(PlayerMoveEvent event, Player player) {
        UUID uuid = player.getUniqueId();

        // Block entry while combat tagged
        if (timerService.hasActiveTimerSync(uuid, TimerType.COMBAT_TAG)) {
            event.setCancelled(true);
            player.sendMessage(PREFIX + ChatColor.RED + "No puedes entrar al spawn estando en "
                    + ChatColor.BOLD + "Combat Tag" + ChatColor.RED + "!");
            return;
        }

        // Cancel PvP timer on spawn entry — player is now in a safe zone
        if (timerService.hasActiveTimerSync(uuid, TimerType.PVP_TIMER)) {
            timerService.cancelTimer(uuid, TimerType.PVP_TIMER);
            player.sendMessage(PREFIX + ChatColor.GREEN + "Tu "
                    + ChatColor.YELLOW + "PvP Timer"
                    + ChatColor.GREEN + " fue cancelado al entrar al spawn.");
        }
    }
}

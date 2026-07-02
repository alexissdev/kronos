package dev.alexissdev.kronos.plugin.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

@Singleton
public class StuckListener implements Listener {

    private final TimerApplicationService timerService;
    private final MessagesConfig messages;

    @Inject
    public StuckListener(TimerApplicationService timerService, MessagesConfig messages) {
        this.timerService = timerService;
        this.messages     = messages;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        if (!timerService.hasActiveTimerSync(player.getUniqueId(), TimerType.STUCK)) return;

        timerService.cancelTimer(player.getUniqueId(), TimerType.STUCK);
        player.sendMessage(messages.get("stuck.cancelled-move"));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!timerService.hasActiveTimerSync(player.getUniqueId(), TimerType.STUCK)) return;

        timerService.cancelTimer(player.getUniqueId(), TimerType.STUCK);
        player.sendMessage(messages.get("stuck.cancelled-damage"));
    }
}

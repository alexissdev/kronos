package dev.alexissdev.kronos.plugin.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import dev.alexissdev.kronos.factions.service.FactionService;
import dev.alexissdev.kronos.players.service.PlayerService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

@Singleton
public class PvpListener implements Listener {

    private final TimerApplicationService timerService;
    private final PlayerService playerService;
    private final FactionService factionService;

    @Inject
    public PvpListener(TimerApplicationService timerService, PlayerService playerService,
                       FactionService factionService) {
        this.timerService = timerService;
        this.playerService = playerService;
        this.factionService = factionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        if (timerService.hasActiveTimerSync(victim.getUniqueId(), TimerType.PVP_TIMER)) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + victim.getName() + " tiene PvP Timer activo.");
            return;
        }

        if (timerService.hasActiveTimerSync(attacker.getUniqueId(), TimerType.PVP_TIMER)) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "No puedes atacar con PvP Timer activo.");
            return;
        }

        timerService.tagForCombat(victim.getUniqueId(), attacker.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        timerService.cancelTimer(victim.getUniqueId(), TimerType.COMBAT_TAG);
        timerService.cancelTimer(victim.getUniqueId(), TimerType.PVP_TIMER);

        if (killer != null) {
            playerService.recordKill(killer.getUniqueId(), victim.getUniqueId());
        }

        factionService.getByPlayer(victim.getUniqueId())
                .thenAccept(opt -> opt.ifPresent(f ->
                        factionService.notifyMemberDeath(f.getId(), victim.getUniqueId())));
    }
}

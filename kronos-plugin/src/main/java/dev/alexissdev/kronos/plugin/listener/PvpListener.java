package dev.alexissdev.kronos.plugin.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.players.repository.DeathbanRepository;
import dev.alexissdev.kronos.players.service.PlayerService;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

@Singleton
public class PvpListener implements Listener {

    private final TimerApplicationService timerService;
    private final PlayerService playerService;
    private final FactionService factionService;
    private final DeathbanRepository deathbanRepository;
    private final MessagesConfig messages;
    private final Plugin plugin;
    private final long deathbanDurationSeconds;
    private final long enderpearlCooldownMs;

    @Inject
    public PvpListener(TimerApplicationService timerService,
                       PlayerService playerService,
                       FactionService factionService,
                       DeathbanRepository deathbanRepository,
                       MessagesConfig messages,
                       Plugin plugin,
                       @Named("hcf.deathban-seconds")   long deathbanDurationSeconds,
                       @Named("enderpearl.cooldown-ms") long enderpearlCooldownMs) {
        this.timerService = timerService;
        this.playerService = playerService;
        this.factionService = factionService;
        this.deathbanRepository = deathbanRepository;
        this.messages = messages;
        this.plugin = plugin;
        this.deathbanDurationSeconds = deathbanDurationSeconds;
        this.enderpearlCooldownMs = enderpearlCooldownMs;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        Player victim   = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        if (timerService.hasActiveTimerSync(victim.getUniqueId(), TimerType.PVP_TIMER)) {
            event.setCancelled(true);
            attacker.sendMessage(messages.format("pvp.target-has-pvp-timer", "player", victim.getName()));
            return;
        }

        if (timerService.hasActiveTimerSync(attacker.getUniqueId(), TimerType.PVP_TIMER)) {
            event.setCancelled(true);
            attacker.sendMessage(messages.get("pvp.attacker-has-pvp-timer"));
            return;
        }

        timerService.tagForCombat(victim.getUniqueId(), attacker.getUniqueId());

        // Cancel pending home teleports for both players
        cancelHomeTeleport(victim);
        cancelHomeTeleport(attacker);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnderpearlThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) event.getEntity().getShooter();
        UUID uuid = player.getUniqueId();

        if (timerService.hasActiveTimerSync(uuid, TimerType.ENDERPEARL)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL)));
            timerService.getRemainingMillis(uuid, TimerType.ENDERPEARL).thenAccept(opt ->
                    opt.ifPresent(ms -> player.sendMessage(
                            messages.format("timers.enderpearl.on-cooldown", "remaining", formatMs(ms)))));
            return;
        }

        timerService.startEnderpearlCooldown(uuid, enderpearlCooldownMs);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        UUID victimUuid = victim.getUniqueId();

        timerService.cancelTimer(victimUuid, TimerType.COMBAT_TAG);
        timerService.cancelTimer(victimUuid, TimerType.PVP_TIMER);
        timerService.cancelTimer(victimUuid, TimerType.HOME);

        if (killer != null) {
            playerService.recordKill(killer.getUniqueId(), victimUuid);
        }

        factionService.getByPlayer(victimUuid)
                .thenAccept(opt -> opt.ifPresent(f ->
                        factionService.notifyMemberDeath(f.getId(), victimUuid)));

        playerService.decrementLives(victimUuid).thenAccept(remainingLives -> {
            if (remainingLives <= 0) {
                deathbanRepository.setDeathban(victimUuid, deathbanDurationSeconds).thenRun(() ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player p = Bukkit.getPlayer(victimUuid);
                            if (p != null) {
                                p.kickPlayer(messages.format("deathban.kick-message",
                                        "time", formatSeconds(deathbanDurationSeconds)));
                            }
                        }));
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(victimUuid);
                    if (p != null) {
                        p.sendMessage(messages.format("deathban.lives-remaining", "lives", remainingLives));
                    }
                });
            }
        });
    }

    private void cancelHomeTeleport(Player player) {
        if (timerService.hasActiveTimerSync(player.getUniqueId(), TimerType.HOME)) {
            timerService.cancelTimer(player.getUniqueId(), TimerType.HOME);
            player.sendMessage(messages.get("faction.cmd.home-cancelled"));
        }
    }

    private static String formatMs(long ms) {
        long totalSecs = ms / 1000;
        long minutes = totalSecs / 60;
        long seconds = totalSecs % 60;
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    private static String formatSeconds(long totalSeconds) {
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0)   return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}

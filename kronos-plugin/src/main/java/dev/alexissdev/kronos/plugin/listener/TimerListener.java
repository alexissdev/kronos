package dev.alexissdev.kronos.plugin.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.event.PlayerCombatTagEvent;
import dev.alexissdev.kronos.api.event.PlayerTimerExpireEvent;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.factions.domain.FactionHome;
import dev.alexissdev.kronos.factions.service.FactionService;
import dev.alexissdev.kronos.spawn.SpawnApplicationService;
import dev.alexissdev.kronos.spawn.domain.SpawnZone;
import dev.alexissdev.kronos.timers.event.PlayerCombatTaggedDomainEvent;
import dev.alexissdev.kronos.timers.event.PlayerTimerExpiredDomainEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

@Singleton
public class TimerListener implements Listener {

    private final Plugin plugin;
    private final EventBus eventBus;
    private final MessagesConfig messages;
    private final FactionService factionService;
    private final SpawnApplicationService spawnService;

    @Inject
    public TimerListener(Plugin plugin, EventBus eventBus, MessagesConfig messages,
                         FactionService factionService, SpawnApplicationService spawnService) {
        this.plugin        = plugin;
        this.eventBus      = eventBus;
        this.messages      = messages;
        this.factionService = factionService;
        this.spawnService  = spawnService;
        this.eventBus.register(this);
    }

    @Subscribe
    public void onCombatTagged(PlayerCombatTaggedDomainEvent domainEvent) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerCombatTagEvent bukkitEvent = new PlayerCombatTagEvent(
                    domainEvent.getTaggedUuid(), domainEvent.getTaggerUuid(), domainEvent.getDurationMillis());
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            if (!bukkitEvent.isCancelled()) {
                Player tagged = Bukkit.getPlayer(domainEvent.getTaggedUuid());
                Player tagger = Bukkit.getPlayer(domainEvent.getTaggerUuid());
                if (tagged != null) tagged.sendMessage(messages.get("timers.combat-tag.tagged"));
                if (tagger != null) tagger.sendMessage(messages.get("timers.combat-tag.tagger"));
            }
        });
    }

    @Subscribe
    public void onTimerExpired(PlayerTimerExpiredDomainEvent domainEvent) {
        switch (domainEvent.getTimerType()) {
            case HOME:
                handleHomeExpiry(domainEvent.getPlayerUuid());
                return;
            case STUCK:
                handleStuckExpiry(domainEvent.getPlayerUuid());
                return;
            default:
                break;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerTimerExpireEvent bukkitEvent = new PlayerTimerExpireEvent(
                    domainEvent.getPlayerUuid(), domainEvent.getTimerType());
            Bukkit.getPluginManager().callEvent(bukkitEvent);

            Player player = Bukkit.getPlayer(domainEvent.getPlayerUuid());
            if (player == null) return;

            switch (domainEvent.getTimerType()) {
                case COMBAT_TAG:
                    player.sendMessage(messages.get("timers.combat-tag.expired"));
                    break;
                case PVP_TIMER:
                    player.sendMessage(messages.get("timers.pvp-timer.expired"));
                    break;
                case ENDERPEARL:
                    player.sendMessage(messages.get("timers.enderpearl.expired"));
                    break;
                default:
                    break;
            }
        });
    }

    private void handleHomeExpiry(UUID playerUuid) {
        factionService.getByPlayer(playerUuid).thenAccept(opt -> {
            if (!opt.isPresent()) return;
            FactionHome home = opt.get().getHome();
            if (home == null) return;
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null) return;
            World world = Bukkit.getWorld(home.getWorld());
            if (world == null) return;
            Location loc = new Location(world, home.getX(), home.getY(), home.getZ(),
                    home.getYaw(), home.getPitch());
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.teleport(loc);
                player.sendMessage(messages.get("faction.cmd.home-arrived"));
            });
        });
    }

    private void handleStuckExpiry(UUID playerUuid) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null) return;
            spawnService.getZone().ifPresentOrElse(zone -> {
                World world = Bukkit.getWorld(zone.getWorld());
                if (world == null) { player.sendMessage(messages.get("stuck.no-spawn")); return; }
                int midX = (zone.getMinX() + zone.getMaxX()) / 2;
                int midZ = (zone.getMinZ() + zone.getMaxZ()) / 2;
                Location spawnLoc = world.getHighestBlockAt(midX, midZ).getLocation().add(0, 1, 0);
                player.teleport(spawnLoc);
                player.sendMessage(messages.get("stuck.teleported"));
            }, () -> player.sendMessage(messages.get("stuck.no-spawn")));
        });
    }
}

package dev.alexissdev.kronos.plugin.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.event.PlayerCombatTagEvent;
import dev.alexissdev.kronos.api.event.PlayerTimerExpireEvent;
import dev.alexissdev.kronos.timers.event.PlayerCombatTaggedDomainEvent;
import dev.alexissdev.kronos.timers.event.PlayerTimerExpiredDomainEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

@Singleton
public class TimerListener implements Listener {

    private final Plugin plugin;
    private final EventBus eventBus;

    @Inject
    public TimerListener(Plugin plugin, EventBus eventBus) {
        this.plugin = plugin;
        this.eventBus = eventBus;
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
                if (tagged != null) tagged.sendMessage(ChatColor.RED + "¡Estás en combate! No puedes desconectarte.");
                if (tagger != null) tagger.sendMessage(ChatColor.RED + "¡Estás en combate!");
            }
        });
    }

    @Subscribe
    public void onTimerExpired(PlayerTimerExpiredDomainEvent domainEvent) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerTimerExpireEvent bukkitEvent = new PlayerTimerExpireEvent(
                    domainEvent.getPlayerUuid(), domainEvent.getTimerType());
            Bukkit.getPluginManager().callEvent(bukkitEvent);

            Player player = Bukkit.getPlayer(domainEvent.getPlayerUuid());
            if (player == null) return;

            switch (domainEvent.getTimerType()) {
                case COMBAT_TAG:
                    player.sendMessage(ChatColor.GREEN + "Ya no estás en combate.");
                    break;
                case PVP_TIMER:
                    player.sendMessage(ChatColor.RED + "Tu PvP Timer expiró. ¡Ya puedes ser atacado!");
                    break;
                case ENDERPEARL:
                    player.sendMessage(ChatColor.GREEN + "Puedes usar enderpearl de nuevo.");
                    break;
                case LOGOUT:
                    break;
                default:
                    break;
            }
        });
    }
}

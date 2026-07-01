package dev.alexissdev.kronos.plugin.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.event.PlayerCombatTagEvent;
import dev.alexissdev.kronos.api.event.PlayerTimerExpireEvent;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.timers.event.PlayerCombatTaggedDomainEvent;
import dev.alexissdev.kronos.timers.event.PlayerTimerExpiredDomainEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

@Singleton
public class TimerListener implements Listener {

    private final Plugin plugin;
    private final EventBus eventBus;
    private final MessagesConfig messages;

    @Inject
    public TimerListener(Plugin plugin, EventBus eventBus, MessagesConfig messages) {
        this.plugin = plugin;
        this.eventBus = eventBus;
        this.messages = messages;
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
}

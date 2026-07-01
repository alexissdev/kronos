package dev.alexissdev.kronos.presentation.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.event.*;
import dev.alexissdev.kronos.api.model.FactionSnapshot;
import dev.alexissdev.kronos.core.event.*;
import dev.alexissdev.kronos.core.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Singleton
public class FactionEventListener implements Listener {

    private final Plugin plugin;
    private final FactionService factionService;
    private final EventBus eventBus;

    @Inject
    public FactionEventListener(Plugin plugin, FactionService factionService, EventBus eventBus) {
        this.plugin = plugin;
        this.factionService = factionService;
        this.eventBus = eventBus;
        this.eventBus.register(this);
    }

    @Subscribe
    public void onFactionCreated(FactionCreatedDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            FactionSnapshot snapshot = new FactionSnapshot(
                    event.getFactionId(), event.getFactionName(), event.getLeaderId(),
                    List.of(event.getLeaderId()), Map.of(event.getLeaderId(), "LEADER"),
                    0, 0, 20, 0.0, java.time.Instant.now()
            );
            FactionCreateEvent bukkitEvent = new FactionCreateEvent(snapshot);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
        });
    }

    @Subscribe
    public void onFactionDisbanded(FactionDisbandedDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            FactionDisbandEvent bukkitEvent = new FactionDisbandEvent(
                    event.getFactionId(), event.getFactionName(), event.getActorUuid());
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            Bukkit.broadcastMessage(ChatColor.GRAY + "[Facción] " +
                    ChatColor.WHITE + event.getFactionName() +
                    ChatColor.GRAY + " fue disuelta.");
        });
    }

    @Subscribe
    public void onPlayerJoined(PlayerJoinedFactionDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerJoinFactionEvent bukkitEvent = new PlayerJoinFactionEvent(
                    event.getPlayerUuid(), event.getFactionId());
            Bukkit.getPluginManager().callEvent(bukkitEvent);
        });
    }

    @Subscribe
    public void onPlayerLeft(PlayerLeftFactionDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerLeaveFactionEvent bukkitEvent = new PlayerLeaveFactionEvent(
                    event.getPlayerUuid(), event.getFactionId(), event.wasKicked());
            Bukkit.getPluginManager().callEvent(bukkitEvent);

            Player player = Bukkit.getPlayer(event.getPlayerUuid());
            if (player != null) {
                String reason = event.wasKicked() ? "expulsado" : "salió";
                factionService.getById(event.getFactionId()).thenAccept(opt ->
                        opt.ifPresent(f -> Bukkit.getScheduler().runTask(plugin, () ->
                                f.getMembers().values().stream()
                                        .map(m -> Bukkit.getPlayer(m.getUuid()))
                                        .filter(p -> p != null)
                                        .forEach(p -> p.sendMessage(ChatColor.YELLOW +
                                                player.getName() + " fue " + reason + " de la facción.")))));
            }
        });
    }

    @Subscribe
    public void onFactionClaimed(FactionClaimedDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            dev.alexissdev.kronos.api.model.ClaimSnapshot snapshot =
                    new dev.alexissdev.kronos.api.model.ClaimSnapshot(
                            event.getClaim().getId(), event.getClaim().getFactionId(),
                            event.getClaim().getType(), event.getClaim().getWorld(),
                            event.getClaim().getMinChunkX(), event.getClaim().getMinChunkZ(),
                            event.getClaim().getMaxChunkX(), event.getClaim().getMaxChunkZ());
            FactionClaimEvent bukkitEvent = new FactionClaimEvent(event.getFactionId(), snapshot);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
        });
    }
}

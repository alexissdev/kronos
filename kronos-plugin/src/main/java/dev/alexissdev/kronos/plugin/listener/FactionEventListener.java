package dev.alexissdev.kronos.plugin.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.event.FactionCreateEvent;
import dev.alexissdev.kronos.api.event.FactionDisbandEvent;
import dev.alexissdev.kronos.api.event.FactionClaimEvent;
import dev.alexissdev.kronos.api.event.PlayerJoinFactionEvent;
import dev.alexissdev.kronos.api.event.PlayerLeaveFactionEvent;
import dev.alexissdev.kronos.api.model.ClaimSnapshot;
import dev.alexissdev.kronos.api.model.FactionSnapshot;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.factions.event.FactionCreatedDomainEvent;
import dev.alexissdev.kronos.factions.event.FactionDisbandedDomainEvent;
import dev.alexissdev.kronos.factions.event.FactionDtkDecrementedDomainEvent;
import dev.alexissdev.kronos.factions.event.FactionRaidableDomainEvent;
import dev.alexissdev.kronos.factions.event.PlayerJoinedFactionDomainEvent;
import dev.alexissdev.kronos.factions.event.PlayerLeftFactionDomainEvent;
import dev.alexissdev.kronos.factions.event.FactionClaimedDomainEvent;
import dev.alexissdev.kronos.claims.domain.ClaimType;
import dev.alexissdev.kronos.factions.service.FactionService;
import dev.alexissdev.kronos.plugin.tablist.TabListManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
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
    private final MessagesConfig messages;
    private final TabListManager tabListManager;

    @Inject
    public FactionEventListener(Plugin plugin, FactionService factionService,
                                 EventBus eventBus, MessagesConfig messages,
                                 TabListManager tabListManager) {
        this.plugin = plugin;
        this.factionService = factionService;
        this.eventBus = eventBus;
        this.messages = messages;
        this.tabListManager = tabListManager;
        this.eventBus.register(this);
    }

    @Subscribe
    public void onFactionCreated(FactionCreatedDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            FactionSnapshot snapshot = new FactionSnapshot(
                    event.getFactionId(), event.getFactionName(), event.getLeaderId(),
                    List.of(event.getLeaderId()), Map.of(event.getLeaderId(), "LEADER"),
                    0, 0, 20, 0.0, Instant.now()
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
            String msg = messages.format("faction.broadcast.disbanded", "name", event.getFactionName());
            for (Player online : Bukkit.getOnlinePlayers()) online.sendMessage(msg);
            // Reset tab names for all online players (former members get no-faction format)
            tabListManager.refreshAll();
        });
    }

    @Subscribe
    public void onPlayerJoined(PlayerJoinedFactionDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerJoinFactionEvent bukkitEvent = new PlayerJoinFactionEvent(
                    event.getPlayerUuid(), event.getFactionId());
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            tabListManager.refresh(event.getPlayerUuid());
        });
    }

    @Subscribe
    public void onPlayerLeft(PlayerLeftFactionDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerLeaveFactionEvent bukkitEvent = new PlayerLeaveFactionEvent(
                    event.getPlayerUuid(), event.getFactionId(), event.wasKicked());
            Bukkit.getPluginManager().callEvent(bukkitEvent);

            tabListManager.refresh(event.getPlayerUuid());

            Player player = Bukkit.getPlayer(event.getPlayerUuid());
            if (player != null) {
                String reason = event.wasKicked() ? "expulsado" : "salió";
                factionService.getById(event.getFactionId()).thenAccept(opt ->
                        opt.ifPresent(f -> Bukkit.getScheduler().runTask(plugin, () ->
                                f.getMembers().values().stream()
                                        .map(m -> Bukkit.getPlayer(m.getUuid()))
                                        .filter(p -> p != null)
                                        .forEach(p -> p.sendMessage(messages.format(
                                                "faction.member.left",
                                                "player", player.getName(),
                                                "reason", reason))))));
            }
        });
    }

    @Subscribe
    public void onDtkDecremented(FactionDtkDecrementedDomainEvent event) {
        factionService.getById(event.getFactionId()).thenAccept(opt ->
                opt.ifPresent(f -> Bukkit.getScheduler().runTask(plugin, () -> {
                    String msg = messages.format("faction.dtk-decremented",
                            "faction", event.getFactionName(),
                            "dtk",     String.valueOf(event.getNewDtk()),
                            "max",     String.valueOf(event.getMaxDtk()));
                    for (UUID memberUuid : f.getMembers().keySet()) {
                        Player member = Bukkit.getPlayer(memberUuid);
                        if (member != null) member.sendMessage(msg);
                    }
                })));
    }

    @Subscribe
    public void onFactionRaidable(FactionRaidableDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String msg = messages.format("faction.broadcast.raidable", "name", event.getFactionName());
            for (Player online : Bukkit.getOnlinePlayers()) online.sendMessage(msg);
        });
    }

    @Subscribe
    public void onFactionClaimed(FactionClaimedDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            ClaimSnapshot snapshot = new ClaimSnapshot(
                            event.getClaimId(), event.getFactionId(),
                            ClaimType.valueOf(event.getClaimType()), event.getWorld(),
                            event.getMinChunkX(), event.getMinChunkZ(),
                            event.getMaxChunkX(), event.getMaxChunkZ());
            FactionClaimEvent bukkitEvent = new FactionClaimEvent(event.getFactionId(), snapshot);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
        });
    }
}

package dev.alexissdev.kronos.presentation.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.core.domain.Claim;
import dev.alexissdev.kronos.core.domain.ClaimType;
import dev.alexissdev.kronos.core.event.*;
import dev.alexissdev.kronos.core.service.ClaimService;
import dev.alexissdev.kronos.core.service.FactionService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ClaimListener implements Listener {

    private final ClaimService claimService;
    private final FactionService factionService;
    private final Plugin plugin;
    private final EventBus eventBus;

    private final ConcurrentHashMap<String, Claim> claimCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> playerFactionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> playerChunkCache = new ConcurrentHashMap<>();

    @Inject
    public ClaimListener(ClaimService claimService, FactionService factionService,
                         Plugin plugin, EventBus eventBus) {
        this.claimService = claimService;
        this.factionService = factionService;
        this.plugin = plugin;
        this.eventBus = eventBus;
        this.eventBus.register(this);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("hcf.bypass.claimprotection")) return;

        Claim claim = claimCache.get(chunkKey(
                event.getBlock().getWorld().getName(),
                event.getBlock().getChunk().getX(),
                event.getBlock().getChunk().getZ()));

        if (canModify(player, claim)) return;

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "No puedes destruir bloques aquí.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("hcf.bypass.claimprotection")) return;

        Claim claim = claimCache.get(chunkKey(
                event.getBlock().getWorld().getName(),
                event.getBlock().getChunk().getX(),
                event.getBlock().getChunk().getZ()));

        if (canModify(player, claim)) return;

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "No puedes colocar bloques aquí.");
    }

    private boolean canModify(Player player, Claim claim) {
        if (claim == null) return true;
        if (!claim.getType().isProtectedFromBuild()) return true;
        if (claim.getType() != ClaimType.FACTION) return false;
        String playerFaction = playerFactionMap.get(player.getUniqueId());
        return claim.getFactionId().equals(playerFaction);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!crossedChunkBorder(event)) return;

        Player player = event.getPlayer();
        String newKey = chunkKey(event.getTo().getWorld().getName(),
                event.getTo().getChunk().getX(),
                event.getTo().getChunk().getZ());

        String previousKey = playerChunkCache.put(player.getUniqueId(), newKey);
        if (Objects.equals(previousKey, newKey)) return;

        Claim newClaim = claimCache.get(newKey);
        if (newClaim != null) {
            notifyClaimEntry(player, newClaim);
        } else if (previousKey != null && claimCache.containsKey(previousKey)) {
            player.sendMessage(ChatColor.YELLOW + "Entraste a " + ChatColor.WHITE + "Tierra de Nadie");
        }
    }

    private void notifyClaimEntry(Player player, Claim claim) {
        if (claim.getType() == ClaimType.FACTION) {
            factionService.getById(claim.getFactionId()).thenAccept(opt ->
                    opt.ifPresent(f -> plugin.getServer().getScheduler().runTask(plugin, () ->
                            player.sendMessage(ChatColor.YELLOW + "Entraste al territorio de " +
                                    ChatColor.WHITE + f.getName()))));
        } else {
            player.sendMessage(ChatColor.YELLOW + "Entraste a " + ChatColor.WHITE + claim.getType().name());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        factionService.getByPlayer(event.getPlayer().getUniqueId())
                .thenAccept(opt -> opt.ifPresent(f ->
                        playerFactionMap.put(event.getPlayer().getUniqueId(), f.getId())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        playerFactionMap.remove(event.getPlayer().getUniqueId());
        playerChunkCache.remove(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onClaimed(FactionClaimedDomainEvent event) {
        Claim claim = event.getClaim();
        for (int x = claim.getMinChunkX(); x <= claim.getMaxChunkX(); x++) {
            for (int z = claim.getMinChunkZ(); z <= claim.getMaxChunkZ(); z++) {
                claimCache.put(chunkKey(claim.getWorld(), x, z), claim);
            }
        }
    }

    @Subscribe
    public void onDisbanded(FactionDisbandedDomainEvent event) {
        claimCache.entrySet().removeIf(e ->
                event.getFactionId().equals(e.getValue().getFactionId()));
    }

    @Subscribe
    public void onPlayerJoined(PlayerJoinedFactionDomainEvent event) {
        playerFactionMap.put(event.getPlayerUuid(), event.getFactionId());
    }

    @Subscribe
    public void onPlayerLeft(PlayerLeftFactionDomainEvent event) {
        playerFactionMap.remove(event.getPlayerUuid());
    }

    public void preloadCache() {
        claimService.getAllClaims().thenAccept(claims -> {
            for (Claim claim : claims) {
                for (int x = claim.getMinChunkX(); x <= claim.getMaxChunkX(); x++) {
                    for (int z = claim.getMinChunkZ(); z <= claim.getMaxChunkZ(); z++) {
                        claimCache.put(chunkKey(claim.getWorld(), x, z), claim);
                    }
                }
            }
            plugin.getLogger().info("Cargados " + claims.size() + " claims en caché.");
        }).exceptionally(ex -> {
            plugin.getLogger().warning("Error al precargar claims: " + ex.getMessage());
            return null;
        });
    }

    private boolean crossedChunkBorder(PlayerMoveEvent event) {
        return event.getFrom().getChunk().getX() != event.getTo().getChunk().getX()
                || event.getFrom().getChunk().getZ() != event.getTo().getChunk().getZ();
    }

    private String chunkKey(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + ":" + chunkZ;
    }
}

package dev.alexissdev.kronos.claims.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.claims.domain.Claim;
import dev.alexissdev.kronos.claims.domain.ClaimType;
import dev.alexissdev.kronos.claims.service.ClaimService;
import dev.alexissdev.kronos.common.domain.SotwService;
import dev.alexissdev.kronos.factions.event.FactionClaimedDomainEvent;
import dev.alexissdev.kronos.factions.event.FactionDisbandedDomainEvent;
import dev.alexissdev.kronos.factions.event.FactionRaidableDomainEvent;
import dev.alexissdev.kronos.factions.event.PlayerJoinedFactionDomainEvent;
import dev.alexissdev.kronos.factions.event.PlayerLeftFactionDomainEvent;
import dev.alexissdev.kronos.factions.service.FactionService;
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
import java.util.Map;

/**
 * Central listener for the claims subsystem in the HCF plugin.
 *
 * <p>Combines a Bukkit {@link Listener} with a Guava EventBus subscriber to fulfil
 * two main responsibilities:</p>
 * <ol>
 *   <li><strong>Block protection:</strong> cancels block-break and block-place events
 *       when the player does not have permission over the territory.</li>
 *   <li><strong>Movement notification:</strong> informs the player when they cross a
 *       claim boundary, showing the territory name or the owning faction's name.</li>
 * </ol>
 *
 * <p>Maintains three in-memory caches to avoid frequent queries to MongoDB:</p>
 * <ul>
 *   <li>{@code claimCache}: maps {@code "world:chunkX:chunkZ"} → {@link Claim}.</li>
 *   <li>{@code playerFactionMap}: maps player UUID → their current faction ID.</li>
 *   <li>{@code playerChunkCache}: maps player UUID → the key of the last visited chunk.</li>
 * </ul>
 *
 * <p>Also listens to domain events from the EventBus (faction claimed, faction disbanded,
 * player joined/left faction, faction became raidable) to keep the caches updated in
 * real time without requiring manual reloads.</p>
 */
@Singleton
public class ClaimListener implements Listener {

    private final ClaimService claimService;
    private final FactionService factionService;
    private final SotwService sotwService;
    private final Plugin plugin;
    private final EventBus eventBus;

    private final Map<String, Claim>  claimCache        = new ConcurrentHashMap<>();
    private final Map<UUID, String>   playerFactionMap  = new ConcurrentHashMap<>();
    private final Map<UUID, String>   playerChunkCache  = new ConcurrentHashMap<>();
    private final Set<String>         raidableFactions  = ConcurrentHashMap.newKeySet();

    /**
     * Constructs the listener by injecting its dependencies and registers it on the Guava EventBus.
     *
     * @param claimService   claim application service used for territory queries
     * @param factionService faction service used to resolve faction names in notifications
     * @param sotwService    SOTW/EOTW state service; when EOTW is active, building is allowed everywhere
     * @param plugin         main plugin instance, required for scheduling tasks on the server thread
     * @param eventBus       Guava event bus where {@link Subscribe} handlers are registered
     */
    @Inject
    public ClaimListener(ClaimService claimService, FactionService factionService,
                         SotwService sotwService, Plugin plugin, EventBus eventBus) {
        this.claimService = claimService;
        this.factionService = factionService;
        this.sotwService = sotwService;
        this.plugin = plugin;
        this.eventBus = eventBus;
        this.eventBus.register(this);
    }

    /**
     * Cancels block-breaking in protected territories when the player is not authorised.
     *
     * <p>Operators and players with the {@code hcf.bypass.claimprotection} permission may
     * break blocks in any territory. In {@link ClaimType#FACTION} claims, only members of
     * the owning faction or factions currently raiding it can modify the territory, unless
     * EOTW is active.</p>
     *
     * @param event Bukkit block-break event
     */
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

    /**
     * Cancels block-placing in protected territories when the player is not authorised.
     *
     * <p>Applies the same rules as {@link #onBlockBreak(BlockBreakEvent)}: operators and
     * players with the bypass permission may place blocks anywhere.</p>
     *
     * @param event Bukkit block-place event
     */
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

    /**
     * Marks a faction as raidable in the local cache upon receiving the corresponding domain event.
     *
     * <p>Once a faction is raidable, any player — including enemies — may break and place
     * blocks within that faction's territory, simulating a raid in progress.</p>
     *
     * @param event domain event indicating that the faction has entered a raidable state
     */
    @Subscribe
    public void onFactionRaidable(FactionRaidableDomainEvent event) {
        raidableFactions.add(event.getFactionId());
    }

    private boolean canModify(Player player, Claim claim) {
        if (claim == null) return true;
        if (!claim.getType().isProtectedFromBuild()) return true;
        if (sotwService.isEotwActive()) return true;
        if (claim.getType() != ClaimType.FACTION) return false;
        if (raidableFactions.contains(claim.getFactionId())) return true;
        String playerFaction = playerFactionMap.get(player.getUniqueId());
        return claim.getFactionId().equals(playerFaction);
    }

    /**
     * Detects when a player crosses a chunk boundary and notifies them of the territory they entered.
     *
     * <p>To minimise performance impact, the event is only processed when the player has
     * actually crossed a chunk border (change in chunk X or Z). When the player enters a
     * claimed territory, {@link #notifyClaimEntry(Player, Claim)} is called; when they leave
     * one for open land, the corresponding message is displayed.</p>
     *
     * @param event Bukkit player-move event
     */
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

    /**
     * Sends the player a welcome message upon entering a territory.
     *
     * <p>If the claim is of type {@link ClaimType#FACTION}, the faction name is resolved
     * asynchronously and displayed on the server's main thread. For any other claim type
     * (KOTH, WARZONE, SAFEZONE, etc.) the type name is shown directly.</p>
     *
     * @param player the player who entered the territory
     * @param claim  the claim the player has entered
     */
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

    /**
     * Loads the player's faction ID into the local cache when they connect to the server.
     *
     * <p>Allows block-permission checks in block events to be instant without needing
     * to query the database on every action.</p>
     *
     * @param event player join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        factionService.getByPlayer(event.getPlayer().getUniqueId())
                .thenAccept(opt -> opt.ifPresent(f ->
                        playerFactionMap.put(event.getPlayer().getUniqueId(), f.getId())));
    }

    /**
     * Removes the player's data from both caches when they disconnect from the server.
     *
     * <p>Prevents memory leaks by cleaning up entries in the faction map and the chunk
     * cache for the player whose session has ended.</p>
     *
     * @param event player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        playerFactionMap.remove(event.getPlayer().getUniqueId());
        playerChunkCache.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Updates the in-memory claim cache when a new territory is registered.
     *
     * <p>Upon receiving a {@link FactionClaimedDomainEvent}, rebuilds the {@link Claim}
     * entity and inserts it into the {@code claimCache} for every chunk covered by the
     * new territory. This ensures that permission checks and movement notifications work
     * without hitting MongoDB.</p>
     *
     * @param event domain event describing the newly claimed territory
     */
    @Subscribe
    public void onClaimed(FactionClaimedDomainEvent event) {
        ClaimType type;
        try {
            type = ClaimType.valueOf(event.getClaimType());
        } catch (IllegalArgumentException e) {
            type = ClaimType.FACTION;
        }
        Claim claim = new Claim(event.getClaimId(), event.getFactionId(), type, event.getWorld(),
                event.getMinChunkX(), event.getMinChunkZ(), event.getMaxChunkX(), event.getMaxChunkZ());
        for (int x = claim.getMinChunkX(); x <= claim.getMaxChunkX(); x++) {
            for (int z = claim.getMinChunkZ(); z <= claim.getMaxChunkZ(); z++) {
                claimCache.put(chunkKey(claim.getWorld(), x, z), claim);
            }
        }
    }

    /**
     * Removes all claims belonging to a disbanded faction from the in-memory cache.
     *
     * <p>Without this cleanup, the cached chunks would continue to be treated as territory
     * of the disbanded faction, preventing other players from claiming them in real time.</p>
     *
     * @param event domain event indicating which faction was disbanded
     */
    @Subscribe
    public void onDisbanded(FactionDisbandedDomainEvent event) {
        claimCache.entrySet().removeIf(e ->
                event.getFactionId().equals(e.getValue().getFactionId()));
    }

    /**
     * Records a player's faction in the local cache when they join a faction.
     *
     * <p>Ensures that claim ownership checks are correct for players who change factions
     * without reconnecting to the server.</p>
     *
     * @param event domain event containing the player's UUID and their new faction ID
     */
    @Subscribe
    public void onPlayerJoined(PlayerJoinedFactionDomainEvent event) {
        playerFactionMap.put(event.getPlayerUuid(), event.getFactionId());
    }

    /**
     * Removes a player's faction association from the local cache when they leave their faction.
     *
     * <p>After this cleanup, the player is treated as faction-less in territory permission
     * checks until they join a new faction.</p>
     *
     * @param event domain event containing the UUID of the player who left the faction
     */
    @Subscribe
    public void onPlayerLeft(PlayerLeftFactionDomainEvent event) {
        playerFactionMap.remove(event.getPlayerUuid());
    }

    /**
     * Preloads all claims and raidable factions from MongoDB into the in-memory caches.
     *
     * <p>Should be called exactly once during plugin initialisation, after Guice has
     * constructed the listener. Queries are executed asynchronously and results are stored
     * in their respective caches. Errors are logged to the server log without interrupting
     * the startup sequence.</p>
     */
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

        factionService.getRaidableFactions().thenAccept(factions ->
                factions.forEach(f -> raidableFactions.add(f.getId()))
        ).exceptionally(ex -> {
            plugin.getLogger().warning("Error al cargar facciones raidable: " + ex.getMessage());
            return null;
        });
    }

    /**
     * Determines whether the player has crossed the boundary between two different chunks.
     *
     * <p>Used as a fast filter in {@link #onMove(PlayerMoveEvent)} to discard movement
     * within the same chunk without processing any additional logic.</p>
     *
     * @param event the player-move event to evaluate
     * @return {@code true} if the source and destination chunks are different
     */
    private boolean crossedChunkBorder(PlayerMoveEvent event) {
        return event.getFrom().getChunk().getX() != event.getTo().getChunk().getX()
                || event.getFrom().getChunk().getZ() != event.getTo().getChunk().getZ();
    }

    /**
     * Generates the composite key used to index claims in the in-memory cache.
     *
     * <p>The format is {@code "world:chunkX:chunkZ"}, which guarantees global uniqueness
     * across worlds that share identical chunk coordinates.</p>
     *
     * @param world  Minecraft world name
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return cache key in the format {@code "world:X:Z"}
     */
    private String chunkKey(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + ":" + chunkZ;
    }
}

package dev.alexissdev.kronos.classes.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.factions.service.FactionService;
import dev.alexissdev.kronos.players.domain.KitType;
import dev.alexissdev.kronos.timers.domain.TimerType;
import dev.alexissdev.kronos.classes.service.KitService;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Bukkit listener that applies the passive and active abilities of all HCF classes.
 *
 * <p>Manages the full class lifecycle throughout each player's session:</p>
 * <ul>
 *   <li>Detects the active class on login and automatically updates it when the
 *       player changes their helmet (the helmet type determines the class).</li>
 *   <li>Applies passive combat effects based on the attacker's class.</li>
 *   <li>Executes the active ability on right-clicking a {@code BLAZE_ROD},
 *       respecting the cooldown managed by {@link dev.alexissdev.kronos.timers.TimerApplicationService}.</li>
 *   <li>Runs the Bard's periodic aura to grant effects to nearby allies.</li>
 * </ul>
 *
 * <p>Maintains an in-memory cache ({@code playerKitCache}) to avoid asynchronous database
 * queries during high-frequency events such as combat.</p>
 *
 * <p>Registered as a singleton by Guice through {@link dev.alexissdev.kronos.classes.ClassesModule}.</p>
 */
@Singleton
public class ClassListener implements Listener {

    private final KitService kitService;
    private final TimerApplicationService timerService;
    private final FactionService factionService;
    private final Plugin plugin;

    /** Cache mapping each online player's UUID to their currently active class. */
    private final Map<UUID, KitType> playerKitCache = new ConcurrentHashMap<>();

    /**
     * Constructs the listener by injecting its dependencies and starts the Bard's periodic aura.
     *
     * @param kitService     kit service used to detect and persist the active kit
     * @param timerService   timer service used to manage active-ability cooldowns
     * @param factionService faction service used to identify allies within the Bard aura
     * @param plugin         main plugin instance, required for scheduling Bukkit tasks
     */
    @Inject
    public ClassListener(KitService kitService, TimerApplicationService timerService,
                         FactionService factionService, Plugin plugin) {
        this.kitService = kitService;
        this.timerService = timerService;
        this.factionService = factionService;
        this.plugin = plugin;
        scheduleBardAura();
    }

    /**
     * Schedules the Bard's periodic aura task, which runs every 2 seconds (40 ticks).
     *
     * <p>For each online player whose active class is {@link KitType#BARD}, grants Speed II
     * and Regeneration I to the Bard and to all members of their faction within a 15-block
     * radius. The query runs asynchronously and effects are applied on the Bukkit main thread.</p>
     */
    private void scheduleBardAura() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, KitType> entry : playerKitCache.entrySet()) {
                    if (entry.getValue() != KitType.BARD) continue;
                    org.bukkit.entity.Player bard = plugin.getServer().getPlayer(entry.getKey());
                    if (bard == null || !bard.isOnline()) continue;

                    List<Player> nearby = bard.getNearbyEntities(15, 15, 15).stream()
                            .filter(e -> e instanceof Player)
                            .map(e -> (Player) e)
                            .collect(Collectors.toList());

                    factionService.getByPlayer(bard.getUniqueId()).thenAccept(bardFactionOpt -> {
                        java.util.Set<UUID> teammates = new java.util.HashSet<>();
                        bardFactionOpt.ifPresent(f -> teammates.addAll(f.getMembers().keySet()));

                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false);
                            PotionEffect regen = new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false);
                            if (bard.isOnline()) {
                                bard.addPotionEffect(speed, true);
                                bard.addPotionEffect(regen, true);
                            }
                            for (Player near : nearby) {
                                if (!near.isOnline()) continue;
                                if (!teammates.isEmpty() && teammates.contains(near.getUniqueId())) {
                                    near.addPotionEffect(speed, true);
                                    near.addPotionEffect(regen, true);
                                }
                            }
                        });
                    });
                }
            }
        }.runTaskTimerAsynchronously(plugin, 40L, 40L);
    }

    /**
     * Loads the player's active class into the local cache when they connect to the server.
     *
     * <p>The load is asynchronous to avoid blocking the main thread. If the player has no
     * kit assigned in their profile, no entry is added to the cache for them.</p>
     *
     * @param event player join event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        kitService.detectKit(event.getPlayer().getUniqueId())
                .thenAccept(opt -> opt.ifPresent(kit ->
                        playerKitCache.put(event.getPlayer().getUniqueId(), kit)));
    }

    /**
     * Removes the player's class from the local cache when they disconnect.
     *
     * <p>Prevents memory leaks by ensuring that no stale entries remain in the
     * session cache.</p>
     *
     * @param event player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        playerKitCache.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Detects class changes when the player interacts with helmet-related inventory slots.
     *
     * <p>Listens for inventory clicks that may affect the helmet slot (armour slot or any
     * movement involving a helmet item). Schedules the class update for the next server
     * tick so that the equipment change is already reflected in the inventory.</p>
     *
     * @param event Bukkit inventory-click event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean helmetInvolved = event.getSlotType() == InventoryType.SlotType.ARMOR
                || (current != null && isHelmetMaterial(current.getType()))
                || (cursor != null && isHelmetMaterial(cursor.getType()));

        if (!helmetInvolved) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> updateKit(player));
    }

    /**
     * Increases the velocity of arrows shot by players with the {@link KitType#ARCHER} class.
     *
     * <p>Multiplies the arrow's velocity vector by {@code 1.3}, increasing its range and
     * making it harder to dodge. Only affects arrows, not other bow projectiles.</p>
     *
     * @param event Bukkit bow-shoot event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player archer = (Player) event.getEntity();

        if (playerKitCache.getOrDefault(archer.getUniqueId(), KitType.DIAMOND) != KitType.ARCHER) return;
        if (!(event.getProjectile() instanceof Arrow)) return;

        Arrow arrow = (Arrow) event.getProjectile();
        arrow.setVelocity(arrow.getVelocity().multiply(1.3));
    }

    /**
     * Applies passive combat effects based on the attacking player's class.
     *
     * <p>Only processes events where both the attacker and the victim are players.
     * Delegates the actual effect application to {@link #applyPassiveEffect(Player, Player, KitType)}.</p>
     *
     * @param event Bukkit entity-damage-by-entity event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        KitType kit = playerKitCache.getOrDefault(attacker.getUniqueId(), KitType.DIAMOND);
        applyPassiveEffect(attacker, victim, kit);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (playerKitCache.getOrDefault(player.getUniqueId(), KitType.DIAMOND) != KitType.MINER) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 80, 1, false, false), true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType() != Material.BLAZE_ROD) return;
        Player player = event.getPlayer();

        if (timerService.hasActiveTimerSync(player.getUniqueId(), TimerType.CLASS_COOLDOWN)) {
            player.sendMessage(ChatColor.RED + "Tu habilidad está en cooldown.");
            return;
        }

        KitType kit = playerKitCache.getOrDefault(player.getUniqueId(), KitType.DIAMOND);
        if (kit == KitType.DIAMOND) return;

        kitService.activateClassAbility(player.getUniqueId(), kit);
        activateAbility(player, kit);
    }

    private void updateKit(Player player) {
        if (!player.isOnline()) return;
        KitType detected = detectKitFromHelmet(player.getInventory().getHelmet());
        KitType previous = playerKitCache.get(player.getUniqueId());
        if (detected == previous) return;
        playerKitCache.put(player.getUniqueId(), detected);
        kitService.updateActiveKit(player.getUniqueId(), detected);
        player.sendMessage(ChatColor.GOLD + "Clase activa: " + detected.name());
    }

    private boolean isHelmetMaterial(Material material) {
        switch (material) {
            case LEATHER_HELMET:
            case GOLD_HELMET:
            case CHAINMAIL_HELMET:
            case IRON_HELMET:
            case DIAMOND_HELMET:
                return true;
            default:
                return false;
        }
    }

    private KitType detectKitFromHelmet(ItemStack helmet) {
        if (helmet == null || helmet.getType() == Material.AIR) return KitType.DIAMOND;
        switch (helmet.getType()) {
            case LEATHER_HELMET:   return KitType.ARCHER;
            case GOLD_HELMET:      return KitType.BARD;
            case CHAINMAIL_HELMET: return KitType.ROGUE;
            case IRON_HELMET:      return KitType.MINER;
            case DIAMOND_HELMET:   return KitType.KNIGHT;
            default:               return KitType.DIAMOND;
        }
    }

    private void applyPassiveEffect(Player attacker, Player victim, KitType kit) {
        switch (kit) {
            case ARCHER:
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1, false, false));
                break;
            case ROGUE:
                attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false));
                break;
            case KNIGHT:
                attacker.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, 0, false, false));
                break;
            default:
                break;
        }
    }

    private void activateAbility(Player player, KitType kit) {
        switch (kit) {
            case ARCHER:
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 100, 1, false, false));
                break;
            case BARD:
                player.getNearbyEntities(15, 15, 15).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .forEach(p -> p.addPotionEffect(
                                new PotionEffect(PotionEffectType.SPEED, 200, 1, false, false)));
                break;
            case ROGUE:
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false));
                break;
            case KNIGHT:
                player.getNearbyEntities(5, 5, 5).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .forEach(p -> p.setVelocity(
                                p.getLocation().toVector()
                                        .subtract(player.getLocation().toVector())
                                        .normalize().multiply(2)));
                break;
            case MINER:
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 300, 1, false, false));
                break;
            default:
                break;
        }
        player.sendMessage(ChatColor.GOLD + "¡Habilidad activada!");
    }
}

package dev.alexissdev.kronos.classes.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ClassListener implements Listener {

    private final KitService kitService;
    private final TimerApplicationService timerService;
    private final Plugin plugin;

    private final ConcurrentHashMap<UUID, KitType> playerKitCache = new ConcurrentHashMap<>();

    @Inject
    public ClassListener(KitService kitService, TimerApplicationService timerService, Plugin plugin) {
        this.kitService = kitService;
        this.timerService = timerService;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        kitService.detectKit(event.getPlayer().getUniqueId())
                .thenAccept(opt -> opt.ifPresent(kit ->
                        playerKitCache.put(event.getPlayer().getUniqueId(), kit)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        playerKitCache.remove(event.getPlayer().getUniqueId());
    }

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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player archer = (Player) event.getEntity();

        if (playerKitCache.getOrDefault(archer.getUniqueId(), KitType.DIAMOND) != KitType.ARCHER) return;
        if (!(event.getProjectile() instanceof Arrow)) return;

        Arrow arrow = (Arrow) event.getProjectile();
        arrow.setVelocity(arrow.getVelocity().multiply(1.3));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        KitType kit = playerKitCache.getOrDefault(attacker.getUniqueId(), KitType.DIAMOND);
        applyPassiveEffect(attacker, victim, kit);
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
        player.sendTitle("", ChatColor.GOLD + "Clase: " + detected.name(), 5, 30, 10);
    }

    private boolean isHelmetMaterial(Material material) {
        switch (material) {
            case LEATHER_HELMET:
            case GOLDEN_HELMET:
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
            case GOLDEN_HELMET:    return KitType.BARD;
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
        player.sendTitle("", ChatColor.GOLD + "¡Habilidad activada!", 5, 30, 10);
    }
}

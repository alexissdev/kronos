package dev.alexissdev.kronos.plugin.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.players.domain.CrateLocation;
import dev.alexissdev.kronos.players.inventory.CrateInventory;
import dev.alexissdev.kronos.players.service.CrateService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class CrateListener implements Listener {

    private static final String LORE_PREFIX = ChatColor.DARK_GRAY + "crate:";

    // key: "world:x:y:z" → CrateType
    private final Map<String, CrateType> crateCache = new ConcurrentHashMap<>();

    private final CrateService crateService;
    private final CrateInventory crateInventory;
    private final MessagesConfig messages;
    private final Plugin plugin;

    @Inject
    public CrateListener(CrateService crateService, CrateInventory crateInventory,
                         MessagesConfig messages, Plugin plugin) {
        this.crateService   = crateService;
        this.crateInventory = crateInventory;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    public void loadCrates() {
        crateService.getAllCrates().thenAccept(list -> {
            crateCache.clear();
            for (CrateLocation loc : list) {
                crateCache.put(locationKey(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()), loc.getType());
            }
            plugin.getLogger().info("Cargados " + list.size() + " crates en caché.");
        });
    }

    public void registerCrate(CrateLocation loc) {
        crateCache.put(locationKey(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()), loc.getType());
    }

    public void unregisterCrate(String world, int x, int y, int z) {
        crateCache.remove(locationKey(world, x, y, z));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        String key = locationKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        CrateType crateType = crateCache.get(key);
        if (crateType == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        ItemStack held = player.getItemInHand();
        CrateType keyType = getCrateType(held);

        if (keyType == null) {
            player.sendMessage(messages.format("crate.need-key", "type", crateType.name()));
            return;
        }

        if (keyType != crateType) {
            player.sendMessage(messages.format("crate.wrong-key",
                    "required", crateType.name(), "held", keyType.name()));
            return;
        }

        // Consume one key
        if (held.getAmount() > 1) {
            held.setAmount(held.getAmount() - 1);
        } else {
            player.setItemInHand(null);
        }

        crateInventory.openCrateAnimation(player, crateType);
    }

    public static ItemStack createKey(CrateType type) {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Llave de Crate: " + ChatColor.YELLOW + type.name());
        meta.setLore(Arrays.asList(LORE_PREFIX + type.name()));
        item.setItemMeta(meta);
        return item;
    }

    private static CrateType getCrateType(ItemStack item) {
        if (item == null || item.getType() != Material.TRIPWIRE_HOOK) return null;
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            if (line.startsWith(LORE_PREFIX)) {
                try {
                    return CrateType.valueOf(line.substring(LORE_PREFIX.length()));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return null;
    }

    private static String locationKey(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }
}

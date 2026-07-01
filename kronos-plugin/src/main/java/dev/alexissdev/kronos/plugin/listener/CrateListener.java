package dev.alexissdev.kronos.plugin.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.players.inventory.CrateInventory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

@Singleton
public class CrateListener implements Listener {

    private static final String LORE_PREFIX = ChatColor.DARK_GRAY + "crate:";

    private final CrateInventory crateInventory;

    @Inject
    public CrateListener(CrateInventory crateInventory) {
        this.crateInventory = crateInventory;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        CrateType type = getCrateType(item);
        if (type == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        item.setAmount(item.getAmount() - 1);
        crateInventory.openCrateAnimation(player, type);
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
}

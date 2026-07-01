package dev.alexissdev.kronos.players.inventory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.domain.CrateType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

@Singleton
public class CrateInventory {

    private static final int SIZE = 27;

    private final Plugin plugin;

    @Inject
    public CrateInventory(Plugin plugin) {
        this.plugin = plugin;
    }

    public void openCrateAnimation(Player player, CrateType crateType) {
        Inventory inv = Bukkit.createInventory(null, SIZE,
                ChatColor.GOLD + "Crate: " + crateType.name());

        fillBorderWithGlass(inv);
        startSpinAnimation(player, inv, crateType);
    }

    private void fillBorderWithGlass(Inventory inv) {
        // data value 7 = gray in 1.8.8 stained glass pane
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_GRAY + " ");
        glass.setItemMeta(meta);
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, glass);
        }
    }

    private void startSpinAnimation(Player player, Inventory inv, CrateType crateType) {
        player.openInventory(inv);
        final int maxTicks = 30;

        new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || tick >= maxTicks) {
                    cancel();
                    if (player.isOnline()) {
                        ItemStack prize = selectPrize(crateType);
                        inv.setItem(13, prize);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.closeInventory();
                            player.getInventory().addItem(prize);
                            player.sendMessage(ChatColor.GOLD + "¡Obtuviste: " +
                                    ChatColor.WHITE + prize.getItemMeta().getDisplayName() + ChatColor.GOLD + "!");
                        }, 40L);
                    }
                    return;
                }
                randomizeMiddleRow(inv, crateType);
                tick++;
            }
        }.runTaskTimer(plugin, 5L, 3L);
    }

    private void randomizeMiddleRow(Inventory inv, CrateType crateType) {
        int[] middleSlots = {9, 10, 11, 12, 13, 14, 15, 16, 17};
        for (int slot : middleSlots) {
            inv.setItem(slot, selectPrize(crateType));
        }
    }

    private ItemStack selectPrize(CrateType crateType) {
        List<ItemStack> prizes = getPrizesFor(crateType);
        return prizes.get(new Random().nextInt(prizes.size()));
    }

    private List<ItemStack> getPrizesFor(CrateType crateType) {
        List<ItemStack> prizes = new ArrayList<>();
        switch (crateType) {
            case KOTH:
                prizes.add(createItem(Material.DIAMOND_SWORD, ChatColor.AQUA + "Espada KOTH"));
                prizes.add(createItem(Material.GOLDEN_APPLE, ChatColor.GOLD + "Manzana Dorada"));
                prizes.add(createItem(Material.DIAMOND, ChatColor.AQUA + "x5 Diamantes"));
                prizes.add(createItem(Material.EXP_BOTTLE, ChatColor.GREEN + "XP Bottle"));
                break;
            case VOTE:
                prizes.add(createItem(Material.IRON_INGOT, ChatColor.GRAY + "x10 Hierro"));
                prizes.add(createItem(Material.GOLD_INGOT, ChatColor.GOLD + "x5 Oro"));
                prizes.add(createItem(Material.DIAMOND, ChatColor.AQUA + "x2 Diamantes"));
                prizes.add(createItem(Material.EMERALD, ChatColor.GREEN + "x3 Esmeraldas"));
                break;
            case RANK:
                prizes.add(createItem(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "Rango VIP"));
                prizes.add(createItem(Material.DIAMOND_CHESTPLATE, ChatColor.AQUA + "Pechera Diamante"));
                break;
            case EVENT:
                prizes.add(createItem(Material.NETHER_STAR, ChatColor.YELLOW + "Estrella del Nether"));
                prizes.add(createItem(Material.ENCHANTED_BOOK, ChatColor.LIGHT_PURPLE + "Libro Encantado"));
                prizes.add(createItem(Material.IRON_CHESTPLATE, ChatColor.WHITE + "Pechera de Hierro"));
                break;
        }
        return prizes;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

}

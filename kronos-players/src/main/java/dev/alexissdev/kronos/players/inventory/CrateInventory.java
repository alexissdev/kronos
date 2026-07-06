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

/**
 * GUI component that manages the crate-opening animation using a 27-slot (3-row)
 * Bukkit inventory.
 *
 * <p>When a player opens a crate they are shown an inventory with a grey stained-glass
 * border and a spinning animation in the middle row that runs for 30 ticks. Once the
 * animation ends, a random prize is selected from the crate type's reward table,
 * delivered to the player's inventory, and announced in chat.</p>
 *
 * <p>The animation runs asynchronously via the Bukkit task scheduler ({@link BukkitRunnable})
 * to avoid blocking the main server thread. This class is a Guice-managed singleton and
 * is part of the {@link dev.alexissdev.kronos.players.PlayersModule} module.</p>
 */
@Singleton
public class CrateInventory {

    private static final int SIZE = 27;

    private final Plugin plugin;

    /**
     * Creates the crate inventory handler with the plugin instance injected by Guice.
     * The plugin reference is required to schedule tasks on the Bukkit scheduler.
     *
     * @param plugin main Bukkit plugin instance
     */
    @Inject
    public CrateInventory(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the crate animation for the specified player.
     *
     * <p>Creates a 27-slot inventory titled with the crate type name, fills the border
     * with stained-glass panes, and starts the spin animation in the middle row. After
     * 30 ticks the final prize is selected and delivered to the player.</p>
     *
     * @param player    the Bukkit player who is opening the crate and will see the animation
     * @param crateType the type of crate, which determines the reward table to use
     */
    public void openCrateAnimation(Player player, CrateType crateType) {
        Inventory inv = Bukkit.createInventory(null, SIZE,
                ChatColor.GOLD + "Crate: " + crateType.name());

        fillBorderWithGlass(inv);
        startSpinAnimation(player, inv, crateType);
    }

    /**
     * Fills every slot in the inventory with grey stained-glass panes to create the
     * decorative border surrounding the central animation row.
     *
     * @param inv Bukkit inventory to fill with the glass border
     */
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

    /**
     * Starts the spin animation that cycles random prizes through the middle row of the inventory.
     *
     * <p>Schedules a {@link BukkitRunnable} that rotates random prizes every 3 ticks for a
     * maximum of 30 ticks. If the player disconnects during the animation it is cancelled without
     * awarding any prize. Once finished, the winning prize is displayed in the centre slot
     * (slot 13) and delivered to the player's inventory 40 ticks later.</p>
     *
     * @param player    the player watching the animation
     * @param inv       the inventory in which the animation plays
     * @param crateType the crate type used to select prizes from the correct reward table
     */
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

    /**
     * Fills the 9 slots of the inventory's middle row with random prizes,
     * creating the visual roulette effect during the spin animation.
     *
     * @param inv       inventory whose middle slots are being updated
     * @param crateType crate type used to determine which prizes to display during animation
     */
    private void randomizeMiddleRow(Inventory inv, CrateType crateType) {
        int[] middleSlots = {9, 10, 11, 12, 13, 14, 15, 16, 17};
        for (int slot : middleSlots) {
            inv.setItem(slot, selectPrize(crateType));
        }
    }

    /**
     * Randomly selects a prize from the reward table of the given crate type.
     *
     * @param crateType crate type whose reward table will be used for the selection
     * @return a Bukkit ItemStack randomly chosen as the prize
     */
    private ItemStack selectPrize(CrateType crateType) {
        List<ItemStack> prizes = getPrizesFor(crateType);
        return prizes.get(new Random().nextInt(prizes.size()));
    }

    /**
     * Builds and returns the possible prize pool for the specified crate type.
     *
     * <p>Each crate type has its own themed item selection: KOTH awards high-tier combat
     * items, VOTE awards basic resources, RANK awards special rank items, and EVENT awards
     * exclusive event-only items.</p>
     *
     * @param crateType crate type whose prize pool should be retrieved
     * @return list of possible prize items for this crate type
     */
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

    /**
     * Creates a Bukkit ItemStack with the given material and display name.
     *
     * @param material material of the item to create
     * @param name     display name for the item (may include Bukkit ChatColor codes)
     * @return Bukkit ItemStack with its metadata configured
     */
    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

}

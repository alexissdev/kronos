package dev.alexissdev.kronos.factions.inventory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.domain.FactionMember;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Manages the faction GUI displayed to players in a 54-slot Bukkit inventory.
 *
 * <p>The menu presents general faction information (members, kills, DTK, balance)
 * and shows each member represented by their player head, role, and online status.
 *
 * <p>This class operates in two asynchronous phases:
 * <ol>
 *   <li>Queries the player's faction asynchronously via {@link FactionService}.</li>
 *   <li>Builds and opens the inventory on the Bukkit main thread via
 *       {@code Bukkit.getScheduler().runTask(...)}.</li>
 * </ol>
 *
 * <p>Registered as a {@link Singleton} in the Guice container through
 * {@link dev.alexissdev.kronos.factions.FactionsModule}.
 */
@Singleton
public class FactionInventory {

    private final FactionService factionService;

    /**
     * Constructs the inventory component with the faction service needed
     * to query the player's data.
     *
     * @param factionService faction service injected by Guice
     */
    @Inject
    public FactionInventory(FactionService factionService) {
        this.factionService = factionService;
    }

    /**
     * Opens the faction menu for the given player.
     *
     * <p>Asynchronously queries the faction the player belongs to.
     * If the player is not in any faction, an error message is sent to them
     * on the main thread. Otherwise, the inventory is opened with their faction's information.
     *
     * @param player the player for whom the faction menu will be opened
     */
    public void openFactionMenu(Player player) {
        factionService.getByPlayer(player.getUniqueId()).thenAccept(opt -> {
            if (opt.isEmpty()) {
                Bukkit.getScheduler().runTask(
                        Bukkit.getPluginManager().getPlugin("KronosHCF"),
                        () -> player.sendMessage(ChatColor.RED + "No estás en ninguna facción."));
                return;
            }
            Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("KronosHCF"),
                    () -> openInventory(player, opt.get()));
        });
    }

    /**
     * Builds and opens the Bukkit inventory populated with the given faction's information.
     *
     * <p>Must always be called from the server main thread to avoid concurrency
     * exceptions with the Bukkit API.
     *
     * @param player  the player for whom the inventory will be opened
     * @param faction the faction whose data will be displayed in the GUI
     */
    private void openInventory(Player player, Faction faction) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.DARK_GREEN + "" + ChatColor.BOLD + faction.getName());

        inv.setItem(0, createInfoItem(faction));
        inv.setItem(8, createTopItem());

        int slot = 9;
        for (Map.Entry<UUID, FactionMember> entry : faction.getMembers().entrySet()) {
            if (slot >= 45) break;
            inv.setItem(slot++, createMemberItem(entry.getKey(), entry.getValue()));
        }

        for (int i = 45; i < 54; i++) {
            inv.setItem(i, createBorder());
        }

        player.openInventory(inv);
    }

    /**
     * Creates the general faction information item placed in slot 0.
     *
     * <p>Displays member count, total kills, remaining DTK, and balance in the item lore.
     *
     * @param faction the faction from which the data is extracted
     * @return a paper item with the faction's information as lore
     */
    private ItemStack createInfoItem(Faction faction) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Información de " + faction.getName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Miembros: " + ChatColor.WHITE + faction.getMembers().size(),
                ChatColor.GRAY + "Kills: " + ChatColor.WHITE + faction.getKills(),
                ChatColor.GRAY + "DTK: " + ChatColor.WHITE + faction.getDtkRemaining() + "/" + faction.getMaxDtk(),
                ChatColor.GRAY + "Balance: " + ChatColor.WHITE + "$" + String.format("%.2f", faction.getBalance())
        ));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the decorative "View Top Factions" item placed in slot 8.
     *
     * <p>Serves as a visual shortcut to the server's faction ranking.
     * Click logic must be handled in the corresponding inventory listener.
     *
     * @return a gold ingot item with the ranking access label
     */
    private ItemStack createTopItem() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Ver Top Facciones");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMemberItem(UUID uuid, FactionMember member) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        Player online = Bukkit.getPlayer(uuid);
        String name = online != null ? online.getName() : uuid.toString().substring(0, 8);
        meta.setDisplayName((online != null ? ChatColor.GREEN : ChatColor.RED) + name);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Rol: " + ChatColor.YELLOW + member.getRole().name(),
                ChatColor.GRAY + "Estado: " + (online != null ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline")
        ));
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack createBorder() {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_GRAY + " ");
        item.setItemMeta(meta);
        return item;
    }
}

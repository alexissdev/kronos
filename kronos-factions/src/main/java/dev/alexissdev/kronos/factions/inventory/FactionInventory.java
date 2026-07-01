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

@Singleton
public class FactionInventory {

    private final FactionService factionService;

    @Inject
    public FactionInventory(FactionService factionService) {
        this.factionService = factionService;
    }

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

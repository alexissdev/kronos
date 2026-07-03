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
 * Gestiona la interfaz gráfica (GUI) de facción que se muestra a los jugadores
 * en un inventario de Bukkit de 54 slots.
 *
 * <p>El menú presenta información general de la facción (miembros, kills, DTK, balance)
 * y muestra a cada miembro representado con su cabeza de jugador, rol y estado de conexión.
 *
 * <p>Esta clase opera en dos fases asíncronas:
 * <ol>
 *   <li>Consulta la facción del jugador de forma asíncrona mediante {@link FactionService}.</li>
 *   <li>Construye y abre el inventario en el hilo principal de Bukkit mediante
 *       {@code Bukkit.getScheduler().runTask(...)}.</li>
 * </ol>
 *
 * <p>Registrada como {@link Singleton} en el contenedor Guice a través de
 * {@link dev.alexissdev.kronos.factions.FactionsModule}.
 */
@Singleton
public class FactionInventory {

    private final FactionService factionService;

    /**
     * Construye el componente de inventario con el servicio de facciones necesario
     * para consultar los datos del jugador.
     *
     * @param factionService servicio de facciones inyectado por Guice
     */
    @Inject
    public FactionInventory(FactionService factionService) {
        this.factionService = factionService;
    }

    /**
     * Abre el menú de facción para el jugador dado.
     *
     * <p>Consulta de forma asíncrona la facción a la que pertenece el jugador.
     * Si el jugador no está en ninguna facción, le envía un mensaje de error
     * en el hilo principal. De lo contrario, abre el inventario con la información
     * de su facción.
     *
     * @param player jugador al que se le abrirá el menú de facción
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
     * Construye y abre el inventario Bukkit con la información de la facción dada.
     *
     * <p>Debe llamarse siempre desde el hilo principal del servidor para evitar
     * excepciones de concurrencia con la API de Bukkit.
     *
     * @param player  jugador al que se abrirá el inventario
     * @param faction facción cuyos datos se mostrarán en el GUI
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
     * Crea el ítem de información general de la facción (slot 0).
     *
     * <p>Muestra en el lore el número de miembros, kills totales, DTK restante y balance.
     *
     * @param faction facción de la que se extraen los datos
     * @return ítem de papel con la información de la facción como lore
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
     * Crea el ítem decorativo "Ver Top Facciones" ubicado en el slot 8.
     *
     * <p>Sirve como acceso rápido visual al ranking de facciones del servidor.
     * La lógica de click debe manejarse en el listener de inventario correspondiente.
     *
     * @return ítem de lingote de oro con el texto de acceso al ranking
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

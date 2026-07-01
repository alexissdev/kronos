package dev.alexissdev.kronos.spawn.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.spawn.creation.SpawnCreationService;
import dev.alexissdev.kronos.spawn.creation.SpawnCreationSession;
import dev.alexissdev.kronos.spawn.domain.SpawnZone;
import dev.alexissdev.kronos.spawn.service.SpawnService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

@Singleton
public class SpawnWandListener implements Listener {

    private static final String PREFIX = ChatColor.AQUA + "[Spawn] " + ChatColor.RESET;

    private final SpawnCreationService creationService;
    private final SpawnService spawnService;
    private final JavaPlugin plugin;

    @Inject
    public SpawnWandListener(SpawnCreationService creationService,
                              SpawnService spawnService,
                              JavaPlugin plugin) {
        this.creationService = creationService;
        this.spawnService    = spawnService;
        this.plugin          = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (!creationService.isWand(hand)) return;

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        event.setCancelled(true);

        Optional<SpawnCreationSession> sessionOpt = creationService.getSession(player.getUniqueId());
        if (!sessionOpt.isPresent()) {
            player.sendMessage(PREFIX + ChatColor.RED + "No tienes sesión activa. Usa /spawn setzone.");
            return;
        }

        SpawnCreationSession session = sessionOpt.get();
        int x = block.getX();
        int z = block.getZ();
        boolean isPos1 = action == Action.LEFT_CLICK_BLOCK;

        if (isPos1) {
            session.setPos1(block.getWorld().getName(), x, z);
            player.sendMessage(PREFIX + ChatColor.GREEN + "Pos 1: " + ChatColor.WHITE + x + ", " + z);
        } else {
            if (!session.hasPos1()) {
                player.sendMessage(PREFIX + ChatColor.RED + "Primero establece la Pos 1 (clic izquierdo).");
                return;
            }
            session.setPos2(x, z);
            player.sendMessage(PREFIX + ChatColor.YELLOW + "Pos 2: " + ChatColor.WHITE + x + ", " + z);
            finalizeZone(player, session);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        creationService.cancelSession(event.getPlayer().getUniqueId());
    }

    private void finalizeZone(Player player, SpawnCreationSession session) {
        SpawnZone zone = session.build();

        spawnService.setZone(zone)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    removeWand(player);
                    creationService.cancelSession(player.getUniqueId());

                    player.sendMessage(PREFIX + ChatColor.GREEN + "¡Zona de Spawn configurada!");
                    player.sendMessage(ChatColor.GRAY + "  Mundo: " + ChatColor.WHITE + zone.getWorld());
                    player.sendMessage(ChatColor.GRAY + "  Esquinas: "
                            + ChatColor.WHITE + "(" + zone.getMinX() + ", " + zone.getMinZ() + ")"
                            + ChatColor.GRAY + " → "
                            + ChatColor.WHITE + "(" + zone.getMaxX() + ", " + zone.getMaxZ() + ")");
                }))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.sendMessage(PREFIX + ChatColor.RED + "Error al guardar: " + ex.getMessage()));
                    return null;
                });
    }

    private void removeWand(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (creationService.isWand(contents[i])) {
                player.getInventory().setItem(i, null);
                return;
            }
        }
    }
}

package dev.alexissdev.kronos.spawn.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.spawn.creation.SpawnCreationService;
import dev.alexissdev.kronos.spawn.creation.SpawnCreationSession;
import dev.alexissdev.kronos.spawn.domain.SpawnZone;
import dev.alexissdev.kronos.spawn.service.SpawnService;
import org.bukkit.Bukkit;
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

    private final SpawnCreationService creationService;
    private final SpawnService spawnService;
    private final JavaPlugin plugin;
    private final MessagesConfig messages;

    @Inject
    public SpawnWandListener(SpawnCreationService creationService,
                              SpawnService spawnService,
                              JavaPlugin plugin,
                              MessagesConfig messages) {
        this.creationService = creationService;
        this.spawnService    = spawnService;
        this.plugin          = plugin;
        this.messages        = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInHand();

        if (!creationService.isWand(hand)) return;

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        event.setCancelled(true);

        Optional<SpawnCreationSession> sessionOpt = creationService.getSession(player.getUniqueId());
        if (!sessionOpt.isPresent()) {
            player.sendMessage(messages.get("spawn.wand.no-session"));
            return;
        }

        SpawnCreationSession session = sessionOpt.get();
        int x = block.getX();
        int z = block.getZ();
        boolean isPos1 = action == Action.LEFT_CLICK_BLOCK;

        if (isPos1) {
            session.setPos1(block.getWorld().getName(), x, z);
            player.sendMessage(messages.format("spawn.wand.pos1-set", "x", x, "z", z));
        } else {
            if (!session.hasPos1()) {
                player.sendMessage(messages.get("spawn.wand.need-pos1"));
                return;
            }
            session.setPos2(x, z);
            player.sendMessage(messages.format("spawn.wand.pos2-set", "x", x, "z", z));
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

                    player.sendMessage(messages.get("spawn.wand.zone-set"));
                    player.sendMessage(messages.format("spawn.wand.zone-world", "world", zone.getWorld()));
                    player.sendMessage(messages.format("spawn.wand.zone-corners",
                            "minX", zone.getMinX(), "minZ", zone.getMinZ(),
                            "maxX", zone.getMaxX(), "maxZ", zone.getMaxZ()));
                }))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.sendMessage(messages.format("spawn.wand.save-error",
                                    "error", ex.getMessage())));
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

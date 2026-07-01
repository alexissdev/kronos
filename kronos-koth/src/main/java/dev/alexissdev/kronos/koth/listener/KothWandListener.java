package dev.alexissdev.kronos.koth.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.koth.creation.KothCreationService;
import dev.alexissdev.kronos.koth.creation.KothCreationSession;
import dev.alexissdev.kronos.koth.domain.KothZone;
import dev.alexissdev.kronos.koth.service.KothService;
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
public class KothWandListener implements Listener {

    private final KothCreationService creationService;
    private final KothService kothService;
    private final JavaPlugin plugin;
    private final MessagesConfig messages;

    @Inject
    public KothWandListener(KothCreationService creationService,
                             KothService kothService,
                             JavaPlugin plugin,
                             MessagesConfig messages) {
        this.creationService = creationService;
        this.kothService = kothService;
        this.plugin = plugin;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (!creationService.isWand(hand)) return;

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        event.setCancelled(true);

        Optional<KothCreationSession> sessionOpt = creationService.getSession(player.getUniqueId());
        if (!sessionOpt.isPresent()) {
            player.sendMessage(messages.get("koth.wand.no-session"));
            return;
        }

        KothCreationSession session = sessionOpt.get();
        int x = block.getX();
        int z = block.getZ();
        boolean isPos1 = action == Action.LEFT_CLICK_BLOCK;

        if (session.getPhase() == KothCreationSession.Phase.CLAIM) {
            handleClaimSelection(player, session, x, z, block.getWorld().getName(), isPos1);
        } else {
            handleCaptureSelection(player, session, x, z, isPos1);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        creationService.cancelSession(event.getPlayer().getUniqueId());
    }

    // ── selection phases ──────────────────────────────────────────────────

    private void handleClaimSelection(Player player, KothCreationSession session,
                                       int x, int z, String world, boolean isPos1) {
        if (isPos1) {
            session.setClaimPos1(world, x, z);
            player.sendMessage(messages.format("koth.wand.claim-pos1", "x", x, "z", z));
        } else {
            if (!session.hasClaimPos1()) {
                player.sendMessage(messages.get("koth.wand.need-pos1"));
                return;
            }
            session.setClaimPos2(x, z);
            player.sendMessage(messages.format("koth.wand.claim-pos2", "x", x, "z", z));
            player.sendMessage(messages.get("koth.wand.claim-done"));
            player.sendMessage(messages.get("koth.wand.phase-controls"));
            session.advanceToCapture();
        }
    }

    private void handleCaptureSelection(Player player, KothCreationSession session,
                                         int x, int z, boolean isPos1) {
        if (isPos1) {
            session.setCapturePos1(x, z);
            player.sendMessage(messages.format("koth.wand.capture-pos1", "x", x, "z", z));
        } else {
            if (!session.hasCapturePos1()) {
                player.sendMessage(messages.get("koth.wand.need-pos1"));
                return;
            }
            session.setCapturePos2(x, z);
            player.sendMessage(messages.format("koth.wand.capture-pos2", "x", x, "z", z));
            finalizeKoth(player, session);
        }
    }

    private void finalizeKoth(Player player, KothCreationSession session) {
        KothZone zone = session.build();

        kothService.createKoth(zone)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    removeWand(player);
                    creationService.cancelSession(player.getUniqueId());

                    player.sendMessage(messages.format("koth.wand.created", "name", zone.getName()));
                    player.sendMessage(messages.format("koth.wand.created-claim",
                            "minX", zone.getMinX(), "minZ", zone.getMinZ(),
                            "maxX", zone.getMaxX(), "maxZ", zone.getMaxZ()));
                    player.sendMessage(messages.format("koth.wand.created-capture",
                            "minX", zone.getCaptureMinX(), "minZ", zone.getCaptureMinZ(),
                            "maxX", zone.getCaptureMaxX(), "maxZ", zone.getCaptureMaxZ()));
                    player.sendMessage(messages.format("koth.wand.created-time",
                            "seconds", zone.getCaptureTimeSeconds()));
                }))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.sendMessage(messages.format("koth.wand.create-error",
                                    "error", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage())));
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

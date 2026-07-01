package dev.alexissdev.kronos.koth.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.koth.creation.KothCreationService;
import dev.alexissdev.kronos.koth.creation.KothCreationSession;
import dev.alexissdev.kronos.koth.domain.KothZone;
import dev.alexissdev.kronos.koth.service.KothService;
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
public class KothWandListener implements Listener {

    private static final String PREFIX = ChatColor.GOLD + "[KOTH] " + ChatColor.RESET;

    private final KothCreationService creationService;
    private final KothService kothService;
    private final JavaPlugin plugin;

    @Inject
    public KothWandListener(KothCreationService creationService,
                             KothService kothService,
                             JavaPlugin plugin) {
        this.creationService = creationService;
        this.kothService = kothService;
        this.plugin = plugin;
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
            player.sendMessage(PREFIX + ChatColor.RED + "No tienes una sesión activa. Usa /koth create.");
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
            player.sendMessage(PREFIX + ChatColor.GREEN + "Claim Pos 1: "
                    + ChatColor.WHITE + x + ", " + z);
        } else {
            if (!session.hasClaimPos1()) {
                player.sendMessage(PREFIX + ChatColor.RED + "Primero establece la Pos 1 (clic izquierdo).");
                return;
            }
            session.setClaimPos2(x, z);
            player.sendMessage(PREFIX + ChatColor.YELLOW + "Claim Pos 2: "
                    + ChatColor.WHITE + x + ", " + z);
            player.sendMessage(PREFIX + ChatColor.GOLD + "¡Claim zone lista! "
                    + ChatColor.GRAY + "Ahora selecciona la "
                    + ChatColor.RED + "zona de captura" + ChatColor.GRAY + ".");
            player.sendMessage(ChatColor.GRAY + "  Clic izquierdo → " + ChatColor.GREEN + "Pos 1"
                    + ChatColor.GRAY + "  │  Clic derecho → " + ChatColor.YELLOW + "Pos 2");
            session.advanceToCapture();
        }
    }

    private void handleCaptureSelection(Player player, KothCreationSession session,
                                         int x, int z, boolean isPos1) {
        if (isPos1) {
            session.setCapturePos1(x, z);
            player.sendMessage(PREFIX + ChatColor.GREEN + "Captura Pos 1: "
                    + ChatColor.WHITE + x + ", " + z);
        } else {
            if (!session.hasCapturePos1()) {
                player.sendMessage(PREFIX + ChatColor.RED + "Primero establece la Pos 1 (clic izquierdo).");
                return;
            }
            session.setCapturePos2(x, z);
            player.sendMessage(PREFIX + ChatColor.YELLOW + "Captura Pos 2: "
                    + ChatColor.WHITE + x + ", " + z);
            finalizeKoth(player, session);
        }
    }

    private void finalizeKoth(Player player, KothCreationSession session) {
        KothZone zone = session.build();

        kothService.createKoth(zone)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    removeWand(player);
                    creationService.cancelSession(player.getUniqueId());

                    player.sendMessage(PREFIX + ChatColor.GREEN + "¡KOTH "
                            + ChatColor.YELLOW + zone.getName()
                            + ChatColor.GREEN + " creado con éxito!");
                    player.sendMessage(ChatColor.GRAY + "  Claim: "
                            + ChatColor.WHITE + "(" + zone.getMinX() + ", " + zone.getMinZ() + ")"
                            + ChatColor.GRAY + " → "
                            + ChatColor.WHITE + "(" + zone.getMaxX() + ", " + zone.getMaxZ() + ")");
                    player.sendMessage(ChatColor.GRAY + "  Captura: "
                            + ChatColor.WHITE + "(" + zone.getCaptureMinX() + ", " + zone.getCaptureMinZ() + ")"
                            + ChatColor.GRAY + " → "
                            + ChatColor.WHITE + "(" + zone.getCaptureMaxX() + ", " + zone.getCaptureMaxZ() + ")");
                    player.sendMessage(ChatColor.GRAY + "  Tiempo: "
                            + ChatColor.WHITE + zone.getCaptureTimeSeconds() + "s");
                }))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.sendMessage(PREFIX + ChatColor.RED + "Error al crear KOTH: "
                                    + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage())));
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

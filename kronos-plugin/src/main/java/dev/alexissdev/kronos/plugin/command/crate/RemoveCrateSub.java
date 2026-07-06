package dev.alexissdev.kronos.plugin.command.crate;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.players.service.CrateService;
import dev.alexissdev.kronos.plugin.listener.CrateListener;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;

/**
 * Sub-command {@code /crate remove} that deletes the reward crate located at the
 * block the executor is looking at (up to a maximum of 5 blocks away). After
 * removing the database record it also unregisters the crate from the active
 * listener so it stops being processed in real time.
 */
@Singleton
public class RemoveCrateSub extends SubCommand {

    private final CrateService   crateService;
    private final CrateListener  crateListener;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Constructs the sub-command by injecting its dependencies via Guice.
     *
     * @param crateService  service used to remove crates from the persistence layer
     * @param crateListener crate listener used to unregister the crate from real-time processing
     * @param messages      localised message configuration
     * @param plugin        plugin instance used to schedule tasks on the main thread
     */
    @Inject
    public RemoveCrateSub(CrateService crateService, CrateListener crateListener,
                          MessagesConfig messages, Plugin plugin) {
        this.crateService  = crateService;
        this.crateListener = crateListener;
        this.messages      = messages;
        this.plugin        = plugin;
    }

    /** @return the sub-command name: {@code "remove"} */
    @Override public String name() { return "remove"; }

    /**
     * Retrieves the block the player is looking at (up to 5 blocks away), removes
     * the registered crate at that position, and unregisters it from the listener.
     * On failure, the executor is notified with the corresponding error message.
     *
     * @param sender command executor; must be a {@link org.bukkit.entity.Player}
     * @param args   additional arguments (not used by this sub-command)
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        Block target = player.getTargetBlock((HashSet<Byte>) null, 5);
        if (target == null) { player.sendMessage(messages.get("crate.cmd.no-target")); return; }

        String world = target.getWorld().getName();
        int x = target.getX(), y = target.getY(), z = target.getZ();

        crateService.removeCrate(world, x, y, z)
                .thenRun(() -> {
                    crateListener.unregisterCrate(world, x, y, z);
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(messages.get("crate.cmd.removed")));
                }).exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin, () ->
                player.sendMessage(messages.format("crate.cmd.error", "error", ex.getMessage()))); return null; });
    }
}

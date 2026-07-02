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

@Singleton
public class RemoveCrateSub extends SubCommand {

    private final CrateService   crateService;
    private final CrateListener  crateListener;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    @Inject
    public RemoveCrateSub(CrateService crateService, CrateListener crateListener,
                          MessagesConfig messages, Plugin plugin) {
        this.crateService  = crateService;
        this.crateListener = crateListener;
        this.messages      = messages;
        this.plugin        = plugin;
    }

    @Override public String name() { return "remove"; }

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

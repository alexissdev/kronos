package dev.alexissdev.kronos.plugin.command.crate;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.players.service.CrateService;
import dev.alexissdev.kronos.plugin.listener.CrateListener;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class SetCrateSub extends SubCommand {

    private final CrateService   crateService;
    private final CrateListener  crateListener;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    @Inject
    public SetCrateSub(CrateService crateService, CrateListener crateListener,
                       MessagesConfig messages, Plugin plugin) {
        this.crateService  = crateService;
        this.crateListener = crateListener;
        this.messages      = messages;
        this.plugin        = plugin;
    }

    @Override public String name() { return "set"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2
                ? filterPrefix(Arrays.stream(CrateType.values()).map(CrateType::name).collect(Collectors.toList()), args[1])
                : List.of();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (!requireArgs(player, args, 2, "/crate set <tipo>")) return;

        CrateType type;
        try { type = CrateType.valueOf(args[1].toUpperCase()); }
        catch (IllegalArgumentException e) {
            String valid = Arrays.stream(CrateType.values()).map(CrateType::name).collect(Collectors.joining(", "));
            player.sendMessage(messages.format("crate.cmd.invalid-type", "valid", valid)); return;
        }

        Block target = player.getTargetBlock((HashSet<Byte>) null, 5);
        if (target == null) { player.sendMessage(messages.get("crate.cmd.no-target")); return; }

        CrateType finalType = type;
        crateService.setCrate(target.getWorld().getName(), target.getX(), target.getY(), target.getZ(), type)
                .thenAccept(loc -> {
                    crateListener.registerCrate(loc);
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.sendMessage(messages.format("crate.cmd.set",
                                    "type", finalType.name(),
                                    "x", String.valueOf(target.getX()),
                                    "y", String.valueOf(target.getY()),
                                    "z", String.valueOf(target.getZ()))));
                }).exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin, () ->
                player.sendMessage(messages.format("crate.cmd.error", "error", ex.getMessage()))); return null; });
    }
}

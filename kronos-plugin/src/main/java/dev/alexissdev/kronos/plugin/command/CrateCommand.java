package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.players.domain.CrateLocation;
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
public class CrateCommand extends BaseCommand {

    private final CrateService   crateService;
    private final CrateListener  crateListener;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    @Inject
    public CrateCommand(CrateService crateService, CrateListener crateListener,
                        MessagesConfig messages, Plugin plugin) {
        super("hcf.admin");
        this.crateService  = crateService;
        this.crateListener = crateListener;
        this.messages      = messages;
        this.plugin        = plugin;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        if (args.length == 0) { sendHelp(player); return; }

        switch (args[0].toLowerCase()) {
            case "set":    handleSet(player, args); break;
            case "remove": handleRemove(player);    break;
            case "list":   handleList(player);      break;
            default:       sendHelp(player);        break;
        }
    }

    private void handleSet(Player player, String[] args) {
        if (!requireArgs(player, args, 2, "/crate set <tipo>")) return;

        CrateType type;
        try {
            type = CrateType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            String valid = Arrays.stream(CrateType.values())
                    .map(CrateType::name).collect(Collectors.joining(", "));
            player.sendMessage(messages.format("crate.cmd.invalid-type", "valid", valid));
            return;
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
                                    "x",    String.valueOf(target.getX()),
                                    "y",    String.valueOf(target.getY()),
                                    "z",    String.valueOf(target.getZ()))));
                }).exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.sendMessage(messages.format("crate.cmd.error", "error", ex.getMessage())));
                    return null;
                });
    }

    private void handleRemove(Player player) {
        Block target = player.getTargetBlock((HashSet<Byte>) null, 5);
        if (target == null) { player.sendMessage(messages.get("crate.cmd.no-target")); return; }

        String world = target.getWorld().getName();
        int x = target.getX(), y = target.getY(), z = target.getZ();

        crateService.removeCrate(world, x, y, z)
                .thenRun(() -> {
                    crateListener.unregisterCrate(world, x, y, z);
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.sendMessage(messages.get("crate.cmd.removed")));
                }).exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.sendMessage(messages.format("crate.cmd.error", "error", ex.getMessage())));
                    return null;
                });
    }

    private void handleList(Player player) {
        crateService.getAllCrates().thenAccept(list -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (list.isEmpty()) { player.sendMessage(messages.get("crate.cmd.list-empty")); return; }
            player.sendMessage(messages.get("crate.cmd.list-header"));
            for (CrateLocation loc : list) {
                player.sendMessage(messages.format("crate.cmd.list-entry",
                        "type",  loc.getType().name(),
                        "world", loc.getWorld(),
                        "x",     String.valueOf(loc.getX()),
                        "y",     String.valueOf(loc.getY()),
                        "z",     String.valueOf(loc.getZ())));
            }
        }));
    }

    private void sendHelp(Player player) {
        player.sendMessage(messages.get("crate.cmd.help-set"));
        player.sendMessage(messages.get("crate.cmd.help-remove"));
        player.sendMessage(messages.get("crate.cmd.help-list"));
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return filterPrefix(List.of("set", "remove", "list"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return filterPrefix(
                    Arrays.stream(CrateType.values()).map(CrateType::name).collect(Collectors.toList()),
                    args[1]);
        }
        return List.of();
    }
}

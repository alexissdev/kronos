package dev.alexissdev.kronos.koth.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.koth.creation.KothCreationService;
import dev.alexissdev.kronos.koth.domain.KothZone;
import dev.alexissdev.kronos.koth.service.KothService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Singleton
public class KothCommand extends BaseCommand {

    private final KothService kothService;
    private final KothCreationService creationService;
    private final Plugin plugin;
    private final MessagesConfig messages;

    @Inject
    public KothCommand(KothService kothService,
                       KothCreationService creationService,
                       Plugin plugin,
                       MessagesConfig messages) {
        super("hcf.koth.admin");
        this.kothService     = kothService;
        this.creationService = creationService;
        this.plugin          = plugin;
        this.messages        = messages;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sendHelp(sender); return; }

        switch (args[0].toLowerCase()) {
            case "start":  handleStart(sender, args);  break;
            case "end":    handleEnd(sender, args);    break;
            case "list":   handleList(sender);         break;
            case "create": handleCreate(sender, args); break;
            case "delete": handleDelete(sender, args); break;
            default:       sendHelp(sender);
        }
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/koth start <nombre>")) return;
        kothService.startKoth(args[1])
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> sender.sendMessage(messages.format("koth.cmd.started", "name", args[1]))))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> sender.sendMessage(messages.format("koth.cmd.error", "error", rootMsg(ex))));
                    return null;
                });
    }

    private void handleEnd(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/koth end <nombre>")) return;
        kothService.endKoth(args[1])
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> sender.sendMessage(messages.format("koth.cmd.ended", "name", args[1]))))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> sender.sendMessage(messages.format("koth.cmd.error", "error", rootMsg(ex))));
                    return null;
                });
    }

    private void handleList(CommandSender sender) {
        kothService.getAllKoths().thenAccept(koths ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(messages.get("koth.cmd.list-header"));
                    if (koths.isEmpty()) {
                        sender.sendMessage(messages.get("koth.cmd.list-none"));
                        return;
                    }
                    for (KothZone z : koths) {
                        String status = z.isActive()
                                ? messages.format("koth.cmd.list-entry",
                                        "name", z.getName(),
                                        "status", "§aACTIVO",
                                        "seconds", z.getCaptureTimeSeconds())
                                : messages.format("koth.cmd.list-entry",
                                        "name", z.getName(),
                                        "status", "§cINACTIVO",
                                        "seconds", z.getCaptureTimeSeconds());
                        sender.sendMessage(status);
                    }
                }));
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("koth.cmd.player-only"));
            return;
        }
        if (!requireArgs(sender, args, 3, "/koth create <nombre> <tiempoCaptura(s)>")) return;

        Player player = (Player) sender;
        String name = args[1];
        int captureTime;
        try {
            captureTime = Integer.parseInt(args[2]);
            if (captureTime <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(messages.get("koth.cmd.capture-time-invalid"));
            return;
        }

        creationService.startSession(player.getUniqueId(), name, captureTime);
        player.getInventory().addItem(creationService.createWand());

        player.sendMessage(messages.format("koth.cmd.creating", "name", name, "seconds", captureTime));
        player.sendMessage(messages.get("koth.cmd.select-claim-hint"));
        player.sendMessage(messages.get("koth.cmd.select-controls"));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/koth delete <nombre>")) return;
        kothService.deleteKoth(args[1])
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> sender.sendMessage(messages.format("koth.cmd.deleted", "name", args[1]))));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messages.get("koth.cmd.help-header"));
        sender.sendMessage(messages.get("koth.cmd.help-create"));
        sender.sendMessage(messages.get("koth.cmd.help-start"));
        sender.sendMessage(messages.get("koth.cmd.help-end"));
        sender.sendMessage(messages.get("koth.cmd.help-list"));
        sender.sendMessage(messages.get("koth.cmd.help-delete"));
    }

    private static String rootMsg(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}

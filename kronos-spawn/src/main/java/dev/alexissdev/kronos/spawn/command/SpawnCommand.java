package dev.alexissdev.kronos.spawn.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.spawn.creation.SpawnCreationService;
import dev.alexissdev.kronos.spawn.domain.SpawnZone;
import dev.alexissdev.kronos.spawn.service.SpawnService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

@Singleton
public class SpawnCommand extends BaseCommand {

    private final SpawnService spawnService;
    private final SpawnCreationService creationService;
    private final Plugin plugin;
    private final MessagesConfig messages;

    @Inject
    public SpawnCommand(SpawnService spawnService,
                        SpawnCreationService creationService,
                        Plugin plugin,
                        MessagesConfig messages) {
        super("hcf.spawn.admin");
        this.spawnService    = spawnService;
        this.creationService = creationService;
        this.plugin          = plugin;
        this.messages        = messages;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sendHelp(sender); return; }

        switch (args[0].toLowerCase()) {
            case "setzone": handleSetZone(sender); break;
            case "info":    handleInfo(sender);    break;
            case "remove":  handleRemove(sender);  break;
            default:        sendHelp(sender);
        }
    }

    private void handleSetZone(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("spawn.cmd.player-only"));
            return;
        }
        Player player = (Player) sender;
        creationService.startSession(player.getUniqueId());
        player.getInventory().addItem(creationService.createWand());
        player.sendMessage(messages.get("spawn.cmd.select-hint"));
        player.sendMessage(messages.get("spawn.cmd.select-controls"));
    }

    private void handleInfo(CommandSender sender) {
        Optional<SpawnZone> zoneOpt = spawnService.getZone();
        if (!zoneOpt.isPresent()) {
            sender.sendMessage(messages.get("spawn.cmd.no-zone"));
            return;
        }
        SpawnZone zone = zoneOpt.get();
        sender.sendMessage(messages.get("spawn.cmd.info-header"));
        sender.sendMessage(messages.format("spawn.cmd.info-world", "world", zone.getWorld()));
        sender.sendMessage(messages.format("spawn.cmd.info-corners",
                "minX", zone.getMinX(), "minZ", zone.getMinZ(),
                "maxX", zone.getMaxX(), "maxZ", zone.getMaxZ()));
    }

    private void handleRemove(CommandSender sender) {
        spawnService.removeZone()
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> sender.sendMessage(messages.get("spawn.cmd.removed"))));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messages.get("spawn.cmd.help-header"));
        sender.sendMessage(messages.get("spawn.cmd.help-setzone"));
        sender.sendMessage(messages.get("spawn.cmd.help-info"));
        sender.sendMessage(messages.get("spawn.cmd.help-remove"));
    }
}

package dev.alexissdev.kronos.plugin.command.hcf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.players.service.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class UnbanSub extends SubCommand {

    private final PlayerService  playerService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    @Inject
    public UnbanSub(PlayerService playerService, MessagesConfig messages, Plugin plugin) {
        this.playerService = playerService;
        this.messages      = messages;
        this.plugin        = plugin;
    }

    @Override public String name() { return "unban"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? onlinePlayers(args[1]) : List.of();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/hcf unban <jugador>")) return;
        String targetName = args[1];
        Player online = Bukkit.getPlayer(targetName);
        UUID uuid = online != null ? online.getUniqueId() : Bukkit.getOfflinePlayer(targetName).getUniqueId();

        playerService.isDeathbanned(uuid).thenCompose(banned -> {
            if (!banned) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(messages.format("hcf.unban-not-banned", "player", targetName)));
                return CompletableFuture.completedFuture(null);
            }
            return playerService.removeDeathban(uuid).thenRun(() ->
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(messages.format("hcf.unban-success", "player", targetName))));
        }).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(messages.format("hcf.error", "error", ex.getMessage())));
            return null;
        });
    }
}

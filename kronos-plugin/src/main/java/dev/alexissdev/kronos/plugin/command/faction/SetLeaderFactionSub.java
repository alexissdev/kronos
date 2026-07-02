package dev.alexissdev.kronos.plugin.command.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.exception.HCFException;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

@Singleton
public class SetLeaderFactionSub extends FactionSubCommand {

    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    @Inject
    public SetLeaderFactionSub(FactionService factionService, MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    @Override public String name() { return "setleader"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? onlinePlayers(args[1]) : List.of();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (!requireArgs(player, args, 2, "/f setleader <jugador>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(messages.get("hcf.player-not-found")); return; }

        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.setLeader(opt.get().getId(), target.getUniqueId(), player.getUniqueId());
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendMessage(messages.format("faction.cmd.setleader-success", "player", target.getName()));
            target.sendMessage(messages.format("faction.cmd.setleader-target", "player", player.getName()));
        })).exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(ChatColor.RED + rootMsg(ex))); return null; });
    }
}

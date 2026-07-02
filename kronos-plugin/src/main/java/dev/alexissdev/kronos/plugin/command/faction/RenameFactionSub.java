package dev.alexissdev.kronos.plugin.command.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.exception.HCFException;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Singleton
public class RenameFactionSub extends FactionSubCommand {

    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    @Inject
    public RenameFactionSub(FactionService factionService, MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    @Override public String name() { return "rename"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (!requireArgs(player, args, 2, "/f rename <nombre>")) return;
        final String newName = args[1];

        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.renameFaction(opt.get().getId(), newName, player.getUniqueId());
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.format("faction.cmd.renamed", "name", newName))))
                .exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin,
                        () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex)))); return null; });
    }
}

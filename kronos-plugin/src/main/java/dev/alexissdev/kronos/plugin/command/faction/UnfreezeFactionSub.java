package dev.alexissdev.kronos.plugin.command.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.exception.HCFException;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

@Singleton
public class UnfreezeFactionSub extends FactionSubCommand {

    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    @Inject
    public UnfreezeFactionSub(FactionService factionService, MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    @Override public String name() { return "unfreeze"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? onlinePlayers(args[1]) : List.of();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (!player.hasPermission("hcf.admin")) { player.sendMessage(messages.get("hcf.no-permission")); return; }
        if (!requireArgs(player, args, 2, "/f unfreeze <faccion>")) return;
        String factionName = args[1];

        factionService.getByName(factionName).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new HCFException("Facción no encontrada: " + factionName));
            return factionService.unfreezeFaction(faction.getId(), player.getUniqueId())
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(messages.format("faction.unfrozen", "name", factionName));
                        notifyMembers(faction, messages.get("faction.unfrozen-self"));
                    }));
        }).exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex)))); return null; });
    }
}

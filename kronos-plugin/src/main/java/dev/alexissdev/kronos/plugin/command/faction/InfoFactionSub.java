package dev.alexissdev.kronos.plugin.command.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.domain.FactionMember;
import dev.alexissdev.kronos.factions.domain.FactionRole;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class InfoFactionSub extends FactionSubCommand {

    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    @Inject
    public InfoFactionSub(FactionService factionService, MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    @Override public String name() { return "info"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? onlinePlayers(args[1]) : List.of();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        if (args.length >= 2) {
            factionService.getByName(args[1]).thenAccept(opt ->
                    Bukkit.getScheduler().runTask(plugin, () ->
                            opt.ifPresentOrElse(f -> printInfo(player, f),
                                    () -> player.sendMessage(messages.get("faction.cmd.faction-not-found")))));
        } else {
            factionService.getByPlayer(player.getUniqueId()).thenAccept(opt ->
                    Bukkit.getScheduler().runTask(plugin, () ->
                            opt.ifPresentOrElse(f -> printInfo(player, f),
                                    () -> player.sendMessage(messages.get("faction.cmd.not-in-faction")))));
        }
    }

    private void printInfo(Player player, Faction f) {
        player.sendMessage(messages.get("faction.cmd.info-sep"));
        player.sendMessage(messages.format("faction.cmd.info-name",    "name",      f.getName()));
        player.sendMessage(messages.format("faction.cmd.info-stats",   "kills",     String.valueOf(f.getKills()),
                                                                        "deaths",   String.valueOf(f.getDeaths())));
        player.sendMessage(messages.format("faction.cmd.info-dtk",     "remaining", String.valueOf(f.getDtkRemaining()),
                                                                        "max",      String.valueOf(f.getMaxDtk())));
        player.sendMessage(messages.format("faction.cmd.info-balance",  "balance",  String.format("%.2f", f.getBalance())));
        player.sendMessage(messages.format("faction.cmd.info-strikes",  "strikes",  String.valueOf(f.getStrikes()),
                                                                         "max",     String.valueOf(f.getMaxStrikes())));
        if (f.isFrozen()) player.sendMessage(messages.get("faction.cmd.info-frozen"));

        for (FactionRole role : new FactionRole[]{ FactionRole.LEADER, FactionRole.CO_LEADER, FactionRole.CAPTAIN, FactionRole.MEMBER }) {
            List<String> names = f.getMembers().values().stream()
                    .filter(m -> m.getRole() == role)
                    .map(m -> {
                        String name = Bukkit.getOfflinePlayer(m.getUuid()).getName();
                        boolean online = Bukkit.getPlayer(m.getUuid()) != null;
                        return (online ? ChatColor.GREEN : ChatColor.GRAY) + (name != null ? name : m.getUuid().toString());
                    })
                    .collect(Collectors.toList());
            if (!names.isEmpty()) {
                player.sendMessage(messages.format("faction.cmd.info-role-members",
                        "role", role.name(), "members", String.join(ChatColor.WHITE + ", ", names)));
            }
        }
        player.sendMessage(messages.get("faction.cmd.info-sep"));
    }
}

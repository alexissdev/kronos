package dev.alexissdev.kronos.plugin.command.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Singleton
public class TopFactionSub extends FactionSubCommand {

    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    @Inject
    public TopFactionSub(FactionService factionService, MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    @Override public String name() { return "top"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        factionService.getTopFactions(10).thenAccept(factions ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(messages.get("faction.cmd.top-header"));
                    for (int i = 0; i < factions.size(); i++) {
                        Faction f = factions.get(i);
                        player.sendMessage(messages.format("faction.cmd.top-entry",
                                "rank", i + 1, "name", f.getName(), "kills", f.getKills()));
                    }
                }));
    }
}

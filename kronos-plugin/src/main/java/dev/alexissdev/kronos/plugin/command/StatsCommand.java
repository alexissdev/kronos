package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.factions.service.FactionService;
import dev.alexissdev.kronos.players.service.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class StatsCommand extends BaseCommand {

    private final PlayerService playerService;
    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin plugin;

    @Inject
    public StatsCommand(PlayerService playerService, FactionService factionService,
                        MessagesConfig messages, Plugin plugin) {
        super(null);
        this.playerService = playerService;
        this.factionService = factionService;
        this.messages = messages;
        this.plugin = plugin;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(messages.get("hcf.player-not-found"));
                return;
            }
        } else {
            target = requirePlayer(sender);
            if (target == null) return;
        }

        UUID uuid = target.getUniqueId();
        String targetName = target.getName();

        playerService.getPlayer(uuid).thenCombine(
                factionService.getByPlayer(uuid),
                (playerOpt, factionOpt) -> {
                    int kills  = playerOpt.map(p -> p.getKills()).orElse(0);
                    int deaths = playerOpt.map(p -> p.getDeaths()).orElse(0);
                    int lives  = playerOpt.map(p -> p.getLives()).orElse(0);
                    String faction = factionOpt.map(f -> f.getName()).orElse(messages.get("stats.no-faction"));

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(messages.format("stats.header", "player", targetName));
                        sender.sendMessage(messages.format("stats.faction", "faction", faction));
                        sender.sendMessage(messages.format("stats.kills",   "kills",   String.valueOf(kills)));
                        sender.sendMessage(messages.format("stats.deaths",  "deaths",  String.valueOf(deaths)));
                        sender.sendMessage(messages.format("stats.lives",   "lives",   String.valueOf(lives)));
                    });
                    return null;
                }
        ).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(messages.format("hcf.error", "error", ex.getMessage())));
            return null;
        });
    }
}

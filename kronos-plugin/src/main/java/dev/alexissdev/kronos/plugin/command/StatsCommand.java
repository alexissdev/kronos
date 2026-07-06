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

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Command {@code /stats} that displays a player's combat statistics: kills,
 * deaths, remaining lives, and the faction they belong to. When executed without
 * arguments it shows the executor's own stats; when a name is specified it shows
 * that online player's stats instead. Queries are performed asynchronously,
 * combining data from the player profile and faction services.
 */
@Singleton
public class StatsCommand extends BaseCommand {

    private final PlayerService playerService;
    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin plugin;

    /**
     * Constructs the command by injecting its dependencies via Guice.
     *
     * @param playerService  service used to retrieve the player's HCF profile (kills, deaths, lives)
     * @param factionService service used to retrieve the faction the player belongs to
     * @param messages       localised message configuration
     * @param plugin         plugin instance used to schedule tasks on the main thread
     */
    @Inject
    public StatsCommand(PlayerService playerService, FactionService factionService,
                        MessagesConfig messages, Plugin plugin) {
        super(null);
        this.playerService = playerService;
        this.factionService = factionService;
        this.messages = messages;
        this.plugin = plugin;
    }

    /**
     * Provides tab-completion suggestions with the names of online players for the
     * first argument of the command.
     *
     * @param sender command executor
     * @param args   arguments typed so far
     * @return list of online player names that match the current prefix, or an empty
     *         list if the first argument has already been provided
     */
    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return onlinePlayers(args[0]);
        return Collections.emptyList();
    }

    /**
     * Resolves the target player (the executor themselves or the one specified as an
     * argument), asynchronously queries their HCF profile and faction, then displays
     * the stats output on the Bukkit main thread.
     *
     * @param sender command executor; must be a {@link org.bukkit.entity.Player} when
     *               no target argument is provided
     * @param args   optional arguments; {@code args[0]} may be the name of an online player
     */
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

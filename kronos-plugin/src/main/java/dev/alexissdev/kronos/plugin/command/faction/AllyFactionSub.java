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

import java.util.List;

/**
 * Sub-command {@code /f ally <faction>} that establishes an alliance relationship
 * between the executor's faction and the specified faction. Allies cannot attack
 * each other and share a dedicated ally chat channel. The executor must be a
 * faction member with sufficient rank to modify diplomatic relations.
 */
@Singleton
public class AllyFactionSub extends FactionSubCommand {

    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Constructs the sub-command by injecting its dependencies via Guice.
     *
     * @param factionService faction service used to manage inter-faction relationships
     * @param messages       localised message configuration
     * @param plugin         plugin instance used to schedule tasks on the main thread
     */
    @Inject
    public AllyFactionSub(FactionService factionService, MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    /** @return the sub-command name: {@code "ally"} */
    @Override public String name() { return "ally"; }

    /**
     * Provides tab-completion suggestions with the names of online players for the
     * second argument (target faction name).
     *
     * @param sender command executor
     * @param args   arguments typed so far
     * @return list of online player names filtered by the current prefix
     */
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? onlinePlayers(args[1]) : List.of();
    }

    /**
     * Retrieves the executor's faction and the target faction by name, then
     * asynchronously establishes an alliance between them. The executor is notified
     * on success or on any domain error.
     *
     * @param sender command executor; must be a {@link org.bukkit.entity.Player}
     * @param args   arguments; {@code args[1]} is the name of the faction to ally with
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (!requireArgs(player, args, 2, "/f ally <faccion>")) return;

        factionService.getByPlayer(player.getUniqueId()).thenCompose(optA -> {
            if (optA.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.getByName(args[1]).thenCompose(optB -> {
                if (optB.isEmpty()) throw new HCFException("Facción no encontrada");
                return factionService.setAlly(optA.get().getId(), optB.get().getId(), player.getUniqueId());
            });
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.get("faction.cmd.ally-set"))))
                .exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin,
                        () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex)))); return null; });
    }
}

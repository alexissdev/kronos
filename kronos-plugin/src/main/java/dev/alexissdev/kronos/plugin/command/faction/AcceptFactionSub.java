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

/**
 * Sub-command {@code /f accept <faction>} (alias: {@code /f join}) that allows a
 * player to accept a pending invitation and join the specified faction. The player
 * must have previously received an invitation from that faction; otherwise the
 * service will throw a domain exception.
 */
@Singleton
public class AcceptFactionSub extends FactionSubCommand {

    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Constructs the sub-command by injecting its dependencies via Guice.
     *
     * @param factionService faction service used to validate and accept invitations
     * @param messages       localised message configuration
     * @param plugin         plugin instance used to schedule tasks on the main thread
     */
    @Inject
    public AcceptFactionSub(FactionService factionService, MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    /** @return the sub-command name: {@code "accept"} */
    @Override public String   name()    { return "accept"; }

    /** @return sub-command aliases: {@code ["join"]} */
    @Override public String[] aliases() { return new String[]{"join"}; }

    /**
     * Looks up the faction by name and accepts the executing player's invitation to
     * join it. On error (faction not found or no pending invitation) the player is
     * notified with the domain exception message.
     *
     * @param sender command executor; must be a {@link org.bukkit.entity.Player}
     * @param args   arguments; {@code args[1]} is the name of the faction to join
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (!requireArgs(player, args, 2, "/f accept <faccion>")) return;

        factionService.getByName(args[1]).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("Facción no encontrada");
            return factionService.acceptInvite(player.getUniqueId(), opt.get().getId());
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.get("faction.cmd.joined"))))
                .exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin,
                        () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex)))); return null; });
    }
}

package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import dev.alexissdev.kronos.common.command.DispatchCommand;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import org.bukkit.command.CommandSender;

import java.util.Set;

/**
 * Root command {@code /f} ({@code /faction}) that groups all sub-commands of
 * the HCF faction system. It acts as the central entry point and dispatches each
 * operation to the corresponding sub-command (create, disband, invite, accept,
 * leave, kick, info, top, ally, enemy, deposit, withdraw, claim, unclaim,
 * overclaim, map, sethome, home, rename, neutral, among others).
 */
@Singleton
public class FactionCommand extends DispatchCommand {

    private final MessagesConfig messages;

    /**
     * Constructs the command by registering all faction sub-commands injected by Guice.
     *
     * @param subs     set of sub-commands for the {@code faction} group, annotated with {@code @Named("faction")}
     * @param messages localised message configuration
     */
    @Inject
    public FactionCommand(@Named("faction") Set<SubCommand> subs, MessagesConfig messages) {
        super(null);
        this.messages = messages;
        register(subs);
    }

    /**
     * Sends the full {@code /f} help menu to the executor, listing every available
     * sub-command and its usage syntax.
     *
     * @param sender command executor who will receive the help messages
     */
    @Override
    protected void sendUsage(CommandSender sender) {
        sender.sendMessage(messages.get("faction.cmd.help-header"));
        sender.sendMessage(messages.get("faction.cmd.help-create"));
        sender.sendMessage(messages.get("faction.cmd.help-disband"));
        sender.sendMessage(messages.get("faction.cmd.help-invite"));
        sender.sendMessage(messages.get("faction.cmd.help-accept"));
        sender.sendMessage(messages.get("faction.cmd.help-leave"));
        sender.sendMessage(messages.get("faction.cmd.help-kick"));
        sender.sendMessage(messages.get("faction.cmd.help-info"));
        sender.sendMessage(messages.get("faction.cmd.help-top"));
        sender.sendMessage(messages.get("faction.cmd.help-ally"));
        sender.sendMessage(messages.get("faction.cmd.help-enemy"));
        sender.sendMessage(messages.get("faction.cmd.help-deposit"));
        sender.sendMessage(messages.get("faction.cmd.help-withdraw"));
        sender.sendMessage(messages.get("faction.cmd.help-claim"));
        sender.sendMessage(messages.get("faction.cmd.help-unclaim"));
        sender.sendMessage(messages.get("faction.cmd.help-overclaim"));
        sender.sendMessage(messages.get("faction.cmd.help-map"));
        sender.sendMessage(messages.get("faction.cmd.help-sethome"));
        sender.sendMessage(messages.get("faction.cmd.help-home"));
        sender.sendMessage(messages.get("faction.cmd.help-rename"));
        sender.sendMessage(messages.get("faction.cmd.help-neutral"));
    }
}

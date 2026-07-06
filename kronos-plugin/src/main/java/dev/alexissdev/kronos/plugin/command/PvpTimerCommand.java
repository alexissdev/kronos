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
 * Root command {@code /pvptimer} that groups the sub-commands for managing the
 * PvP protection timer for new or returning players. The PvP Timer prevents
 * players from taking damage from other players during an initial grace period.
 * Requires the {@code hcf.admin} permission and delegates to the {@code give}
 * and {@code remove} sub-commands.
 */
@Singleton
public class PvpTimerCommand extends DispatchCommand {

    private final MessagesConfig messages;

    /**
     * Constructs the command by registering the {@code pvptimer} group sub-commands
     * injected by Guice.
     *
     * @param subs     set of sub-commands annotated with {@code @Named("pvptimer")}
     * @param messages localised message configuration
     */
    @Inject
    public PvpTimerCommand(@Named("pvptimer") Set<SubCommand> subs, MessagesConfig messages) {
        super("hcf.admin");
        this.messages = messages;
        register(subs);
    }

    /**
     * Sends the {@code /pvptimer} help menu to the executor, listing the available
     * sub-commands and their usage syntax.
     *
     * @param sender command executor who will receive the help messages
     */
    @Override
    protected void sendUsage(CommandSender sender) {
        sender.sendMessage(messages.get("pvptimer.help-header"));
        sender.sendMessage(messages.get("pvptimer.help-give"));
        sender.sendMessage(messages.get("pvptimer.help-remove"));
    }
}

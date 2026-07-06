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
 * Root command {@code /crate} for the administrative management of reward crates
 * on the HCF server. Requires the {@code hcf.admin} permission and delegates each
 * operation to its registered sub-commands: {@code set}, {@code remove}, and
 * {@code list}.
 */
@Singleton
public class CrateCommand extends DispatchCommand {

    private final MessagesConfig messages;

    /**
     * Constructs the command by registering the crate sub-commands injected by Guice.
     *
     * @param subs     set of sub-commands for the {@code crate} group, annotated with {@code @Named("crate")}
     * @param messages localised message configuration
     */
    @Inject
    public CrateCommand(@Named("crate") Set<SubCommand> subs, MessagesConfig messages) {
        super("hcf.admin");
        this.messages = messages;
        register(subs);
    }

    /**
     * Sends the {@code /crate} help menu to the executor, listing all available
     * sub-commands and their usage syntax.
     *
     * @param sender command executor who will receive the help messages
     */
    @Override
    protected void sendUsage(CommandSender sender) {
        sender.sendMessage(messages.get("crate.cmd.help-set"));
        sender.sendMessage(messages.get("crate.cmd.help-remove"));
        sender.sendMessage(messages.get("crate.cmd.help-list"));
    }
}

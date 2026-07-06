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
 * Admin command {@code /hcf} that groups the advanced server-management operations
 * reserved for staff members. Requires the {@code hcf.admin} permission and delegates
 * to sub-commands such as {@code reload}, {@code give-money}, {@code set-money},
 * {@code give-key}, {@code sotw}, {@code eotw}, and {@code unban}.
 */
@Singleton
public class HCFCommand extends DispatchCommand {

    private final MessagesConfig messages;

    /**
     * Constructs the command by registering the {@code hcf} group sub-commands
     * injected by Guice.
     *
     * @param subs     set of sub-commands annotated with {@code @Named("hcf")}
     * @param messages localised message configuration
     */
    @Inject
    public HCFCommand(@Named("hcf") Set<SubCommand> subs, MessagesConfig messages) {
        super("hcf.admin");
        this.messages = messages;
        register(subs);
    }

    /**
     * Sends the {@code /hcf} help menu to the executor, listing all available
     * administrative sub-commands and their usage syntax.
     *
     * @param sender command executor who will receive the help messages
     */
    @Override
    protected void sendUsage(CommandSender sender) {
        sender.sendMessage(messages.get("hcf.help-header"));
        sender.sendMessage(messages.get("hcf.help-reload"));
        sender.sendMessage(messages.get("hcf.help-give-money"));
        sender.sendMessage(messages.get("hcf.help-set-money"));
        sender.sendMessage(messages.get("hcf.help-give-key"));
        sender.sendMessage(color("&e/hcf sotw <start <horas>|stop> &7- Iniciar/detener SOTW"));
        sender.sendMessage(color("&e/hcf eotw <start <horas>|stop> &7- Iniciar/detener EOTW"));
        sender.sendMessage(color("&e/hcf unban <jugador> &7- Quitar deathban a un jugador"));
    }
}

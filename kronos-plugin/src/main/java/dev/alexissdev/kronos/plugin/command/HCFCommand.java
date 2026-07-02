package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import dev.alexissdev.kronos.common.command.DispatchCommand;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import org.bukkit.command.CommandSender;

import java.util.Set;

@Singleton
public class HCFCommand extends DispatchCommand {

    private final MessagesConfig messages;

    @Inject
    public HCFCommand(@Named("hcf") Set<SubCommand> subs, MessagesConfig messages) {
        super("hcf.admin");
        this.messages = messages;
        register(subs);
    }

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

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
public class CrateCommand extends DispatchCommand {

    private final MessagesConfig messages;

    @Inject
    public CrateCommand(@Named("crate") Set<SubCommand> subs, MessagesConfig messages) {
        super("hcf.admin");
        this.messages = messages;
        register(subs);
    }

    @Override
    protected void sendUsage(CommandSender sender) {
        sender.sendMessage(messages.get("crate.cmd.help-set"));
        sender.sendMessage(messages.get("crate.cmd.help-remove"));
        sender.sendMessage(messages.get("crate.cmd.help-list"));
    }
}

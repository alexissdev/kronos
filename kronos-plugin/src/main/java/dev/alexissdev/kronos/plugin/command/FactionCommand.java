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
public class FactionCommand extends DispatchCommand {

    private final MessagesConfig messages;

    @Inject
    public FactionCommand(@Named("faction") Set<SubCommand> subs, MessagesConfig messages) {
        super(null);
        this.messages = messages;
        register(subs);
    }

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

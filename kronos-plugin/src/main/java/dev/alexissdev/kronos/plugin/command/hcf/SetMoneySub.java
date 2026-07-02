package dev.alexissdev.kronos.plugin.command.hcf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import org.bukkit.command.CommandSender;

@Singleton
public class SetMoneySub extends SubCommand {

    private final MessagesConfig messages;

    @Inject
    public SetMoneySub(MessagesConfig messages) {
        this.messages = messages;
    }

    @Override public String name() { return "set-money"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 3, "/hcf set-money <jugador> <cantidad>")) return;
        sender.sendMessage(messages.get("hcf.set-money-wip"));
    }
}

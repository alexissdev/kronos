package dev.alexissdev.kronos.plugin.command.hcf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.plugin.listener.CrateListener;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class GiveKeySub extends SubCommand {

    private final MessagesConfig messages;

    @Inject
    public GiveKeySub(MessagesConfig messages) {
        this.messages = messages;
    }

    @Override public String name() { return "give-key"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) return onlinePlayers(args[1]);
        if (args.length == 3) return filterPrefix(
                Arrays.stream(CrateType.values()).map(CrateType::name).collect(Collectors.toList()), args[2]);
        return List.of();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 3, "/hcf give-key <jugador> <tipo>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(messages.get("hcf.player-not-found")); return; }

        CrateType type;
        try { type = CrateType.valueOf(args[2].toUpperCase()); }
        catch (IllegalArgumentException e) { sender.sendMessage(messages.get("hcf.give-key-invalid-type")); return; }

        ItemStack key = CrateListener.createKey(type);
        target.getInventory().addItem(key);
        sender.sendMessage(messages.format("hcf.give-key-sender", "type", type.name(), "player", target.getName()));
        target.sendMessage(messages.format("hcf.give-key-target", "type", type.name()));
    }
}

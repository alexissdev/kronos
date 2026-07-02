package dev.alexissdev.kronos.plugin.command.hcf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.economy.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

@Singleton
public class GiveMoneySub extends SubCommand {

    private final EconomyService economyService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    @Inject
    public GiveMoneySub(EconomyService economyService, MessagesConfig messages, Plugin plugin) {
        this.economyService = economyService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    @Override public String name() { return "give-money"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? onlinePlayers(args[1]) : List.of();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 3, "/hcf give-money <jugador> <cantidad>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(messages.get("hcf.player-not-found")); return; }

        double amount;
        try { amount = Double.parseDouble(args[2]); }
        catch (NumberFormatException e) { sender.sendMessage(messages.get("hcf.amount-invalid")); return; }
        final double finalAmount = amount;

        economyService.deposit(target.getUniqueId(), finalAmount)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(messages.format("hcf.give-money-sender",
                            "amount", String.format("%.2f", finalAmount), "player", target.getName()));
                    target.sendMessage(messages.format("hcf.give-money-target",
                            "amount", String.format("%.2f", finalAmount)));
                }));
    }
}

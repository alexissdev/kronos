package dev.alexissdev.kronos.economy.command;

import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.economy.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Singleton
public class MoneyCommand extends BaseCommand {

    private final EconomyService economyService;
    private final Plugin plugin;
    private final MessagesConfig messages;

    @Inject
    public MoneyCommand(EconomyService economyService, Plugin plugin, MessagesConfig messages) {
        super(null);
        this.economyService = economyService;
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        if (args.length == 0) { showBalance(player, player.getUniqueId(), player.getName()); return; }

        switch (args[0].toLowerCase()) {
            case "pay": handlePay(player, args); break;
            case "top": handleTop(player); break;
            default:
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) showBalance(player, target.getUniqueId(), target.getName());
                else player.sendMessage(messages.get("economy.player-not-found"));
        }
    }

    private void showBalance(Player viewer, UUID targetUuid, String targetName) {
        economyService.getBalance(targetUuid)
                .thenAccept(balance -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (viewer.getUniqueId().equals(targetUuid)) {
                        viewer.sendMessage(messages.format("economy.balance-self",
                                "amount", String.format("%.2f", balance)));
                    } else {
                        viewer.sendMessage(messages.format("economy.balance-other",
                                "player", targetName,
                                "amount", String.format("%.2f", balance)));
                    }
                }));
    }

    private void handlePay(Player sender, String[] args) {
        if (!requireArgs(sender, args, 3, "/money pay <jugador> <cantidad>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(messages.get("economy.player-not-found")); return; }
        if (target.equals(sender)) { sender.sendMessage(messages.get("economy.pay-self")); return; }

        double amount;
        try { amount = Double.parseDouble(args[2]); } catch (NumberFormatException e) {
            sender.sendMessage(messages.get("economy.amount-invalid")); return;
        }
        if (amount <= 0) { sender.sendMessage(messages.get("economy.amount-positive")); return; }

        final double finalAmount = amount;
        economyService.transfer(sender.getUniqueId(), target.getUniqueId(), finalAmount)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(messages.format("economy.pay-sent",
                            "amount", String.format("%.2f", finalAmount),
                            "player", target.getName()));
                    target.sendMessage(messages.format("economy.pay-received",
                            "sender", sender.getName(),
                            "amount", String.format("%.2f", finalAmount)));
                }))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(messages.format("economy.error",
                                    "error", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage())));
                    return null;
                });
    }

    private void handleTop(Player player) {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (online.isEmpty()) { player.sendMessage(messages.get("economy.player-not-found")); return; }

        List<CompletableFuture<Map.Entry<String, Double>>> futures = online.stream()
                .map(p -> economyService.getBalance(p.getUniqueId())
                        .thenApply(bal -> (Map.Entry<String, Double>) new AbstractMap.SimpleEntry<>(p.getName(), bal)))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenAccept(v -> {
                    List<Map.Entry<String, Double>> sorted = futures.stream()
                            .map(CompletableFuture::join)
                            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                            .limit(10)
                            .collect(Collectors.toList());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(messages.get("economy.top-header"));
                        for (int i = 0; i < sorted.size(); i++) {
                            player.sendMessage(messages.format("economy.top-entry",
                                    "rank", i + 1,
                                    "player", sorted.get(i).getKey(),
                                    "amount", String.format("%.2f", sorted.get(i).getValue())));
                        }
                    });
                });
    }
}

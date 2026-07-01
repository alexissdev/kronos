package dev.alexissdev.kronos.economy.command;

import dev.alexissdev.kronos.common.command.BaseCommand;

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

    @Inject
    public MoneyCommand(EconomyService economyService, Plugin plugin) {
        super(null);
        this.economyService = economyService;
        this.plugin = plugin;
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
                else msg(player, "&cJugador no encontrado o no está online.");
        }
    }

    private void showBalance(Player viewer, UUID targetUuid, String targetName) {
        economyService.getBalance(targetUuid)
                .thenAccept(balance -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (viewer.getUniqueId().equals(targetUuid)) {
                        msg(viewer, "&6Tu balance: &e$" + String.format("%.2f", balance));
                    } else {
                        msg(viewer, "&6Balance de &e" + targetName + "&6: &e$" + String.format("%.2f", balance));
                    }
                }));
    }

    private void handlePay(Player sender, String[] args) {
        if (!requireArgs(sender, args, 3, "/money pay <jugador> <cantidad>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { msg(sender, "&cJugador no encontrado."); return; }
        if (target.equals(sender)) { msg(sender, "&cNo puedes pagarte a ti mismo."); return; }

        double amount;
        try { amount = Double.parseDouble(args[2]); } catch (NumberFormatException e) {
            msg(sender, "&cCantidad inválida."); return;
        }
        if (amount <= 0) { msg(sender, "&cLa cantidad debe ser positiva."); return; }

        economyService.transfer(sender.getUniqueId(), target.getUniqueId(), amount)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    msg(sender, "&aPagaste &e$" + String.format("%.2f", amount) + "&a a &e" + target.getName());
                    msg(target, "&e" + sender.getName() + "&a te pagó &e$" + String.format("%.2f", amount));
                }))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(sender, "&c" + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage())));
                    return null;
                });
    }

    private void handleTop(Player player) {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (online.isEmpty()) { msg(player, "&cNo hay jugadores online."); return; }

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
                        msg(player, "&6&lTop Riqueza (online):");
                        for (int i = 0; i < sorted.size(); i++) {
                            msg(player, "&e" + (i + 1) + ". &f" + sorted.get(i).getKey()
                                    + " &7- &e$" + String.format("%.2f", sorted.get(i).getValue()));
                        }
                    });
                });
    }
}

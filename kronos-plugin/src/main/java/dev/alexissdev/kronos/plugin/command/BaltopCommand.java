package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.economy.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Command {@code /baltop} that displays a ranking of the top 10 online players
 * ordered by their economic balance in descending order. Balance queries are
 * executed asynchronously using {@link java.util.concurrent.CompletableFuture},
 * and the result is delivered back on the Bukkit main thread to ensure thread safety.
 */
@Singleton
public class BaltopCommand extends BaseCommand {

    private final EconomyService economyService;
    private final Plugin plugin;
    private final MessagesConfig messages;

    /**
     * Constructs the command by injecting its dependencies via Guice.
     *
     * @param economyService economy service used to query player balances
     * @param plugin         plugin instance used to schedule tasks on the main thread
     * @param messages       localised message configuration
     */
    @Inject
    public BaltopCommand(EconomyService economyService, Plugin plugin, MessagesConfig messages) {
        super(null);
        this.economyService = economyService;
        this.plugin         = plugin;
        this.messages       = messages;
    }

    /**
     * Asynchronously queries the balance of every online player, sorts them in
     * descending order, and displays the top 10 results to the command executor.
     * If no players are online, the corresponding error message is sent instead.
     *
     * @param sender command executor (player or console)
     * @param args   additional arguments (not used by this command)
     */
    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            sender.sendMessage(messages.get("economy.player-not-found"));
            return;
        }

        List<CompletableFuture<Map.Entry<String, Double>>> futures = Bukkit.getOnlinePlayers().stream()
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
                        sender.sendMessage(messages.get("economy.top-header"));
                        for (int i = 0; i < sorted.size(); i++) {
                            sender.sendMessage(messages.format("economy.top-entry",
                                    "rank",   i + 1,
                                    "player", sorted.get(i).getKey(),
                                    "amount", String.format("%.2f", sorted.get(i).getValue())));
                        }
                    });
                });
    }
}

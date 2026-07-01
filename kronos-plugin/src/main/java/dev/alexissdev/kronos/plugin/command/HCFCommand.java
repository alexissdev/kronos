package dev.alexissdev.kronos.plugin.command;

import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.economy.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@Singleton
public class HCFCommand extends BaseCommand {

    private final EconomyService economyService;
    private final Plugin plugin;
    private final MessagesConfig messages;

    @Inject
    public HCFCommand(EconomyService economyService, Plugin plugin, MessagesConfig messages) {
        super("hcf.admin");
        this.economyService = economyService;
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sendHelp(sender); return; }

        switch (args[0].toLowerCase()) {
            case "reload":     handleReload(sender);       break;
            case "give-money": handleGiveMoney(sender, args); break;
            case "set-money":  handleSetMoney(sender, args);  break;
            default:           sendHelp(sender);
        }
    }

    private void handleReload(CommandSender sender) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            sender.sendMessage(messages.get("hcf.reload-file-not-found"));
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : yaml.getKeys(true)) {
            if (!yaml.isConfigurationSection(key)) {
                map.put(key, yaml.getString(key, ""));
            }
        }
        messages.reload(map);
        sender.sendMessage(messages.get("hcf.reloaded"));
    }

    private void handleGiveMoney(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 3, "/hcf give-money <jugador> <cantidad>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(messages.get("hcf.player-not-found")); return; }

        double amount;
        try { amount = Double.parseDouble(args[2]); } catch (NumberFormatException e) {
            sender.sendMessage(messages.get("hcf.amount-invalid")); return;
        }

        final double finalAmount = amount;
        economyService.deposit(target.getUniqueId(), finalAmount)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(messages.format("hcf.give-money-sender",
                            "amount", String.format("%.2f", finalAmount), "player", target.getName()));
                    target.sendMessage(messages.format("hcf.give-money-target",
                            "amount", String.format("%.2f", finalAmount)));
                }));
    }

    private void handleSetMoney(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 3, "/hcf set-money <jugador> <cantidad>")) return;
        sender.sendMessage(messages.get("hcf.set-money-wip"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messages.get("hcf.help-header"));
        sender.sendMessage(messages.get("hcf.help-reload"));
        sender.sendMessage(messages.get("hcf.help-give-money"));
        sender.sendMessage(messages.get("hcf.help-set-money"));
    }
}

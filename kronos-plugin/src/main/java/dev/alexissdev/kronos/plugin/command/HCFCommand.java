package dev.alexissdev.kronos.plugin.command;

import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.common.domain.SotwService;
import dev.alexissdev.kronos.plugin.listener.CrateListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.economy.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@Singleton
public class HCFCommand extends BaseCommand {

    private final EconomyService economyService;
    private final SotwService sotwService;
    private final Plugin plugin;
    private final MessagesConfig messages;

    @Inject
    public HCFCommand(EconomyService economyService, SotwService sotwService,
                      Plugin plugin, MessagesConfig messages) {
        super("hcf.admin");
        this.economyService = economyService;
        this.sotwService = sotwService;
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sendHelp(sender); return; }

        switch (args[0].toLowerCase()) {
            case "reload":     handleReload(sender);          break;
            case "give-money": handleGiveMoney(sender, args); break;
            case "set-money":  handleSetMoney(sender, args);  break;
            case "give-key":   handleGiveKey(sender, args);   break;
            case "sotw":       handleSotw(sender, args);      break;
            case "eotw":       handleEotw(sender, args);      break;
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

    private void handleGiveKey(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 3, "/hcf give-key <jugador> <tipo>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(messages.get("hcf.player-not-found")); return; }

        CrateType type;
        try {
            type = CrateType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(messages.get("hcf.give-key-invalid-type"));
            return;
        }

        ItemStack key = CrateListener.createKey(type);
        target.getInventory().addItem(key);
        sender.sendMessage(messages.format("hcf.give-key-sender", "type", type.name(), "player", target.getName()));
        target.sendMessage(messages.format("hcf.give-key-target", "type", type.name()));
    }

    private void handleSotw(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUso: /hcf sotw <start <horas>|stop>"));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "start":
                int hours = 1;
                if (args.length >= 3) {
                    try { hours = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                }
                final long sotwMs = hours * 3600_000L;
                sotwService.startSotw(sotwMs);
                String sotwMsg = messages.format("sotw.started", "hours", String.valueOf(hours));
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) p.sendMessage(sotwMsg);
                break;
            case "stop":
                sotwService.stopSotw();
                String stopMsg = messages.get("sotw.ended");
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) p.sendMessage(stopMsg);
                break;
            default:
                sender.sendMessage(color("&cUso: /hcf sotw <start <horas>|stop>"));
        }
    }

    private void handleEotw(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUso: /hcf eotw <start <horas>|stop>"));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "start":
                int hours = 1;
                if (args.length >= 3) {
                    try { hours = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                }
                final long eotwMs = hours * 3600_000L;
                sotwService.startEotw(eotwMs);
                String eotwMsg = messages.format("eotw.started", "hours", String.valueOf(hours));
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) p.sendMessage(eotwMsg);
                break;
            case "stop":
                sotwService.stopEotw();
                String stopMsg = messages.get("eotw.ended");
                for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) p.sendMessage(stopMsg);
                break;
            default:
                sender.sendMessage(color("&cUso: /hcf eotw <start <horas>|stop>"));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messages.get("hcf.help-header"));
        sender.sendMessage(messages.get("hcf.help-reload"));
        sender.sendMessage(messages.get("hcf.help-give-money"));
        sender.sendMessage(messages.get("hcf.help-set-money"));
        sender.sendMessage(messages.get("hcf.help-give-key"));
        sender.sendMessage(color("&e/hcf sotw <start <horas>|stop> &7- Iniciar/detener SOTW"));
        sender.sendMessage(color("&e/hcf eotw <start <horas>|stop> &7- Iniciar/detener EOTW"));
    }
}

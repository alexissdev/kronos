package dev.alexissdev.kronos.plugin.command;

import dev.alexissdev.kronos.common.command.BaseCommand;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.economy.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Singleton
public class HCFCommand extends BaseCommand {

    private final EconomyService economyService;
    private final Plugin plugin;

    @Inject
    public HCFCommand(EconomyService economyService, Plugin plugin) {
        super("hcf.admin");
        this.economyService = economyService;
        this.plugin = plugin;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sendHelp(sender); return; }

        switch (args[0].toLowerCase()) {
            case "reload": handleReload(sender); break;
            case "give-money": handleGiveMoney(sender, args); break;
            case "set-money": handleSetMoney(sender, args); break;
            default: sendHelp(sender);
        }
    }

    private void handleReload(CommandSender sender) {
        msg(sender, "&eRecargando configuración... (WIP)");
    }

    private void handleGiveMoney(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 3, "/hcf give-money <jugador> <cantidad>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { msg(sender, "&cJugador no encontrado."); return; }

        double amount;
        try { amount = Double.parseDouble(args[2]); } catch (NumberFormatException e) {
            msg(sender, "&cCantidad inválida."); return;
        }

        economyService.deposit(target.getUniqueId(), amount)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    msg(sender, "&aDiste &e$" + amount + "&a a &e" + target.getName());
                    msg(target, "&aRecibiste &e$" + amount + "&a del staff.");
                }));
    }

    private void handleSetMoney(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 3, "/hcf set-money <jugador> <cantidad>")) return;
        msg(sender, "&eSetear balance: (WIP)");
    }

    private void sendHelp(CommandSender sender) {
        msg(sender, "&6&lComandos Administrativos HCF:");
        msg(sender, "&e/hcf reload &7- Recargar configuración");
        msg(sender, "&e/hcf give-money <jugador> <$> &7- Dar dinero");
        msg(sender, "&e/hcf set-money <jugador> <$> &7- Establecer balance");
    }
}

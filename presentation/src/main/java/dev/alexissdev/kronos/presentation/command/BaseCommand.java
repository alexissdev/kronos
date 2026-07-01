package dev.alexissdev.kronos.presentation.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class BaseCommand implements CommandExecutor {

    private final String permission;

    protected BaseCommand(String permission) {
        this.permission = permission;
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (permission != null && !sender.hasPermission(permission)) {
            sender.sendMessage(color("&cNo tienes permiso para usar este comando."));
            return true;
        }
        execute(sender, args);
        return true;
    }

    protected abstract void execute(CommandSender sender, String[] args);

    protected Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cSolo jugadores pueden ejecutar este comando."));
            return null;
        }
        return (Player) sender;
    }

    protected boolean requireArgs(CommandSender sender, String[] args, int required, String usage) {
        if (args.length < required) {
            sender.sendMessage(color("&cUso: " + usage));
            return false;
        }
        return true;
    }

    protected String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    protected void msg(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }
}

package dev.alexissdev.kronos.common.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    private final String permission;

    protected BaseCommand(String permission) {
        this.permission = permission;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return tabComplete(sender, args);
    }

    protected List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
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

    protected List<String> filterPrefix(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(o -> o.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }

    protected List<String> onlinePlayers(String prefix) {
        String lower = prefix.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }

    protected List<String> subcommands(String[] args, String... subs) {
        if (args.length != 1) return Collections.emptyList();
        return filterPrefix(Arrays.asList(subs), args[0]);
    }
}

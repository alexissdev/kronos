package dev.alexissdev.kronos.plugin.command.hcf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.SotwService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

@Singleton
public class SotwSub extends SubCommand {

    private final SotwService    sotwService;
    private final MessagesConfig messages;

    @Inject
    public SotwSub(SotwService sotwService, MessagesConfig messages) {
        this.sotwService = sotwService;
        this.messages    = messages;
    }

    @Override public String name() { return "sotw"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? filterPrefix(Arrays.asList("start", "stop"), args[1]) : List.of();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(color("&cUso: /hcf sotw <start <horas>|stop>")); return; }
        switch (args[1].toLowerCase()) {
            case "start":
                int hours = args.length >= 3 ? parseHours(args[2]) : 1;
                sotwService.startSotw(hours * 3600_000L);
                final String startMsg = messages.format("sotw.started", "hours", String.valueOf(hours));
                for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(startMsg);
                break;
            case "stop":
                sotwService.stopSotw();
                final String stopMsg = messages.get("sotw.ended");
                for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(stopMsg);
                break;
            default:
                sender.sendMessage(color("&cUso: /hcf sotw <start <horas>|stop>"));
        }
    }

    private static int parseHours(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 1; }
    }
}

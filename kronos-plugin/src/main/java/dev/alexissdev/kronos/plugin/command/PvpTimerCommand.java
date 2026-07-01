package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Singleton
public class PvpTimerCommand extends BaseCommand {

    private static final long PVP_TIMER_DURATION_MS = 60 * 60 * 1000L;

    private final TimerApplicationService timerService;
    private final MessagesConfig messages;
    private final Plugin plugin;

    @Inject
    public PvpTimerCommand(TimerApplicationService timerService, MessagesConfig messages, Plugin plugin) {
        super("hcf.admin");
        this.timerService = timerService;
        this.messages     = messages;
        this.plugin       = plugin;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sendHelp(sender); return; }

        switch (args[0].toLowerCase()) {
            case "give":   handleGive(sender, args);   break;
            case "remove": handleRemove(sender, args); break;
            default:       sendHelp(sender);
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/pvptimer give <jugador>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(messages.get("hcf.player-not-found")); return; }

        timerService.startPvpTimer(target.getUniqueId(), PVP_TIMER_DURATION_MS)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(messages.format("pvptimer.give-sender", "player", target.getName()));
                    target.sendMessage(messages.get("pvptimer.give-target"));
                }))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(messages.format("pvptimer.error", "error", ex.getMessage())));
                    return null;
                });
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/pvptimer remove <jugador>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(messages.get("hcf.player-not-found")); return; }

        timerService.cancelTimer(target.getUniqueId(), TimerType.PVP_TIMER)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(messages.format("pvptimer.remove-sender", "player", target.getName()));
                    target.sendMessage(messages.get("pvptimer.remove-target"));
                }))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(messages.format("pvptimer.error", "error", ex.getMessage())));
                    return null;
                });
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messages.get("pvptimer.help-header"));
        sender.sendMessage(messages.get("pvptimer.help-give"));
        sender.sendMessage(messages.get("pvptimer.help-remove"));
    }
}

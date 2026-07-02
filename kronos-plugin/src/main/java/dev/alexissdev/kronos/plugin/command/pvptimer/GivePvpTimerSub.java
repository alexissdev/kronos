package dev.alexissdev.kronos.plugin.command.pvptimer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class GivePvpTimerSub extends SubCommand {

    private static final long DURATION_MS = 60 * 60 * 1000L;

    private final TimerApplicationService timerService;
    private final MessagesConfig          messages;
    private final Plugin                  plugin;

    @Inject
    public GivePvpTimerSub(TimerApplicationService timerService, MessagesConfig messages, Plugin plugin) {
        this.timerService = timerService;
        this.messages     = messages;
        this.plugin       = plugin;
    }

    @Override public String name() { return "give"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? onlinePlayers(args[1]) : List.of();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/pvptimer give <jugador>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(messages.get("hcf.player-not-found")); return; }

        timerService.hasActiveTimer(target.getUniqueId(), TimerType.PVP_TIMER).thenCompose(has -> {
            if (has) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(messages.format("pvptimer.already-has", "player", target.getName())));
                return CompletableFuture.completedFuture(null);
            }
            return timerService.startPvpTimer(target.getUniqueId(), DURATION_MS)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(messages.format("pvptimer.give-sender", "player", target.getName()));
                        target.sendMessage(messages.get("pvptimer.give-target"));
                    }));
        }).exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin, () ->
                sender.sendMessage(messages.format("pvptimer.error", "error", ex.getMessage()))); return null; });
    }
}

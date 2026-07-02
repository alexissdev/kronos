package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Singleton
public class StuckCommand extends BaseCommand {

    private static final long STUCK_DURATION_MS = 30_000L;

    private final TimerApplicationService timerService;
    private final MessagesConfig messages;

    @Inject
    public StuckCommand(TimerApplicationService timerService, MessagesConfig messages) {
        super(null);
        this.timerService = timerService;
        this.messages     = messages;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        if (timerService.hasActiveTimerSync(player.getUniqueId(), TimerType.STUCK)) {
            player.sendMessage(messages.get("stuck.already-active"));
            return;
        }

        timerService.startStuckTimer(player.getUniqueId(), STUCK_DURATION_MS)
                .thenRun(() -> player.sendMessage(messages.get("stuck.started")));
    }
}

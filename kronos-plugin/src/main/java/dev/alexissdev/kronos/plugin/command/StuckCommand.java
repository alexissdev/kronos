package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command {@code /stuck} that lets a player request a rescue teleport when they
 * become trapped inside terrain or a structure. Upon execution a timer of
 * {@value #STUCK_DURATION_MS} ms (30 seconds) is started; if the player remains
 * still and takes no damage during that period, the system teleports them to a
 * safe location. Only one stuck timer may be active per player at a time.
 */
@Singleton
public class StuckCommand extends BaseCommand {

    private static final long STUCK_DURATION_MS = 30_000L;

    private final TimerApplicationService timerService;
    private final MessagesConfig messages;

    /**
     * Constructs the command by injecting its dependencies via Guice.
     *
     * @param timerService timer service that manages the stuck timer
     * @param messages     localised message configuration
     */
    @Inject
    public StuckCommand(TimerApplicationService timerService, MessagesConfig messages) {
        super(null);
        this.timerService = timerService;
        this.messages     = messages;
    }

    /**
     * Verifies that the executor is a player and that they do not already have an
     * active stuck timer. If both checks pass, a new stuck timer is started and the
     * player is notified.
     *
     * @param sender command executor; must be a {@link org.bukkit.entity.Player}
     * @param args   additional arguments (not used by this command)
     */
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

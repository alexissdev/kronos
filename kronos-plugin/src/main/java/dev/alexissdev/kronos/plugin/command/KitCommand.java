package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.players.domain.KitType;
import dev.alexissdev.kronos.players.service.KitService;
import dev.alexissdev.kronos.players.service.PlayerService;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Command {@code /kit} that delivers the kit corresponding to the player's active
 * HCF class (e.g. Archer, Bard, Rogue, etc.). Before applying the kit it checks
 * whether the player has an active class cooldown ({@link TimerType#CLASS_COOLDOWN})
 * and starts a new 60-second cooldown after a successful kit delivery.
 */
@Singleton
public class KitCommand extends BaseCommand {

    private static final long KIT_COOLDOWN_MS = 60_000L;

    private final PlayerService playerService;
    private final KitService    kitService;
    private final TimerApplicationService timerService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Constructs the command by injecting its dependencies via Guice.
     *
     * @param playerService service used to retrieve or create the player's HCF profile
     * @param kitService    service that applies kit items to the player's inventory
     * @param timerService  timer service used to manage the class cooldown
     * @param messages      localised message configuration
     * @param plugin        plugin instance used to schedule tasks on the main thread
     */
    @Inject
    public KitCommand(PlayerService playerService, KitService kitService,
                      TimerApplicationService timerService,
                      MessagesConfig messages, Plugin plugin) {
        super(null);
        this.playerService = playerService;
        this.kitService    = kitService;
        this.timerService  = timerService;
        this.messages      = messages;
        this.plugin        = plugin;
    }

    /**
     * Verifies that the executor is a player with no active class cooldown, retrieves
     * their active class, checks that they hold the required permission for it, and
     * applies the corresponding kit. The operation is asynchronous; the actual kit
     * application is scheduled on the Bukkit main thread.
     *
     * @param sender command executor; must be a {@link org.bukkit.entity.Player}
     * @param args   additional arguments (not used by this command)
     */
    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        if (timerService.hasActiveTimerSync(player.getUniqueId(), TimerType.CLASS_COOLDOWN)) {
            long remaining = timerService.getRemainingSeconds(player.getUniqueId(), TimerType.CLASS_COOLDOWN);
            player.sendMessage(messages.format("kit.cooldown", "seconds", String.valueOf(remaining)));
            return;
        }

        playerService.getOrCreate(player.getUniqueId(), player.getName())
                .thenAccept(hcfPlayer -> Bukkit.getScheduler().runTask(plugin, () -> {
                    KitType kit = hcfPlayer.getActiveKit();
                    if (!player.hasPermission("hcf.kit." + kit.name().toLowerCase())) {
                        player.sendMessage(messages.format("kit.no-permission", "kit", kit.name()));
                        return;
                    }
                    kitService.applyKit(player.getInventory(), kit);
                    timerService.startTimer(player.getUniqueId(), TimerType.CLASS_COOLDOWN, KIT_COOLDOWN_MS);
                    player.sendMessage(messages.format("kit.given", "kit", kit.name()));
                }));
    }

    /**
     * Returns an empty tab-completion list because this command accepts no arguments.
     *
     * @param sender command executor
     * @param args   argument fragment currently typed
     * @return empty list
     */
    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}

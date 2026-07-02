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

@Singleton
public class KitCommand extends BaseCommand {

    private static final long KIT_COOLDOWN_MS = 60_000L;

    private final PlayerService playerService;
    private final KitService    kitService;
    private final TimerApplicationService timerService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

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

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}

package dev.alexissdev.kronos.plugin.command.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.factions.service.FactionService;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Singleton
public class HomeFactionSub extends FactionSubCommand {

    private final FactionService          factionService;
    private final TimerApplicationService timerService;
    private final MessagesConfig          messages;
    private final Plugin                  plugin;
    private final long                    homeDelayMs;

    @Inject
    public HomeFactionSub(FactionService factionService, TimerApplicationService timerService,
                          MessagesConfig messages, Plugin plugin,
                          @Named("home.delay-ms") long homeDelayMs) {
        this.factionService = factionService;
        this.timerService   = timerService;
        this.messages       = messages;
        this.plugin         = plugin;
        this.homeDelayMs    = homeDelayMs;
    }

    @Override public String name() { return "home"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        if (timerService.hasActiveTimerSync(player.getUniqueId(), TimerType.COMBAT_TAG)) {
            player.sendMessage(messages.get("faction.cmd.home-in-combat")); return;
        }
        if (timerService.hasActiveTimerSync(player.getUniqueId(), TimerType.HOME)) {
            player.sendMessage(messages.get("faction.cmd.home-already-teleporting")); return;
        }

        factionService.getByPlayer(player.getUniqueId()).thenAccept(opt -> {
            if (opt.isEmpty()) { Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(messages.get("faction.cmd.not-in-faction"))); return; }
            if (opt.get().getHome() == null) { Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(messages.get("faction.cmd.home-not-set"))); return; }
            timerService.startHomeTimer(player.getUniqueId(), homeDelayMs);
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(messages.format("faction.cmd.home-teleporting", "seconds", homeDelayMs / 1000)));
        });
    }
}

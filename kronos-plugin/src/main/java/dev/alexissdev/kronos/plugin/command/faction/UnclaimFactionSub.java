package dev.alexissdev.kronos.plugin.command.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.claims.service.ClaimService;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.exception.HCFException;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Singleton
public class UnclaimFactionSub extends FactionSubCommand {

    private final FactionService factionService;
    private final ClaimService   claimService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    @Inject
    public UnclaimFactionSub(FactionService factionService, ClaimService claimService,
                             MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.claimService   = claimService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    @Override public String name() { return "unclaim"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        int cx    = player.getLocation().getChunk().getX();
        int cz    = player.getLocation().getChunk().getZ();
        String world = player.getWorld().getName();

        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return claimService.unclaim(opt.get().getId(), player.getUniqueId(), world, cx, cz);
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.format("faction.cmd.unclaimed", "x", cx, "z", cz))))
                .exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin,
                        () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex)))); return null; });
    }
}

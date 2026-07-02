package dev.alexissdev.kronos.plugin.command.crate;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.players.domain.CrateLocation;
import dev.alexissdev.kronos.players.service.CrateService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Singleton
public class ListCrateSub extends SubCommand {

    private final CrateService   crateService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    @Inject
    public ListCrateSub(CrateService crateService, MessagesConfig messages, Plugin plugin) {
        this.crateService = crateService;
        this.messages     = messages;
        this.plugin       = plugin;
    }

    @Override public String name() { return "list"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        crateService.getAllCrates().thenAccept(list -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (list.isEmpty()) { player.sendMessage(messages.get("crate.cmd.list-empty")); return; }
            player.sendMessage(messages.get("crate.cmd.list-header"));
            for (CrateLocation loc : list) {
                player.sendMessage(messages.format("crate.cmd.list-entry",
                        "type",  loc.getType().name(),
                        "world", loc.getWorld(),
                        "x",     String.valueOf(loc.getX()),
                        "y",     String.valueOf(loc.getY()),
                        "z",     String.valueOf(loc.getZ())));
            }
        }));
    }
}

package dev.alexissdev.kronos.spawn.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.spawn.creation.SpawnCreationService;
import dev.alexissdev.kronos.spawn.domain.SpawnZone;
import dev.alexissdev.kronos.spawn.service.SpawnService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

@Singleton
public class SpawnCommand extends BaseCommand {

    private static final String PREFIX = ChatColor.AQUA + "[Spawn] " + ChatColor.RESET;

    private final SpawnService spawnService;
    private final SpawnCreationService creationService;
    private final Plugin plugin;

    @Inject
    public SpawnCommand(SpawnService spawnService,
                        SpawnCreationService creationService,
                        Plugin plugin) {
        super("hcf.spawn.admin");
        this.spawnService    = spawnService;
        this.creationService = creationService;
        this.plugin          = plugin;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sendHelp(sender); return; }

        switch (args[0].toLowerCase()) {
            case "setzone": handleSetZone(sender); break;
            case "info":    handleInfo(sender);    break;
            case "remove":  handleRemove(sender);  break;
            default:        sendHelp(sender);
        }
    }

    private void handleSetZone(CommandSender sender) {
        if (!(sender instanceof Player)) {
            msg(sender, "&cEste comando solo puede usarlo un jugador.");
            return;
        }
        Player player = (Player) sender;
        creationService.startSession(player.getUniqueId());
        player.getInventory().addItem(creationService.createWand());

        player.sendMessage(PREFIX + ChatColor.WHITE + "Selecciona las 2 esquinas de la "
                + ChatColor.AQUA + "zona de spawn" + ChatColor.WHITE + " con el wand:");
        player.sendMessage(ChatColor.GRAY + "  Clic izquierdo → " + ChatColor.GREEN + "Pos 1"
                + ChatColor.GRAY + "  │  Clic derecho → " + ChatColor.YELLOW + "Pos 2");
    }

    private void handleInfo(CommandSender sender) {
        Optional<SpawnZone> zoneOpt = spawnService.getZone();
        if (!zoneOpt.isPresent()) {
            msg(sender, "&cNo hay zona de spawn configurada.");
            return;
        }
        SpawnZone zone = zoneOpt.get();
        msg(sender, "&b&lZona de Spawn:");
        msg(sender, "&7  Mundo: &f" + zone.getWorld());
        msg(sender, "&7  Esquinas: &f(" + zone.getMinX() + ", " + zone.getMinZ() + ")"
                + " &7→ &f(" + zone.getMaxX() + ", " + zone.getMaxZ() + ")");
    }

    private void handleRemove(CommandSender sender) {
        spawnService.removeZone()
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> msg(sender, "&eZona de spawn eliminada.")));
    }

    private void sendHelp(CommandSender sender) {
        msg(sender, "&b&lComandos de Spawn:");
        msg(sender, "&e/spawn setzone &7- Configurar zona con wand");
        msg(sender, "&e/spawn info    &7- Ver zona actual");
        msg(sender, "&e/spawn remove  &7- Eliminar zona");
    }
}

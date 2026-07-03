package dev.alexissdev.kronos.spawn.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.spawn.creation.SpawnCreationService;
import dev.alexissdev.kronos.spawn.domain.SpawnZone;
import dev.alexissdev.kronos.spawn.service.SpawnService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * Comando administrativo {@code /spawn} para la gestión de la zona de spawn del servidor HCF.
 *
 * <p>Permite a los administradores (permiso {@code hcf.spawn.admin}) configurar y consultar
 * la zona de spawn mediante los siguientes subcomandos:
 * <ul>
 *   <li>{@code /spawn setzone} — inicia una sesión de creación interactiva y entrega la varita
 *       de selección al administrador para definir la región de spawn.</li>
 *   <li>{@code /spawn info} — muestra las coordenadas y el mundo de la zona de spawn activa.</li>
 *   <li>{@code /spawn remove} — elimina la zona de spawn configurada.</li>
 * </ul>
 *
 * <p>Solo jugadores pueden usar {@code setzone}; los demás subcomandos están disponibles
 * también para la consola.</p>
 */
@Singleton
public class SpawnCommand extends BaseCommand {

    private final SpawnService spawnService;
    private final SpawnCreationService creationService;
    private final Plugin plugin;
    private final MessagesConfig messages;

    /**
     * Constructor inyectado por Guice con todas las dependencias del comando.
     *
     * @param spawnService    servicio de negocio para gestionar la zona de spawn
     * @param creationService servicio que gestiona la sesión de creación interactiva
     * @param plugin          instancia del plugin de Bukkit, usada para el scheduler
     * @param messages        configuración de mensajes para internacionalización
     */
    @Inject
    public SpawnCommand(SpawnService spawnService,
                        SpawnCreationService creationService,
                        Plugin plugin,
                        MessagesConfig messages) {
        super("hcf.spawn.admin");
        this.spawnService    = spawnService;
        this.creationService = creationService;
        this.plugin          = plugin;
        this.messages        = messages;
    }

    /**
     * Despacha la ejecución del comando {@code /spawn} al sub-manejador correspondiente.
     * Si no se proveen argumentos o el subcomando no es reconocido, muestra el menú de ayuda.
     *
     * @param sender remitente del comando (jugador o consola)
     * @param args   argumentos del comando; {@code args[0]} determina el subcomando
     */
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
            sender.sendMessage(messages.get("spawn.cmd.player-only"));
            return;
        }
        Player player = (Player) sender;
        creationService.startSession(player.getUniqueId());
        player.getInventory().addItem(creationService.createWand());
        player.sendMessage(messages.get("spawn.cmd.select-hint"));
        player.sendMessage(messages.get("spawn.cmd.select-controls"));
    }

    private void handleInfo(CommandSender sender) {
        Optional<SpawnZone> zoneOpt = spawnService.getZone();
        if (!zoneOpt.isPresent()) {
            sender.sendMessage(messages.get("spawn.cmd.no-zone"));
            return;
        }
        SpawnZone zone = zoneOpt.get();
        sender.sendMessage(messages.get("spawn.cmd.info-header"));
        sender.sendMessage(messages.format("spawn.cmd.info-world", "world", zone.getWorld()));
        sender.sendMessage(messages.format("spawn.cmd.info-corners",
                "minX", zone.getMinX(), "minZ", zone.getMinZ(),
                "maxX", zone.getMaxX(), "maxZ", zone.getMaxZ()));
    }

    private void handleRemove(CommandSender sender) {
        spawnService.removeZone()
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> sender.sendMessage(messages.get("spawn.cmd.removed"))));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messages.get("spawn.cmd.help-header"));
        sender.sendMessage(messages.get("spawn.cmd.help-setzone"));
        sender.sendMessage(messages.get("spawn.cmd.help-info"));
        sender.sendMessage(messages.get("spawn.cmd.help-remove"));
    }
}

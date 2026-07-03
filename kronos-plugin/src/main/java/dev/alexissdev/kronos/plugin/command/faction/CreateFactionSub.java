package dev.alexissdev.kronos.plugin.command.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Sub-comando {@code /f create <nombre>} que crea una nueva facción con el
 * nombre indicado y designa al jugador ejecutor como líder. La validación del
 * nombre (unicidad, longitud, caracteres permitidos) es responsabilidad del
 * servicio de facciones.
 */
@Singleton
public class CreateFactionSub extends FactionSubCommand {

    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param factionService servicio de facciones para crear la nueva facción
     * @param messages       configuración de mensajes localizados
     * @param plugin         instancia del plugin, usada para programar tareas en el hilo principal
     */
    @Inject
    public CreateFactionSub(FactionService factionService, MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    /** @return el nombre del sub-comando: {@code "create"} */
    @Override public String name() { return "create"; }

    /**
     * Crea la facción con el nombre proporcionado y establece al ejecutor como líder.
     * En caso de éxito notifica al jugador con el nombre de la facción creada.
     *
     * @param sender ejecutor del comando; debe ser un {@link org.bukkit.entity.Player}
     * @param args   argumentos; {@code args[1]} es el nombre deseado para la facción
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (!requireArgs(player, args, 2, "/f create <nombre>")) return;

        factionService.createFaction(args[1], player.getUniqueId())
                .thenAccept(f -> Bukkit.getScheduler().runTask(plugin,
                        () -> player.sendMessage(messages.format("faction.cmd.created", "name", f.getName()))))
                .exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin,
                        () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex)))); return null; });
    }
}

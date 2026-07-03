package dev.alexissdev.kronos.plugin.command.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.exception.HCFException;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Sub-comando {@code /f ally <faccion>} que establece una relación de alianza
 * entre la facción del ejecutor y la facción indicada. Los aliados no pueden
 * atacarse entre sí y comparten canal de chat especial. El ejecutor debe ser
 * miembro de una facción y tener rango suficiente para modificar relaciones.
 */
@Singleton
public class AllyFactionSub extends FactionSubCommand {

    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param factionService servicio de facciones para gestionar relaciones entre ellas
     * @param messages       configuración de mensajes localizados
     * @param plugin         instancia del plugin, usada para programar tareas en el hilo principal
     */
    @Inject
    public AllyFactionSub(FactionService factionService, MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    /** @return el nombre del sub-comando: {@code "ally"} */
    @Override public String name() { return "ally"; }

    /**
     * Proporciona sugerencias de autocompletado con los nombres de los jugadores
     * en línea para el segundo argumento (nombre de facción objetivo).
     *
     * @param sender ejecutor del comando
     * @param args   argumentos escritos hasta el momento
     * @return lista de jugadores en línea filtrada por prefijo
     */
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? onlinePlayers(args[1]) : List.of();
    }

    /**
     * Obtiene la facción del ejecutor y la facción objetivo por nombre, y establece
     * la relación de alianza entre ambas de forma asíncrona. Notifica al ejecutor
     * en caso de éxito o de error de dominio.
     *
     * @param sender ejecutor del comando; debe ser un {@link org.bukkit.entity.Player}
     * @param args   argumentos; {@code args[1]} es el nombre de la facción con la que aliarse
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (!requireArgs(player, args, 2, "/f ally <faccion>")) return;

        factionService.getByPlayer(player.getUniqueId()).thenCompose(optA -> {
            if (optA.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.getByName(args[1]).thenCompose(optB -> {
                if (optB.isEmpty()) throw new HCFException("Facción no encontrada");
                return factionService.setAlly(optA.get().getId(), optB.get().getId(), player.getUniqueId());
            });
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.get("faction.cmd.ally-set"))))
                .exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin,
                        () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex)))); return null; });
    }
}

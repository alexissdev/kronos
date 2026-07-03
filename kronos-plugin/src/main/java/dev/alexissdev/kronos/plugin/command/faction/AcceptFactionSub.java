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

/**
 * Sub-comando {@code /f accept <faccion>} (alias: {@code /f join}) que permite
 * a un jugador aceptar una invitación pendiente y unirse a la facción especificada.
 * El jugador debe haber recibido previamente una invitación de esa facción;
 * de lo contrario el servicio lanzará una excepción de dominio.
 */
@Singleton
public class AcceptFactionSub extends FactionSubCommand {

    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param factionService servicio de facciones para validar y aceptar invitaciones
     * @param messages       configuración de mensajes localizados
     * @param plugin         instancia del plugin, usada para programar tareas en el hilo principal
     */
    @Inject
    public AcceptFactionSub(FactionService factionService, MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    /** @return el nombre del sub-comando: {@code "accept"} */
    @Override public String   name()    { return "accept"; }

    /** @return alias del sub-comando: {@code ["join"]} */
    @Override public String[] aliases() { return new String[]{"join"}; }

    /**
     * Busca la facción por nombre y acepta la invitación del jugador ejecutor.
     * En caso de error (facción no encontrada o invitación inexistente) notifica
     * al jugador con el mensaje de la excepción de dominio.
     *
     * @param sender ejecutor del comando; debe ser un {@link org.bukkit.entity.Player}
     * @param args   argumentos; {@code args[1]} es el nombre de la facción a unirse
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (!requireArgs(player, args, 2, "/f accept <faccion>")) return;

        factionService.getByName(args[1]).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("Facción no encontrada");
            return factionService.acceptInvite(player.getUniqueId(), opt.get().getId());
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.get("faction.cmd.joined"))))
                .exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin,
                        () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex)))); return null; });
    }
}

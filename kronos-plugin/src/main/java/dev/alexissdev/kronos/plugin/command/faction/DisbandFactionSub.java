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
 * Sub-comando {@code /f disband} que disuelve permanentemente la facción del
 * jugador ejecutor. Esta acción libera todos los chunks reclamados, elimina
 * las relaciones con otras facciones y borra la facción de la persistencia.
 * Solo el líder (u otro rango con permisos suficientes) puede ejecutar este comando.
 */
@Singleton
public class DisbandFactionSub extends FactionSubCommand {

    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param factionService servicio de facciones que gestiona la disolución
     * @param messages       configuración de mensajes localizados
     * @param plugin         instancia del plugin, usada para programar tareas en el hilo principal
     */
    @Inject
    public DisbandFactionSub(FactionService factionService, MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    /** @return el nombre del sub-comando: {@code "disband"} */
    @Override public String name() { return "disband"; }

    /**
     * Obtiene la facción del ejecutor y la disuelve de forma asíncrona.
     * Notifica al jugador en caso de éxito o si ocurre un error de dominio
     * (por ejemplo, si no es el líder).
     *
     * @param sender ejecutor del comando; debe ser un {@link org.bukkit.entity.Player}
     * @param args   argumentos adicionales (no utilizados por este sub-comando)
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.disbandFaction(opt.get().getId(), player.getUniqueId());
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.get("faction.cmd.disbanded"))))
                .exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin,
                        () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex)))); return null; });
    }
}

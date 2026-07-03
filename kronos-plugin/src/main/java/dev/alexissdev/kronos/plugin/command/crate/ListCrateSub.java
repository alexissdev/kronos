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

/**
 * Sub-comando {@code /crate list} que muestra al ejecutor la lista de todos
 * los cofres de recompensas (crates) registrados en el servidor, incluyendo
 * su tipo, mundo y coordenadas. La consulta se realiza de forma asíncrona.
 */
@Singleton
public class ListCrateSub extends SubCommand {

    private final CrateService   crateService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param crateService servicio para consultar los cofres registrados
     * @param messages     configuración de mensajes localizados
     * @param plugin       instancia del plugin, usada para programar tareas en el hilo principal
     */
    @Inject
    public ListCrateSub(CrateService crateService, MessagesConfig messages, Plugin plugin) {
        this.crateService = crateService;
        this.messages     = messages;
        this.plugin       = plugin;
    }

    /** @return el nombre del sub-comando: {@code "list"} */
    @Override public String name() { return "list"; }

    /**
     * Obtiene de forma asíncrona todos los cofres registrados y los imprime
     * al ejecutor en el hilo principal. Si no hay cofres registrados, envía
     * el mensaje correspondiente.
     *
     * @param sender ejecutor del comando; debe ser un {@link org.bukkit.entity.Player}
     * @param args   argumentos adicionales (no utilizados por este sub-comando)
     */
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

package dev.alexissdev.kronos.plugin.command.crate;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.players.service.CrateService;
import dev.alexissdev.kronos.plugin.listener.CrateListener;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;

/**
 * Sub-comando {@code /crate remove} que elimina el cofre de recompensas ubicado
 * en el bloque al que está mirando el ejecutor (a un máximo de 5 bloques de distancia).
 * Tras eliminar el registro de la base de datos, también desregistra el cofre del
 * listener activo para que deje de procesarse en tiempo real.
 */
@Singleton
public class RemoveCrateSub extends SubCommand {

    private final CrateService   crateService;
    private final CrateListener  crateListener;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param crateService  servicio para eliminar cofres de la persistencia
     * @param crateListener listener de cofres, usado para desregistrar el cofre en tiempo real
     * @param messages      configuración de mensajes localizados
     * @param plugin        instancia del plugin, usada para programar tareas en el hilo principal
     */
    @Inject
    public RemoveCrateSub(CrateService crateService, CrateListener crateListener,
                          MessagesConfig messages, Plugin plugin) {
        this.crateService  = crateService;
        this.crateListener = crateListener;
        this.messages      = messages;
        this.plugin        = plugin;
    }

    /** @return el nombre del sub-comando: {@code "remove"} */
    @Override public String name() { return "remove"; }

    /**
     * Obtiene el bloque que el jugador tiene en la mira (máx. 5 bloques),
     * elimina el cofre registrado en esa posición y lo desregistra del listener.
     * En caso de error, notifica al ejecutor con el mensaje de error correspondiente.
     *
     * @param sender ejecutor del comando; debe ser un {@link org.bukkit.entity.Player}
     * @param args   argumentos adicionales (no utilizados por este sub-comando)
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        Block target = player.getTargetBlock((HashSet<Byte>) null, 5);
        if (target == null) { player.sendMessage(messages.get("crate.cmd.no-target")); return; }

        String world = target.getWorld().getName();
        int x = target.getX(), y = target.getY(), z = target.getZ();

        crateService.removeCrate(world, x, y, z)
                .thenRun(() -> {
                    crateListener.unregisterCrate(world, x, y, z);
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(messages.get("crate.cmd.removed")));
                }).exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin, () ->
                player.sendMessage(messages.format("crate.cmd.error", "error", ex.getMessage()))); return null; });
    }
}

package dev.alexissdev.kronos.plugin.command.hcf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.players.service.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Sub-comando {@code /hcf unban <jugador>} que elimina el deathban (baneo por muerte)
 * de un jugador, permitiéndole volver a conectarse al servidor. En los servidores HCF
 * el deathban es la penalización que impide jugar tras morir; este sub-comando es la
 * herramienta del staff para revertirlo manualmente. Soporta tanto jugadores en línea
 * como desconectados.
 */
@Singleton
public class UnbanSub extends SubCommand {

    private final PlayerService  playerService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param playerService servicio de jugadores para verificar y eliminar deathbans
     * @param messages      configuración de mensajes localizados
     * @param plugin        instancia del plugin, usada para programar tareas en el hilo principal
     */
    @Inject
    public UnbanSub(PlayerService playerService, MessagesConfig messages, Plugin plugin) {
        this.playerService = playerService;
        this.messages      = messages;
        this.plugin        = plugin;
    }

    /** @return el nombre del sub-comando: {@code "unban"} */
    @Override public String name() { return "unban"; }

    /**
     * Proporciona sugerencias de autocompletado con los nombres de los jugadores
     * en línea para el segundo argumento.
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
     * Localiza al jugador objetivo (en línea o desconectado), verifica si tiene
     * un deathban activo y, de ser así, lo elimina de forma asíncrona.
     * Notifica al ejecutor del resultado mediante mensajes de éxito o error.
     *
     * @param sender ejecutor del comando (jugador o consola)
     * @param args   argumentos; {@code args[1]} es el nombre del jugador a desbanear
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/hcf unban <jugador>")) return;
        String targetName = args[1];
        Player online = Bukkit.getPlayer(targetName);
        UUID uuid = online != null ? online.getUniqueId() : Bukkit.getOfflinePlayer(targetName).getUniqueId();

        playerService.isDeathbanned(uuid).thenCompose(banned -> {
            if (!banned) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(messages.format("hcf.unban-not-banned", "player", targetName)));
                return CompletableFuture.completedFuture(null);
            }
            return playerService.removeDeathban(uuid).thenRun(() ->
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(messages.format("hcf.unban-success", "player", targetName))));
        }).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(messages.format("hcf.error", "error", ex.getMessage())));
            return null;
        });
    }
}

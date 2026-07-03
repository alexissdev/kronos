package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.factions.service.FactionService;
import dev.alexissdev.kronos.players.service.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Comando {@code /stats} que muestra las estadísticas de combate de un jugador:
 * kills, muertes, vidas restantes y facción a la que pertenece. Si se ejecuta
 * sin argumentos muestra las estadísticas propias; si se especifica un nombre,
 * muestra las de ese jugador (que debe estar en línea). Las consultas se realizan
 * de forma asíncrona combinando los datos del perfil del jugador y su facción.
 */
@Singleton
public class StatsCommand extends BaseCommand {

    private final PlayerService playerService;
    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin plugin;

    /**
     * Construye el comando inyectando sus dependencias mediante Guice.
     *
     * @param playerService  servicio para obtener el perfil HCF del jugador (kills, muertes, vidas)
     * @param factionService servicio para obtener la facción a la que pertenece el jugador
     * @param messages       configuración de mensajes localizados
     * @param plugin         instancia del plugin, usada para programar tareas en el hilo principal
     */
    @Inject
    public StatsCommand(PlayerService playerService, FactionService factionService,
                        MessagesConfig messages, Plugin plugin) {
        super(null);
        this.playerService = playerService;
        this.factionService = factionService;
        this.messages = messages;
        this.plugin = plugin;
    }

    /**
     * Proporciona sugerencias de autocompletado con los nombres de los jugadores
     * en línea para el primer argumento del comando.
     *
     * @param sender ejecutor del comando
     * @param args   argumentos escritos hasta el momento
     * @return lista de nombres de jugadores en línea que comienzan con el prefijo indicado,
     *         o lista vacía si ya se proporcionó el primer argumento
     */
    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return onlinePlayers(args[0]);
        return Collections.emptyList();
    }

    /**
     * Determina el jugador objetivo (el propio ejecutor o el especificado como argumento),
     * consulta de forma asíncrona su perfil HCF y su facción, y muestra las estadísticas
     * en el hilo principal de Bukkit.
     *
     * @param sender ejecutor del comando; debe ser un {@link org.bukkit.entity.Player}
     *               si no se especifica un objetivo
     * @param args   argumentos opcionales; {@code args[0]} puede ser el nombre de un jugador en línea
     */
    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(messages.get("hcf.player-not-found"));
                return;
            }
        } else {
            target = requirePlayer(sender);
            if (target == null) return;
        }

        UUID uuid = target.getUniqueId();
        String targetName = target.getName();

        playerService.getPlayer(uuid).thenCombine(
                factionService.getByPlayer(uuid),
                (playerOpt, factionOpt) -> {
                    int kills  = playerOpt.map(p -> p.getKills()).orElse(0);
                    int deaths = playerOpt.map(p -> p.getDeaths()).orElse(0);
                    int lives  = playerOpt.map(p -> p.getLives()).orElse(0);
                    String faction = factionOpt.map(f -> f.getName()).orElse(messages.get("stats.no-faction"));

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(messages.format("stats.header", "player", targetName));
                        sender.sendMessage(messages.format("stats.faction", "faction", faction));
                        sender.sendMessage(messages.format("stats.kills",   "kills",   String.valueOf(kills)));
                        sender.sendMessage(messages.format("stats.deaths",  "deaths",  String.valueOf(deaths)));
                        sender.sendMessage(messages.format("stats.lives",   "lives",   String.valueOf(lives)));
                    });
                    return null;
                }
        ).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(messages.format("hcf.error", "error", ex.getMessage())));
            return null;
        });
    }
}

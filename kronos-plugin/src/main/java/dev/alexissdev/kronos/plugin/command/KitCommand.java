package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.players.domain.KitType;
import dev.alexissdev.kronos.players.service.KitService;
import dev.alexissdev.kronos.players.service.PlayerService;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Comando {@code /kit} que entrega al jugador el kit correspondiente a su clase
 * activa en el sistema HCF (por ejemplo, Archer, Bard, Rogue, etc.).
 * Antes de aplicar el kit verifica si el jugador tiene un cooldown de clase
 * activo ({@link TimerType#CLASS_COOLDOWN}) e inicia uno nuevo de 60 segundos
 * tras la entrega exitosa.
 */
@Singleton
public class KitCommand extends BaseCommand {

    private static final long KIT_COOLDOWN_MS = 60_000L;

    private final PlayerService playerService;
    private final KitService    kitService;
    private final TimerApplicationService timerService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Construye el comando inyectando sus dependencias mediante Guice.
     *
     * @param playerService servicio para obtener o crear el perfil HCF del jugador
     * @param kitService    servicio que aplica los ítems del kit al inventario
     * @param timerService  servicio de temporizadores para gestionar el cooldown de clase
     * @param messages      configuración de mensajes localizados
     * @param plugin        instancia del plugin, usada para programar tareas en el hilo principal
     */
    @Inject
    public KitCommand(PlayerService playerService, KitService kitService,
                      TimerApplicationService timerService,
                      MessagesConfig messages, Plugin plugin) {
        super(null);
        this.playerService = playerService;
        this.kitService    = kitService;
        this.timerService  = timerService;
        this.messages      = messages;
        this.plugin        = plugin;
    }

    /**
     * Verifica que el ejecutor sea un jugador sin cooldown activo, obtiene su clase
     * activa, comprueba que tenga permiso para ella y aplica el kit correspondiente.
     * El proceso es asíncrono; la aplicación del kit ocurre en el hilo principal de Bukkit.
     *
     * @param sender ejecutor del comando; debe ser un {@link org.bukkit.entity.Player}
     * @param args   argumentos adicionales (no utilizados por este comando)
     */
    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        if (timerService.hasActiveTimerSync(player.getUniqueId(), TimerType.CLASS_COOLDOWN)) {
            long remaining = timerService.getRemainingSeconds(player.getUniqueId(), TimerType.CLASS_COOLDOWN);
            player.sendMessage(messages.format("kit.cooldown", "seconds", String.valueOf(remaining)));
            return;
        }

        playerService.getOrCreate(player.getUniqueId(), player.getName())
                .thenAccept(hcfPlayer -> Bukkit.getScheduler().runTask(plugin, () -> {
                    KitType kit = hcfPlayer.getActiveKit();
                    if (!player.hasPermission("hcf.kit." + kit.name().toLowerCase())) {
                        player.sendMessage(messages.format("kit.no-permission", "kit", kit.name()));
                        return;
                    }
                    kitService.applyKit(player.getInventory(), kit);
                    timerService.startTimer(player.getUniqueId(), TimerType.CLASS_COOLDOWN, KIT_COOLDOWN_MS);
                    player.sendMessage(messages.format("kit.given", "kit", kit.name()));
                }));
    }

    /**
     * Devuelve una lista vacía de sugerencias de autocompletado, ya que este
     * comando no acepta argumentos.
     *
     * @param sender ejecutor del comando
     * @param args   fragmento de argumento actualmente escrito
     * @return lista vacía
     */
    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}

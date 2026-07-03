package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando {@code /stuck} que permite a un jugador solicitar ser teletransportado
 * cuando queda atrapado en el terreno o en una construcción. Al ejecutarlo se
 * inicia un temporizador de {@value #STUCK_DURATION_MS} ms (30 segundos); si el
 * jugador permanece quieto y sin recibir daño, el sistema lo teletransporta a un
 * lugar seguro. Solo puede haber un temporizador de stuck activo por jugador a la vez.
 */
@Singleton
public class StuckCommand extends BaseCommand {

    private static final long STUCK_DURATION_MS = 30_000L;

    private final TimerApplicationService timerService;
    private final MessagesConfig messages;

    /**
     * Construye el comando inyectando las dependencias mediante Guice.
     *
     * @param timerService servicio de temporizadores que gestiona el temporizador de stuck
     * @param messages     configuración de mensajes localizados
     */
    @Inject
    public StuckCommand(TimerApplicationService timerService, MessagesConfig messages) {
        super(null);
        this.timerService = timerService;
        this.messages     = messages;
    }

    /**
     * Verifica que el ejecutor sea un jugador y que no tenga ya un temporizador de
     * stuck activo. Si todo es correcto, inicia el temporizador y notifica al jugador.
     *
     * @param sender ejecutor del comando; debe ser un {@link org.bukkit.entity.Player}
     * @param args   argumentos adicionales (no utilizados por este comando)
     */
    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        if (timerService.hasActiveTimerSync(player.getUniqueId(), TimerType.STUCK)) {
            player.sendMessage(messages.get("stuck.already-active"));
            return;
        }

        timerService.startStuckTimer(player.getUniqueId(), STUCK_DURATION_MS)
                .thenRun(() -> player.sendMessage(messages.get("stuck.started")));
    }
}

package dev.alexissdev.kronos.plugin.command.pvptimer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Sub-comando {@code /pvptimer give <jugador>} que otorga al jugador indicado
 * un temporizador de protección PvP con una duración de 1 hora (3 600 000 ms).
 * Si el jugador ya posee un temporizador activo, la operación es rechazada para
 * evitar duplicaciones.
 */
@Singleton
public class GivePvpTimerSub extends SubCommand {

    private static final long DURATION_MS = 60 * 60 * 1000L;

    private final TimerApplicationService timerService;
    private final MessagesConfig          messages;
    private final Plugin                  plugin;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param timerService servicio de temporizadores que inicia el PvP Timer
     * @param messages     configuración de mensajes localizados
     * @param plugin       instancia del plugin, usada para programar tareas en el hilo principal
     */
    @Inject
    public GivePvpTimerSub(TimerApplicationService timerService, MessagesConfig messages, Plugin plugin) {
        this.timerService = timerService;
        this.messages     = messages;
        this.plugin       = plugin;
    }

    /** @return el nombre del sub-comando: {@code "give"} */
    @Override public String name() { return "give"; }

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
     * Verifica que el jugador objetivo no tenga ya un PvP Timer activo y,
     * de ser posible, inicia uno de {@value #DURATION_MS} ms. Notifica al
     * ejecutor y al destinatario del resultado en el hilo principal de Bukkit.
     *
     * @param sender ejecutor del comando (jugador o consola)
     * @param args   argumentos; {@code args[1]} es el nombre del jugador objetivo
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/pvptimer give <jugador>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(messages.get("hcf.player-not-found")); return; }

        timerService.hasActiveTimer(target.getUniqueId(), TimerType.PVP_TIMER).thenCompose(has -> {
            if (has) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(messages.format("pvptimer.already-has", "player", target.getName())));
                return CompletableFuture.completedFuture(null);
            }
            return timerService.startPvpTimer(target.getUniqueId(), DURATION_MS)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(messages.format("pvptimer.give-sender", "player", target.getName()));
                        target.sendMessage(messages.get("pvptimer.give-target"));
                    }));
        }).exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin, () ->
                sender.sendMessage(messages.format("pvptimer.error", "error", ex.getMessage()))); return null; });
    }
}

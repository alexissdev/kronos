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
 * Sub-comando {@code /pvptimer remove <jugador>} que cancela el temporizador
 * de protección PvP activo de un jugador, exponiéndolo inmediatamente al
 * combate. Si el jugador no tiene ningún temporizador activo, la operación
 * es rechazada con el mensaje de error correspondiente.
 */
@Singleton
public class RemovePvpTimerSub extends SubCommand {

    private final TimerApplicationService timerService;
    private final MessagesConfig          messages;
    private final Plugin                  plugin;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param timerService servicio de temporizadores que cancela el PvP Timer
     * @param messages     configuración de mensajes localizados
     * @param plugin       instancia del plugin, usada para programar tareas en el hilo principal
     */
    @Inject
    public RemovePvpTimerSub(TimerApplicationService timerService, MessagesConfig messages, Plugin plugin) {
        this.timerService = timerService;
        this.messages     = messages;
        this.plugin       = plugin;
    }

    /** @return el nombre del sub-comando: {@code "remove"} */
    @Override public String name() { return "remove"; }

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
     * Verifica que el jugador objetivo tenga un PvP Timer activo y,
     * de ser así, lo cancela de forma asíncrona. Notifica al ejecutor y
     * al destinatario del resultado en el hilo principal de Bukkit.
     *
     * @param sender ejecutor del comando (jugador o consola)
     * @param args   argumentos; {@code args[1]} es el nombre del jugador objetivo
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/pvptimer remove <jugador>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(messages.get("hcf.player-not-found")); return; }

        timerService.hasActiveTimer(target.getUniqueId(), TimerType.PVP_TIMER).thenCompose(has -> {
            if (!has) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(messages.format("pvptimer.does-not-have", "player", target.getName())));
                return CompletableFuture.completedFuture(null);
            }
            return timerService.cancelTimer(target.getUniqueId(), TimerType.PVP_TIMER)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(messages.format("pvptimer.remove-sender", "player", target.getName()));
                        target.sendMessage(messages.get("pvptimer.remove-target"));
                    }));
        }).exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin, () ->
                sender.sendMessage(messages.format("pvptimer.error", "error", ex.getMessage()))); return null; });
    }
}

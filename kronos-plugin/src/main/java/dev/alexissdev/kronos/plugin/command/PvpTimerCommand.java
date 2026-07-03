package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import dev.alexissdev.kronos.common.command.DispatchCommand;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import org.bukkit.command.CommandSender;

import java.util.Set;

/**
 * Comando principal {@code /pvptimer} que agrupa los sub-comandos de gestión
 * del temporizador de protección PvP para jugadores nuevos o recientes.
 * El PvP Timer evita que los jugadores reciban daño de otros jugadores durante
 * un período inicial. Requiere el permiso {@code hcf.admin} y delega a los
 * sub-comandos {@code give} y {@code remove}.
 */
@Singleton
public class PvpTimerCommand extends DispatchCommand {

    private final MessagesConfig messages;

    /**
     * Construye el comando registrando los sub-comandos del grupo {@code pvptimer}
     * inyectados por Guice.
     *
     * @param subs     conjunto de sub-comandos anotado con {@code @Named("pvptimer")}
     * @param messages configuración de mensajes localizados
     */
    @Inject
    public PvpTimerCommand(@Named("pvptimer") Set<SubCommand> subs, MessagesConfig messages) {
        super("hcf.admin");
        this.messages = messages;
        register(subs);
    }

    /**
     * Envía al ejecutor el menú de ayuda del comando {@code /pvptimer} con los
     * sub-comandos disponibles y su sintaxis.
     *
     * @param sender ejecutor del comando que recibirá los mensajes de ayuda
     */
    @Override
    protected void sendUsage(CommandSender sender) {
        sender.sendMessage(messages.get("pvptimer.help-header"));
        sender.sendMessage(messages.get("pvptimer.help-give"));
        sender.sendMessage(messages.get("pvptimer.help-remove"));
    }
}

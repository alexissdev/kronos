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
 * Comando de administración {@code /hcf} que agrupa las operaciones avanzadas
 * del servidor HCF reservadas para el staff. Requiere el permiso {@code hcf.admin}
 * y delega a sub-comandos como {@code reload}, {@code give-money}, {@code set-money},
 * {@code give-key}, {@code sotw}, {@code eotw} y {@code unban}.
 */
@Singleton
public class HCFCommand extends DispatchCommand {

    private final MessagesConfig messages;

    /**
     * Construye el comando registrando los sub-comandos del grupo {@code hcf}
     * inyectados por Guice.
     *
     * @param subs     conjunto de sub-comandos anotado con {@code @Named("hcf")}
     * @param messages configuración de mensajes localizados
     */
    @Inject
    public HCFCommand(@Named("hcf") Set<SubCommand> subs, MessagesConfig messages) {
        super("hcf.admin");
        this.messages = messages;
        register(subs);
    }

    /**
     * Envía al ejecutor el menú de ayuda del comando {@code /hcf} con todos
     * los sub-comandos administrativos disponibles y su sintaxis.
     *
     * @param sender ejecutor del comando que recibirá los mensajes de ayuda
     */
    @Override
    protected void sendUsage(CommandSender sender) {
        sender.sendMessage(messages.get("hcf.help-header"));
        sender.sendMessage(messages.get("hcf.help-reload"));
        sender.sendMessage(messages.get("hcf.help-give-money"));
        sender.sendMessage(messages.get("hcf.help-set-money"));
        sender.sendMessage(messages.get("hcf.help-give-key"));
        sender.sendMessage(color("&e/hcf sotw <start <horas>|stop> &7- Iniciar/detener SOTW"));
        sender.sendMessage(color("&e/hcf eotw <start <horas>|stop> &7- Iniciar/detener EOTW"));
        sender.sendMessage(color("&e/hcf unban <jugador> &7- Quitar deathban a un jugador"));
    }
}

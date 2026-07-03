package dev.alexissdev.kronos.plugin.command.hcf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import org.bukkit.command.CommandSender;

/**
 * Sub-comando {@code /hcf set-money <jugador> <cantidad>} destinado a establecer
 * el balance económico de un jugador a un valor exacto. Actualmente la funcionalidad
 * está pendiente de implementación (WIP) y únicamente muestra un mensaje informativo
 * al ejecutor.
 */
@Singleton
public class SetMoneySub extends SubCommand {

    private final MessagesConfig messages;

    /**
     * Construye el sub-comando inyectando las dependencias mediante Guice.
     *
     * @param messages configuración de mensajes localizados
     */
    @Inject
    public SetMoneySub(MessagesConfig messages) {
        this.messages = messages;
    }

    /** @return el nombre del sub-comando: {@code "set-money"} */
    @Override public String name() { return "set-money"; }

    /**
     * Valida que se proporcionen los argumentos requeridos y notifica al ejecutor
     * que esta funcionalidad está pendiente de implementación.
     *
     * @param sender ejecutor del comando (jugador o consola)
     * @param args   argumentos; {@code args[1]} es el nombre del jugador,
     *               {@code args[2]} es la cantidad deseada
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 3, "/hcf set-money <jugador> <cantidad>")) return;
        sender.sendMessage(messages.get("hcf.set-money-wip"));
    }
}

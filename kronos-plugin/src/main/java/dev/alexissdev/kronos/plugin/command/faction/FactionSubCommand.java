package dev.alexissdev.kronos.plugin.command.faction;

import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.domain.FactionMember;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Clase base abstracta para todos los sub-comandos del sistema de facciones HCF.
 * Proporciona utilidades comunes para la extracción de mensajes de error de
 * excepciones encadenadas ({@link #rootMsg}) y para notificar en tiempo real a
 * los miembros de una facción que estén conectados ({@link #notifyMembers}).
 */
abstract class FactionSubCommand extends SubCommand {

    /**
     * Extrae el mensaje raíz de una excepción, siguiendo la cadena de causas si existe.
     * Si el mensaje es nulo, devuelve el nombre simple de la clase de la causa.
     * Útil para mostrar errores de dominio ({@link dev.alexissdev.kronos.common.exception.HCFException})
     * de forma legible al jugador.
     *
     * @param ex excepción cuya causa raíz se desea obtener
     * @return mensaje de la causa raíz, o nombre de la clase si el mensaje es {@code null}
     */
    protected static String rootMsg(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }

    /**
     * Envía un mensaje a todos los miembros de la facción que estén conectados en ese momento.
     * Los miembros desconectados son ignorados.
     *
     * @param faction facción cuyos miembros serán notificados
     * @param msg     mensaje a enviar a cada miembro en línea
     */
    protected static void notifyMembers(Faction faction, String msg) {
        for (FactionMember m : faction.getMembers().values()) {
            Player p = Bukkit.getPlayer(m.getUuid());
            if (p != null) p.sendMessage(msg);
        }
    }
}

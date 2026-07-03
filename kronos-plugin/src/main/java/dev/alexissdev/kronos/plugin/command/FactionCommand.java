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
 * Comando principal {@code /f} ({@code /faction}) que agrupa todos los
 * sub-comandos del sistema de facciones HCF. Actúa como punto de entrada
 * central y delega cada operación al sub-comando correspondiente (create,
 * disband, invite, accept, leave, kick, info, top, ally, enemy, deposit,
 * withdraw, claim, unclaim, overclaim, map, sethome, home, rename, neutral,
 * entre otros).
 */
@Singleton
public class FactionCommand extends DispatchCommand {

    private final MessagesConfig messages;

    /**
     * Construye el comando registrando todos los sub-comandos de facción
     * inyectados por Guice.
     *
     * @param subs     conjunto de sub-comandos del grupo {@code faction} anotado con {@code @Named("faction")}
     * @param messages configuración de mensajes localizados
     */
    @Inject
    public FactionCommand(@Named("faction") Set<SubCommand> subs, MessagesConfig messages) {
        super(null);
        this.messages = messages;
        register(subs);
    }

    /**
     * Envía al ejecutor el menú de ayuda completo del comando {@code /f} con
     * la lista de todos los sub-comandos disponibles y su sintaxis.
     *
     * @param sender ejecutor del comando que recibirá los mensajes de ayuda
     */
    @Override
    protected void sendUsage(CommandSender sender) {
        sender.sendMessage(messages.get("faction.cmd.help-header"));
        sender.sendMessage(messages.get("faction.cmd.help-create"));
        sender.sendMessage(messages.get("faction.cmd.help-disband"));
        sender.sendMessage(messages.get("faction.cmd.help-invite"));
        sender.sendMessage(messages.get("faction.cmd.help-accept"));
        sender.sendMessage(messages.get("faction.cmd.help-leave"));
        sender.sendMessage(messages.get("faction.cmd.help-kick"));
        sender.sendMessage(messages.get("faction.cmd.help-info"));
        sender.sendMessage(messages.get("faction.cmd.help-top"));
        sender.sendMessage(messages.get("faction.cmd.help-ally"));
        sender.sendMessage(messages.get("faction.cmd.help-enemy"));
        sender.sendMessage(messages.get("faction.cmd.help-deposit"));
        sender.sendMessage(messages.get("faction.cmd.help-withdraw"));
        sender.sendMessage(messages.get("faction.cmd.help-claim"));
        sender.sendMessage(messages.get("faction.cmd.help-unclaim"));
        sender.sendMessage(messages.get("faction.cmd.help-overclaim"));
        sender.sendMessage(messages.get("faction.cmd.help-map"));
        sender.sendMessage(messages.get("faction.cmd.help-sethome"));
        sender.sendMessage(messages.get("faction.cmd.help-home"));
        sender.sendMessage(messages.get("faction.cmd.help-rename"));
        sender.sendMessage(messages.get("faction.cmd.help-neutral"));
    }
}

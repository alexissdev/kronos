package dev.alexissdev.kronos.plugin.command.hcf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.plugin.listener.CrateListener;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sub-comando {@code /hcf give-key <jugador> <tipo>} que entrega al jugador
 * indicado una llave de cofre (crate key) del tipo especificado. Las llaves se
 * crean como {@link org.bukkit.inventory.ItemStack} personalizados y se añaden
 * directamente al inventario del destinatario.
 */
@Singleton
public class GiveKeySub extends SubCommand {

    private final MessagesConfig messages;

    /**
     * Construye el sub-comando inyectando las dependencias mediante Guice.
     *
     * @param messages configuración de mensajes localizados
     */
    @Inject
    public GiveKeySub(MessagesConfig messages) {
        this.messages = messages;
    }

    /** @return el nombre del sub-comando: {@code "give-key"} */
    @Override public String name() { return "give-key"; }

    /**
     * Proporciona sugerencias de autocompletado: nombres de jugadores en línea para
     * el segundo argumento y tipos de crate para el tercero.
     *
     * @param sender ejecutor del comando
     * @param args   argumentos escritos hasta el momento
     * @return lista de sugerencias filtradas por el prefijo del argumento actual
     */
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) return onlinePlayers(args[1]);
        if (args.length == 3) return filterPrefix(
                Arrays.stream(CrateType.values()).map(CrateType::name).collect(Collectors.toList()), args[2]);
        return List.of();
    }

    /**
     * Valida los argumentos, localiza al jugador objetivo y el tipo de crate,
     * crea la llave correspondiente y la añade al inventario del destinatario.
     * Notifica tanto al ejecutor como al receptor del ítem.
     *
     * @param sender ejecutor del comando (jugador o consola)
     * @param args   argumentos; {@code args[1]} es el nombre del jugador objetivo,
     *               {@code args[2]} es el tipo de crate
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 3, "/hcf give-key <jugador> <tipo>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(messages.get("hcf.player-not-found")); return; }

        CrateType type;
        try { type = CrateType.valueOf(args[2].toUpperCase()); }
        catch (IllegalArgumentException e) { sender.sendMessage(messages.get("hcf.give-key-invalid-type")); return; }

        ItemStack key = CrateListener.createKey(type);
        target.getInventory().addItem(key);
        sender.sendMessage(messages.format("hcf.give-key-sender", "type", type.name(), "player", target.getName()));
        target.sendMessage(messages.format("hcf.give-key-target", "type", type.name()));
    }
}

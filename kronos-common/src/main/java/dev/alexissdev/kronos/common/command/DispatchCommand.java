package dev.alexissdev.kronos.common.command;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extension of {@link BaseCommand} that implements the sub-command dispatch pattern
 * for the Kronos HCF plugin's command framework.
 *
 * <p>Allows registering multiple {@link SubCommand} instances with their respective aliases
 * and delegates execution and tab-completion to the sub-command whose name matches the
 * first argument provided by the player. If the first argument does not match any
 * registered sub-command, {@link #sendUsage(CommandSender)} is called to guide the user.</p>
 *
 * <p>Typical usage example: the {@code /faction} command has sub-commands such as
 * {@code create}, {@code invite}, {@code leave}, etc., each registered as a
 * {@link SubCommand} instance.</p>
 */
public abstract class DispatchCommand extends BaseCommand {

    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    /**
     * Construye un DispatchCommand con el permiso de Bukkit requerido para ejecutarlo.
     *
     * @param permission nodo de permiso necesario, o {@code null} si no se requiere permiso
     */
    protected DispatchCommand(String permission) {
        super(permission);
    }

    /**
     * Registra un conjunto de {@link SubCommand} de forma masiva.
     * Equivale a llamar {@link #register(SubCommand)} para cada elemento del conjunto.
     *
     * @param subs conjunto de subcomandos a registrar en este comando padre
     */
    protected void register(Set<SubCommand> subs) {
        subs.forEach(this::register);
    }

    /**
     * Registra un {@link SubCommand} individual junto con todos sus aliases.
     * Tanto el nombre principal como cada alias se mapean al mismo objeto para
     * que el despacho funcione independientemente de cuál forma use el jugador.
     *
     * @param sub subcomando a registrar; se registra su nombre y todos sus aliases
     */
    protected void register(SubCommand sub) {
        subCommands.put(sub.name(), sub);
        for (String alias : sub.aliases()) {
            subCommands.put(alias, sub);
        }
    }

    /**
     * Despacha la ejecución al {@link SubCommand} cuyo nombre o alias coincida con
     * el primer argumento del comando. Si no se proporcionan argumentos o el primer
     * argumento no coincide con ningún subcomando registrado, se muestra el uso.
     *
     * @param sender quien ejecutó el comando
     * @param args   argumentos del comando; {@code args[0]} identifica el subcomando destino
     */
    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sendUsage(sender); return; }
        SubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub == null) { sendUsage(sender); return; }
        sub.execute(sender, args);
    }

    /**
     * Genera sugerencias de autocompletado de forma jerárquica:
     * <ul>
     *   <li>Si solo hay un argumento escrito, sugiere los nombres de los subcomandos registrados.</li>
     *   <li>Si hay más argumentos, delega el autocompletado al {@link SubCommand} que coincida.</li>
     * </ul>
     *
     * @param sender quien está escribiendo el comando
     * @param args   argumentos parciales ya introducidos
     * @return lista de sugerencias apropiadas para el contexto actual
     */
    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return filterPrefix(new ArrayList<>(subCommands.keySet()), args[0]);
        SubCommand sub = subCommands.get(args[0].toLowerCase());
        return sub != null ? sub.tabComplete(sender, args) : List.of();
    }

    /**
     * Muestra el mensaje de uso o ayuda del comando al ejecutante.
     * Las subclases deben sobreescribir este método para enviar información
     * contextual sobre los subcomandos disponibles.
     *
     * @param sender quien recibirá el mensaje de uso
     */
    protected void sendUsage(CommandSender sender) {}
}

package dev.alexissdev.kronos.common.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract base class for all commands in the Kronos HCF plugin.
 *
 * <p>Implements Spigot's {@link CommandExecutor} and {@link TabCompleter} to provide
 * a unified command-handling structure. Validates permissions before delegating
 * execution to the {@link #execute(CommandSender, String[])} method that each
 * subclass must implement.</p>
 *
 * <p>Provides shared utility methods for player verification, argument validation,
 * message colorization, and tab-completion suggestion generation.</p>
 */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    private final String permission;

    /**
     * Construye un comando con el permiso de Bukkit requerido para ejecutarlo.
     *
     * @param permission nodo de permiso que el ejecutante debe poseer, o {@code null}
     *                   si el comando no requiere permiso especial
     */
    protected BaseCommand(String permission) {
        this.permission = permission;
    }

    /**
     * Punto de entrada del autocompletado de Spigot. Delega la lógica al método
     * {@link #tabComplete(CommandSender, String[])} para que las subclases puedan
     * sobreescribirlo sin lidiar con la firma completa de la API de Bukkit.
     *
     * @param sender  quien envía el comando (jugador o consola)
     * @param command objeto de comando de Bukkit
     * @param alias   alias utilizado para invocar el comando
     * @param args    argumentos escritos hasta el momento
     * @return lista de sugerencias de autocompletado; nunca {@code null}
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return tabComplete(sender, args);
    }

    /**
     * Genera las sugerencias de autocompletado para este comando.
     *
     * <p>Las subclases deben sobreescribir este método para devolver opciones
     * contextuales según los argumentos ya escritos. Por defecto devuelve una lista vacía.</p>
     *
     * @param sender quien escribe el comando
     * @param args   argumentos parciales ya introducidos
     * @return lista (posiblemente vacía) de sugerencias
     */
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    /**
     * Punto de entrada de ejecución de Spigot. Verifica el permiso antes de llamar a
     * {@link #execute(CommandSender, String[])}. Este método es {@code final} para
     * garantizar que la comprobación de permisos no pueda omitirse en subclases.
     *
     * @param sender  quien ejecuta el comando
     * @param command objeto de comando de Bukkit
     * @param label   alias utilizado
     * @param args    argumentos del comando
     * @return siempre {@code true} para evitar que Bukkit muestre el mensaje de uso genérico
     */
    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (permission != null && !sender.hasPermission(permission)) {
            sender.sendMessage(color("&cNo tienes permiso para usar este comando."));
            return true;
        }
        execute(sender, args);
        return true;
    }

    /**
     * Contiene la lógica principal del comando. Cada subclase debe implementar aquí
     * el comportamiento específico, sabiendo que los permisos ya fueron verificados.
     *
     * @param sender quien ejecutó el comando (jugador, consola, bloque de comandos, etc.)
     * @param args   arreglo de argumentos proporcionados por el ejecutante
     */
    protected abstract void execute(CommandSender sender, String[] args);

    /**
     * Verifica que el ejecutante sea un jugador conectado. Si es consola u otro tipo
     * de entidad, envía un mensaje de error y retorna {@code null}.
     *
     * <p>Uso típico al inicio de {@code execute} cuando el comando solo aplica a jugadores.</p>
     *
     * @param sender el ejecutante a verificar
     * @return el {@link Player} correspondiente, o {@code null} si no es un jugador
     */
    protected Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cSolo jugadores pueden ejecutar este comando."));
            return null;
        }
        return (Player) sender;
    }

    /**
     * Valida que el ejecutante haya proporcionado al menos la cantidad mínima de argumentos.
     * En caso contrario, muestra el uso correcto del comando.
     *
     * @param sender   quien ejecutó el comando
     * @param args     arreglo de argumentos recibidos
     * @param required cantidad mínima de argumentos esperada
     * @param usage    texto de uso que se mostrará al jugador si faltan argumentos (p. ej. {@code "/money pay <jugador> <cantidad>"})
     * @return {@code true} si se cumple el mínimo de argumentos; {@code false} en caso contrario
     */
    protected boolean requireArgs(CommandSender sender, String[] args, int required, String usage) {
        if (args.length < required) {
            sender.sendMessage(color("&cUso: " + usage));
            return false;
        }
        return true;
    }

    /**
     * Traduce los códigos de color estilo Bukkit ({@code &a}, {@code &c}, etc.)
     * al formato de sección ({@code §}) que Minecraft reconoce.
     *
     * @param message texto con códigos de color con prefijo {@code &}
     * @return texto con los colores procesados listos para enviarse a un jugador
     */
    protected String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Atajo para colorizar y enviar un mensaje a un {@link CommandSender}.
     *
     * @param sender  destinatario del mensaje
     * @param message texto con códigos de color con prefijo {@code &}
     */
    protected void msg(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    /**
     * Filtra una lista de opciones conservando solo las que comienzan con el prefijo dado.
     * Útil para generar sugerencias de autocompletado basadas en lo que el jugador ya escribió.
     *
     * @param options lista completa de opciones posibles
     * @param prefix  texto ya escrito por el jugador que se usa como filtro
     * @return sublista de opciones cuyo inicio coincide con {@code prefix} (sin distinguir mayúsculas)
     */
    protected List<String> filterPrefix(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(o -> o.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }

    /**
     * Devuelve los nombres de los jugadores actualmente conectados al servidor
     * que coincidan con el prefijo dado. Se usa para autocompletar argumentos que
     * esperan el nombre de un jugador online.
     *
     * @param prefix texto inicial del nombre a autocompletar
     * @return lista de nombres de jugadores online cuyo nombre empieza con {@code prefix}
     */
    protected List<String> onlinePlayers(String prefix) {
        String lower = prefix.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }

    /**
     * Genera sugerencias de autocompletado para el primer argumento de un comando
     * a partir de los nombres de subcomandos registrados, filtrando por el texto ya escrito.
     *
     * <p>Solo produce resultados cuando {@code args.length == 1}; en cualquier otro caso
     * devuelve una lista vacía.</p>
     *
     * @param args arreglo de argumentos actual del comando
     * @param subs nombres de los subcomandos disponibles
     * @return lista filtrada de subcomandos que coinciden con lo escrito
     */
    protected List<String> subcommands(String[] args, String... subs) {
        if (args.length != 1) return Collections.emptyList();
        return filterPrefix(Arrays.asList(subs), args[0]);
    }
}

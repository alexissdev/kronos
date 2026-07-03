package dev.alexissdev.kronos.common.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract base class for sub-commands in the Kronos HCF plugin.
 *
 * <p>A {@code SubCommand} represents a single action within a {@link DispatchCommand}.
 * Each subclass encapsulates the logic of a specific operation (e.g. {@code /faction create},
 * {@code /money pay}) and can register aliases so the player can invoke it using
 * alternative forms of the primary name.</p>
 *
 * <p>Sub-commands are registered in a {@link DispatchCommand} via
 * {@link DispatchCommand#register(SubCommand)} and are automatically invoked when
 * the first argument of the parent command matches their name or one of their aliases.</p>
 *
 * <p>Provides the same validation and colorization utilities as {@link dev.alexissdev.kronos.common.command.BaseCommand}
 * to maintain consistency across the entire command layer of the plugin.</p>
 */
public abstract class SubCommand {

    /**
     * Devuelve el nombre principal de este subcomando, en minúsculas.
     * Este valor es la clave con la que se registra en {@link DispatchCommand}.
     *
     * @return nombre único del subcomando (p. ej. {@code "pay"}, {@code "create"})
     */
    public abstract String name();

    /**
     * Devuelve los aliases alternativos con los que también puede invocarse este subcomando.
     * Por defecto no hay aliases registrados.
     *
     * @return arreglo de aliases; vacío si el subcomando no tiene alternativas
     */
    public String[] aliases() {
        return new String[0];
    }

    /**
     * Contiene la lógica principal de este subcomando. Se invoca cuando el jugador
     * usa el nombre o alias de este subcomando como primer argumento del comando padre.
     *
     * @param sender quien ejecutó el comando
     * @param args   todos los argumentos del comando padre, donde {@code args[0]} es el nombre
     *               de este subcomando y los siguientes son sus propios argumentos
     */
    public abstract void execute(CommandSender sender, String[] args);

    /**
     * Genera sugerencias de autocompletado específicas para este subcomando.
     * Se invoca cuando el jugador ya escribió el nombre del subcomando y continúa
     * con más argumentos. Por defecto devuelve una lista vacía.
     *
     * @param sender quien está escribiendo el comando
     * @param args   argumentos actuales, donde {@code args[0]} ya es el nombre del subcomando
     * @return lista de sugerencias para el argumento actual; vacía si no hay sugerencias
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    /**
     * Verifica que el ejecutante sea un jugador conectado. Si es consola u otro tipo,
     * envía un mensaje de error y retorna {@code null}.
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
     * Valida que se hayan proporcionado al menos la cantidad mínima de argumentos.
     * En caso contrario, muestra el uso correcto del subcomando.
     *
     * @param sender   quien ejecutó el subcomando
     * @param args     argumentos recibidos
     * @param required cantidad mínima de argumentos requerida
     * @param usage    texto de uso que se mostrará si faltan argumentos
     * @return {@code true} si hay suficientes argumentos; {@code false} en caso contrario
     */
    protected boolean requireArgs(CommandSender sender, String[] args, int required, String usage) {
        if (args.length < required) {
            sender.sendMessage(color("&cUso: " + usage));
            return false;
        }
        return true;
    }

    /**
     * Traduce los códigos de color con prefijo {@code &} al formato de sección
     * que Minecraft reconoce en los mensajes de chat.
     *
     * @param text texto con códigos de color {@code &}
     * @return texto con los colores procesados
     */
    protected String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Filtra una lista de opciones conservando solo las que comienzan con el prefijo dado.
     * Útil para generar sugerencias de autocompletado contextuales.
     *
     * @param options lista completa de opciones posibles
     * @param prefix  texto ya escrito por el jugador
     * @return sublista filtrada por el prefijo (sin distinguir mayúsculas)
     */
    protected List<String> filterPrefix(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(o -> o.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }

    /**
     * Devuelve los nombres de los jugadores online cuyo nombre empieza con el prefijo dado.
     * Se utiliza para autocompletar argumentos que esperan el nombre de un jugador conectado.
     *
     * @param prefix texto inicial del nombre a autocompletar
     * @return lista de nombres de jugadores online que coinciden con el prefijo
     */
    protected List<String> onlinePlayers(String prefix) {
        String lower = prefix.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}

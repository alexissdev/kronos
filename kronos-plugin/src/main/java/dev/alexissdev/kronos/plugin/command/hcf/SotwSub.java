package dev.alexissdev.kronos.plugin.command.hcf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.SotwService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Sub-comando {@code /hcf sotw <start <horas>|stop>} que gestiona el período
 * SOTW (Start of The World): la fase inicial de un nuevo mapa HCF en la que
 * el PvP entre jugadores está deshabilitado para dar tiempo a que las facciones
 * se establezcan. Al iniciarse o detenerse se difunde un mensaje global a todos
 * los jugadores en línea.
 */
@Singleton
public class SotwSub extends SubCommand {

    private final SotwService    sotwService;
    private final MessagesConfig messages;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param sotwService servicio que controla el estado del SOTW/EOTW
     * @param messages    configuración de mensajes localizados
     */
    @Inject
    public SotwSub(SotwService sotwService, MessagesConfig messages) {
        this.sotwService = sotwService;
        this.messages    = messages;
    }

    /** @return el nombre del sub-comando: {@code "sotw"} */
    @Override public String name() { return "sotw"; }

    /**
     * Proporciona sugerencias de autocompletado con las opciones {@code start}
     * y {@code stop} para el segundo argumento del sub-comando.
     *
     * @param sender ejecutor del comando
     * @param args   argumentos escritos hasta el momento
     * @return lista con las opciones {@code ["start", "stop"]} filtradas por prefijo
     */
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? filterPrefix(Arrays.asList("start", "stop"), args[1]) : List.of();
    }

    /**
     * Inicia o detiene el período SOTW según la subacción indicada.
     * Con {@code start} activa el SOTW por la duración en horas especificada (por defecto 1 hora)
     * y notifica a todos los jugadores; con {@code stop} lo desactiva con notificación global.
     *
     * @param sender ejecutor del comando
     * @param args   argumentos; {@code args[0]} es {@code "sotw"}, {@code args[1]} es
     *               {@code "start"} o {@code "stop"}, {@code args[2]} es la duración en horas (opcional)
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(color("&cUso: /hcf sotw <start <horas>|stop>")); return; }
        switch (args[1].toLowerCase()) {
            case "start":
                int hours = args.length >= 3 ? parseHours(args[2]) : 1;
                sotwService.startSotw(hours * 3600_000L);
                final String startMsg = messages.format("sotw.started", "hours", String.valueOf(hours));
                for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(startMsg);
                break;
            case "stop":
                sotwService.stopSotw();
                final String stopMsg = messages.get("sotw.ended");
                for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(stopMsg);
                break;
            default:
                sender.sendMessage(color("&cUso: /hcf sotw <start <horas>|stop>"));
        }
    }

    /**
     * Intenta parsear una cadena de texto como número entero de horas.
     * Si el texto no es un número válido, devuelve {@code 1} como valor por defecto.
     *
     * @param s cadena de texto a parsear
     * @return número de horas o {@code 1} si la cadena no es un entero válido
     */
    private static int parseHours(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 1; }
    }
}

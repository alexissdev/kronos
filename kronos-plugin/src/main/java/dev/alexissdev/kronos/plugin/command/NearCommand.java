package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Comando {@code /near} que muestra al jugador la lista de otros jugadores
 * presentes en su mismo mundo dentro de un radio especificado.
 * El radio por defecto es de {@value #DEFAULT_RADIUS} bloques y el máximo
 * permitido es {@value #MAX_RADIUS} bloques. Los resultados se ordenan
 * alfabéticamente e incluyen la distancia de cada jugador al ejecutor.
 */
@Singleton
public class NearCommand extends BaseCommand {

    private static final int DEFAULT_RADIUS = 200;
    private static final int MAX_RADIUS     = 500;

    private final MessagesConfig messages;

    /**
     * Construye el comando inyectando las dependencias mediante Guice.
     *
     * @param messages configuración de mensajes localizados
     */
    @Inject
    public NearCommand(MessagesConfig messages) {
        super(null);
        this.messages = messages;
    }

    /**
     * Busca los jugadores cercanos al ejecutor dentro del radio indicado
     * (o el radio por defecto si no se especifica), los ordena alfabéticamente
     * con su distancia y los muestra en pantalla.
     *
     * @param sender ejecutor del comando; debe ser un {@link org.bukkit.entity.Player}
     * @param args   argumentos opcionales; {@code args[0]} puede ser un radio numérico
     */
    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        int radius = DEFAULT_RADIUS;
        if (args.length >= 1) {
            try {
                radius = Math.min(Math.abs(Integer.parseInt(args[0])), MAX_RADIUS);
            } catch (NumberFormatException e) {
                sender.sendMessage(messages.get("hcf.amount-invalid"));
                return;
            }
        }

        List<String> nearby = new ArrayList<>();
        final double radiusSquared = (double) radius * radius;

        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) continue;
            double distSq = player.getLocation().distanceSquared(other.getLocation());
            if (distSq <= radiusSquared) {
                nearby.add(other.getName() + " &7(" + (int) Math.sqrt(distSq) + "m)");
            }
        }

        nearby.sort(Comparator.naturalOrder());

        if (nearby.isEmpty()) {
            sender.sendMessage(messages.format("near.none", "radius", String.valueOf(radius)));
        } else {
            sender.sendMessage(messages.format("near.header", "radius", String.valueOf(radius),
                    "count", String.valueOf(nearby.size())));
            for (String entry : nearby) {
                sender.sendMessage(messages.format("near.entry", "player", entry));
            }
        }
    }
}

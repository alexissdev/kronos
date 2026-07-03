package dev.alexissdev.kronos.plugin.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.players.repository.DeathbanRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.OptionalLong;
import java.util.UUID;

/**
 * Listener que bloquea el acceso al servidor de los jugadores con un deathban activo.
 *
 * <p>En el sistema HCF, cuando un jugador pierde todas sus vidas es sometido a un deathban:
 * una prohibición temporal que le impide volver a conectarse durante un período configurado
 * (típicamente 24 horas). Este listener intercepta el evento de pre-login (antes de que el
 * jugador complete la conexión) para rechazarlo con un mensaje informativo que indica el
 * tiempo restante de ban.
 *
 * <p>La consulta al repositorio se realiza de forma bloqueante ({@code .join()}) porque el
 * evento {@link org.bukkit.event.player.AsyncPlayerPreLoginEvent} ocurre en un hilo asíncrono
 * y requiere una respuesta inmediata antes de continuar el handshake.
 */
@Singleton
public class DeathbanListener implements Listener {

    private final DeathbanRepository deathbanRepository;
    private final MessagesConfig messages;

    /**
     * Crea el listener con sus dependencias inyectadas por Guice.
     *
     * @param deathbanRepository repositorio para consultar el tiempo restante del deathban de un jugador
     * @param messages           configuración de mensajes localizada del plugin
     */
    @Inject
    public DeathbanListener(DeathbanRepository deathbanRepository, MessagesConfig messages) {
        this.deathbanRepository = deathbanRepository;
        this.messages = messages;
    }

    /**
     * Verifica si el jugador tiene un deathban activo al intentar conectarse y, en ese caso,
     * rechaza la conexión mostrando el tiempo restante del ban.
     *
     * <p>La consulta es bloqueante dado que el evento se ejecuta en un hilo asíncrono. Si el
     * jugador no tiene deathban (o ya expiró), el evento no se modifica y la conexión continúa.
     *
     * @param event evento de pre-login asíncrono que permite rechazar la conexión antes del join
     */
    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        OptionalLong remaining = deathbanRepository.getRemainingSeconds(uuid).join();
        if (remaining.isPresent()) {
            String formatted = formatDuration(remaining.getAsLong());
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    messages.format("deathban.banned-screen", "time", formatted));
        }
    }

    private static String formatDuration(long totalSeconds) {
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0)   return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}

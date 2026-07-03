package dev.alexissdev.kronos.scoreboard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Componente responsable de construir la lista de líneas que se mostrarán en el
 * marcador lateral (sidebar) de cada jugador durante un tick del servidor.
 * <p>
 * Recibe un {@link PlayerBoardData} con el snapshot actual del jugador y la colección
 * de {@link KothEntry} activos, y produce una lista de cadenas de texto formateadas
 * con colores de Minecraft. El resultado es consumido por {@link PlayerBoard#render(java.util.List)}
 * para actualizar el marcador sin parpadeo.
 * </p>
 * <p>
 * El contenido del marcador se construye en este orden:
 * </p>
 * <ol>
 *   <li>Separador superior</li>
 *   <li>Facción y DTK (o mensaje de "sin facción")</li>
 *   <li>Kills, deaths y balance económico</li>
 *   <li>Información de KOTHs activos (nombre, coordenadas, tiempo de captura)</li>
 *   <li>Timers individuales activos del jugador (combate, PvP, enderpearl, etc.)</li>
 *   <li>Contadores globales SOTW/EOTW si están activos</li>
 *   <li>Separador y pie de página</li>
 * </ol>
 * <p>
 * Todas las plantillas de texto se obtienen de {@link dev.alexissdev.kronos.common.config.MessagesConfig},
 * lo que permite personalizar el formato del scoreboard sin modificar el código fuente.
 * </p>
 */
@Singleton
final class ScoreboardRenderer {

    private final MessagesConfig messages;

    /**
     * Construye el renderer inyectando la configuración de mensajes del plugin.
     *
     * @param messages configuración de mensajes y plantillas de texto usadas para
     *                 formatear las líneas del marcador lateral
     */
    @Inject
    ScoreboardRenderer(MessagesConfig messages) {
        this.messages = messages;
    }

    /**
     * Genera la lista de líneas del marcador lateral para el jugador en su estado actual.
     * <p>
     * Las líneas se construyen de forma dinámica: las secciones de KOTH y timers solo
     * aparecen si hay datos relevantes, evitando mostrar separadores vacíos. Cada cadena
     * puede contener códigos de color de Minecraft ({@code §}).
     * </p>
     *
     * @param player jugador propietario del marcador (no se utiliza actualmente, pero
     *               se pasa por extensibilidad futura para permisos o contexto específico)
     * @param data   snapshot mutable con las estadísticas y timers del jugador para este tick
     * @param koths  colección de KOTHs activos en el servidor en el momento del renderizado
     * @return lista de cadenas de texto formateadas, en orden descendente de visualización;
     *         el primer elemento aparece en la parte superior del marcador lateral
     */
    List<String> render(Player player, PlayerBoardData data, Collection<KothEntry> koths) {
        List<String> lines = new ArrayList<>();
        String sep = messages.get("scoreboard.separator");

        lines.add(sep);

        String faction = data.getFactionName();
        if (faction != null) {
            lines.add(messages.format("scoreboard.faction", "faction", faction));
            lines.add(messages.format("scoreboard.dtk", "dtk", String.valueOf(data.getDtkRemaining())));
        } else {
            lines.add(messages.get("scoreboard.no-faction"));
        }

        lines.add(messages.format("scoreboard.kills",   "kills",   String.valueOf(data.getKills())));
        lines.add(messages.format("scoreboard.deaths",  "deaths",  String.valueOf(data.getDeaths())));
        lines.add(messages.format("scoreboard.balance", "balance", fmtBalance(data.getBalance())));

        if (!koths.isEmpty()) {
            lines.add(sep);
            for (KothEntry koth : koths) {
                long captureMs = koth.name.equals(data.getCapturingKothName())
                        ? data.getCaptureRemainingMs() : 0;
                lines.add(messages.format("scoreboard.koth-name", "name", koth.name));
                lines.add(messages.format("scoreboard.koth-loc",
                        "x", String.valueOf(koth.centerX),
                        "z", String.valueOf(koth.centerZ)));
                lines.add(messages.format("scoreboard.koth-cap", "time",
                        fmtMs(captureMs > 0 ? captureMs : (long) koth.captureTimeSeconds * 1000L)));
            }
        }

        List<String> timerLines = buildTimerLines(data);
        if (!timerLines.isEmpty()) {
            lines.add(sep);
            lines.addAll(timerLines);
        }

        long sotwMs = data.getSotwRemainingMs();
        long eotwMs = data.getEotwRemainingMs();
        if (sotwMs > 0 || eotwMs > 0) {
            lines.add(sep);
            if (sotwMs > 0) lines.add(messages.format("scoreboard.sotw", "time", fmtMs(sotwMs)));
            if (eotwMs > 0) lines.add(messages.format("scoreboard.eotw", "time", fmtMs(eotwMs)));
        }

        lines.add(sep);
        lines.add(messages.get("scoreboard.footer"));

        return lines;
    }

    private List<String> buildTimerLines(PlayerBoardData data) {
        List<String> lines = new ArrayList<>();
        addTimer(lines, data, TimerType.COMBAT_TAG,     "scoreboard.timer.combat");
        addTimer(lines, data, TimerType.PVP_TIMER,      "scoreboard.timer.pvp");
        addTimer(lines, data, TimerType.ENDERPEARL,     "scoreboard.timer.enderpearl");
        addTimer(lines, data, TimerType.GAPPLE,         "scoreboard.timer.gapple");
        addTimer(lines, data, TimerType.HOME,           "scoreboard.timer.home");
        addTimer(lines, data, TimerType.LOGOUT,         "scoreboard.timer.logout");
        addTimer(lines, data, TimerType.CLASS_COOLDOWN, "scoreboard.timer.class");
        addTimer(lines, data, TimerType.STUCK,          "scoreboard.timer.stuck");
        return lines;
    }

    private void addTimer(List<String> out, PlayerBoardData data, TimerType type, String key) {
        long ms = data.getRemainingMs(type);
        if (ms > 0) {
            out.add(messages.format(key, "time", fmtMs(ms)));
        }
    }

    private static String fmtMs(long ms) {
        long s = ms / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, sec);
        return String.format("%02d:%02d", m, sec);
    }

    private static String fmtBalance(double balance) {
        if (balance >= 1_000_000) return String.format("%.1fM", balance / 1_000_000);
        if (balance >= 1_000)     return String.format("%.1fK", balance / 1_000);
        return String.format("%.0f", balance);
    }
}

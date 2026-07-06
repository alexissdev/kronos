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
 * Component responsible for building the list of lines displayed on the sidebar
 * scoreboard of each player during a server tick.
 * <p>
 * Receives a {@link PlayerBoardData} holding the player's current snapshot and the
 * collection of active {@link KothEntry} instances, and produces a list of
 * Minecraft-colour-formatted strings. The result is consumed by
 * {@link PlayerBoard#render(java.util.List)} to update the scoreboard without flicker.
 * </p>
 * <p>
 * The scoreboard content is assembled in the following order:
 * </p>
 * <ol>
 *   <li>Top separator</li>
 *   <li>Faction name and DTK (or a "no faction" message)</li>
 *   <li>Kills, deaths, and economic balance</li>
 *   <li>Active KOTH information (name, coordinates, capture time)</li>
 *   <li>Active per-player timers (combat tag, PvP, enderpearl, etc.)</li>
 *   <li>Global SOTW/EOTW countdowns, if active</li>
 *   <li>Bottom separator and footer</li>
 * </ol>
 * <p>
 * All text templates are sourced from {@link dev.alexissdev.kronos.common.config.MessagesConfig},
 * allowing the scoreboard format to be customised without modifying the source code.
 * </p>
 */
@Singleton
final class ScoreboardRenderer {

    private final MessagesConfig messages;

    /**
     * Constructs the renderer by injecting the plugin's message configuration.
     *
     * @param messages message configuration and text templates used to format
     *                 the lines of the sidebar scoreboard
     */
    @Inject
    ScoreboardRenderer(MessagesConfig messages) {
        this.messages = messages;
    }

    /**
     * Generates the list of sidebar lines for the player in their current state.
     * <p>
     * Lines are built dynamically: KOTH and timer sections only appear when there
     * is relevant data, avoiding empty separators. Each string may contain
     * Minecraft colour codes ({@code §}).
     * </p>
     *
     * @param player the player who owns the scoreboard (not currently used directly,
     *               but included for future extensibility such as permission checks
     *               or player-specific context)
     * @param data   mutable snapshot containing the player's statistics and timers for this tick
     * @param koths  collection of KOTHs currently active on the server at render time
     * @return list of formatted strings in descending display order;
     *         the first element appears at the top of the sidebar scoreboard
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

package dev.alexissdev.kronos.scoreboard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
final class ScoreboardRenderer {

    private final MessagesConfig messages;

    @Inject
    ScoreboardRenderer(MessagesConfig messages) {
        this.messages = messages;
    }

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

        lines.add(sep);
        lines.add(messages.get("scoreboard.footer"));

        return lines;
    }

    private List<String> buildTimerLines(PlayerBoardData data) {
        List<String> lines = new ArrayList<>();
        addTimer(lines, data, TimerType.COMBAT_TAG,     "scoreboard.timer.combat");
        addTimer(lines, data, TimerType.PVP_TIMER,      "scoreboard.timer.pvp");
        addTimer(lines, data, TimerType.ENDERPEARL,     "scoreboard.timer.enderpearl");
        addTimer(lines, data, TimerType.HOME,           "scoreboard.timer.home");
        addTimer(lines, data, TimerType.LOGOUT,         "scoreboard.timer.logout");
        addTimer(lines, data, TimerType.CLASS_COOLDOWN, "scoreboard.timer.class");
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
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private static String fmtBalance(double balance) {
        if (balance >= 1_000_000) return String.format("%.1fM", balance / 1_000_000);
        if (balance >= 1_000)     return String.format("%.1fK", balance / 1_000);
        return String.format("%.0f", balance);
    }
}

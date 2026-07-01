package dev.alexissdev.kronos.scoreboard;

import com.google.inject.Singleton;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
final class ScoreboardRenderer {

    private static final String SEP = "§8§m               §r";

    List<String> render(Player player, PlayerBoardData data, Collection<KothEntry> koths) {
        List<String> lines = new ArrayList<>();

        lines.add(SEP);
        lines.add("§fOnline: §e" + Bukkit.getOnlinePlayers().size());
        lines.add(SEP);

        String faction = data.getFactionName();
        if (faction != null) {
            lines.add("§7Faction: §e" + faction);
            lines.add("§7DTK: §e" + data.getDtkRemaining());
        } else {
            lines.add("§7Facción:");
        }

        lines.add("§7Kills: §a" + data.getKills());
        lines.add("§7Deaths: §c" + data.getDeaths());
        lines.add("§7Balance: §a$" + fmtBalance(data.getBalance()));

        // Active KOTHs (global, same for every player)
        if (!koths.isEmpty()) {
            lines.add(SEP);
            for (KothEntry koth : koths) {
                lines.add("§6KOTH §e" + koth.name);
                lines.add(" §7Loc: §a" + koth.centerX + "§7, §a" + koth.centerZ);
            }
        }

        // Per-player KOTH capture countdown
        long captureMs = data.getCaptureRemainingMs();
        if (captureMs > 0) {
            lines.add(SEP);
            lines.add("§6Capturando §e" + data.getCapturingKothName());
            lines.add(" §7Tiempo: §f" + fmtMs(captureMs));
        }

        // Per-player timers
        List<String> timerLines = buildTimerLines(data);
        if (!timerLines.isEmpty()) {
            lines.add(SEP);
            lines.addAll(timerLines);
        }

        lines.add(SEP);
        lines.add("§fplay.kronos.net");

        return lines;
    }

    private List<String> buildTimerLines(PlayerBoardData data) {
        List<String> lines = new ArrayList<>();
        addTimer(lines, data, TimerType.COMBAT_TAG,     "§cCombat");
        addTimer(lines, data, TimerType.PVP_TIMER,      "§6PvP Timer");
        addTimer(lines, data, TimerType.ENDERPEARL,     "§bEnderpearl");
        addTimer(lines, data, TimerType.HOME,           "§aCasa");
        addTimer(lines, data, TimerType.LOGOUT,         "§cDesconexión");
        addTimer(lines, data, TimerType.CLASS_COOLDOWN, "§dClase");
        return lines;
    }

    private static void addTimer(List<String> out, PlayerBoardData data,
                                  TimerType type, String label) {
        long ms = data.getRemainingMs(type);
        if (ms > 0) {
            out.add(label + ": §f" + fmtMs(ms));
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

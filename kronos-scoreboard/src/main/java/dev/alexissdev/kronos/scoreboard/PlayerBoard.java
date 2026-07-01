package dev.alexissdev.kronos.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;

/**
 * Wraps a per-player Bukkit Scoreboard using the Teams trick to update text
 * without flickering. Each of the 15 slots has a unique invisible fake entry
 * (a single color-code character) and a Team whose prefix holds the real text.
 */
final class PlayerBoard {

    private static final int MAX_LINES = 15;
    private static final String[] PALETTE = {
        "§0","§1","§2","§3","§4","§5","§6","§7","§8","§9",
        "§a","§b","§c","§d","§e"
    };

    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Team[] teams = new Team[MAX_LINES];
    private int renderedCount = 0;

    PlayerBoard(Player player, String title) {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("hcf", "dummy");
        objective.setDisplayName(title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 0; i < MAX_LINES; i++) {
            teams[i] = scoreboard.registerNewTeam("hcf_" + i);
            teams[i].addEntry(PALETTE[i]);
        }

        player.setScoreboard(scoreboard);
    }

    /**
     * Renders the scoreboard. {@code lines.get(0)} is the topmost entry.
     * Lines beyond MAX_LINES are silently ignored.
     */
    void render(List<String> lines) {
        int count = Math.min(lines.size(), MAX_LINES);

        for (int i = 0; i < count; i++) {
            String text = clip(lines.get(i));
            if (!text.equals(teams[i].getPrefix())) {
                teams[i].setPrefix(text);
            }
            objective.getScore(PALETTE[i]).setScore(count - i);
        }

        for (int i = count; i < renderedCount; i++) {
            scoreboard.resetScores(PALETTE[i]);
        }

        renderedCount = count;
    }

    private static String clip(String s) {
        return s.length() > 64 ? s.substring(0, 64) : s;
    }
}

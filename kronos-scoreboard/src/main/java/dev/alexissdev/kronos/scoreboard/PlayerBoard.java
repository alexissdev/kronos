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

    void render(List<String> lines) {
        int count = Math.min(lines.size(), MAX_LINES);

        for (int i = 0; i < count; i++) {
            setLine(i, lines.get(i));
            objective.getScore(PALETTE[i]).setScore(count - i);
        }

        for (int i = count; i < renderedCount; i++) {
            scoreboard.resetScores(PALETTE[i]);
            applyLine(teams[i], "", "");
        }

        renderedCount = count;
    }

    // Splits text into prefix (≤16) + suffix (≤16) with color carry-over.
    // Supports up to 32 visible characters per line on all Spigot versions.
    private static void setLine(Team team, String text) {
        if (text.length() <= 16) {
            applyLine(team, text, "");
            return;
        }
        String prefix = text.substring(0, 16);
        String carry  = ChatColor.getLastColors(prefix);
        String rest   = carry + text.substring(16);
        String suffix = rest.length() > 16 ? rest.substring(0, 16) : rest;
        applyLine(team, prefix, suffix);
    }

    private void setLine(int i, String text) {
        setLine(teams[i], text);
    }

    private static void applyLine(Team team, String prefix, String suffix) {
        if (!prefix.equals(team.getPrefix())) team.setPrefix(prefix);
        if (!suffix.equals(team.getSuffix())) team.setSuffix(suffix);
    }
}

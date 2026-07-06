package dev.alexissdev.kronos.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;

/**
 * Represents the individual sidebar scoreboard assigned to a single player in Bukkit.
 * <p>
 * Uses the Teams trick to update each line's text without causing visual flicker:
 * each of the 15 possible slots has an invisible phantom entry (a unique colour code
 * such as {@code §0}, {@code §1}, etc.) and a {@link org.bukkit.scoreboard.Team}
 * whose prefix and suffix hold the actual visible text.
 * </p>
 * <p>
 * The scoreboard is assigned directly to the player at creation time
 * (in {@link ScoreboardManager#createBoard(org.bukkit.entity.Player)}) and is
 * redrawn every second by {@link ScoreboardTask} via {@link ScoreboardManager#tickAll()}.
 * </p>
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

    /**
     * Creates and assigns a new sidebar scoreboard to the given player.
     * <p>
     * Initialises Bukkit's {@link org.bukkit.scoreboard.Scoreboard} with a
     * {@code SIDEBAR} objective, registers all 15 teams (one per slot) with their
     * invisible phantom entries, and immediately assigns the scoreboard to the player.
     * </p>
     *
     * @param player the player who will receive the scoreboard
     * @param title  text displayed as the header title of the sidebar scoreboard
     */
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
     * Renders the provided list of lines onto the player's sidebar scoreboard.
     * <p>
     * Only updates slots whose text has actually changed, avoiding unnecessary
     * writes to Bukkit's scoreboard. Slots that exceeded the line count from the
     * previous tick are hidden by resetting their score. Supports up to
     * {@value MAX_LINES} simultaneous lines.
     * </p>
     *
     * @param lines list of strings to display, in descending order; the first element
     *              appears at the top of the sidebar scoreboard
     */
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

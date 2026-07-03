package dev.alexissdev.kronos.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;

/**
 * Representa el marcador lateral (sidebar) individual de un jugador en Bukkit.
 * <p>
 * Utiliza el truco de Teams para actualizar el texto de cada línea sin producir
 * parpadeo visual: cada una de las 15 ranuras posibles tiene una entrada falsa
 * invisible (un código de color único como {@code §0}, {@code §1}, etc.) y un
 * {@link org.bukkit.scoreboard.Team} cuyo prefijo y sufijo contienen el texto real.
 * </p>
 * <p>
 * El marcador se asigna directamente al jugador al momento de su creación
 * (en {@link ScoreboardManager#createBoard(org.bukkit.entity.Player)}) y es
 * actualizado cada segundo desde {@link ScoreboardTask} a través de
 * {@link ScoreboardManager#tickAll()}.
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
     * Crea y asigna un nuevo marcador lateral al jugador indicado.
     * <p>
     * Inicializa el {@link org.bukkit.scoreboard.Scoreboard} de Bukkit con un objetivo
     * en el slot {@code SIDEBAR}, registra los 15 equipos (uno por ranura) con sus
     * entradas invisibles y asigna el marcador al jugador inmediatamente.
     * </p>
     *
     * @param player jugador al que se asignará el marcador
     * @param title  texto que aparece como título en la cabecera del marcador lateral
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
     * Renderiza la lista de líneas proporcionada en el marcador lateral del jugador.
     * <p>
     * Actualiza únicamente las ranuras cuyo texto haya cambiado, evitando escrituras
     * innecesarias en el scoreboard de Bukkit. Las ranuras que superaban el número de
     * líneas del tick anterior son ocultadas reseteando su puntuación. Soporta hasta
     * {@value MAX_LINES} líneas simultáneas.
     * </p>
     *
     * @param lines lista de cadenas a mostrar, en orden descendente; el primer elemento
     *              aparece en la parte superior del marcador lateral
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

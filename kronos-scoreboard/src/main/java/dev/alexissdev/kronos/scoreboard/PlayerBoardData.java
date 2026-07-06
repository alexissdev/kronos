package dev.alexissdev.kronos.scoreboard;

import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Mutable snapshot of a player's data used by {@link ScoreboardRenderer}
 * to generate the sidebar scoreboard lines on each tick.
 * <p>
 * Fields are grouped into three categories with different update frequencies:
 * </p>
 * <ul>
 *   <li><b>Slow stats</b> (kills, deaths, balance, faction, DTK): refreshed
 *       asynchronously every 5 seconds from {@link ScoreboardManager#refreshAllStats()}.</li>
 *   <li><b>Per-player timers</b> (combat tag, PvP, enderpearl, etc.): updated
 *       synchronously upon receiving EventBus events in {@link ScoreboardManager}.</li>
 *   <li><b>Global counters and KOTH</b> (SOTW, EOTW, KOTH capture progress): propagated
 *       on every main-thread tick from {@link ScoreboardManager#tickAll()}.</li>
 * </ul>
 */
final class PlayerBoardData {

    private volatile int kills;
    private volatile int deaths;
    private volatile double balance;
    private volatile String factionName;
    private volatile int dtkRemaining;

    // Maps TimerType → absolute epoch-ms when the timer expires.
    private final Map<TimerType, Long> timerExpiryMs = new EnumMap<>(TimerType.class);

    // KOTH capture progress for this player
    private volatile String capturingKothName;
    private volatile long   captureExpiresAt;

    // Global SOTW/EOTW countdown (same value for all players, updated each tick)
    private volatile long sotwRemainingMs = 0L;
    private volatile long eotwRemainingMs = 0L;

    // ── stats ─────────────────────────────────────────────────────────────

    /** @return the player's total accumulated kill count on the server */
    int getKills()        { return kills; }

    /** @return the player's total accumulated death count on the server */
    int getDeaths()       { return deaths; }

    /** @return the player's current economic balance */
    double getBalance()   { return balance; }

    /**
     * @return the name of the faction the player belongs to,
     *         or {@code null} if the player is not in any faction
     */
    String getFactionName() { return factionName; }

    /**
     * @return the number of DTK (Deaths To Kick) remaining in the player's faction;
     *         {@code 0} if the player has no faction
     */
    int getDtkRemaining() { return dtkRemaining; }

    /**
     * Updates the player's kill count.
     *
     * @param v new kill value retrieved from {@code PlayerService}
     */
    void setKills(int v)          { this.kills = v; }

    /**
     * Updates the player's death count.
     *
     * @param v new death value retrieved from {@code PlayerService}
     */
    void setDeaths(int v)         { this.deaths = v; }

    /**
     * Updates the player's economic balance.
     *
     * @param v new balance value retrieved from {@code EconomyService}
     */
    void setBalance(double v)     { this.balance = v; }

    /**
     * Updates the player's faction name.
     *
     * @param v the faction name, or {@code null} if the player does not belong to any faction
     */
    void setFactionName(String v) { this.factionName = v; }

    /**
     * Updates the number of DTK remaining in the player's faction.
     *
     * @param v deaths remaining before the faction is kicked from the server
     */
    void setDtkRemaining(int v)   { this.dtkRemaining = v; }

    // ── timers ────────────────────────────────────────────────────────────

    /**
     * Registers or updates the expiry time for one of the player's individual timers.
     * <p>
     * Called from {@link ScoreboardManager} upon receiving a
     * {@code PlayerTimerStartedDomainEvent}. This method is safe for concurrent
     * calls between the main thread and EventBus threads.
     * </p>
     *
     * @param type          the timer type being started (e.g. {@code COMBAT_TAG}, {@code PVP_TIMER})
     * @param expiryEpochMs epoch timestamp in milliseconds at which the timer will expire
     */
    synchronized void setTimer(TimerType type, long expiryEpochMs) {
        timerExpiryMs.put(type, expiryEpochMs);
    }

    /**
     * Removes an active timer from the player's data upon receiving its expiry event.
     * <p>
     * Called from {@link ScoreboardManager} upon receiving a
     * {@code PlayerTimerExpiredDomainEvent}. This method is safe for concurrent calls.
     * </p>
     *
     * @param type the timer type that has expired and must be removed from the scoreboard
     */
    synchronized void clearTimer(TimerType type) {
        timerExpiryMs.remove(type);
    }

    /**
     * Calculates the time remaining in milliseconds for the specified timer.
     * <p>
     * Called by {@link ScoreboardRenderer} on every tick to display an up-to-date
     * countdown. This method is safe for concurrent calls.
     * </p>
     *
     * @param type the timer type to query
     * @return milliseconds remaining until the timer expires, or {@code 0L}
     *         if the timer is not active or has already expired
     */
    synchronized long getRemainingMs(TimerType type) {
        Long expiry = timerExpiryMs.get(type);
        if (expiry == null) return 0L;
        return Math.max(0L, expiry - System.currentTimeMillis());
    }

    // ── KOTH capture ──────────────────────────────────────────────────────

    /**
     * Records that the player is in the process of capturing a KOTH and sets
     * the remaining time needed to complete the capture.
     * <p>
     * Called from {@link ScoreboardManager#updateKothCapture(java.util.UUID, String, long)}
     * while the player remains inside the capture zone.
     * </p>
     *
     * @param kothName    name of the KOTH the player is capturing
     * @param remainingMs milliseconds remaining to complete the capture from this moment
     */
    void setKothCapture(String kothName, long remainingMs) {
        this.capturingKothName = kothName;
        this.captureExpiresAt  = System.currentTimeMillis() + remainingMs;
    }

    /**
     * Clears the player's KOTH capture state, indicating they have left the capture
     * zone or the KOTH event has ended.
     */
    void clearKothCapture() {
        this.capturingKothName = null;
        this.captureExpiresAt  = 0;
    }

    /**
     * @return the name of the KOTH the player is currently capturing,
     *         or {@code null} if no capture is in progress
     */
    String getCapturingKothName()   { return capturingKothName; }

    /**
     * Calculates the time remaining for the player to complete the current KOTH capture.
     *
     * @return milliseconds remaining until capture is complete, or {@code 0L} if the
     *         player is not capturing any KOTH or the capture window has already expired
     */
    long getCaptureRemainingMs() {
        if (capturingKothName == null) return 0L;
        return Math.max(0L, captureExpiresAt - System.currentTimeMillis());
    }

    // ── SOTW / EOTW ───────────────────────────────────────────────────────

    /**
     * Updates the remaining time of the SOTW (Start Of The World) period.
     * This value is global — the same for all players — and is propagated on
     * every tick from {@link ScoreboardManager#tickAll()}.
     *
     * @param ms milliseconds remaining in SOTW, or {@code 0L} if SOTW is not active
     */
    void setSotwRemainingMs(long ms) { this.sotwRemainingMs = ms; }

    /**
     * Updates the remaining time of the EOTW (End Of The World) period.
     * This value is global — the same for all players — and is propagated on
     * every tick from {@link ScoreboardManager#tickAll()}.
     *
     * @param ms milliseconds remaining in EOTW, or {@code 0L} if EOTW is not active
     */
    void setEotwRemainingMs(long ms) { this.eotwRemainingMs = ms; }

    /**
     * @return milliseconds remaining in the SOTW period, or {@code 0L} if not active
     */
    long getSotwRemainingMs()        { return sotwRemainingMs; }

    /**
     * @return milliseconds remaining in the EOTW period, or {@code 0L} if not active
     */
    long getEotwRemainingMs()        { return eotwRemainingMs; }
}

package dev.alexissdev.kronos.scoreboard;

import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Mutable per-player snapshot used by the renderer.
 * Stats (kills/deaths/balance/faction) are refreshed async every few seconds.
 * Timer expiry timestamps are updated synchronously from EventBus events.
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

    int getKills()        { return kills; }
    int getDeaths()       { return deaths; }
    double getBalance()   { return balance; }
    String getFactionName() { return factionName; }
    int getDtkRemaining() { return dtkRemaining; }

    void setKills(int v)          { this.kills = v; }
    void setDeaths(int v)         { this.deaths = v; }
    void setBalance(double v)     { this.balance = v; }
    void setFactionName(String v) { this.factionName = v; }
    void setDtkRemaining(int v)   { this.dtkRemaining = v; }

    // ── timers ────────────────────────────────────────────────────────────

    synchronized void setTimer(TimerType type, long expiryEpochMs) {
        timerExpiryMs.put(type, expiryEpochMs);
    }

    synchronized void clearTimer(TimerType type) {
        timerExpiryMs.remove(type);
    }

    synchronized long getRemainingMs(TimerType type) {
        Long expiry = timerExpiryMs.get(type);
        if (expiry == null) return 0L;
        return Math.max(0L, expiry - System.currentTimeMillis());
    }

    // ── KOTH capture ──────────────────────────────────────────────────────

    void setKothCapture(String kothName, long remainingMs) {
        this.capturingKothName = kothName;
        this.captureExpiresAt  = System.currentTimeMillis() + remainingMs;
    }

    void clearKothCapture() {
        this.capturingKothName = null;
        this.captureExpiresAt  = 0;
    }

    String getCapturingKothName()   { return capturingKothName; }

    long getCaptureRemainingMs() {
        if (capturingKothName == null) return 0L;
        return Math.max(0L, captureExpiresAt - System.currentTimeMillis());
    }

    // ── SOTW / EOTW ───────────────────────────────────────────────────────

    void setSotwRemainingMs(long ms) { this.sotwRemainingMs = ms; }
    void setEotwRemainingMs(long ms) { this.eotwRemainingMs = ms; }
    long getSotwRemainingMs()        { return sotwRemainingMs; }
    long getEotwRemainingMs()        { return eotwRemainingMs; }
}

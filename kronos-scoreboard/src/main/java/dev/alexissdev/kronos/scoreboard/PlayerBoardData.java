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
}

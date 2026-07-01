package dev.alexissdev.kronos.timers.event;

import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.UUID;

public final class PlayerTimerStartedDomainEvent {

    private final UUID playerUuid;
    private final TimerType timerType;
    private final long durationMillis;

    public PlayerTimerStartedDomainEvent(UUID playerUuid, TimerType timerType, long durationMillis) {
        this.playerUuid = playerUuid;
        this.timerType = timerType;
        this.durationMillis = durationMillis;
    }

    public UUID getPlayerUuid() { return playerUuid; }

    public TimerType getTimerType() { return timerType; }

    public long getDurationMillis() { return durationMillis; }
}

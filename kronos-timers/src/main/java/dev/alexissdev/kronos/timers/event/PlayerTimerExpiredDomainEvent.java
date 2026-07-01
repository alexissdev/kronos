package dev.alexissdev.kronos.timers.event;

import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.UUID;

public final class PlayerTimerExpiredDomainEvent {

    private final UUID playerUuid;
    private final TimerType timerType;

    public PlayerTimerExpiredDomainEvent(UUID playerUuid, TimerType timerType) {
        this.playerUuid = playerUuid;
        this.timerType = timerType;
    }

    public UUID getPlayerUuid() { return playerUuid; }

    public TimerType getTimerType() { return timerType; }
}

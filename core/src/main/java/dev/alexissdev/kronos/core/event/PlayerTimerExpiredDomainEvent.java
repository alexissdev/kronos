package dev.alexissdev.kronos.core.event;

import dev.alexissdev.kronos.core.domain.TimerType;

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

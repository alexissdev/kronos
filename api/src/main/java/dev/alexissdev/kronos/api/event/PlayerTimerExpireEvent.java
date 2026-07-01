package dev.alexissdev.kronos.api.event;

import dev.alexissdev.kronos.core.domain.TimerType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class PlayerTimerExpireEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final TimerType timerType;

    public PlayerTimerExpireEvent(UUID playerUuid, TimerType timerType) {
        this.playerUuid = playerUuid;
        this.timerType = timerType;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public TimerType getTimerType() { return timerType; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}

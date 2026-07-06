package dev.alexissdev.kronos.timers.event;

import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.UUID;

/**
 * Domain event published on the Guava {@code EventBus} when a player's timer
 * expires naturally or is cancelled manually.
 *
 * <p>Posted both when a timer reaches its natural expiration time and when it is
 * explicitly cancelled via {@code TimerService#cancelTimer}.
 * Listeners can react to this event to lift restrictions on the player, notify them
 * that the timer has ended, or perform side-effects specific to the timer type that
 * expired — for example, re-allowing safe disconnection after a combat tag.</p>
 */
public final class PlayerTimerExpiredDomainEvent {

    private final UUID playerUuid;
    private final TimerType timerType;

    /**
     * Creates a timer-expired event for the affected player and timer type.
     *
     * @param playerUuid UUID of the player whose timer has expired or was cancelled
     * @param timerType  type of the timer that has ended
     */
    public PlayerTimerExpiredDomainEvent(UUID playerUuid, TimerType timerType) {
        this.playerUuid = playerUuid;
        this.timerType = timerType;
    }

    /**
     * Returns the UUID of the player whose timer has expired.
     *
     * @return UUID of the player affected by the timer expiration
     */
    public UUID getPlayerUuid() { return playerUuid; }

    /**
     * Returns the type of the timer that has expired, allowing listeners to filter
     * and react only to the timer types they are interested in.
     *
     * @return type of the timer that has ended
     */
    public TimerType getTimerType() { return timerType; }
}

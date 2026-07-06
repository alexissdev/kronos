package dev.alexissdev.kronos.timers.event;

import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.UUID;

/**
 * Domain event published on the Guava {@code EventBus} when a new timer is started
 * for a player.
 *
 * <p>Posted both when {@code TimerService#startTimer} is called and during the initial
 * cache warm-up on player login, if timers are found still active in Redis or MongoDB.
 * Listeners can react to this event to apply visual effects, display messages to the
 * player, or activate restrictions specific to the timer type that was started.</p>
 */
public final class PlayerTimerStartedDomainEvent {

    private final UUID playerUuid;
    private final TimerType timerType;
    private final long durationMillis;

    /**
     * Creates a timer-started event with the player, the timer type, and the remaining duration.
     *
     * @param playerUuid     UUID of the player for whom the timer has been started
     * @param timerType      type of the timer that has just been started
     * @param durationMillis remaining duration of the timer in milliseconds from the moment of the event
     */
    public PlayerTimerStartedDomainEvent(UUID playerUuid, TimerType timerType, long durationMillis) {
        this.playerUuid = playerUuid;
        this.timerType = timerType;
        this.durationMillis = durationMillis;
    }

    /**
     * Returns the UUID of the player for whom the timer has been started.
     *
     * @return UUID of the player affected by the new timer
     */
    public UUID getPlayerUuid() { return playerUuid; }

    /**
     * Returns the type of the timer that has just been started.
     *
     * @return type of the started timer
     */
    public TimerType getTimerType() { return timerType; }

    /**
     * Returns the remaining duration of the timer in milliseconds.
     * For brand-new timers this equals the total duration; for timers restored
     * from Redis or MongoDB it represents the time left at the moment of loading.
     *
     * @return remaining duration of the timer in milliseconds
     */
    public long getDurationMillis() { return durationMillis; }
}

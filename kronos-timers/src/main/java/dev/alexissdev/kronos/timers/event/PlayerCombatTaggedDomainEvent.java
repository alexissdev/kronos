package dev.alexissdev.kronos.timers.event;

import java.util.UUID;

/**
 * Domain event published on the Guava {@code EventBus} when a player is combat-tagged
 * after dealing or receiving damage from another player.
 *
 * <p>When this event is posted, both players involved in the exchange receive a timer
 * of type {@link dev.alexissdev.kronos.timers.domain.TimerType#COMBAT_TAG} that
 * prevents them from disconnecting safely for the timer's duration.
 * Subscribers on the {@code EventBus} can react to this event to display notifications
 * or apply additional combat-related logic.</p>
 */
public final class PlayerCombatTaggedDomainEvent {

    private final UUID taggedUuid;
    private final UUID taggerUuid;
    private final long durationMillis;

    /**
     * Creates a combat-tag event with the players involved and the timer duration.
     *
     * @param taggedUuid     UUID of the player who was tagged (received the combat mark)
     * @param taggerUuid     UUID of the player who initiated combat and triggered the tag
     * @param durationMillis duration of the combat-tag timer in milliseconds
     */
    public PlayerCombatTaggedDomainEvent(UUID taggedUuid, UUID taggerUuid, long durationMillis) {
        this.taggedUuid = taggedUuid;
        this.taggerUuid = taggerUuid;
        this.durationMillis = durationMillis;
    }

    /**
     * Returns the UUID of the player who was marked in combat and had the timer applied.
     *
     * @return UUID of the tagged player
     */
    public UUID getTaggedUuid() { return taggedUuid; }

    /**
     * Returns the UUID of the player who initiated combat and caused the tagging.
     *
     * @return UUID of the player who dealt the attack or started the fight
     */
    public UUID getTaggerUuid() { return taggerUuid; }

    /**
     * Returns the duration of the combat-tag timer applied to both players.
     *
     * @return combat-tag duration in milliseconds (typically 30 000 ms)
     */
    public long getDurationMillis() { return durationMillis; }
}

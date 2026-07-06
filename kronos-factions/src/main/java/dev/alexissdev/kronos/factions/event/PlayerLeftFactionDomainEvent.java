package dev.alexissdev.kronos.factions.event;

import java.util.UUID;

/**
 * Domain event published on the {@code EventBus} when a player leaves
 * or is kicked from a faction.
 *
 * <p>The {@code wasKicked} flag distinguishes between the two situations:
 * <ul>
 *   <li>{@code wasKicked = true}: the player was kicked by a member with
 *       sufficient rank (CAPTAIN or above).</li>
 *   <li>{@code wasKicked = false}: the player voluntarily left the faction.</li>
 * </ul>
 *
 * <p>After this event the system registers a re-invite cooldown for the player,
 * preventing them from being immediately re-invited to the same faction.
 */
public final class PlayerLeftFactionDomainEvent {

    private final UUID playerUuid;
    private final String factionId;
    private final boolean wasKicked;

    /**
     * Creates the event with the data of the player who left the faction.
     *
     * @param playerUuid UUID of the player who left or was kicked
     * @param factionId  ID of the faction the player has left
     * @param wasKicked  {@code true} if the player was kicked; {@code false} if they left voluntarily
     */
    public PlayerLeftFactionDomainEvent(UUID playerUuid, String factionId, boolean wasKicked) {
        this.playerUuid = playerUuid;
        this.factionId = factionId;
        this.wasKicked = wasKicked;
    }

    /**
     * Returns the UUID of the player who left the faction.
     *
     * @return UUID of the player
     */
    public UUID getPlayerUuid() { return playerUuid; }

    /**
     * Returns the ID of the faction the player left or was kicked from.
     *
     * @return faction ID
     */
    public String getFactionId() { return factionId; }

    /**
     * Returns whether the player was kicked by another member or left voluntarily.
     *
     * @return {@code true} if kicked; {@code false} if they left on their own
     */
    public boolean wasKicked() { return wasKicked; }
}

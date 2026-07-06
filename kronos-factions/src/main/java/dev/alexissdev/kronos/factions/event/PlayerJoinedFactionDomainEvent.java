package dev.alexissdev.kronos.factions.event;

import java.util.UUID;

/**
 * Domain event published on the {@code EventBus} when a player accepts an
 * invitation and joins a faction as a new member.
 *
 * <p>Listeners can use this event to, for example, send welcome messages to the
 * player, update statistics, or record the join action in an audit log.
 *
 * <p>This event does not cover the case where the player was promoted to the leader
 * rank via a leadership transfer; that flow occurs internally without publishing this event.
 */
public final class PlayerJoinedFactionDomainEvent {

    private final UUID playerUuid;
    private final String factionId;

    /**
     * Creates the event with the identifiers of the player who joined and the target faction.
     *
     * @param playerUuid UUID of the player who just joined the faction
     * @param factionId  ID of the faction the player joined
     */
    public PlayerJoinedFactionDomainEvent(UUID playerUuid, String factionId) {
        this.playerUuid = playerUuid;
        this.factionId = factionId;
    }

    /**
     * Returns the UUID of the player who joined the faction.
     *
     * @return UUID of the player
     */
    public UUID getPlayerUuid() { return playerUuid; }

    /**
     * Returns the ID of the faction the player joined.
     *
     * @return faction ID
     */
    public String getFactionId() { return factionId; }
}

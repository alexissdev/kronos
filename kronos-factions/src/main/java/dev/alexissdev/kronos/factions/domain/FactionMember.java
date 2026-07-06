package dev.alexissdev.kronos.factions.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a player who belongs to a faction, along with their role and
 * the date on which they joined.
 *
 * <p>This class is part of the {@link Faction} aggregate: instances live
 * inside the faction's internal member map and are not persisted independently —
 * they are embedded in the faction's MongoDB document.
 *
 * <p>The role determines which operations the member can perform on the faction
 * (invite, kick, rename, withdraw funds, etc.). The role can be promoted or
 * demoted at any time by a member with sufficient authority.
 */
public final class FactionMember {

    private final UUID uuid;
    private FactionRole role;
    private final Instant joinedAt;

    /**
     * Creates a new faction member.
     *
     * @param uuid     unique identifier of the player in Minecraft
     * @param role     initial role of the member within the faction
     * @param joinedAt instant at which the player joined the faction
     */
    public FactionMember(UUID uuid, FactionRole role, Instant joinedAt) {
        this.uuid = uuid;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    /**
     * Returns the UUID of the player associated with this member.
     *
     * @return UUID of the player
     */
    public UUID getUuid() { return uuid; }

    /**
     * Returns the current role of the member within the faction.
     *
     * @return member's role
     */
    public FactionRole getRole() { return role; }

    /**
     * Updates the role of the member within the faction.
     *
     * <p>Should only be invoked from the faction service after verifying
     * that the acting member has sufficient authority to promote or demote the target.
     *
     * @param role new role to assign
     */
    public void setRole(FactionRole role) { this.role = role; }

    /**
     * Returns the exact instant at which the player joined the faction.
     *
     * @return join timestamp
     */
    public Instant getJoinedAt() { return joinedAt; }
}

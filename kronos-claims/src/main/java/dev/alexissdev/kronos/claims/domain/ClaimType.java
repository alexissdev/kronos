package dev.alexissdev.kronos.claims.domain;

/**
 * Enumeration of the territory (claim) types available in the HCF system.
 *
 * <p>Each value represents a zone category with distinct combat and building rules.
 * System types ({@link #isSystemClaim()}) are administered by the server and cannot
 * be claimed by player factions.</p>
 */
public enum ClaimType {

    /** Territory claimed by a player faction. */
    FACTION,

    /** Safe zone where PvP is disabled and players cannot be attacked. */
    SAFEZONE,

    /** War zone where PvP is always active and protection rules do not apply. */
    WARZONE,

    /** System-administered road separating territories; protected against building. */
    ROAD,

    /** Unclaimed wilderness open to all players; PvP and free building are allowed. */
    WILDERNESS,

    /** King of the Hill zone: an event territory with PvP enabled. */
    KOTH,

    /** Citadel zone: a high-value special event area with PvP enabled. */
    CITADEL;

    /**
     * Indicates whether this claim type is administered by the server system
     * and does not belong to any player faction.
     *
     * @return {@code true} if the type is {@link #SAFEZONE}, {@link #WARZONE},
     *         {@link #ROAD}, or {@link #WILDERNESS}; {@code false} otherwise
     */
    public boolean isSystemClaim() {
        return this == SAFEZONE || this == WARZONE || this == ROAD || this == WILDERNESS;
    }

    /**
     * Indicates whether PvP (player-versus-player combat) is permitted within this zone type.
     *
     * @return {@code true} if PvP is allowed in this territory type
     */
    public boolean allowsPvp() {
        return this == WARZONE || this == KOTH || this == CITADEL || this == WILDERNESS;
    }

    /**
     * Indicates whether blocks within this territory type are protected
     * against breaking and placing by unauthorized players.
     *
     * @return {@code true} for every zone type except {@link #WILDERNESS}
     */
    public boolean isProtectedFromBuild() {
        return this != WILDERNESS;
    }
}

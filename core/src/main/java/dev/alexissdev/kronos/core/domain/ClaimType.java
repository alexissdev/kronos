package dev.alexissdev.kronos.core.domain;

public enum ClaimType {
    FACTION,
    SAFEZONE,
    WARZONE,
    ROAD,
    WILDERNESS,
    KOTH,
    CITADEL;

    public boolean isSystemClaim() {
        return this == SAFEZONE || this == WARZONE || this == ROAD || this == WILDERNESS;
    }

    public boolean allowsPvp() {
        return this == WARZONE || this == KOTH || this == CITADEL || this == WILDERNESS;
    }

    public boolean isProtectedFromBuild() {
        return this != WILDERNESS;
    }
}

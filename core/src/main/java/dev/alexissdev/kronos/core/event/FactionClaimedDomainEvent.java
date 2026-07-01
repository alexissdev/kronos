package dev.alexissdev.kronos.core.event;

import dev.alexissdev.kronos.core.domain.Claim;

public final class FactionClaimedDomainEvent {

    private final String factionId;
    private final Claim claim;

    public FactionClaimedDomainEvent(String factionId, Claim claim) {
        this.factionId = factionId;
        this.claim = claim;
    }

    public String getFactionId() { return factionId; }

    public Claim getClaim() { return claim; }
}

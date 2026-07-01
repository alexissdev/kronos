package dev.alexissdev.kronos.core.domain;

import java.time.Instant;
import java.util.UUID;

public final class FactionMember {

    private final UUID uuid;
    private FactionRole role;
    private final Instant joinedAt;

    public FactionMember(UUID uuid, FactionRole role, Instant joinedAt) {
        this.uuid = uuid;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public UUID getUuid() { return uuid; }

    public FactionRole getRole() { return role; }

    public void setRole(FactionRole role) { this.role = role; }

    public Instant getJoinedAt() { return joinedAt; }
}

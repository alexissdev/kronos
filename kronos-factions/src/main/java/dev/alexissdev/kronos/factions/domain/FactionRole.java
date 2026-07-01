package dev.alexissdev.kronos.factions.domain;

public enum FactionRole {
    LEADER(4),
    CO_LEADER(3),
    CAPTAIN(2),
    MEMBER(1);

    private final int priority;

    FactionRole(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isAtLeast(FactionRole role) {
        return this.priority >= role.priority;
    }
}

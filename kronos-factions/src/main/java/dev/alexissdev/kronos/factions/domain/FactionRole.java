package dev.alexissdev.kronos.factions.domain;

/**
 * Role hierarchy within an HCF faction.
 *
 * <p>Roles define the permissions each member holds over faction
 * operations. The ascending order of authority is:
 * {@link #MEMBER} &lt; {@link #CAPTAIN} &lt; {@link #CO_LEADER} &lt; {@link #LEADER}.
 *
 * <p>Each role has a numeric priority value that allows hierarchical comparisons
 * via {@link #isAtLeast(FactionRole)}.
 */
public enum FactionRole {

    /** Highest rank; only one leader can exist per faction. */
    LEADER(4),

    /** Co-leader with broad administrative permissions, including withdrawing funds and changing roles. */
    CO_LEADER(3),

    /** Captain with permissions to invite, kick members, and set the faction home. */
    CAPTAIN(2),

    /** Base member with no administrative permissions. */
    MEMBER(1);

    private final int priority;

    FactionRole(int priority) {
        this.priority = priority;
    }

    /**
     * Returns the numeric priority value of this role.
     *
     * <p>A higher number indicates greater authority within the hierarchy.
     *
     * @return role priority (1–4)
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Checks whether this role has at least the same authority as the given role.
     *
     * <p>Used extensively in faction service permission checks to verify that the
     * acting member meets the minimum required rank before executing a privileged operation.
     *
     * @param role the minimum role required for the operation
     * @return {@code true} if this role's priority is greater than or equal to that of the given role
     */
    public boolean isAtLeast(FactionRole role) {
        return this.priority >= role.priority;
    }
}

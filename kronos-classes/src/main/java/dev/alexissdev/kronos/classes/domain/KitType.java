package dev.alexissdev.kronos.classes.domain;

/**
 * Enumeration of the classes (kits) available to players in the HCF system.
 *
 * <p>Each value represents a combat or utility role with unique passive and active
 * abilities. A player's active class is determined by the type of helmet they are
 * wearing and is stored in their {@code KronosPlayer} profile. The {@link #DIAMOND}
 * class acts as the default for players without a specific kit assigned (diamond helmet
 * or no helmet).</p>
 *
 * <p>The abilities of each class are applied by
 * {@link dev.alexissdev.kronos.classes.listener.ClassListener}.</p>
 */
public enum KitType {

    /**
     * Archer class: fires arrows at greater velocity and applies slowness on melee hits.
     * Activated by wearing a leather helmet. Its active ability grants a brief damage boost.
     */
    ARCHER,

    /**
     * Bard class: support aura that grants Speed and Regeneration to all nearby faction
     * members within a 15-block radius. Activated by wearing a gold helmet.
     * Its active ability amplifies the movement speed of allies in the area.
     */
    BARD,

    /**
     * Rogue class: gains Speed on landing a melee hit against an enemy.
     * Activated by wearing a chainmail helmet. Its active ability briefly turns the player invisible.
     */
    ROGUE,

    /**
     * Miner class: passively gains Haste while breaking blocks.
     * Activated by wearing an iron helmet. Its active ability further increases mining speed.
     */
    MINER,

    /**
     * Knight class: gains Resistance on landing a melee hit against an enemy and can repel
     * nearby players with its active ability. Activated by wearing a diamond helmet
     * (distinguished from the default kit by inventory context).
     */
    KNIGHT,

    /**
     * Default kit for players without an explicitly assigned class.
     * No special abilities are associated with this kit.
     */
    DIAMOND
}

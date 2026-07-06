package dev.alexissdev.kronos.players.domain;

/**
 * Enumeration of the kit types available on the HCF server.
 *
 * <p>Each kit represents a combat class with a specific loadout and role.
 * The player's active kit is stored in their {@link HCFPlayer} profile
 * and can be applied to their inventory through {@code KitService}.</p>
 *
 * <ul>
 *   <li>{@link #ARCHER} — ranged class with an enchanted bow and light armour.</li>
 *   <li>{@link #BARD} — support class that uses a blaze rod to buff teammates.</li>
 *   <li>{@link #ROGUE} — stealthy class with a diamond sword and chainmail armour.</li>
 *   <li>{@link #MINER} — resource-gathering class with an Efficiency V diamond pickaxe.</li>
 *   <li>{@link #KNIGHT} — frontline melee class with full diamond armour and sword.</li>
 *   <li>{@link #DIAMOND} — standard diamond kit; the default assigned to new players.</li>
 * </ul>
 */
public enum KitType {

    /**
     * Archer class: light leather/chainmail armour and a bow enchanted with Power III and Infinity.
     * Ideal for ranged combat and harassing enemies from a distance.
     */
    ARCHER,

    /**
     * Bard class: mixed gold/iron armour and a blaze rod.
     * A support role that grants positive potion effects to allies during combat.
     */
    BARD,

    /**
     * Rogue class: full chainmail armour and a diamond sword with Sharpness III and Unbreaking III.
     * Geared toward fast, agile combat and hit-and-run tactics.
     */
    ROGUE,

    /**
     * Miner class: full iron armour and a diamond pickaxe with Efficiency V and Unbreaking III.
     * Designed for fast and efficient resource gathering.
     */
    MINER,

    /**
     * Knight class: full diamond armour with Protection II on every piece and a sword with Sharpness IV.
     * A tanky frontliner designed for sustained face-to-face combat.
     */
    KNIGHT,

    /**
     * Standard diamond kit, functionally equivalent to {@link #KNIGHT}.
     * This is the default kit assigned to newly registered players.
     */
    DIAMOND
}

package dev.alexissdev.kronos.scoreboard;

/**
 * Holds the metadata of an active KOTH (King Of The Hill) event on the server.
 * <p>
 * An instance of this class is created when a {@code KothStartedDomainEvent}
 * is received by the {@link ScoreboardManager} and stored in its active-KOTH map.
 * The {@link ScoreboardRenderer} consumes these entries to display the name,
 * location, and capture time of each KOTH on every player's sidebar scoreboard.
 * </p>
 * <p>
 * The instance is removed from the map when a {@code KothCapturedDomainEvent}
 * or a {@code KothEndedDomainEvent} arrives.
 * </p>
 */
final class KothEntry {

    final String name;
    final int centerX;
    final int centerZ;
    final int captureTimeSeconds;

    /**
     * Creates a new KOTH entry from the data provided by the start event.
     *
     * @param name               unique name of the KOTH (e.g. {@code "nether"}, {@code "koth1"})
     * @param centerX            X coordinate of the capture zone centre
     * @param centerZ            Z coordinate of the capture zone centre
     * @param captureTimeSeconds total time in seconds required to capture the KOTH
     */
    KothEntry(String name, int centerX, int centerZ, int captureTimeSeconds) {
        this.name = name;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.captureTimeSeconds = captureTimeSeconds;
    }
}

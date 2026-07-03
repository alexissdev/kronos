package dev.alexissdev.kronos.scoreboard;

/**
 * Representa los metadatos de un KOTH (King Of The Hill) activo en el servidor.
 * <p>
 * Una instancia de esta clase se crea cuando el evento
 * {@code KothStartedDomainEvent} es recibido por el {@link ScoreboardManager}
 * y se almacena en su mapa de KOTHs activos. El {@link ScoreboardRenderer}
 * consume estas entradas para mostrar el nombre, la ubicación y el tiempo de
 * captura de cada KOTH en el marcador lateral de todos los jugadores.
 * </p>
 * <p>
 * La instancia se elimina del mapa cuando llega un {@code KothCapturedDomainEvent}
 * o un {@code KothEndedDomainEvent}.
 * </p>
 */
final class KothEntry {

    final String name;
    final int centerX;
    final int centerZ;
    final int captureTimeSeconds;

    /**
     * Construye una nueva entrada de KOTH con los datos del evento de inicio.
     *
     * @param name               nombre único del KOTH (ej. {@code "nether"}, {@code "koth1"})
     * @param centerX            coordenada X del centro de la zona de captura
     * @param centerZ            coordenada Z del centro de la zona de captura
     * @param captureTimeSeconds tiempo total en segundos requerido para capturar el KOTH
     */
    KothEntry(String name, int centerX, int centerZ, int captureTimeSeconds) {
        this.name = name;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.captureTimeSeconds = captureTimeSeconds;
    }
}

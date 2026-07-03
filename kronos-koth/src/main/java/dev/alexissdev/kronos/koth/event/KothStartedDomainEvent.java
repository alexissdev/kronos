package dev.alexissdev.kronos.koth.event;

import dev.alexissdev.kronos.koth.domain.KothZone;

/**
 * Evento de dominio publicado vía Guava {@code EventBus} cuando se inicia un nuevo evento KOTH.
 *
 * <p>Contiene toda la información necesaria para que los módulos consumidores puedan configurar
 * su estado: el nombre del KOTH, las coordenadas del centro de la zona de captura (útiles
 * para brújulas y hologramas), el tiempo total de captura y la referencia completa a la
 * {@link KothZone} activa.</p>
 *
 * <p>Los suscriptores típicos son el sistema de scoreboard, el de temporizadores y el módulo
 * de anuncios globales al servidor.</p>
 */
public final class KothStartedDomainEvent {

    private final String kothName;
    private final int centerX;
    private final int centerZ;
    private final int captureTimeSeconds;
    private final KothZone zone;

    /**
     * Construye el evento con todos los datos del KOTH que acaba de iniciar.
     *
     * @param kothName           nombre único del KOTH iniciado
     * @param centerX            coordenada X del punto central de la zona de captura
     * @param centerZ            coordenada Z del punto central de la zona de captura
     * @param captureTimeSeconds segundos que un jugador debe permanecer para capturarlo
     * @param zone               referencia a la zona KOTH completa con sus límites geográficos
     */
    public KothStartedDomainEvent(String kothName, int centerX, int centerZ,
                                   int captureTimeSeconds, KothZone zone) {
        this.kothName            = kothName;
        this.centerX             = centerX;
        this.centerZ             = centerZ;
        this.captureTimeSeconds  = captureTimeSeconds;
        this.zone                = zone;
    }

    /** @return nombre único del KOTH que inició */
    public String  getKothName()           { return kothName; }
    /** @return coordenada X del centro de la zona de captura */
    public int     getCenterX()            { return centerX; }
    /** @return coordenada Z del centro de la zona de captura */
    public int     getCenterZ()            { return centerZ; }
    /** @return segundos totales de captura requeridos para ganar el KOTH */
    public int     getCaptureTimeSeconds() { return captureTimeSeconds; }
    /** @return referencia completa a la zona KOTH con todas sus coordenadas y configuración */
    public KothZone getZone()              { return zone; }
}

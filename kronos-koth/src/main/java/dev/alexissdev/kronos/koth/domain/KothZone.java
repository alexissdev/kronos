package dev.alexissdev.kronos.koth.domain;

import dev.alexissdev.kronos.common.domain.CrateType;

/**
 * Entidad de dominio que representa una zona KOTH (King of The Hill) en el servidor HCF.
 *
 * <p>Una {@code KothZone} define dos regiones rectangulares en el plano XZ del mundo:
 * <ul>
 *   <li><b>Zona de claim</b> ({@code minX/minZ – maxX/maxZ}): territorio completo del KOTH,
 *       usado para protección y visualización en el mapa.</li>
 *   <li><b>Zona de captura</b> ({@code captureMinX/captureMinZ – captureMaxX/captureMaxZ}):
 *       área interior donde el jugador debe permanecer para acumular tiempo de captura.</li>
 * </ul>
 *
 * <p>La zona es inmutable en sus coordenadas geográficas; únicamente el estado {@code active}
 * puede cambiar en tiempo de ejecución a través de {@link #setActive(boolean)}.</p>
 *
 * <p>Al capturar el KOTH, el sistema entrega al ganador un cofre del tipo {@link CrateType}
 * configurado en {@code rewardCrateType}.</p>
 */
public final class KothZone {

    private final String name;
    private final String world;

    // Full territory — used for protection and claim display
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;

    // Inner area where a player must stand to capture
    private final int captureMinX;
    private final int captureMinZ;
    private final int captureMaxX;
    private final int captureMaxZ;

    private final int captureTimeSeconds;
    private final CrateType rewardCrateType;
    private boolean active;

    /**
     * Construye una nueva zona KOTH con todas sus propiedades geográficas y de juego.
     *
     * @param name               nombre único que identifica este KOTH
     * @param world              nombre del mundo de Bukkit donde se ubica la zona
     * @param minX               coordenada X mínima del territorio de claim
     * @param minZ               coordenada Z mínima del territorio de claim
     * @param maxX               coordenada X máxima del territorio de claim
     * @param maxZ               coordenada Z máxima del territorio de claim
     * @param captureMinX        coordenada X mínima de la zona de captura interior
     * @param captureMinZ        coordenada Z mínima de la zona de captura interior
     * @param captureMaxX        coordenada X máxima de la zona de captura interior
     * @param captureMaxZ        coordenada Z máxima de la zona de captura interior
     * @param captureTimeSeconds segundos que un jugador debe permanecer en la zona de captura
     *                           para ganar el evento
     * @param rewardCrateType    tipo de cofre que se entregará al jugador que capture el KOTH
     */
    public KothZone(String name, String world,
                    int minX, int minZ, int maxX, int maxZ,
                    int captureMinX, int captureMinZ, int captureMaxX, int captureMaxZ,
                    int captureTimeSeconds, CrateType rewardCrateType) {
        this.name = name;
        this.world = world;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.captureMinX = captureMinX;
        this.captureMinZ = captureMinZ;
        this.captureMaxX = captureMaxX;
        this.captureMaxZ = captureMaxZ;
        this.captureTimeSeconds = captureTimeSeconds;
        this.rewardCrateType = rewardCrateType;
        this.active = false;
    }

    /**
     * Comprueba si una ubicación dada se encuentra dentro del territorio de claim de esta zona.
     * Se utiliza para determinar protecciones de construcción/PvP en el área completa del KOTH.
     *
     * @param world nombre del mundo donde se encuentra la ubicación
     * @param x     coordenada X a evaluar
     * @param z     coordenada Z a evaluar
     * @return {@code true} si la ubicación pertenece al territorio de claim de esta zona
     */
    public boolean containsLocation(String world, double x, double z) {
        return this.world.equals(world)
                && x >= minX && x < maxX + 1
                && z >= minZ && z < maxZ + 1;
    }

    /**
     * Comprueba si una ubicación dada está dentro de la zona de captura interior.
     * Un jugador debe permanecer en esta área durante {@link #getCaptureTimeSeconds()} segundos
     * para ganar el evento KOTH.
     *
     * @param world nombre del mundo donde se encuentra la ubicación
     * @param x     coordenada X a evaluar
     * @param z     coordenada Z a evaluar
     * @return {@code true} si la ubicación está dentro de la zona de captura
     */
    public boolean isInCaptureZone(String world, double x, double z) {
        return this.world.equals(world)
                && x >= captureMinX && x < captureMaxX + 1
                && z >= captureMinZ && z < captureMaxZ + 1;
    }

    /** @return nombre único que identifica esta zona KOTH */
    public String getName()            { return name; }
    /** @return nombre del mundo de Bukkit donde está ubicada la zona */
    public String getWorld()           { return world; }
    /** @return coordenada X mínima del territorio de claim */
    public int getMinX()               { return minX; }
    /** @return coordenada Z mínima del territorio de claim */
    public int getMinZ()               { return minZ; }
    /** @return coordenada X máxima del territorio de claim */
    public int getMaxX()               { return maxX; }
    /** @return coordenada Z máxima del territorio de claim */
    public int getMaxZ()               { return maxZ; }
    /** @return coordenada X mínima de la zona de captura interior */
    public int getCaptureMinX()        { return captureMinX; }
    /** @return coordenada Z mínima de la zona de captura interior */
    public int getCaptureMinZ()        { return captureMinZ; }
    /** @return coordenada X máxima de la zona de captura interior */
    public int getCaptureMaxX()        { return captureMaxX; }
    /** @return coordenada Z máxima de la zona de captura interior */
    public int getCaptureMaxZ()        { return captureMaxZ; }
    /** @return segundos que un jugador debe permanecer en la zona de captura para ganar */
    public int getCaptureTimeSeconds() { return captureTimeSeconds; }
    /** @return tipo de cofre que se entrega como recompensa al capturar el KOTH */
    public CrateType getRewardCrateType() { return rewardCrateType; }
    /** @return {@code true} si el evento KOTH está actualmente en curso */
    public boolean isActive()          { return active; }
    /**
     * Cambia el estado activo del evento KOTH. Este método es invocado por
     * {@code KothApplicationService} durante el inicio, fin y captura del evento.
     *
     * @param active {@code true} para marcar el KOTH como activo; {@code false} para desactivarlo
     */
    public void setActive(boolean active) { this.active = active; }
}

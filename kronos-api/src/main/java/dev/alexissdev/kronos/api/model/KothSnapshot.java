package dev.alexissdev.kronos.api.model;

import dev.alexissdev.kronos.common.domain.CrateType;

/**
 * Immutable read-only snapshot of a KOTH (King of The Hill) zone in the HCF system.
 * <p>
 * Represents the state of a KOTH zone at the moment it was queried, including its
 * geographic boundaries in block coordinates, the world it is located in, whether it
 * is currently active, and the type of reward crate delivered to the captor.
 * Because it is immutable, instances are safe to share across threads without synchronization.
 * </p>
 * <p>
 * <b>KOTH context:</b> a KOTH zone defines a rectangular area in the world where players
 * compete to remain without being eliminated. The winner receives a loot crate whose type
 * is determined by {@link #getRewardCrateType()}.
 * </p>
 */
public final class KothSnapshot {

    private final String name;
    private final String world;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final boolean active;
    private final CrateType rewardCrateType;

    /**
     * Construye una nueva instantánea de zona KOTH con todos sus atributos.
     *
     * @param name            nombre identificador único de la zona KOTH en el servidor
     * @param world           nombre del mundo de Bukkit donde se ubica la zona KOTH
     * @param minX            coordenada X mínima de la zona en coordenadas de bloque
     * @param minZ            coordenada Z mínima de la zona en coordenadas de bloque
     * @param maxX            coordenada X máxima de la zona en coordenadas de bloque
     * @param maxZ            coordenada Z máxima de la zona en coordenadas de bloque
     * @param active          {@code true} si la zona está actualmente en período de captura
     * @param rewardCrateType tipo de caja de loot que recibirá el jugador que capture la zona
     */
    public KothSnapshot(String name, String world, int minX, int minZ, int maxX, int maxZ,
                        boolean active, CrateType rewardCrateType) {
        this.name = name;
        this.world = world;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.active = active;
        this.rewardCrateType = rewardCrateType;
    }

    /**
     * Retorna el nombre identificador único de esta zona KOTH en el servidor.
     * <p>
     * El nombre se define en la configuración del servidor y puede usarse con
     * {@code KothApi#getKoth(String)} para obtener el estado actualizado.
     * </p>
     *
     * @return nombre único de la zona KOTH
     */
    public String getName() { return name; }

    /**
     * Retorna el nombre del mundo de Bukkit donde se ubica esta zona KOTH.
     * <p>
     * Puede usarse con {@code Bukkit.getWorld(String)} para obtener la instancia del mundo.
     * </p>
     *
     * @return nombre del mundo donde se encuentra la zona KOTH
     */
    public String getWorld() { return world; }

    /**
     * Retorna la coordenada X mínima de la zona KOTH en coordenadas de bloque.
     *
     * @return coordenada X mínima de la zona
     */
    public int getMinX() { return minX; }

    /**
     * Retorna la coordenada Z mínima de la zona KOTH en coordenadas de bloque.
     *
     * @return coordenada Z mínima de la zona
     */
    public int getMinZ() { return minZ; }

    /**
     * Retorna la coordenada X máxima de la zona KOTH en coordenadas de bloque.
     *
     * @return coordenada X máxima de la zona
     */
    public int getMaxX() { return maxX; }

    /**
     * Retorna la coordenada Z máxima de la zona KOTH en coordenadas de bloque.
     *
     * @return coordenada Z máxima de la zona
     */
    public int getMaxZ() { return maxZ; }

    /**
     * Indica si esta zona KOTH está actualmente en período de captura activo.
     * <p>
     * Una zona activa acepta jugadores competidores y tiene en marcha la cuenta regresiva
     * de captura. Una zona inactiva puede estar en período de espera entre eventos.
     * </p>
     *
     * @return {@code true} si la zona está en período de captura; {@code false} si está inactiva
     */
    public boolean isActive() { return active; }

    /**
     * Retorna el tipo de caja de recompensa que se entregará al jugador que capture esta zona.
     * <p>
     * El {@link CrateType} determina la calidad y contenido del loot que recibirá el captor.
     * Las cajas de mayor rareza suelen tener mejor equipo y más recursos.
     * </p>
     *
     * @return {@link CrateType} de la recompensa de captura de esta zona KOTH
     */
    public CrateType getRewardCrateType() { return rewardCrateType; }
}

package dev.alexissdev.kronos.api.model;

import dev.alexissdev.kronos.claims.domain.ClaimType;

/**
 * Immutable read-only snapshot of a claimed territory in the HCF system.
 * <p>
 * Represents the state of a claimed chunk or region at the moment it was queried,
 * encapsulating its chunk coordinates, zone type, and owning faction. Because it is
 * immutable, instances are safe to share across threads without synchronization.
 * </p>
 * <p>
 * This class does not reflect subsequent changes to the claim's state; to obtain
 * up-to-date data, query again through {@link dev.alexissdev.kronos.api.facade.ClaimApi}.
 * </p>
 */
public final class ClaimSnapshot {

    private final String id;
    private final String factionId;
    private final ClaimType type;
    private final String world;
    private final int minChunkX;
    private final int minChunkZ;
    private final int maxChunkX;
    private final int maxChunkZ;

    /**
     * Construye una nueva instantánea de territorio reclamado con todos sus atributos.
     *
     * @param id        identificador único interno del claim en la base de datos
     * @param factionId identificador de la facción propietaria del territorio;
     *                  puede ser {@code null} o un valor especial para zonas del sistema
     *                  como SafeZone o WarZone
     * @param type      tipo de zona que representa este claim ({@link ClaimType})
     * @param world     nombre del mundo de Bukkit donde se ubica el territorio
     * @param minChunkX coordenada X mínima del claim en el sistema de coordenadas de chunk
     * @param minChunkZ coordenada Z mínima del claim en el sistema de coordenadas de chunk
     * @param maxChunkX coordenada X máxima del claim en el sistema de coordenadas de chunk
     * @param maxChunkZ coordenada Z máxima del claim en el sistema de coordenadas de chunk
     */
    public ClaimSnapshot(String id, String factionId, ClaimType type, String world,
                         int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        this.id = id;
        this.factionId = factionId;
        this.type = type;
        this.world = world;
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.maxChunkX = maxChunkX;
        this.maxChunkZ = maxChunkZ;
    }

    /**
     * Retorna el identificador único interno del claim en la base de datos.
     *
     * @return ID único del territorio reclamado
     */
    public String getId() { return id; }

    /**
     * Retorna el identificador de la facción propietaria de este territorio.
     * <p>
     * Para zonas del sistema como SafeZone o WarZone, este valor puede ser {@code null}
     * o un ID especial reservado. Para territorios de facción regulares, puede usarse con
     * {@code FactionApi#getById(String)} para obtener los detalles completos de la facción.
     * </p>
     *
     * @return ID de la facción propietaria, o {@code null} si es una zona del sistema
     */
    public String getFactionId() { return factionId; }

    /**
     * Retorna el tipo de zona que representa este claim.
     * <p>
     * Los tipos posibles incluyen Wilderness, SafeZone, WarZone y territorio de facción.
     * El tipo determina las reglas de PvP, construcción y acceso aplicables en la zona.
     * </p>
     *
     * @return {@link ClaimType} que clasifica este territorio
     */
    public ClaimType getType() { return type; }

    /**
     * Retorna el nombre del mundo de Bukkit donde se ubica este territorio.
     * <p>
     * Puede usarse con {@code Bukkit.getWorld(String)} para obtener la instancia del mundo.
     * </p>
     *
     * @return nombre del mundo donde se ubica el claim
     */
    public String getWorld() { return world; }

    /**
     * Retorna la coordenada X mínima del claim en el sistema de coordenadas de chunk.
     * <p>
     * Para convertir a coordenadas de bloque, multiplicar por 16.
     * </p>
     *
     * @return coordenada X mínima del chunk del claim
     */
    public int getMinChunkX() { return minChunkX; }

    /**
     * Retorna la coordenada Z mínima del claim en el sistema de coordenadas de chunk.
     * <p>
     * Para convertir a coordenadas de bloque, multiplicar por 16.
     * </p>
     *
     * @return coordenada Z mínima del chunk del claim
     */
    public int getMinChunkZ() { return minChunkZ; }

    /**
     * Retorna la coordenada X máxima del claim en el sistema de coordenadas de chunk.
     * <p>
     * Un claim puede abarcar múltiples chunks; la diferencia entre {@code maxChunkX}
     * y {@code minChunkX} más uno indica el ancho del territorio en chunks.
     * </p>
     *
     * @return coordenada X máxima del chunk del claim
     */
    public int getMaxChunkX() { return maxChunkX; }

    /**
     * Retorna la coordenada Z máxima del claim en el sistema de coordenadas de chunk.
     * <p>
     * Un claim puede abarcar múltiples chunks; la diferencia entre {@code maxChunkZ}
     * y {@code minChunkZ} más uno indica la profundidad del territorio en chunks.
     * </p>
     *
     * @return coordenada Z máxima del chunk del claim
     */
    public int getMaxChunkZ() { return maxChunkZ; }
}

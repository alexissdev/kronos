package dev.alexissdev.kronos.factions.event;

/**
 * Evento de dominio publicado en el {@code EventBus} cuando una facción
 * reclama un nuevo territorio (claim) en el mundo.
 *
 * <p>Los listeners de este evento pueden, por ejemplo, actualizar el mapa
 * visual del servidor, registrar el claim en la base de datos de territorios
 * o notificar a los miembros de la facción que ganaron terreno.
 *
 * <p>El territorio reclamado se describe como un rectángulo de chunks delimitado
 * por las coordenadas ({@code minChunkX}, {@code minChunkZ}) y
 * ({@code maxChunkX}, {@code maxChunkZ}) dentro del mundo indicado.
 */
public final class FactionClaimedDomainEvent {

    private final String factionId;
    private final String claimId;
    private final String claimType;
    private final String world;
    private final int minChunkX;
    private final int minChunkZ;
    private final int maxChunkX;
    private final int maxChunkZ;

    /**
     * Crea el evento con todos los datos del claim recién establecido.
     *
     * @param factionId  ID de la facción que realizó el claim
     * @param claimId    identificador único del claim creado
     * @param claimType  tipo de claim (p.ej. {@code "FACTION"}, {@code "WARZONE"}, {@code "ROAD"})
     * @param world      nombre del mundo de Bukkit donde se ubica el claim
     * @param minChunkX  coordenada X mínima del chunk en el rectángulo del claim
     * @param minChunkZ  coordenada Z mínima del chunk en el rectángulo del claim
     * @param maxChunkX  coordenada X máxima del chunk en el rectángulo del claim
     * @param maxChunkZ  coordenada Z máxima del chunk en el rectángulo del claim
     */
    public FactionClaimedDomainEvent(String factionId, String claimId, String claimType,
                                      String world, int minChunkX, int minChunkZ,
                                      int maxChunkX, int maxChunkZ) {
        this.factionId = factionId;
        this.claimId = claimId;
        this.claimType = claimType;
        this.world = world;
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.maxChunkX = maxChunkX;
        this.maxChunkZ = maxChunkZ;
    }

    /**
     * Devuelve el ID de la facción que realizó el claim.
     *
     * @return ID de la facción
     */
    public String getFactionId() { return factionId; }

    /**
     * Devuelve el identificador único del claim recién creado.
     *
     * @return ID del claim
     */
    public String getClaimId() { return claimId; }

    /**
     * Devuelve el tipo de claim (p.ej. {@code "FACTION"}, {@code "WARZONE"}).
     *
     * @return tipo de claim
     */
    public String getClaimType() { return claimType; }

    /**
     * Devuelve el nombre del mundo donde se ubica el claim.
     *
     * @return nombre del mundo
     */
    public String getWorld() { return world; }

    /**
     * Devuelve la coordenada X mínima del chunk que delimita el claim.
     *
     * @return coordenada X mínima del chunk
     */
    public int getMinChunkX() { return minChunkX; }

    /**
     * Devuelve la coordenada Z mínima del chunk que delimita el claim.
     *
     * @return coordenada Z mínima del chunk
     */
    public int getMinChunkZ() { return minChunkZ; }

    /**
     * Devuelve la coordenada X máxima del chunk que delimita el claim.
     *
     * @return coordenada X máxima del chunk
     */
    public int getMaxChunkX() { return maxChunkX; }

    /**
     * Devuelve la coordenada Z máxima del chunk que delimita el claim.
     *
     * @return coordenada Z máxima del chunk
     */
    public int getMaxChunkZ() { return maxChunkZ; }
}

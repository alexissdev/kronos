package dev.alexissdev.kronos.claims.domain;

/**
 * Entidad de dominio que representa un territorio (claim) en el mundo de Minecraft.
 *
 * <p>Un {@code Claim} delimita un área rectangular de chunks que pertenece a una
 * facción o a una zona especial del servidor (spawn, warzone, koth, etc.). La posición
 * se expresa en coordenadas de chunk, no de bloque, con el fin de simplificar las
 * consultas de pertenencia durante el movimiento de jugadores.</p>
 *
 * <p>La clase es inmutable: una vez creada, sus atributos no cambian. Para modificar
 * el propietario o los límites de un territorio se debe eliminar el claim existente
 * y crear uno nuevo.</p>
 */
public final class Claim {

    private final String id;
    private final String factionId;
    private final ClaimType type;
    private final String world;
    private final int minChunkX;
    private final int minChunkZ;
    private final int maxChunkX;
    private final int maxChunkZ;

    /**
     * Construye un nuevo claim con todos sus atributos.
     *
     * @param id        identificador único del claim (UUID en formato String)
     * @param factionId identificador de la facción propietaria, o {@code null} para zonas del sistema
     * @param type      categoría del territorio (ver {@link ClaimType})
     * @param world     nombre del mundo de Minecraft donde se ubica el claim
     * @param minChunkX coordenada X mínima del chunk que delimita el rectángulo
     * @param minChunkZ coordenada Z mínima del chunk que delimita el rectángulo
     * @param maxChunkX coordenada X máxima del chunk que delimita el rectángulo
     * @param maxChunkZ coordenada Z máxima del chunk que delimita el rectángulo
     */
    public Claim(String id, String factionId, ClaimType type, String world,
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
     * Comprueba si el chunk especificado se encuentra dentro de los límites de este claim.
     *
     * <p>Se usa principalmente en el listener de movimiento para determinar en qué
     * territorio se encuentra un jugador.</p>
     *
     * @param chunkX coordenada X del chunk a evaluar
     * @param chunkZ coordenada Z del chunk a evaluar
     * @return {@code true} si el chunk está contenido en el rectángulo del claim
     */
    public boolean containsChunk(int chunkX, int chunkZ) {
        return chunkX >= minChunkX && chunkX <= maxChunkX
                && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
    }

    /**
     * Calcula el número total de chunks que abarca este claim.
     *
     * <p>Se utiliza para validar límites de tamaño al reclamar territorio o para
     * mostrar estadísticas de la facción.</p>
     *
     * @return cantidad de chunks en el área rectangular del claim
     */
    public int getChunkCount() {
        return (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
    }

    /**
     * Retorna el identificador único de este claim.
     *
     * @return UUID en formato String asignado al claim
     */
    public String getId() { return id; }

    /**
     * Retorna el identificador de la facción propietaria de este territorio.
     *
     * @return ID de la facción, o {@code null} si el claim es de tipo sistema
     */
    public String getFactionId() { return factionId; }

    /**
     * Retorna la categoría de este territorio.
     *
     * @return tipo de claim según {@link ClaimType}
     */
    public ClaimType getType() { return type; }

    /**
     * Retorna el nombre del mundo de Minecraft donde se ubica el claim.
     *
     * @return nombre del mundo (por ejemplo, {@code "world"})
     */
    public String getWorld() { return world; }

    /**
     * Retorna la coordenada X mínima del chunk que delimita el claim.
     *
     * @return límite oeste del territorio en coordenadas de chunk
     */
    public int getMinChunkX() { return minChunkX; }

    /**
     * Retorna la coordenada Z mínima del chunk que delimita el claim.
     *
     * @return límite norte del territorio en coordenadas de chunk
     */
    public int getMinChunkZ() { return minChunkZ; }

    /**
     * Retorna la coordenada X máxima del chunk que delimita el claim.
     *
     * @return límite este del territorio en coordenadas de chunk
     */
    public int getMaxChunkX() { return maxChunkX; }

    /**
     * Retorna la coordenada Z máxima del chunk que delimita el claim.
     *
     * @return límite sur del territorio en coordenadas de chunk
     */
    public int getMaxChunkZ() { return maxChunkZ; }
}

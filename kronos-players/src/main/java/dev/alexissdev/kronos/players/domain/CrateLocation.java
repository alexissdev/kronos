package dev.alexissdev.kronos.players.domain;

import dev.alexissdev.kronos.common.domain.CrateType;

/**
 * Entidad de dominio que representa la ubicación de un crate (cofre de recompensas)
 * en el mundo del servidor.
 *
 * <p>Los crates son cofres especiales colocados en coordenadas específicas del mapa
 * que los jugadores pueden abrir para obtener recompensas aleatorias. Cada crate
 * tiene un tipo que determina la categoría de recompensas que puede otorgar.</p>
 *
 * <p>Las ubicaciones de crates se persisten en MongoDB a través de
 * {@code CrateLocationRepository} para que sobrevivan reinicios del servidor.</p>
 */
public final class CrateLocation {

    private final String id;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final CrateType type;

    /**
     * Crea una nueva ubicación de crate con todos sus datos.
     *
     * @param id    identificador único del crate, generado como UUID aleatorio
     * @param world nombre del mundo de Minecraft donde está ubicado el crate
     * @param x     coordenada X del bloque donde se encuentra el crate
     * @param y     coordenada Y del bloque donde se encuentra el crate
     * @param z     coordenada Z del bloque donde se encuentra el crate
     * @param type  tipo de crate que determina la tabla de recompensas disponibles
     */
    public CrateLocation(String id, String world, int x, int y, int z, CrateType type) {
        this.id    = id;
        this.world = world;
        this.x     = x;
        this.y     = y;
        this.z     = z;
        this.type  = type;
    }

    /**
     * Obtiene el identificador único de esta ubicación de crate.
     *
     * @return ID único del crate
     */
    public String    getId()    { return id; }

    /**
     * Obtiene el nombre del mundo de Minecraft donde está ubicado el crate.
     *
     * @return nombre del mundo del servidor
     */
    public String    getWorld() { return world; }

    /**
     * Obtiene la coordenada X del bloque donde se encuentra el crate.
     *
     * @return coordenada X en el mundo de Minecraft
     */
    public int       getX()     { return x; }

    /**
     * Obtiene la coordenada Y del bloque donde se encuentra el crate.
     *
     * @return coordenada Y en el mundo de Minecraft
     */
    public int       getY()     { return y; }

    /**
     * Obtiene la coordenada Z del bloque donde se encuentra el crate.
     *
     * @return coordenada Z en el mundo de Minecraft
     */
    public int       getZ()     { return z; }

    /**
     * Obtiene el tipo de crate, que determina la categoría de recompensas disponibles
     * al abrir este cofre.
     *
     * @return tipo de crate asociado a esta ubicación
     */
    public CrateType getType()  { return type; }
}

package dev.alexissdev.kronos.spawn.domain;

import org.bukkit.Location;

/**
 * Entidad de dominio que representa la zona de spawn del servidor HCF.
 *
 * <p>El spawn es una región cuboide (definida por dos esquinas en el plano XZ) donde
 * se aplican reglas especiales de protección: el PvP está deshabilitado y los jugadores
 * con etiqueta de combate ({@code COMBAT_TAG}) son impedidos de entrar.</p>
 *
 * <p>A diferencia de {@code KothZone}, el spawn solo tiene una región (sin zona de captura
 * interior) y el servidor solo puede tener un spawn configurado a la vez.</p>
 *
 * <p>La zona es completamente inmutable tras su construcción.</p>
 */
public final class SpawnZone {

    private final String world;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;

    /**
     * Construye una nueva zona de spawn con los límites rectangulares indicados.
     *
     * @param world nombre del mundo de Bukkit donde se ubica la zona de spawn
     * @param minX  coordenada X mínima de la zona
     * @param minZ  coordenada Z mínima de la zona
     * @param maxX  coordenada X máxima de la zona
     * @param maxZ  coordenada Z máxima de la zona
     */
    public SpawnZone(String world, int minX, int minZ, int maxX, int maxZ) {
        this.world = world;
        this.minX  = minX;
        this.minZ  = minZ;
        this.maxX  = maxX;
        this.maxZ  = maxZ;
    }

    /**
     * Comprueba si una ubicación de Bukkit está dentro de los límites de esta zona de spawn.
     * Se usa en {@code SpawnListener} para detectar entradas y salidas de la zona en tiempo real.
     *
     * @param loc ubicación de Bukkit a evaluar
     * @return {@code true} si la ubicación pertenece a esta zona de spawn
     */
    public boolean contains(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(world)) return false;
        double x = loc.getX();
        double z = loc.getZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /** @return nombre del mundo de Bukkit donde se ubica esta zona de spawn */
    public String getWorld() { return world; }
    /** @return coordenada X mínima de la zona de spawn */
    public int getMinX()     { return minX; }
    /** @return coordenada Z mínima de la zona de spawn */
    public int getMinZ()     { return minZ; }
    /** @return coordenada X máxima de la zona de spawn */
    public int getMaxX()     { return maxX; }
    /** @return coordenada Z máxima de la zona de spawn */
    public int getMaxZ()     { return maxZ; }
}

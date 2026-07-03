package dev.alexissdev.kronos.factions.domain;

/**
 * Punto de teletransporte hogar de una facción.
 *
 * <p>Almacena la posición exacta (mundo, coordenadas XYZ y rotación) a la que
 * serán teleportados los miembros cuando usen el comando {@code /f home}.
 * Una facción puede tener como máximo un hogar activo; si no tiene ninguno,
 * el campo {@code home} en {@link Faction} es {@code null}.
 *
 * <p>Esta clase es inmutable: una vez creada, la ubicación no puede cambiar.
 * Para mover el hogar se debe reemplazar la instancia completa mediante
 * {@link Faction#setHome(FactionHome)}.
 */
public final class FactionHome {

    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    /**
     * Crea una nueva ubicación de hogar para la facción.
     *
     * @param world nombre del mundo de Bukkit donde se ubica el hogar
     * @param x     coordenada X (este/oeste)
     * @param y     coordenada Y (altura)
     * @param z     coordenada Z (norte/sur)
     * @param yaw   rotación horizontal del jugador al ser teleportado (en grados)
     * @param pitch rotación vertical del jugador al ser teleportado (en grados)
     */
    public FactionHome(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /**
     * Devuelve el nombre del mundo donde se encuentra el hogar.
     *
     * @return nombre del mundo de Bukkit
     */
    public String getWorld() { return world; }

    /**
     * Devuelve la coordenada X del hogar.
     *
     * @return coordenada X
     */
    public double getX()     { return x; }

    /**
     * Devuelve la coordenada Y (altura) del hogar.
     *
     * @return coordenada Y
     */
    public double getY()     { return y; }

    /**
     * Devuelve la coordenada Z del hogar.
     *
     * @return coordenada Z
     */
    public double getZ()     { return z; }

    /**
     * Devuelve la rotación horizontal (yaw) con la que el jugador
     * quedará orientado al ser teleportado al hogar.
     *
     * @return yaw en grados
     */
    public float getYaw()    { return yaw; }

    /**
     * Devuelve la rotación vertical (pitch) con la que el jugador
     * quedará orientado al ser teleportado al hogar.
     *
     * @return pitch en grados
     */
    public float getPitch()  { return pitch; }
}

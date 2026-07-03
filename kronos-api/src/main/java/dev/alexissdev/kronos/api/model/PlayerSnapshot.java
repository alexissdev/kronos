package dev.alexissdev.kronos.api.model;

import java.util.UUID;

/**
 * Immutable read-only snapshot of a player's data in the HCF system.
 * <p>
 * Groups the player's statistics (kills and deaths) together with their economic balance
 * and connection status at the time of the query into a single object. Because it is
 * immutable, instances are safe to share across threads without synchronization.
 * </p>
 * <p>
 * This class does not reflect subsequent changes to the player's state; for up-to-date
 * data, query again through {@link dev.alexissdev.kronos.api.facade.PlayerDataApi}.
 * </p>
 */
public final class PlayerSnapshot {

    private final UUID uuid;
    private final String name;
    private final int kills;
    private final int deaths;
    private final double balance;
    private final boolean online;

    /**
     * Construye una nueva instantánea de datos de jugador.
     *
     * @param uuid    UUID único e inmutable del jugador en Minecraft
     * @param name    nombre de usuario (username) del jugador en el momento de la consulta
     * @param kills   número total de kills (eliminaciones a otros jugadores) acumuladas
     * @param deaths  número total de deaths (muertes a manos de otros jugadores) acumuladas
     * @param balance saldo económico del jugador en la moneda del servidor al momento de la consulta
     * @param online  {@code true} si el jugador está actualmente conectado al servidor
     */
    public PlayerSnapshot(UUID uuid, String name, int kills, int deaths, double balance, boolean online) {
        this.uuid = uuid;
        this.name = name;
        this.kills = kills;
        this.deaths = deaths;
        this.balance = balance;
        this.online = online;
    }

    /**
     * Retorna el UUID único del jugador en Minecraft.
     * <p>
     * El UUID es el identificador persistente del jugador y no cambia aunque
     * el jugador modifique su nombre de usuario.
     * </p>
     *
     * @return UUID del jugador
     */
    public UUID getUuid() { return uuid; }

    /**
     * Retorna el nombre de usuario (username) del jugador en el momento en que se
     * tomó esta instantánea.
     * <p>
     * Nótese que los nombres de usuario de Minecraft pueden cambiar; el UUID es
     * el identificador confiable para referirse a un jugador de forma permanente.
     * </p>
     *
     * @return nombre de usuario del jugador
     */
    public String getName() { return name; }

    /**
     * Retorna el número total de kills acumuladas por el jugador en el servidor HCF.
     * <p>
     * Solo se contabilizan las eliminaciones a otros jugadores (PvP), no las
     * muertes causadas por mobs u otras fuentes.
     * </p>
     *
     * @return total de kills del jugador; mínimo {@code 0}
     */
    public int getKills() { return kills; }

    /**
     * Retorna el número total de deaths acumuladas por el jugador en el servidor HCF.
     * <p>
     * En el sistema DTK, cada muerte de un miembro contribuye al decremento del
     * contador de su facción. Solo se cuentan las muertes causadas por otros jugadores.
     * </p>
     *
     * @return total de deaths del jugador; mínimo {@code 0}
     */
    public int getDeaths() { return deaths; }

    /**
     * Retorna el saldo económico del jugador en la moneda del servidor al momento
     * en que se tomó esta instantánea.
     * <p>
     * El balance puede cambiar continuamente por transacciones económicas; esta
     * instantánea refleja el valor en el instante de la consulta.
     * </p>
     *
     * @return balance económico del jugador; puede ser {@code 0.0} si no tiene fondos
     */
    public double getBalance() { return balance; }

    /**
     * Indica si el jugador estaba conectado al servidor en el momento en que se tomó
     * esta instantánea.
     * <p>
     * El estado de conexión puede cambiar en cualquier momento; consultar directamente
     * {@code PlayerDataApi#isOnline(UUID)} para verificar el estado actual en tiempo real.
     * </p>
     *
     * @return {@code true} si el jugador estaba en línea al momento de la consulta;
     *         {@code false} si estaba desconectado
     */
    public boolean isOnline() { return online; }
}

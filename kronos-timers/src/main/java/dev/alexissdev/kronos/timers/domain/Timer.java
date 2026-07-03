package dev.alexissdev.kronos.timers.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad de dominio que representa un timer activo asociado a un jugador HCF.
 *
 * <p>Un timer es una restricción temporal aplicada a un jugador durante un período
 * determinado. Puede ser de diferentes tipos ({@link TimerType}): protección PvP al
 * entrar, combat tag al entrar en combate, deathban tras quedarse sin vidas, cooldown
 * de enderpearl o manzana dorada, timer de logout, etc.</p>
 *
 * <p>El timer almacena el momento exacto de expiración como un {@link Instant} y
 * expone métodos para comprobar si ya expiró y cuánto tiempo le queda. La persistencia
 * primaria se realiza en Redis (con TTL nativo) y existe un respaldo en MongoDB.</p>
 */
public final class Timer {

    private final UUID playerUuid;
    private final TimerType type;
    private final Instant expiresAt;

    /**
     * Crea un nuevo timer para el jugador y tipo especificados con la hora de expiración dada.
     *
     * @param playerUuid UUID del jugador al que pertenece este timer
     * @param type       tipo de timer que determina su comportamiento y restricciones
     * @param expiresAt  instante exacto en que el timer expirará y dejará de estar activo
     */
    public Timer(UUID playerUuid, TimerType type, Instant expiresAt) {
        this.playerUuid = playerUuid;
        this.type = type;
        this.expiresAt = expiresAt;
    }

    /**
     * Verifica si el timer ya ha expirado comparando el instante actual con {@link #expiresAt}.
     *
     * @return {@code true} si el momento actual es posterior al instante de expiración,
     *         {@code false} si el timer sigue activo
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Calcula los milisegundos restantes hasta que el timer expire.
     * Si el timer ya expiró, devuelve cero en lugar de un valor negativo.
     *
     * @return milisegundos restantes hasta la expiración del timer, mínimo {@code 0}
     */
    public long getRemainingMillis() {
        long remaining = expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
        return Math.max(0L, remaining);
    }

    /**
     * Obtiene el UUID del jugador al que pertenece este timer.
     *
     * @return UUID del jugador propietario del timer
     */
    public UUID getPlayerUuid() { return playerUuid; }

    /**
     * Obtiene el tipo de este timer, que determina las restricciones que impone al jugador.
     *
     * @return tipo del timer
     */
    public TimerType getType() { return type; }

    /**
     * Obtiene el instante exacto en que este timer expirará.
     *
     * @return instante UTC de expiración del timer
     */
    public Instant getExpiresAt() { return expiresAt; }
}

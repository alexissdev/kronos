package dev.alexissdev.kronos.api.model;

import dev.alexissdev.kronos.timers.domain.TimerType;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable read-only snapshot of an active player timer in the HCF system.
 * <p>
 * Captures the state of a specific timer (combat tag, PvP timer, enderpearl cooldown, etc.)
 * at the moment it was queried, including when it will expire and how much time remains.
 * Because it is immutable, instances are safe to share across threads without synchronization.
 * </p>
 * <p>
 * Because the value of {@link #getRemainingMillis()} decreases as time passes, this snapshot
 * becomes stale quickly. To verify the current state of a timer, query again through
 * {@link dev.alexissdev.kronos.api.facade.TimerApi}.
 * </p>
 */
public final class TimerSnapshot {

    private final UUID playerUuid;
    private final TimerType type;
    private final Instant expiresAt;
    private final long remainingMillis;

    /**
     * Construye una nueva instantánea de temporizador activo.
     *
     * @param playerUuid      UUID del jugador al que pertenece este temporizador
     * @param type            tipo de temporizador que está activo (por ejemplo,
     *                        {@code TimerType.COMBAT_TAG}, {@code TimerType.PVP_TIMER})
     * @param expiresAt       instante exacto en UTC en que el temporizador expirará
     * @param remainingMillis milisegundos restantes hasta la expiración, calculados
     *                        en el momento de crear esta instantánea
     */
    public TimerSnapshot(UUID playerUuid, TimerType type, Instant expiresAt, long remainingMillis) {
        this.playerUuid = playerUuid;
        this.type = type;
        this.expiresAt = expiresAt;
        this.remainingMillis = remainingMillis;
    }

    /**
     * Retorna el UUID del jugador al que pertenece este temporizador.
     *
     * @return UUID del jugador con el temporizador activo
     */
    public UUID getPlayerUuid() { return playerUuid; }

    /**
     * Retorna el tipo de temporizador representado por esta instantánea.
     * <p>
     * Permite a los consumidores de la API identificar qué restricción está activa
     * sobre el jugador y reaccionar en consecuencia.
     * </p>
     *
     * @return {@link TimerType} que identifica el tipo de temporizador activo
     */
    public TimerType getType() { return type; }

    /**
     * Retorna el instante exacto en UTC en que este temporizador expirará.
     * <p>
     * Puede usarse para mostrar cuenta regresiva en interfaces de usuario o calcular
     * cuándo se habilitarán de nuevo las acciones restringidas por el temporizador.
     * </p>
     *
     * @return {@link Instant} de expiración del temporizador en UTC
     */
    public Instant getExpiresAt() { return expiresAt; }

    /**
     * Retorna los milisegundos restantes hasta la expiración del temporizador,
     * calculados en el momento en que se creó esta instantánea.
     * <p>
     * Este valor <em>no</em> se actualiza automáticamente con el paso del tiempo.
     * Para obtener el tiempo restante actualizado, usar
     * {@code TimerApi#getRemainingMillis(UUID, TimerType)}.
     * </p>
     *
     * @return milisegundos restantes hasta la expiración al momento de la consulta;
     *         valor positivo si el temporizador sigue activo
     */
    public long getRemainingMillis() { return remainingMillis; }
}

package dev.alexissdev.kronos.players.repository;

import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato de acceso asíncrono al estado de Deathban de los jugadores.
 *
 * <p>El Deathban es el mecanismo central del modo HCF: cuando un jugador muere
 * sin vidas restantes, queda baneado temporalmente del servidor durante un período
 * determinado. Este repositorio gestiona la duración del baneo usando Redis como
 * almacenamiento, aprovechando su soporte nativo de TTL (Time To Live) para que el
 * baneo expire automáticamente sin necesidad de tareas programadas.</p>
 *
 * <p>La implementación predeterminada es {@code RedisDeathbanRepository}.</p>
 */
public interface DeathbanRepository {

    /**
     * Registra un Deathban activo para el jugador indicado durante la duración especificada.
     * Si ya existía un Deathban previo, lo sobrescribe con la nueva duración.
     *
     * @param uuid            UUID del jugador al que se le aplica el Deathban
     * @param durationSeconds duración del baneo en segundos
     * @return future que se resuelve cuando el Deathban ha sido registrado en Redis
     */
    CompletableFuture<Void> setDeathban(UUID uuid, long durationSeconds);

    /**
     * Consulta el tiempo restante del Deathban activo de un jugador.
     *
     * @param uuid UUID del jugador a consultar
     * @return future que se resuelve con un {@link OptionalLong} que contiene los segundos
     *         restantes del baneo si está activo, o vacío si el jugador no está baneado
     *         (el TTL ya expiró o nunca fue registrado)
     */
    CompletableFuture<OptionalLong> getRemainingSeconds(UUID uuid);

    /**
     * Elimina el Deathban activo de un jugador antes de que expire naturalmente.
     * Se usa cuando un administrador perdona el baneo manualmente o cuando el jugador
     * cumple un requisito especial para ser liberado.
     *
     * @param uuid UUID del jugador al que se le revoca el Deathban
     * @return future que se resuelve cuando el Deathban ha sido eliminado de Redis
     */
    CompletableFuture<Void> removeDeathban(UUID uuid);
}

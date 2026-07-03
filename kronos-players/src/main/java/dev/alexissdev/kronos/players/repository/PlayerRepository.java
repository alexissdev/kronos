package dev.alexissdev.kronos.players.repository;

import dev.alexissdev.kronos.players.domain.HCFPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato de acceso asíncrono a los datos persistentes de jugadores HCF.
 *
 * <p>Define las operaciones de lectura y escritura sobre entidades {@link HCFPlayer}.
 * La implementación predeterminada ({@code MongoPlayerRepository}) persiste los datos
 * en la colección {@code players} de MongoDB. Todas las operaciones son no bloqueantes
 * y devuelven {@link CompletableFuture} para integrarse con el modelo asíncrono del plugin.</p>
 */
public interface PlayerRepository {

    /**
     * Busca un jugador por su UUID de Minecraft de forma asíncrona.
     *
     * @param uuid UUID único del jugador a buscar
     * @return future que se resuelve con un {@link Optional} que contiene el jugador
     *         si existe en la base de datos, o vacío si no está registrado
     */
    CompletableFuture<Optional<HCFPlayer>> findByUuid(UUID uuid);

    /**
     * Guarda o actualiza el perfil de un jugador en la base de datos (upsert).
     * Si el jugador ya existe, sus datos se reemplazan por completo.
     *
     * @param player entidad {@link HCFPlayer} con los datos actualizados a persistir
     * @return future que se resuelve con la misma entidad guardada
     */
    CompletableFuture<HCFPlayer> save(HCFPlayer player);
}

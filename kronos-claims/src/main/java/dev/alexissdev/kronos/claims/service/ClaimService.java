package dev.alexissdev.kronos.claims.service;

import dev.alexissdev.kronos.claims.domain.Claim;
import dev.alexissdev.kronos.claims.domain.ClaimType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de aplicación que expone las operaciones de alto nivel sobre territorios (claims).
 *
 * <p>Define el contrato que deben implementar los servicios de claims en el sistema HCF.
 * La implementación principal es
 * {@link dev.alexissdev.kronos.claims.ClaimApplicationService}, inyectada por Guice.
 * Todas las operaciones son asíncronas y retornan {@link CompletableFuture} para no
 * bloquear el hilo principal del servidor Bukkit.</p>
 */
public interface ClaimService {

    /**
     * Reclama un área rectangular de chunks en nombre de una facción.
     *
     * <p>Valida que el actor tenga rango de {@code CAPTAIN} o superior dentro de la
     * facción y que ningún chunk del área esté previamente reclamado. Si la validación
     * es exitosa, persiste el nuevo claim y publica un
     * {@link dev.alexissdev.kronos.factions.event.FactionClaimedDomainEvent}.</p>
     *
     * @param factionId identificador de la facción que reclama el territorio
     * @param actorUuid UUID del jugador que ejecuta la acción
     * @param world     nombre del mundo donde se reclama
     * @param minChunkX coordenada X mínima del área (inclusive)
     * @param minChunkZ coordenada Z mínima del área (inclusive)
     * @param maxChunkX coordenada X máxima del área (inclusive)
     * @param maxChunkZ coordenada Z máxima del área (inclusive)
     * @return futuro con el {@link Claim} creado y persistido
     * @throws dev.alexissdev.kronos.claims.exception.ClaimConflictException si algún chunk del área ya está ocupado
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si el actor no tiene el rango requerido
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException    si la facción no existe
     */
    CompletableFuture<Claim> claim(String factionId, UUID actorUuid, String world,
                                   int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ);

    /**
     * Libera el territorio del chunk indicado, eliminando el claim que lo ocupa.
     *
     * <p>Solo el propietario del claim puede desreclamarlo. El actor debe pertenecer
     * a la facción propietaria del chunk.</p>
     *
     * @param factionId identificador de la facción que intenta desreclamar
     * @param actorUuid UUID del jugador que ejecuta la acción
     * @param world     nombre del mundo donde se ubica el chunk
     * @param chunkX    coordenada X del chunk a liberar
     * @param chunkZ    coordenada Z del chunk a liberar
     * @return futuro que se completa cuando el claim ha sido eliminado
     * @throws dev.alexissdev.kronos.claims.exception.ClaimConflictException       si el chunk no tiene ningún claim
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si la facción no es propietaria
     */
    CompletableFuture<Void> unclaim(String factionId, UUID actorUuid, String world, int chunkX, int chunkZ);

    /**
     * Elimina todos los claims de una facción de forma masiva.
     *
     * <p>Se utiliza principalmente al disolver una facción para liberar todo su territorio.</p>
     *
     * @param factionId identificador de la facción cuyo territorio se libera
     * @return futuro que se completa cuando todos los claims han sido eliminados
     */
    CompletableFuture<Void> unclaimAll(String factionId);

    /**
     * Retorna el claim que ocupa el chunk especificado, si existe.
     *
     * @param world  nombre del mundo donde se busca
     * @param chunkX coordenada X del chunk
     * @param chunkZ coordenada Z del chunk
     * @return futuro con el claim encontrado, o vacío si el chunk es tierra libre
     */
    CompletableFuture<Optional<Claim>> getClaimAt(String world, int chunkX, int chunkZ);

    /**
     * Retorna el tipo de territorio en las coordenadas de chunk indicadas.
     *
     * <p>Si el chunk no tiene ningún claim, retorna {@link ClaimType#WILDERNESS}.</p>
     *
     * @param world  nombre del mundo donde se consulta
     * @param chunkX coordenada X del chunk
     * @param chunkZ coordenada Z del chunk
     * @return futuro con el {@link ClaimType} del territorio
     */
    CompletableFuture<ClaimType> getClaimTypeAt(String world, int chunkX, int chunkZ);

    /**
     * Recupera la lista completa de claims de una facción.
     *
     * @param factionId identificador de la facción
     * @return futuro con todos los claims de la facción; lista vacía si no tiene territorio
     */
    CompletableFuture<List<Claim>> getFactionClaims(String factionId);

    /**
     * Recupera todos los claims existentes en el servidor.
     *
     * <p>Principalmente utilizado para precargar el caché en memoria al iniciar el plugin.</p>
     *
     * @return futuro con la lista de todos los claims
     */
    CompletableFuture<List<Claim>> getAllClaims();

    /**
     * Permite a una facción conquistar el territorio de una facción enemiga o en estado raidable.
     *
     * <p>El actor debe tener rango de {@code CAPTAIN} o superior. El chunk objetivo debe
     * pertenecer a una facción que sea enemiga del actor o que esté siendo raideada.
     * El claim existente se elimina y se crea uno nuevo a nombre de la facción atacante.</p>
     *
     * @param factionId identificador de la facción que conquista el territorio
     * @param actorUuid UUID del jugador que ejecuta la acción
     * @param world     nombre del mundo donde se ubica el chunk
     * @param chunkX    coordenada X del chunk a conquistar
     * @param chunkZ    coordenada Z del chunk a conquistar
     * @return futuro que se completa cuando la conquista ha sido registrada
     * @throws dev.alexissdev.kronos.claims.exception.ClaimConflictException       si el chunk es propio o no cumple las condiciones
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si el actor no tiene el rango requerido
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException   si la facción atacante no existe
     */
    CompletableFuture<Void> overclaim(String factionId, UUID actorUuid, String world, int chunkX, int chunkZ);
}

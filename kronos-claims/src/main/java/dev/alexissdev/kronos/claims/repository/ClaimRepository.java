package dev.alexissdev.kronos.claims.repository;

import dev.alexissdev.kronos.claims.domain.Claim;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de persistencia para la entidad {@link dev.alexissdev.kronos.claims.domain.Claim}.
 *
 * <p>Define el contrato que deben cumplir todas las implementaciones de almacenamiento
 * de territories (claims). Actualmente la implementación concreta es
 * {@link dev.alexissdev.kronos.claims.persistence.MongoClaimRepository}, que usa MongoDB.
 * Todas las operaciones son asíncronas y retornan {@link CompletableFuture} para no
 * bloquear el hilo principal del servidor Bukkit.</p>
 */
public interface ClaimRepository {

    /**
     * Busca el claim que contiene el chunk especificado dentro de un mundo dado.
     *
     * <p>La búsqueda evalúa si las coordenadas del chunk caen dentro del rectángulo
     * {@code [minChunkX, maxChunkX] × [minChunkZ, maxChunkZ]} almacenado en el claim.</p>
     *
     * @param world  nombre del mundo de Minecraft donde se busca
     * @param chunkX coordenada X del chunk
     * @param chunkZ coordenada Z del chunk
     * @return futuro con el claim encontrado, o {@link java.util.Optional#empty()} si el
     *         chunk está en tierra libre ({@code WILDERNESS})
     */
    CompletableFuture<Optional<Claim>> findByChunk(String world, int chunkX, int chunkZ);

    /**
     * Recupera todos los claims que pertenecen a una facción concreta.
     *
     * @param factionId identificador de la facción propietaria
     * @return futuro con la lista de claims de la facción; lista vacía si no tiene territorio
     */
    CompletableFuture<List<Claim>> findByFaction(String factionId);

    /**
     * Recupera la totalidad de los claims almacenados en la base de datos.
     *
     * <p>Se usa principalmente al arrancar el servidor para precargar el caché
     * en memoria del {@link dev.alexissdev.kronos.claims.listener.ClaimListener}.</p>
     *
     * @return futuro con la lista completa de todos los claims
     */
    CompletableFuture<List<Claim>> findAll();

    /**
     * Persiste un claim nuevo o actualiza uno existente (upsert por {@code id}).
     *
     * @param claim entidad de dominio a guardar
     * @return futuro con el mismo claim recibido, tras confirmar la escritura
     */
    CompletableFuture<Claim> save(Claim claim);

    /**
     * Elimina todos los claims que pertenecen a una facción.
     *
     * <p>Se invoca cuando una facción es disuelta para limpiar todo su territorio.</p>
     *
     * @param factionId identificador de la facción cuyo territorio se libera
     * @return futuro que se completa cuando todos los claims han sido eliminados
     */
    CompletableFuture<Void> deleteByFaction(String factionId);

    /**
     * Elimina el claim con el identificador especificado.
     *
     * @param id UUID en formato String del claim a eliminar
     * @return futuro que se completa cuando el claim ha sido eliminado
     */
    CompletableFuture<Void> delete(String id);
}

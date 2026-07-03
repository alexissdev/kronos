package dev.alexissdev.kronos.api.facade;

import dev.alexissdev.kronos.api.model.ClaimSnapshot;
import dev.alexissdev.kronos.claims.domain.ClaimType;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

/**
 * Read-only facade for querying the territory (claim) system of the HCF plugin.
 * <p>
 * Allows external plugins to determine the zone type of any map location and verify
 * whether a specific chunk belongs to a faction. The HCF system defines the following
 * zone types:
 * </p>
 * <ul>
 *   <li><b>Wilderness</b>: unclaimed terrain where PvP and raiding are unrestricted.</li>
 *   <li><b>SafeZone</b>: protected area (e.g. spawn), where PvP is completely prohibited.</li>
 *   <li><b>WarZone</b>: permanent combat area with no faction protection.</li>
 *   <li><b>Faction</b>: chunk claimed and owned by a specific faction.</li>
 * </ul>
 * <p>
 * All operations are synchronous from the caller's perspective, even though they may
 * internally resolve futures from the claim service.
 * </p>
 */
public interface ClaimApi {

    /**
     * Retorna la instantánea del territorio reclamado en las coordenadas de chunk indicadas,
     * si existe alguno.
     * <p>
     * Las coordenadas de chunk se obtienen dividiendo las coordenadas de bloque entre 16,
     * o mediante {@code location.getChunk().getX()} / {@code location.getChunk().getZ()}.
     * </p>
     *
     * @param world  mundo de Bukkit donde se ubica el chunk a consultar
     * @param chunkX coordenada X del chunk en el sistema de coordenadas de chunk
     * @param chunkZ coordenada Z del chunk en el sistema de coordenadas de chunk
     * @return {@link Optional} con el {@link ClaimSnapshot} del territorio si el chunk está
     *         reclamado, o vacío si es Wilderness u otro tipo sin facción propietaria
     */
    Optional<ClaimSnapshot> getClaimAt(World world, int chunkX, int chunkZ);

    /**
     * Retorna el tipo de zona correspondiente a la ubicación exacta indicada.
     * <p>
     * Determina si la ubicación pertenece a Wilderness, SafeZone, WarZone
     * o al territorio de una facción. Es la forma más directa de verificar
     * restricciones de PvP o construcción en una posición dada.
     * </p>
     *
     * @param location ubicación de Bukkit cuyo tipo de zona se desea conocer;
     *                 debe tener un mundo válido no nulo
     * @return {@link ClaimType} que representa el tipo de zona en esa ubicación
     */
    ClaimType getClaimTypeAt(Location location);

    /**
     * Verifica si el chunk en las coordenadas indicadas está reclamado por alguna facción.
     * <p>
     * Las zonas especiales como SafeZone y WarZone también se consideran reclamadas
     * en el sentido de que tienen un propietario o tipo asignado; sin embargo,
     * este método retorna {@code false} únicamente si el chunk es Wilderness.
     * </p>
     *
     * @param world  mundo de Bukkit donde se ubica el chunk
     * @param chunkX coordenada X del chunk
     * @param chunkZ coordenada Z del chunk
     * @return {@code true} si el chunk tiene un claim asignado; {@code false} si es Wilderness
     */
    boolean isClaimed(World world, int chunkX, int chunkZ);

    /**
     * Verifica si la ubicación indicada se encuentra en territorio Wilderness (zona sin reclamar).
     * <p>
     * El Wilderness es el terreno neutral donde las facciones pueden entablar combate,
     * construir bases sin protección y reclamar nuevos territorios.
     * </p>
     *
     * @param location ubicación de Bukkit a verificar
     * @return {@code true} si la ubicación está en Wilderness; {@code false} en caso contrario
     */
    boolean isWilderness(Location location);

    /**
     * Verifica si la ubicación indicada se encuentra en una zona segura (SafeZone).
     * <p>
     * Las SafeZones son áreas protegidas donde el PvP está completamente prohibido
     * y los jugadores no pueden ser atacados. Generalmente incluyen el área del spawn.
     * </p>
     *
     * @param location ubicación de Bukkit a verificar
     * @return {@code true} si la ubicación está en una SafeZone; {@code false} en caso contrario
     */
    boolean isSafeZone(Location location);

    /**
     * Verifica si la ubicación indicada se encuentra en una zona de guerra (WarZone).
     * <p>
     * Las WarZones son áreas de combate permanente sin protección de facción,
     * típicamente ubicadas alrededor del spawn o en regiones estratégicas del mapa.
     * El PvP siempre está habilitado y no se pueden reclamar territorios dentro de ellas.
     * </p>
     *
     * @param location ubicación de Bukkit a verificar
     * @return {@code true} si la ubicación está en una WarZone; {@code false} en caso contrario
     */
    boolean isWarZone(Location location);
}

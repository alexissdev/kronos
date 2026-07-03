package dev.alexissdev.kronos.players.repository;

import dev.alexissdev.kronos.players.domain.CrateLocation;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato de acceso asíncrono a las ubicaciones de crates registradas en el servidor.
 *
 * <p>Permite registrar, consultar y eliminar cofres de recompensas en posiciones
 * específicas del mapa. La implementación predeterminada ({@code MongoCrateLocationRepository})
 * persiste los datos en la colección {@code crate_locations} de MongoDB, garantizando
 * que las ubicaciones sobrevivan reinicios del servidor.</p>
 */
public interface CrateLocationRepository {

    /**
     * Busca un crate por sus coordenadas exactas en un mundo específico.
     *
     * @param world nombre del mundo de Minecraft donde se busca el crate
     * @param x     coordenada X del bloque del crate
     * @param y     coordenada Y del bloque del crate
     * @param z     coordenada Z del bloque del crate
     * @return future que se resuelve con un {@link Optional} que contiene el crate
     *         si existe en esas coordenadas, o vacío si no hay ningún crate allí
     */
    CompletableFuture<Optional<CrateLocation>> findByLocation(String world, int x, int y, int z);

    /**
     * Obtiene todas las ubicaciones de crates registradas en el servidor.
     * Se usa típicamente al iniciar el servidor para cargar los crates en caché.
     *
     * @return future que se resuelve con la lista completa de ubicaciones de crates;
     *         la lista estará vacía si no hay ningún crate registrado
     */
    CompletableFuture<List<CrateLocation>> findAll();

    /**
     * Guarda o actualiza una ubicación de crate en la base de datos (upsert).
     *
     * @param location entidad {@link CrateLocation} con los datos del crate a persistir
     * @return future que se resuelve con la misma entidad guardada
     */
    CompletableFuture<CrateLocation> save(CrateLocation location);

    /**
     * Elimina permanentemente un crate de la base de datos por su identificador único.
     *
     * @param id identificador único del crate a eliminar
     * @return future que se resuelve cuando la eliminación se ha completado
     */
    CompletableFuture<Void> delete(String id);
}

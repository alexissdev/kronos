package dev.alexissdev.kronos.spawn.repository;

import dev.alexissdev.kronos.spawn.domain.SpawnZone;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de persistencia para la zona de spawn del servidor.
 *
 * <p>A diferencia del repositorio KOTH que gestiona múltiples entidades, este repositorio
 * almacena un único documento en la colección {@code "spawn"} de MongoDB. Siempre hay
 * como máximo una zona de spawn activa en el servidor.</p>
 *
 * <p>La implementación concreta es {@code MongoSpawnRepository}. Todas las operaciones
 * son asíncronas para no bloquear el hilo principal de Bukkit.</p>
 */
public interface SpawnRepository {

    /**
     * Recupera la zona de spawn almacenada en la base de datos.
     *
     * @return future con un {@link Optional} que contiene la zona si está configurada,
     *         o vacío si nunca se ha definido una zona de spawn
     */
    CompletableFuture<Optional<SpawnZone>> findZone();

    /**
     * Guarda o reemplaza la zona de spawn en la base de datos (operación upsert).
     *
     * @param zone la zona de spawn a persistir
     * @return future que se completa cuando la zona ha sido guardada exitosamente
     */
    CompletableFuture<Void> saveZone(SpawnZone zone);

    /**
     * Elimina el documento de zona de spawn de la base de datos.
     *
     * @return future que se completa cuando el documento ha sido eliminado
     */
    CompletableFuture<Void> deleteZone();
}

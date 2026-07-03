package dev.alexissdev.kronos.koth.repository;

import dev.alexissdev.kronos.koth.domain.KothZone;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de persistencia para entidades {@link KothZone}.
 *
 * <p>Define las operaciones CRUD asíncronas que el dominio necesita para almacenar
 * y recuperar zonas KOTH. La implementación concreta ({@code MongoKothRepository})
 * se encarga de traducir entre documentos BSON y objetos de dominio, sin que la
 * capa de aplicación conozca los detalles de MongoDB.</p>
 *
 * <p>Todas las operaciones son no bloqueantes y se ejecutan en un pool de hilos
 * dedicado para evitar bloqueos en el hilo principal del servidor.</p>
 */
public interface KothRepository {

    /**
     * Busca una zona KOTH por su nombre identificador.
     *
     * @param name nombre único de la zona KOTH (equivale al {@code _id} en MongoDB)
     * @return future con un {@link Optional} que contiene la zona si existe, o vacío si no se encontró
     */
    CompletableFuture<Optional<KothZone>> findByName(String name);

    /**
     * Recupera todas las zonas KOTH almacenadas en la base de datos.
     *
     * @return future con la lista completa de zonas; puede ser vacía si no hay ninguna registrada
     */
    CompletableFuture<List<KothZone>> findAll();

    /**
     * Guarda o actualiza una zona KOTH (operación upsert). Si ya existe un documento
     * con el mismo nombre, lo reemplaza completamente.
     *
     * @param zone la zona KOTH a persistir
     * @return future con la zona persistida, permitiendo encadenar operaciones adicionales
     */
    CompletableFuture<KothZone> save(KothZone zone);

    /**
     * Elimina de forma permanente la zona KOTH con el nombre indicado.
     *
     * @param name nombre único de la zona KOTH a eliminar
     * @return future que se completa cuando el documento ha sido eliminado
     */
    CompletableFuture<Void> delete(String name);
}

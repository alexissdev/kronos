package dev.alexissdev.kronos.factions.repository;

import dev.alexissdev.kronos.factions.domain.Faction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de persistencia asíncrono para el agregado {@link Faction}.
 *
 * <p>Define el contrato de acceso a datos del módulo de facciones siguiendo el
 * patrón Repository de Domain-Driven Design. La implementación concreta
 * ({@link dev.alexissdev.kronos.factions.persistence.MongoFactionRepository})
 * almacena los datos en MongoDB, pero el dominio solo depende de esta interfaz,
 * lo que facilita el reemplazo de la capa de persistencia.
 *
 * <p>Todos los métodos devuelven {@link CompletableFuture} para garantizar que
 * las operaciones de I/O no bloqueen el hilo principal del servidor Spigot.
 */
public interface FactionRepository {

    /**
     * Busca una facción por su identificador único.
     *
     * @param id ID de la facción (representación de UUID como cadena)
     * @return futuro con un {@link Optional} que contiene la facción si fue encontrada
     */
    CompletableFuture<Optional<Faction>> findById(String id);

    /**
     * Busca una facción por nombre, ignorando mayúsculas y minúsculas.
     *
     * @param name nombre de la facción a buscar
     * @return futuro con un {@link Optional} que contiene la facción si fue encontrada
     */
    CompletableFuture<Optional<Faction>> findByName(String name);

    /**
     * Busca la facción a la que pertenece un jugador dado.
     *
     * <p>Un jugador solo puede pertenecer a una facción a la vez, por lo que
     * el resultado nunca contendrá más de una facción.
     *
     * @param playerUuid UUID del jugador del que se busca la facción
     * @return futuro con un {@link Optional} con la facción del jugador, o vacío si no está en ninguna
     */
    CompletableFuture<Optional<Faction>> findByMember(UUID playerUuid);

    /**
     * Devuelve las {@code limit} facciones con más kills, ordenadas de mayor a menor.
     *
     * <p>Utilizado para el ranking de facciones del servidor.
     *
     * @param limit número máximo de facciones a devolver
     * @return futuro con la lista de facciones ordenada por kills descendente
     */
    CompletableFuture<List<Faction>> findTopByKills(int limit);

    /**
     * Devuelve todas las facciones que actualmente están en estado raideable.
     *
     * <p>Una facción es raideable cuando su DTK ha llegado a 0, permitiendo que
     * sus claims sean sobrereclamados por facciones enemigas.
     *
     * @return futuro con la lista de facciones raideables
     */
    CompletableFuture<List<Faction>> findRaidable();

    /**
     * Persiste el estado actual de una facción (crea o actualiza mediante upsert).
     *
     * @param faction instancia de {@link Faction} con el estado a persistir
     * @return futuro con la misma instancia de facción tras ser guardada
     */
    CompletableFuture<Faction> save(Faction faction);

    /**
     * Elimina permanentemente la facción con el ID dado de la base de datos.
     *
     * <p>Se utiliza al disolver una facción voluntaria o forzosamente (por strikes).
     * Los claims y demás recursos asociados deben limpiarse mediante listeners de
     * {@link dev.alexissdev.kronos.factions.event.FactionDisbandedDomainEvent}.
     *
     * @param id ID de la facción a eliminar
     * @return futuro que se completa cuando la eliminación ha finalizado
     */
    CompletableFuture<Void> delete(String id);
}

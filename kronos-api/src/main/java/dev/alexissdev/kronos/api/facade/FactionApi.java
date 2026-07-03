package dev.alexissdev.kronos.api.facade;

import dev.alexissdev.kronos.api.model.FactionSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only facade for querying the faction system of the HCF plugin.
 * <p>
 * Exposes lookup and relationship-verification operations for factions without allowing
 * any mutations to system state. Designed to be consumed by external plugins through
 * Bukkit's {@link org.bukkit.plugin.ServicesManager}.
 * </p>
 * <p>
 * All operations are synchronous from the caller's perspective and internally resolve
 * the {@link java.util.concurrent.CompletableFuture} instances returned by the faction
 * service. Avoid calling these methods from Bukkit's async threads to prevent unexpected
 * blocking on the main server thread.
 * </p>
 */
public interface FactionApi {

    /**
     * Busca la facción a la que pertenece el jugador indicado.
     * <p>
     * Un jugador puede pertenecer a lo sumo a una facción en cualquier momento.
     * Si el jugador no está en ninguna facción, el resultado estará vacío.
     * </p>
     *
     * @param playerUuid UUID del jugador cuya facción se desea conocer
     * @return {@link Optional} con el {@link FactionSnapshot} de la facción del jugador,
     *         o vacío si el jugador no pertenece a ninguna facción
     */
    Optional<FactionSnapshot> getByPlayer(UUID playerUuid);

    /**
     * Busca una facción por su identificador único interno.
     * <p>
     * El ID de facción es generado internamente por el sistema al momento de la creación
     * y no cambia durante el ciclo de vida de la facción.
     * </p>
     *
     * @param factionId identificador único interno de la facción
     * @return {@link Optional} con el {@link FactionSnapshot} si la facción existe,
     *         o vacío si no se encontró ninguna facción con ese ID
     */
    Optional<FactionSnapshot> getById(String factionId);

    /**
     * Busca una facción por su nombre público tal como aparece en el servidor.
     * <p>
     * La búsqueda distingue mayúsculas y minúsculas. Si el nombre fue cambiado
     * recientemente, los resultados reflejan el nombre actual registrado en el sistema.
     * </p>
     *
     * @param name nombre de la facción a buscar (sensible a mayúsculas)
     * @return {@link Optional} con el {@link FactionSnapshot} si existe una facción con ese nombre,
     *         o vacío si no se encontró ninguna coincidencia
     */
    Optional<FactionSnapshot> getByName(String name);

    /**
     * Retorna una lista de las facciones con mayor puntaje ordenadas de mayor a menor.
     * <p>
     * El criterio de ordenación depende de la implementación del servicio subyacente,
     * pero generalmente considera kills, deaths y saldo de la facción.
     * Esta operación es útil para construir tablas de clasificación (leaderboards).
     * </p>
     *
     * @param limit número máximo de facciones a retornar; debe ser un valor positivo
     * @return lista inmutable de {@link FactionSnapshot} ordenada por puntaje descendente,
     *         con un máximo de {@code limit} elementos; puede estar vacía si no hay facciones
     */
    List<FactionSnapshot> getTopFactions(int limit);

    /**
     * Verifica si el jugador indicado pertenece actualmente a alguna facción.
     * <p>
     * Equivale a comprobar si {@link #getByPlayer(UUID)} retorna un valor presente,
     * pero es más eficiente para usos donde solo se necesita el resultado booleano.
     * </p>
     *
     * @param playerUuid UUID del jugador a verificar
     * @return {@code true} si el jugador pertenece a una facción; {@code false} en caso contrario
     */
    boolean isInFaction(UUID playerUuid);

    /**
     * Verifica si dos facciones tienen una relación de alianza activa entre sí.
     * <p>
     * La alianza es bidireccional: si A es aliado de B, entonces B también es aliado de A.
     * Los aliados no pueden infligirse daño entre sí en zonas donde el PvP está habilitado.
     * </p>
     *
     * @param factionIdA identificador único de la primera facción
     * @param factionIdB identificador único de la segunda facción
     * @return {@code true} si ambas facciones son aliadas; {@code false} en caso contrario
     *         o si alguna de ellas no existe
     */
    boolean areAllies(String factionIdA, String factionIdB);

    /**
     * Verifica si dos facciones tienen una relación de enemistad declarada entre sí.
     * <p>
     * La enemistad puede ser unilateral: una facción puede declarar enemiga a otra
     * sin que la segunda lo haya hecho recíprocamente.
     * Los enemigos reciben indicadores visuales especiales en el HUD del juego.
     * </p>
     *
     * @param factionIdA identificador único de la primera facción
     * @param factionIdB identificador único de la segunda facción
     * @return {@code true} si la facción A tiene declarada como enemiga a la facción B;
     *         {@code false} en caso contrario o si alguna de ellas no existe
     */
    boolean areEnemies(String factionIdA, String factionIdB);
}

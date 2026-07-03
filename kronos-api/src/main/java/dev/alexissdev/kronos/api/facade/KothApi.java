package dev.alexissdev.kronos.api.facade;

import dev.alexissdev.kronos.api.model.KothSnapshot;

import java.util.List;
import java.util.Optional;

/**
 * Read-only facade for querying the state of KOTH (King of The Hill) events in the HCF plugin.
 * <p>
 * KOTH is a PvP event where players compete to remain inside a bounded zone for a required
 * period of time. The first player to complete the required time without being eliminated
 * captures the KOTH and receives a reward (typically a loot crate of type {@code CrateType}).
 * </p>
 * <p>
 * This facade allows external plugins to check which KOTH zones are currently active in
 * real time and retrieve their geographic and status details.
 * </p>
 */
public interface KothApi {

    /**
     * Retorna una lista de todas las zonas KOTH que están actualmente activas en el servidor.
     * <p>
     * Una zona KOTH se considera activa cuando su período de captura ha comenzado y aún
     * no ha sido capturada ni cancelada por un administrador. La lista puede estar vacía
     * si no hay ningún KOTH en curso.
     * </p>
     *
     * @return lista de {@link KothSnapshot} de las zonas KOTH actualmente activas;
     *         lista vacía si ningún KOTH está en curso
     */
    List<KothSnapshot> getActiveKoths();

    /**
     * Busca una zona KOTH por su nombre identificador.
     * <p>
     * El nombre del KOTH es único y se define en la configuración del servidor.
     * Se puede usar para verificar los detalles geográficos de la zona (coordenadas,
     * mundo) y su estado de actividad actual.
     * </p>
     *
     * @param name nombre identificador de la zona KOTH a buscar (sensible a mayúsculas)
     * @return {@link Optional} con el {@link KothSnapshot} si existe una zona con ese nombre,
     *         o vacío si no se encontró ninguna zona KOTH con ese identificador
     */
    Optional<KothSnapshot> getKoth(String name);

    /**
     * Verifica si la zona KOTH con el nombre indicado está actualmente activa.
     * <p>
     * Equivale a comprobar {@code getKoth(name).map(KothSnapshot::isActive).orElse(false)},
     * pero ofrece una forma más directa para usos donde solo se necesita el estado booleano.
     * </p>
     *
     * @param name nombre identificador de la zona KOTH a verificar
     * @return {@code true} si el KOTH existe y está activo; {@code false} si no existe
     *         o si la zona no está en curso en este momento
     */
    boolean isKothActive(String name);
}

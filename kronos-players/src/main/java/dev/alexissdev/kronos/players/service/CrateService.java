package dev.alexissdev.kronos.players.service;

import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.players.domain.CrateLocation;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Interfaz de servicio de dominio para la gestión de ubicaciones de crates en el servidor.
 *
 * <p>Proporciona las operaciones necesarias para colocar, consultar y eliminar cofres de
 * recompensas en coordenadas específicas del mapa. Los crates pueden ser de diferentes
 * tipos (KOTH, VOTE, RANK, EVENT), cada uno con su propia tabla de recompensas.</p>
 *
 * <p>La implementación predeterminada es {@code CrateApplicationService}, que delega
 * la persistencia en {@code CrateLocationRepository}.</p>
 */
public interface CrateService {

    /**
     * Registra o actualiza un crate en las coordenadas especificadas del mundo.
     * Si ya existe un crate en esa posición, se actualiza su tipo; si no existe,
     * se crea uno nuevo con un ID aleatorio.
     *
     * @param world nombre del mundo de Minecraft donde se coloca el crate
     * @param x     coordenada X del bloque donde se coloca el crate
     * @param y     coordenada Y del bloque donde se coloca el crate
     * @param z     coordenada Z del bloque donde se coloca el crate
     * @param type  tipo de crate que determina las recompensas disponibles al abrirlo
     * @return future que se resuelve con la entidad {@link CrateLocation} guardada
     */
    CompletableFuture<CrateLocation> setCrate(String world, int x, int y, int z, CrateType type);

    /**
     * Elimina el crate ubicado en las coordenadas especificadas del mundo.
     *
     * @param world nombre del mundo de Minecraft donde se elimina el crate
     * @param x     coordenada X del bloque donde está el crate
     * @param y     coordenada Y del bloque donde está el crate
     * @param z     coordenada Z del bloque donde está el crate
     * @return future que se resuelve cuando el crate ha sido eliminado de la base de datos
     * @throws dev.alexissdev.kronos.common.exception.HCFException si no existe ningún crate en las coordenadas indicadas
     */
    CompletableFuture<Void> removeCrate(String world, int x, int y, int z);

    /**
     * Busca si existe un crate registrado en las coordenadas exactas indicadas.
     * Se usa para detectar si un jugador interactúa con un bloque que es un crate.
     *
     * @param world nombre del mundo de Minecraft
     * @param x     coordenada X del bloque
     * @param y     coordenada Y del bloque
     * @param z     coordenada Z del bloque
     * @return future que se resuelve con un {@link Optional} que contiene el crate
     *         si existe en esas coordenadas, o vacío si el bloque no es un crate
     */
    CompletableFuture<Optional<CrateLocation>> getCrateAt(String world, int x, int y, int z);

    /**
     * Obtiene la lista completa de todos los crates registrados en el servidor.
     * Útil para mostrar un listado administrativo o para cargar crates al iniciar el servidor.
     *
     * @return future que se resuelve con todas las ubicaciones de crates registradas;
     *         la lista estará vacía si no hay ninguno registrado
     */
    CompletableFuture<List<CrateLocation>> getAllCrates();
}

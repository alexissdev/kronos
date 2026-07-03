package dev.alexissdev.kronos.players.service;

import dev.alexissdev.kronos.players.domain.HCFPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interfaz de servicio de dominio para la gestión integral del perfil de jugadores HCF.
 *
 * <p>Define las operaciones de alto nivel que orquestan la lógica de negocio relacionada
 * con los jugadores: ciclo de vida del perfil, estadísticas de combate, sistema de vidas
 * y Deathban. La implementación principal es {@code PlayerApplicationService}.</p>
 *
 * <p>Todas las operaciones son asíncronas y devuelven {@link java.util.concurrent.CompletableFuture}
 * para evitar bloquear el hilo principal del servidor Bukkit.</p>
 */
public interface PlayerService {

    /**
     * Obtiene el perfil HCF de un jugador o lo crea si es la primera vez que se conecta.
     *
     * <p>Si el jugador ya existe, actualiza su nombre si cambió y restaura sus vidas
     * si estaba en cero (Deathban expirado). Si no existe, crea un perfil nuevo con
     * los valores predeterminados del servidor.</p>
     *
     * @param uuid UUID único del jugador que se conecta
     * @param name nombre de usuario actual del jugador en el momento de la conexión
     * @return future que se resuelve con el perfil {@link HCFPlayer} actualizado o recién creado
     */
    CompletableFuture<HCFPlayer> getOrCreate(UUID uuid, String name);

    /**
     * Busca el perfil HCF de un jugador por su UUID sin crearlo si no existe.
     *
     * @param uuid UUID del jugador a buscar
     * @return future que se resuelve con un {@link Optional} que contiene el perfil del
     *         jugador si está registrado, o vacío si no existe en la base de datos
     */
    CompletableFuture<Optional<HCFPlayer>> getPlayer(UUID uuid);

    /**
     * Persiste los cambios en el perfil de un jugador en la base de datos.
     * Utilizado cuando se modifica el perfil directamente (kit, inventario, etc.)
     * fuera de una operación de servicio específica.
     *
     * @param player entidad {@link HCFPlayer} con los datos actualizados a guardar
     * @return future que se resuelve cuando el guardado se ha completado
     */
    CompletableFuture<Void> savePlayer(HCFPlayer player);

    /**
     * Registra el resultado de un combate PvP: incrementa los kills del matador
     * y las muertes de la víctima, y persiste ambos perfiles.
     *
     * @param killerUuid UUID del jugador que realizó el kill
     * @param victimUuid UUID del jugador que fue asesinado
     * @return future que se resuelve cuando ambos perfiles han sido actualizados y guardados
     * @throws dev.alexissdev.kronos.players.exception.PlayerNotFoundException si alguno de los dos jugadores no está registrado
     */
    CompletableFuture<Void> recordKill(UUID killerUuid, UUID victimUuid);

    /**
     * Decrementa en uno el número de vidas del jugador y persiste el cambio.
     * Se invoca cuando un jugador muere con el timer de Deathban activo.
     *
     * @param uuid UUID del jugador al que se le descuenta la vida
     * @return future que se resuelve con el número de vidas restantes tras el decremento;
     *         devuelve {@code 0} si el jugador no existe en la base de datos
     */
    CompletableFuture<Integer> decrementLives(UUID uuid);

    /**
     * Comprueba si un jugador tiene actualmente un Deathban activo en Redis.
     *
     * @param uuid UUID del jugador a verificar
     * @return future que se resuelve con {@code true} si el jugador está actualmente
     *         baneado por Deathban, {@code false} si el baneo expiró o nunca fue aplicado
     */
    CompletableFuture<Boolean> isDeathbanned(UUID uuid);

    /**
     * Elimina el Deathban activo de un jugador antes de que expire naturalmente.
     * Permite que administradores o eventos del sistema liberen a un jugador del baneo.
     *
     * @param uuid UUID del jugador al que se le revoca el Deathban
     * @return future que se resuelve cuando el Deathban ha sido eliminado
     */
    CompletableFuture<Void> removeDeathban(UUID uuid);
}

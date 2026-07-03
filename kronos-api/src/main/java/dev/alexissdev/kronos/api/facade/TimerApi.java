package dev.alexissdev.kronos.api.facade;

import dev.alexissdev.kronos.api.model.TimerSnapshot;
import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Read-only facade for querying the state of active player timers in the HCF plugin.
 * <p>
 * The Kronos timer system manages the following main timer types:
 * </p>
 * <ul>
 *   <li><b>Combat Tag</b>: activated when the player takes damage from another player;
 *       disconnecting while active results in an automatic death.</li>
 *   <li><b>PvP Timer</b>: temporary protection for new or recently respawned players;
 *       while active, the player cannot deal or receive PvP damage.</li>
 *   <li><b>Enderpearl</b>: cooldown after using an enderpearl; prevents consecutive use
 *       of this item to limit high-mobility combat.</li>
 * </ul>
 * <p>
 * This interface allows external plugins to check the status of these timers without
 * depending on Kronos' internal domain entities.
 * </p>
 */
public interface TimerApi {

    /**
     * Verifica si el jugador indicado tiene un combat-tag activo en este momento.
     * <p>
     * El combat-tag se activa cuando el jugador recibe daño de otro jugador y expira
     * pasado el tiempo configurado sin recibir daño adicional. Mientras está activo,
     * desconectarse del servidor produce la muerte del jugador.
     * </p>
     *
     * @param uuid UUID del jugador a verificar
     * @return {@code true} si el jugador tiene un combat-tag activo; {@code false} en caso contrario
     *         o si el jugador no existe en el sistema
     */
    boolean hasCombatTag(UUID uuid);

    /**
     * Verifica si el jugador indicado tiene un PvP timer de protección activo.
     * <p>
     * El PvP timer protege a jugadores nuevos o recién respawneados de ataques de otros
     * jugadores. Mientras está activo, el jugador no puede infligir ni recibir daño de PvP.
     * El timer se cancela automáticamente si el jugador intenta atacar a otro jugador.
     * </p>
     *
     * @param uuid UUID del jugador a verificar
     * @return {@code true} si el jugador tiene un PvP timer activo; {@code false} en caso contrario
     *         o si el jugador no existe en el sistema
     */
    boolean hasPvpTimer(UUID uuid);

    /**
     * Verifica si el jugador indicado tiene un cooldown de enderpearl activo.
     * <p>
     * El cooldown de enderpearl impide el uso consecutivo de este ítem durante los
     * primeros segundos tras su uso, evitando abusos de movilidad en situaciones de combate.
     * </p>
     *
     * @param uuid UUID del jugador a verificar
     * @return {@code true} si el jugador tiene un cooldown de enderpearl activo;
     *         {@code false} en caso contrario o si el jugador no existe en el sistema
     */
    boolean hasEnderpearlCooldown(UUID uuid);

    /**
     * Retorna el tiempo restante en milisegundos de un temporizador específico para el jugador indicado.
     * <p>
     * Si el jugador no tiene ese tipo de temporizador activo, el resultado estará vacío.
     * El valor retornado refleja el tiempo en el momento exacto de la consulta; en consultas
     * sucesivas, el valor disminuirá conforme avanza el tiempo.
     * </p>
     *
     * @param uuid UUID del jugador cuyo temporizador se desea consultar
     * @param type tipo de temporizador a consultar (por ejemplo, {@code TimerType.COMBAT_TAG})
     * @return {@link OptionalLong} con los milisegundos restantes si el temporizador está activo,
     *         o vacío si el jugador no tiene ese temporizador en curso
     */
    OptionalLong getRemainingMillis(UUID uuid, TimerType type);

    /**
     * Retorna una instantánea completa del temporizador activo de un tipo específico para el jugador.
     * <p>
     * La instantánea incluye el instante de expiración y los milisegundos restantes calculados
     * al momento de la consulta. Si el jugador no tiene ese tipo de temporizador activo,
     * el resultado estará vacío.
     * </p>
     *
     * @param uuid UUID del jugador cuyo temporizador se desea obtener
     * @param type tipo de temporizador a consultar
     * @return {@link Optional} con el {@link TimerSnapshot} del temporizador activo,
     *         o vacío si el jugador no tiene ese tipo de temporizador en curso
     */
    Optional<TimerSnapshot> getTimer(UUID uuid, TimerType type);
}

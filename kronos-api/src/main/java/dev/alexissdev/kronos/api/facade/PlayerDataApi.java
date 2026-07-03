package dev.alexissdev.kronos.api.facade;

import dev.alexissdev.kronos.api.model.PlayerSnapshot;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only facade for querying player statistics and data in the HCF plugin.
 * <p>
 * Exposes information such as kills, deaths, economic balance, and connection status
 * of players registered in the HCF system. All data originates from the internal player
 * service and the economy service, combined into a unified view.
 * </p>
 * <p>
 * This interface is intended for external plugins that need access to player statistics
 * without coupling their logic to Kronos' internal domain entities.
 * </p>
 */
public interface PlayerDataApi {

    /**
     * Retorna una instantánea completa de los datos del jugador indicado.
     * <p>
     * La instantánea incluye nombre, kills, deaths, balance económico y estado de conexión
     * en el momento de la consulta. Si el jugador nunca se ha conectado al servidor HCF,
     * el resultado estará vacío.
     * </p>
     *
     * @param uuid UUID del jugador cuyos datos se desean consultar
     * @return {@link Optional} con el {@link PlayerSnapshot} si el jugador existe en el sistema,
     *         o vacío si no hay datos registrados para ese UUID
     */
    Optional<PlayerSnapshot> getPlayer(UUID uuid);

    /**
     * Retorna el número total de kills (eliminaciones a otros jugadores) acumulados
     * por el jugador durante su historial en el servidor HCF.
     *
     * @param uuid UUID del jugador cuyas kills se desean conocer
     * @return número de kills del jugador; retorna {@code 0} si el jugador no existe
     *         en el sistema o no tiene kills registradas
     */
    int getKills(UUID uuid);

    /**
     * Retorna el número total de deaths (muertes a manos de otros jugadores) acumuladas
     * por el jugador durante su historial en el servidor HCF.
     * <p>
     * Las deaths son relevantes en el contexto del sistema DTK (Deaths To Kick):
     * cuando los miembros de una facción acumulan suficientes muertes, la facción
     * pierde su protección y puede ser raideable.
     * </p>
     *
     * @param uuid UUID del jugador cuyas deaths se desean conocer
     * @return número de deaths del jugador; retorna {@code 0} si el jugador no existe
     *         en el sistema o no tiene muertes registradas
     */
    int getDeaths(UUID uuid);

    /**
     * Retorna el saldo económico actual del jugador en la moneda del servidor HCF.
     * <p>
     * El balance se consulta en tiempo real desde el servicio de economía interno.
     * Puede ser utilizado por plugins externos para verificar requisitos de pago
     * o mostrar el saldo en interfaces de usuario.
     * </p>
     *
     * @param uuid UUID del jugador cuyo balance se desea conocer
     * @return saldo económico del jugador; retorna {@code 0.0} si el jugador no existe
     *         en el sistema o aún no tiene un saldo registrado
     */
    double getBalance(UUID uuid);

    /**
     * Verifica si el jugador indicado está conectado actualmente al servidor.
     * <p>
     * La verificación se realiza consultando {@link org.bukkit.Bukkit#getPlayer(java.util.UUID)},
     * por lo que refleja el estado de conexión en el momento exacto de la llamada.
     * </p>
     *
     * @param uuid UUID del jugador cuyo estado de conexión se desea verificar
     * @return {@code true} si el jugador está actualmente en línea; {@code false} si está
     *         desconectado o si el UUID no corresponde a ningún jugador conocido
     */
    boolean isOnline(UUID uuid);
}

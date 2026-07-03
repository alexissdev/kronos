package dev.alexissdev.kronos.api;

import dev.alexissdev.kronos.api.facade.*;

/**
 * Main entry point for the public API of the HCF (Hardcore Factions) Kronos plugin.
 * <p>
 * This interface is registered in Bukkit's {@link org.bukkit.plugin.ServicesManager}
 * and can be consumed by any external plugin that depends on Kronos.
 * Each method returns a specialized facade exposing read-only queries
 * over the different functional areas of the HCF system.
 * </p>
 * <p>
 * Example of how to obtain the API from an external plugin:
 * <pre>{@code
 * HCFApi api = Bukkit.getServicesManager()
 *         .getRegistration(HCFApi.class).getProvider();
 * }</pre>
 * </p>
 */
public interface HCFApi {

    /**
     * Retorna la fachada de consultas sobre facciones.
     * <p>
     * Permite obtener información de facciones por jugador, ID o nombre,
     * así como verificar relaciones de alianza y enemistad entre ellas.
     * </p>
     *
     * @return instancia de {@link FactionApi} para consultas de solo lectura sobre facciones
     */
    FactionApi factions();

    /**
     * Retorna la fachada de consultas sobre datos de jugadores.
     * <p>
     * Expone estadísticas como kills, deaths, balance económico
     * y estado de conexión de los jugadores registrados en el sistema HCF.
     * </p>
     *
     * @return instancia de {@link PlayerDataApi} para consultas de estadísticas de jugadores
     */
    PlayerDataApi players();

    /**
     * Retorna la fachada de consultas sobre temporizadores activos.
     * <p>
     * Permite verificar si un jugador tiene combat-tag activo, PvP timer
     * en curso o cooldown de enderpearl, y consultar el tiempo restante de cada uno.
     * </p>
     *
     * @return instancia de {@link TimerApi} para consultar el estado de los temporizadores
     */
    TimerApi timers();

    /**
     * Retorna la fachada de consultas sobre territorios reclamados.
     * <p>
     * Permite determinar el tipo de zona de una ubicación (Wilderness, SafeZone,
     * WarZone o territorio de facción) y verificar si un chunk está reclamado.
     * </p>
     *
     * @return instancia de {@link ClaimApi} para consultas sobre chunks y zonas del mapa
     */
    ClaimApi claims();

    /**
     * Retorna la fachada de consultas sobre eventos KOTH (King of The Hill).
     * <p>
     * Permite listar las zonas KOTH actualmente activas y consultar
     * el estado de una zona específica por nombre.
     * </p>
     *
     * @return instancia de {@link KothApi} para consultas sobre el estado de los KOTH
     */
    KothApi koth();

    /**
     * Retorna la versión actual de la implementación de la API.
     * <p>
     * El formato sigue el estándar de versionado semántico (SemVer).
     * Los plugins externos pueden usar este valor para verificar compatibilidad.
     * </p>
     *
     * @return cadena con la versión semántica, por ejemplo {@code "1.0.0-SNAPSHOT"}
     */
    String getVersion();
}

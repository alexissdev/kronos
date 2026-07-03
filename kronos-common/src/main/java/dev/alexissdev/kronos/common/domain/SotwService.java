package dev.alexissdev.kronos.common.domain;

/**
 * Contrato del servicio que gestiona los períodos especiales SOTW y EOTW del servidor HCF Kronos.
 *
 * <p><strong>SOTW (Start of the World)</strong>: período inicial del servidor (típicamente las
 * primeras horas o días de un mapa nuevo) en el que el PvP entre jugadores está desactivado.
 * Su objetivo es dar tiempo a los jugadores para establecerse, construir bases y prepararse
 * antes de la fase de combate activo.</p>
 *
 * <p><strong>EOTW (End of the World)</strong>: período final del mapa en el que todos los
 * jugadores pueden atacarse sin restricciones de facción, marcando el cierre del mapa actual
 * antes de reiniciar el servidor con un mapa nuevo.</p>
 *
 * <p>Las implementaciones de esta interfaz son responsables de controlar los temporizadores,
 * notificar a los jugadores del inicio/fin de cada período y aplicar las reglas de juego
 * correspondientes (p. ej. activar/desactivar el PvP).</p>
 */
public interface SotwService {

    /**
     * Inicia el período SOTW con una duración determinada.
     * Durante este período el PvP entre jugadores está desactivado para permitir
     * que los nuevos participantes se establezcan en el mapa.
     *
     * @param durationMs duración del período SOTW en milisegundos
     */
    void startSotw(long durationMs);

    /**
     * Finaliza el período SOTW antes de que expire su tiempo natural.
     * Al terminar el SOTW, el PvP queda habilitado y el servidor entra en la fase normal de juego.
     */
    void stopSotw();

    /**
     * Indica si el período SOTW está actualmente activo en el servidor.
     *
     * @return {@code true} si el SOTW está en curso; {@code false} en caso contrario
     */
    boolean isSotwActive();

    /**
     * Devuelve el tiempo restante del período SOTW actual.
     *
     * @return milisegundos restantes del SOTW, o {@code 0} si no está activo
     */
    long getSotwRemainingMs();

    /**
     * Inicia el período EOTW con una duración determinada.
     * Durante este período se eliminan todas las restricciones de PvP entre facciones,
     * marcando el cierre del mapa actual y la batalla final entre todos los jugadores.
     *
     * @param durationMs duración del período EOTW en milisegundos
     */
    void startEotw(long durationMs);

    /**
     * Finaliza el período EOTW antes de que expire su tiempo natural.
     * Puede usarse para extender el mapa o corregir situaciones de emergencia.
     */
    void stopEotw();

    /**
     * Indica si el período EOTW está actualmente activo en el servidor.
     *
     * @return {@code true} si el EOTW está en curso; {@code false} en caso contrario
     */
    boolean isEotwActive();

    /**
     * Devuelve el tiempo restante del período EOTW actual.
     *
     * @return milisegundos restantes del EOTW, o {@code 0} si no está activo
     */
    long getEotwRemainingMs();
}

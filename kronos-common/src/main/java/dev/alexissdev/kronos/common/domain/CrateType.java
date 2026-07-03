package dev.alexissdev.kronos.common.domain;

/**
 * Enumera los tipos de cofres de recompensa (crates) disponibles en el servidor HCF Kronos.
 *
 * <p>Los crates son cofres especiales que los jugadores pueden abrir para obtener recompensas
 * aleatorias como objetos, moneda virtual o rangos. Cada tipo tiene asociado un conjunto
 * distinto de recompensas y métodos de obtención dentro del juego:</p>
 * <ul>
 *   <li>{@link #KOTH} — obtenido al ganar el evento King of the Hill.</li>
 *   <li>{@link #VOTE} — otorgado como recompensa por votar por el servidor en sitios de listado.</li>
 *   <li>{@link #RANK} — crate de mayor valor asociado a rangos de donación o logros especiales.</li>
 *   <li>{@link #EVENT} — entregado como premio en eventos temporales organizados por el staff.</li>
 * </ul>
 */
public enum CrateType {
    /** Crate obtenido al capturar y defender el punto de King of the Hill (KOTH). */
    KOTH,
    /** Crate otorgado a jugadores que votan por el servidor en plataformas de listado. */
    VOTE,
    /** Crate de alta calidad vinculado a rangos de donación o logros de alto valor. */
    RANK,
    /** Crate especial entregado como premio en eventos temporales organizados por administradores. */
    EVENT
}

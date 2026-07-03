package dev.alexissdev.kronos.claims.domain;

/**
 * Enumeración de los tipos de territorio (claim) disponibles en el sistema HCF.
 *
 * <p>Cada valor representa una categoría de zona con reglas de combate y construcción
 * distintas. Los tipos de sistema ({@link #isSystemClaim()}) son administrados por el
 * servidor y no pueden ser reclamados por facciones de jugadores.</p>
 */
public enum ClaimType {

    /** Territorio reclamado por una facción de jugadores. */
    FACTION,

    /** Zona segura donde el PvP está deshabilitado y los jugadores no pueden ser atacados. */
    SAFEZONE,

    /** Zona de guerra donde el PvP siempre está activo y las reglas de protección no aplican. */
    WARZONE,

    /** Camino administrado por el sistema que separa territorios; protegido contra construcción. */
    ROAD,

    /** Tierra sin reclamar, abierta a todos los jugadores; permite PvP y construcción libre. */
    WILDERNESS,

    /** Zona de King of the Hill: territorio de evento con PvP habilitado. */
    KOTH,

    /** Zona de Ciudadela: evento especial de alto valor con PvP habilitado. */
    CITADEL;

    /**
     * Indica si este tipo de claim es administrado por el sistema del servidor
     * y no pertenece a ninguna facción de jugadores.
     *
     * @return {@code true} si el tipo es {@link #SAFEZONE}, {@link #WARZONE},
     *         {@link #ROAD} o {@link #WILDERNESS}; {@code false} en caso contrario
     */
    public boolean isSystemClaim() {
        return this == SAFEZONE || this == WARZONE || this == ROAD || this == WILDERNESS;
    }

    /**
     * Indica si el PvP (combate entre jugadores) está permitido dentro de este tipo de zona.
     *
     * @return {@code true} si el PvP es válido en este tipo de territorio
     */
    public boolean allowsPvp() {
        return this == WARZONE || this == KOTH || this == CITADEL || this == WILDERNESS;
    }

    /**
     * Indica si los bloques dentro de este tipo de territorio están protegidos
     * contra rotura y colocación por jugadores no autorizados.
     *
     * @return {@code true} para todo tipo de zona excepto {@link #WILDERNESS}
     */
    public boolean isProtectedFromBuild() {
        return this != WILDERNESS;
    }
}

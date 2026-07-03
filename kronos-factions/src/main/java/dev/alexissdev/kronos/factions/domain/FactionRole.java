package dev.alexissdev.kronos.factions.domain;

/**
 * Jerarquía de roles dentro de una facción HCF.
 *
 * <p>Los roles definen los permisos que tiene cada miembro sobre las
 * operaciones de la facción. El orden ascendente de autoridad es:
 * {@link #MEMBER} &lt; {@link #CAPTAIN} &lt; {@link #CO_LEADER} &lt; {@link #LEADER}.
 *
 * <p>Cada rol tiene un valor numérico de prioridad que permite comparaciones
 * jerárquicas mediante {@link #isAtLeast(FactionRole)}.
 */
public enum FactionRole {

    /** Rango máximo; solo puede existir un líder por facción. */
    LEADER(4),

    /** Co-lider con amplios permisos administrativos, incluyendo retirar fondos y cambiar roles. */
    CO_LEADER(3),

    /** Capitán con permisos para invitar, expulsar miembros y establecer el hogar de la facción. */
    CAPTAIN(2),

    /** Miembro base sin permisos administrativos. */
    MEMBER(1);

    private final int priority;

    FactionRole(int priority) {
        this.priority = priority;
    }

    /**
     * Devuelve el valor numérico de prioridad del rol.
     *
     * <p>Un número mayor indica mayor autoridad dentro de la jerarquía.
     *
     * @return prioridad del rol (1-4)
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Comprueba si este rol tiene al menos la misma autoridad que el rol dado.
     *
     * <p>Se utiliza ampliamente en las comprobaciones de permisos del servicio
     * de facciones para verificar que el actor tiene el rango mínimo requerido
     * antes de ejecutar una operación privilegiada.
     *
     * @param role el rol mínimo requerido para la operación
     * @return {@code true} si la prioridad de este rol es mayor o igual a la del rol dado
     */
    public boolean isAtLeast(FactionRole role) {
        return this.priority >= role.priority;
    }
}

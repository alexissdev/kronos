package dev.alexissdev.kronos.players.domain;

/**
 * Enumeración de los tipos de kit disponibles en el servidor HCF.
 *
 * <p>Cada kit representa una clase de combate con equipamiento y habilidades
 * específicas. El kit activo del jugador se guarda en su perfil {@link HCFPlayer}
 * y se puede aplicar a su inventario a través de {@code KitService}.</p>
 *
 * <ul>
 *   <li>{@link #ARCHER} — clase de distancia con arco encantado y armadura ligera.</li>
 *   <li>{@link #BARD} — clase de soporte que usa varillas de blaze para buff de equipo.</li>
 *   <li>{@link #ROGUE} — clase sigilosa con espada de diamante y armadura de malla.</li>
 *   <li>{@link #MINER} — clase orientada a la recolección con pico de diamante Eficiencia V.</li>
 *   <li>{@link #KNIGHT} — clase de combate cuerpo a cuerpo con armadura y espada de diamante.</li>
 *   <li>{@link #DIAMOND} — kit estándar de diamante, valor por defecto al crear un jugador.</li>
 * </ul>
 */
public enum KitType {

    /**
     * Clase arquera: armadura ligera de cuero/malla y arco con Poder III y Flecha Infinita.
     * Ideal para combate a distancia y hostigamiento.
     */
    ARCHER,

    /**
     * Clase bardo: armadura mixta de oro/hierro y varilla de blaze.
     * Clase de soporte que otorga efectos positivos a sus aliados durante el combate.
     */
    BARD,

    /**
     * Clase pícaro: armadura de malla y espada de diamante con Filo III y Durabilidad III.
     * Orientada al combate rápido y al sigilo.
     */
    ROGUE,

    /**
     * Clase minero: armadura de hierro y pico de diamante con Eficiencia V y Durabilidad III.
     * Pensada para la recolección eficiente de recursos.
     */
    MINER,

    /**
     * Clase caballero: armadura completa de diamante con Protección II y espada con Filo IV.
     * Clase de tanque orientada al combate frontal sostenido.
     */
    KNIGHT,

    /**
     * Kit estándar de diamante, equivalente a {@link #KNIGHT}.
     * Es el valor por defecto asignado a los nuevos jugadores al registrarse.
     */
    DIAMOND
}

package dev.alexissdev.kronos.classes.domain;

/**
 * Enumeración de las clases (kits) disponibles para los jugadores en el sistema HCF.
 *
 * <p>Cada valor representa un rol de combate o utilidad con habilidades pasivas y activas
 * únicas. La clase activa de un jugador se determina por el tipo de casco que lleva puesto
 * y se almacena en su perfil de {@code KronosPlayer}. La clase {@link #DIAMOND} actúa como
 * clase por defecto para jugadores sin un kit específico (casco de diamante o sin casco).</p>
 *
 * <p>Las habilidades de cada clase son aplicadas por
 * {@link dev.alexissdev.kronos.classes.listener.ClassListener}.</p>
 */
public enum KitType {

    /**
     * Clase Arquero: dispara flechas a mayor velocidad y aplica lentitud al golpear en combate cuerpo a cuerpo.
     * Se activa con casco de cuero. Su habilidad activa otorga un breve aumento de daño.
     */
    ARCHER,

    /**
     * Clase Bardo: aura de soporte que otorga velocidad y regeneración periódica a todos los compañeros
     * de facción cercanos (radio de 15 bloques). Se activa con casco de oro.
     * Su habilidad activa amplifica la velocidad de los aliados en el área.
     */
    BARD,

    /**
     * Clase Pícaro: gana velocidad al golpear a un enemigo en combate cuerpo a cuerpo.
     * Se activa con casco de cota de malla. Su habilidad activa lo vuelve invisible brevemente.
     */
    ROGUE,

    /**
     * Clase Minero: obtiene el efecto de prisa al romper bloques de forma pasiva.
     * Se activa con casco de hierro. Su habilidad activa incrementa la velocidad de minería.
     */
    MINER,

    /**
     * Clase Caballero: obtiene resistencia al daño al golpear a un enemigo y puede repeler
     * jugadores cercanos con su habilidad activa. Se activa con casco de diamante puro
     * (distinguido del kit por defecto por el contexto del inventario).
     */
    KNIGHT,

    /**
     * Kit por defecto para jugadores sin una clase asignada explícitamente.
     * No tiene habilidades especiales asociadas.
     */
    DIAMOND
}

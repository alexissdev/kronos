package dev.alexissdev.kronos.players.service;

import dev.alexissdev.kronos.players.domain.KitType;
import org.bukkit.inventory.PlayerInventory;

/**
 * Interfaz de servicio de dominio para la aplicación de kits de combate a jugadores.
 *
 * <p>Define la operación de asignar el equipamiento completo correspondiente a un
 * tipo de kit en el inventario de un jugador. Cada kit representa una clase de combate
 * con armadura y armas específicas según el rol del jugador en el servidor HCF.</p>
 *
 * <p>La implementación predeterminada es {@code KitApplicationService}.</p>
 */
public interface KitService {

    /**
     * Aplica el equipamiento completo del kit especificado al inventario del jugador.
     *
     * <p>Coloca la armadura en los slots correspondientes y agrega las armas o herramientas
     * propias de la clase. El contenido exacto varía según el tipo de kit:
     * ARCHER otorga arco y armadura ligera, BARD otorga varilla de blaze, ROGUE otorga
     * espada de diamante, etc.</p>
     *
     * @param inventory inventario del jugador de Bukkit donde se aplicará el kit
     * @param type      tipo de kit cuyo equipamiento se aplicará al inventario
     */
    void applyKit(PlayerInventory inventory, KitType type);
}

package dev.alexissdev.kronos.koth.creation;

import com.google.inject.Singleton;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Servicio encargado de gestionar las sesiones interactivas de creación de zonas KOTH.
 *
 * <p>Cuando un administrador ejecuta {@code /koth create}, este servicio crea una
 * {@link KothCreationSession} asociada al UUID del jugador y le entrega la varita de
 * selección (KOTH Wand). El flujo de creación consta de dos fases:</p>
 * <ol>
 *   <li><b>CLAIM</b>: el admin selecciona las dos esquinas del territorio completo.</li>
 *   <li><b>CAPTURE</b>: el admin selecciona las dos esquinas de la zona de captura interior.</li>
 * </ol>
 *
 * <p>Al completar la segunda fase, {@code KothWandListener} invoca {@link KothCreationSession#build()}
 * para construir la {@link dev.alexissdev.kronos.koth.domain.KothZone} y la persiste a través
 * del servicio de negocio. Las sesiones son thread-safe al usar {@link ConcurrentHashMap}.</p>
 */
@Singleton
public class KothCreationService {

    private static final String WAND_NAME = ChatColor.GOLD + "" + ChatColor.BOLD + "KOTH Wand";

    private final Map<UUID, KothCreationSession> sessions = new ConcurrentHashMap<>();

    /**
     * Inicia una nueva sesión de creación de KOTH para el jugador indicado.
     * Si ya existía una sesión anterior para ese UUID, se reemplaza.
     *
     * @param uuid               UUID del jugador administrador que inicia la creación
     * @param name               nombre que tendrá el nuevo KOTH
     * @param captureTimeSeconds tiempo en segundos que tomará capturar el KOTH
     */
    public void startSession(UUID uuid, String name, int captureTimeSeconds) {
        sessions.put(uuid, new KothCreationSession(name, captureTimeSeconds));
    }

    /**
     * Recupera la sesión de creación activa asociada al UUID dado.
     *
     * @param uuid UUID del jugador cuya sesión se busca
     * @return {@link Optional} con la sesión activa, o vacío si el jugador no tiene sesión en curso
     */
    public Optional<KothCreationSession> getSession(UUID uuid) {
        return Optional.ofNullable(sessions.get(uuid));
    }

    /**
     * Cancela y elimina la sesión de creación del jugador indicado.
     * Se invoca automáticamente cuando el jugador abandona el servidor o completa la creación.
     *
     * @param uuid UUID del jugador cuya sesión debe cancelarse
     */
    public void cancelSession(UUID uuid) {
        sessions.remove(uuid);
    }

    /**
     * Comprueba si el jugador indicado tiene una sesión de creación activa en curso.
     *
     * @param uuid UUID del jugador a verificar
     * @return {@code true} si existe una sesión activa para ese jugador
     */
    public boolean hasSession(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    /**
     * Crea y devuelve el ítem varita de selección KOTH (KOTH Wand).
     * La varita es una {@code BLAZE_ROD} con nombre y lore personalizados que identifican
     * su función: clic izquierdo para posición 1 y clic derecho para posición 2.
     *
     * @return el ítem varita KOTH listo para ser entregado al administrador
     */
    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(WAND_NAME);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Clic izquierdo " + ChatColor.WHITE + "→ " + ChatColor.GREEN + "Posición 1",
                ChatColor.GRAY + "Clic derecho "   + ChatColor.WHITE + "→ " + ChatColor.YELLOW + "Posición 2"
        ));
        wand.setItemMeta(meta);
        return wand;
    }

    /**
     * Verifica si el ítem dado es una varita KOTH válida comparando su tipo y nombre de display.
     *
     * @param item ítem de inventario a verificar; puede ser {@code null}
     * @return {@code true} si el ítem es una varita KOTH legítima; {@code false} en caso contrario
     */
    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && WAND_NAME.equals(meta.getDisplayName());
    }
}

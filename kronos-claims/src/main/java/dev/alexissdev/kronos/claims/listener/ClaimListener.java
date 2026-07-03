package dev.alexissdev.kronos.claims.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.claims.domain.Claim;
import dev.alexissdev.kronos.claims.domain.ClaimType;
import dev.alexissdev.kronos.claims.service.ClaimService;
import dev.alexissdev.kronos.common.domain.SotwService;
import dev.alexissdev.kronos.factions.event.FactionClaimedDomainEvent;
import dev.alexissdev.kronos.factions.event.FactionDisbandedDomainEvent;
import dev.alexissdev.kronos.factions.event.FactionRaidableDomainEvent;
import dev.alexissdev.kronos.factions.event.PlayerJoinedFactionDomainEvent;
import dev.alexissdev.kronos.factions.event.PlayerLeftFactionDomainEvent;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Listener central del subsistema de claims en el plugin HCF.
 *
 * <p>Combina un {@link Listener} de Bukkit con un suscriptor del EventBus de Guava para
 * cumplir dos responsabilidades principales:</p>
 * <ol>
 *   <li><strong>Protección de bloques:</strong> cancela eventos de rotura y colocación de
 *       bloques cuando el jugador no tiene permiso sobre el territorio.</li>
 *   <li><strong>Notificación de movimiento:</strong> informa al jugador cuando cruza el
 *       límite de un claim, mostrando el nombre del territorio o de la facción propietaria.</li>
 * </ol>
 *
 * <p>Mantiene tres cachés en memoria para evitar consultas frecuentes a MongoDB:</p>
 * <ul>
 *   <li>{@code claimCache}: mapea {@code "mundo:chunkX:chunkZ"} → {@link Claim}.</li>
 *   <li>{@code playerFactionMap}: mapea UUID del jugador → ID de su facción actual.</li>
 *   <li>{@code playerChunkCache}: mapea UUID del jugador → clave del último chunk visitado.</li>
 * </ul>
 *
 * <p>También escucha eventos de dominio del EventBus (facción reclamada, disuelta,
 * jugador que entra/sale de facción, facción en estado raidable) para mantener los
 * cachés actualizados en tiempo real sin necesidad de recargas manuales.</p>
 */
@Singleton
public class ClaimListener implements Listener {

    private final ClaimService claimService;
    private final FactionService factionService;
    private final SotwService sotwService;
    private final Plugin plugin;
    private final EventBus eventBus;

    private final Map<String, Claim>  claimCache        = new ConcurrentHashMap<>();
    private final Map<UUID, String>   playerFactionMap  = new ConcurrentHashMap<>();
    private final Map<UUID, String>   playerChunkCache  = new ConcurrentHashMap<>();
    private final Set<String>         raidableFactions  = ConcurrentHashMap.newKeySet();

    /**
     * Construye el listener inyectando sus dependencias y lo registra en el EventBus de Guava.
     *
     * @param claimService   servicio de aplicación de claims para consultas de territorio
     * @param factionService servicio de facciones para resolver nombres de facción al notificar
     * @param sotwService    servicio de estado SOTW/EOTW; durante EOTW se permite construir en todos los claims
     * @param plugin         instancia del plugin principal, necesaria para programar tareas en el hilo principal
     * @param eventBus       bus de eventos de Guava donde se registran los suscriptores {@link Subscribe}
     */
    @Inject
    public ClaimListener(ClaimService claimService, FactionService factionService,
                         SotwService sotwService, Plugin plugin, EventBus eventBus) {
        this.claimService = claimService;
        this.factionService = factionService;
        this.sotwService = sotwService;
        this.plugin = plugin;
        this.eventBus = eventBus;
        this.eventBus.register(this);
    }

    /**
     * Cancela la rotura de bloques en territorios protegidos cuando el jugador no tiene autorización.
     *
     * <p>Los operadores y jugadores con el permiso {@code hcf.bypass.claimprotection} pueden
     * romper bloques en cualquier territorio. En claims de tipo {@link ClaimType#FACTION}, solo
     * los miembros de la facción propietaria o las facciones que la estén raideando pueden modificar
     * el territorio, a menos que EOTW esté activo.</p>
     *
     * @param event evento de rotura de bloque de Bukkit
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("hcf.bypass.claimprotection")) return;

        Claim claim = claimCache.get(chunkKey(
                event.getBlock().getWorld().getName(),
                event.getBlock().getChunk().getX(),
                event.getBlock().getChunk().getZ()));

        if (canModify(player, claim)) return;

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "No puedes destruir bloques aquí.");
    }

    /**
     * Cancela la colocación de bloques en territorios protegidos cuando el jugador no tiene autorización.
     *
     * <p>Aplica las mismas reglas que {@link #onBlockBreak(BlockBreakEvent)}: los operadores
     * y jugadores con permiso de bypass pueden colocar bloques en cualquier zona.</p>
     *
     * @param event evento de colocación de bloque de Bukkit
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.hasPermission("hcf.bypass.claimprotection")) return;

        Claim claim = claimCache.get(chunkKey(
                event.getBlock().getWorld().getName(),
                event.getBlock().getChunk().getX(),
                event.getBlock().getChunk().getZ()));

        if (canModify(player, claim)) return;

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "No puedes colocar bloques aquí.");
    }

    /**
     * Marca una facción como raidable en el caché local al recibir el evento de dominio correspondiente.
     *
     * <p>Una vez en estado raidable, cualquier jugador (incluso enemigos) puede romper y colocar
     * bloques en el territorio de esa facción, simulando un saqueo durante el raid.</p>
     *
     * @param event evento de dominio que indica que la facción ha entrado en estado raidable
     */
    @Subscribe
    public void onFactionRaidable(FactionRaidableDomainEvent event) {
        raidableFactions.add(event.getFactionId());
    }

    private boolean canModify(Player player, Claim claim) {
        if (claim == null) return true;
        if (!claim.getType().isProtectedFromBuild()) return true;
        if (sotwService.isEotwActive()) return true;
        if (claim.getType() != ClaimType.FACTION) return false;
        if (raidableFactions.contains(claim.getFactionId())) return true;
        String playerFaction = playerFactionMap.get(player.getUniqueId());
        return claim.getFactionId().equals(playerFaction);
    }

    /**
     * Detecta cuando un jugador cruza el límite entre chunks y le notifica el territorio al que ingresó.
     *
     * <p>Para minimizar el impacto en el rendimiento, solo procesa el evento si el jugador
     * ha cruzado realmente la frontera de un chunk (cambio en X o Z del chunk). Cuando el
     * jugador entra en un territorio reclamado llama a {@link #notifyClaimEntry(Player, Claim)};
     * cuando sale de uno hacia tierra libre muestra el mensaje correspondiente.</p>
     *
     * @param event evento de movimiento de Bukkit
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!crossedChunkBorder(event)) return;

        Player player = event.getPlayer();
        String newKey = chunkKey(event.getTo().getWorld().getName(),
                event.getTo().getChunk().getX(),
                event.getTo().getChunk().getZ());

        String previousKey = playerChunkCache.put(player.getUniqueId(), newKey);
        if (Objects.equals(previousKey, newKey)) return;

        Claim newClaim = claimCache.get(newKey);
        if (newClaim != null) {
            notifyClaimEntry(player, newClaim);
        } else if (previousKey != null && claimCache.containsKey(previousKey)) {
            player.sendMessage(ChatColor.YELLOW + "Entraste a " + ChatColor.WHITE + "Tierra de Nadie");
        }
    }

    /**
     * Envía al jugador el mensaje de bienvenida al entrar en un territorio.
     *
     * <p>Si el claim es de tipo {@link ClaimType#FACTION}, resuelve de forma asíncrona
     * el nombre de la facción y lo muestra en el hilo principal del servidor.
     * Para cualquier otro tipo de claim (KOTH, WARZONE, SAFEZONE, etc.) muestra
     * directamente el nombre del tipo.</p>
     *
     * @param player jugador que ha entrado en el territorio
     * @param claim  claim al que ha ingresado el jugador
     */
    private void notifyClaimEntry(Player player, Claim claim) {
        if (claim.getType() == ClaimType.FACTION) {
            factionService.getById(claim.getFactionId()).thenAccept(opt ->
                    opt.ifPresent(f -> plugin.getServer().getScheduler().runTask(plugin, () ->
                            player.sendMessage(ChatColor.YELLOW + "Entraste al territorio de " +
                                    ChatColor.WHITE + f.getName()))));
        } else {
            player.sendMessage(ChatColor.YELLOW + "Entraste a " + ChatColor.WHITE + claim.getType().name());
        }
    }

    /**
     * Carga el ID de facción del jugador en el caché local cuando se conecta al servidor.
     *
     * <p>Permite que las comprobaciones de permiso en los eventos de bloque sean instantáneas
     * sin necesidad de consultar la base de datos en cada acción.</p>
     *
     * @param event evento de conexión del jugador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        factionService.getByPlayer(event.getPlayer().getUniqueId())
                .thenAccept(opt -> opt.ifPresent(f ->
                        playerFactionMap.put(event.getPlayer().getUniqueId(), f.getId())));
    }

    /**
     * Elimina los datos del jugador de ambos cachés cuando se desconecta del servidor.
     *
     * <p>Evita fugas de memoria al limpiar las entradas del mapa de facciones y del
     * mapa de chunks visitados para el jugador que abandonó la sesión.</p>
     *
     * @param event evento de desconexión del jugador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        playerFactionMap.remove(event.getPlayer().getUniqueId());
        playerChunkCache.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Actualiza el caché de claims en memoria cuando se registra un nuevo territorio.
     *
     * <p>Al recibir el evento de dominio {@link FactionClaimedDomainEvent}, reconstruye
     * la entidad {@link Claim} y la inserta en el {@code claimCache} para todos los chunks
     * que abarca el nuevo territorio. Esto garantiza que las comprobaciones de permisos
     * y las notificaciones de movimiento funcionen sin acceder a MongoDB.</p>
     *
     * @param event evento de dominio que describe el territorio recién reclamado
     */
    @Subscribe
    public void onClaimed(FactionClaimedDomainEvent event) {
        ClaimType type;
        try {
            type = ClaimType.valueOf(event.getClaimType());
        } catch (IllegalArgumentException e) {
            type = ClaimType.FACTION;
        }
        Claim claim = new Claim(event.getClaimId(), event.getFactionId(), type, event.getWorld(),
                event.getMinChunkX(), event.getMinChunkZ(), event.getMaxChunkX(), event.getMaxChunkZ());
        for (int x = claim.getMinChunkX(); x <= claim.getMaxChunkX(); x++) {
            for (int z = claim.getMinChunkZ(); z <= claim.getMaxChunkZ(); z++) {
                claimCache.put(chunkKey(claim.getWorld(), x, z), claim);
            }
        }
    }

    /**
     * Elimina todos los claims de una facción del caché en memoria cuando esta es disuelta.
     *
     * <p>Sin esta limpieza, los chunks del caché seguirían considerándose territorio de la
     * facción disuelta, impidiendo que otros jugadores los reclamen en tiempo real.</p>
     *
     * @param event evento de dominio que indica qué facción fue disuelta
     */
    @Subscribe
    public void onDisbanded(FactionDisbandedDomainEvent event) {
        claimCache.entrySet().removeIf(e ->
                event.getFactionId().equals(e.getValue().getFactionId()));
    }

    /**
     * Registra la facción de un jugador en el caché local cuando este se une a ella.
     *
     * <p>Garantiza que las comprobaciones de propiedad de claims sean correctas para
     * los jugadores que cambien de facción sin necesidad de reconectarse.</p>
     *
     * @param event evento de dominio con el UUID del jugador y el ID de su nueva facción
     */
    @Subscribe
    public void onPlayerJoined(PlayerJoinedFactionDomainEvent event) {
        playerFactionMap.put(event.getPlayerUuid(), event.getFactionId());
    }

    /**
     * Elimina la asociación de facción de un jugador del caché local cuando abandona su facción.
     *
     * <p>Tras esta limpieza, el jugador es tratado como sin facción en las comprobaciones
     * de permiso sobre territorios hasta que vuelva a unirse a una.</p>
     *
     * @param event evento de dominio con el UUID del jugador que abandonó la facción
     */
    @Subscribe
    public void onPlayerLeft(PlayerLeftFactionDomainEvent event) {
        playerFactionMap.remove(event.getPlayerUuid());
    }

    /**
     * Precarga todos los claims y las facciones raidables desde MongoDB al caché en memoria.
     *
     * <p>Debe invocarse una sola vez durante la inicialización del plugin, después de que
     * el injector de Guice haya construido el listener. Ejecuta las consultas de forma
     * asíncrona y registra los resultados en los cachés correspondientes. Los errores se
     * registran en el log del servidor sin interrumpir el arranque.</p>
     */
    public void preloadCache() {
        claimService.getAllClaims().thenAccept(claims -> {
            for (Claim claim : claims) {
                for (int x = claim.getMinChunkX(); x <= claim.getMaxChunkX(); x++) {
                    for (int z = claim.getMinChunkZ(); z <= claim.getMaxChunkZ(); z++) {
                        claimCache.put(chunkKey(claim.getWorld(), x, z), claim);
                    }
                }
            }
            plugin.getLogger().info("Cargados " + claims.size() + " claims en caché.");
        }).exceptionally(ex -> {
            plugin.getLogger().warning("Error al precargar claims: " + ex.getMessage());
            return null;
        });

        factionService.getRaidableFactions().thenAccept(factions ->
                factions.forEach(f -> raidableFactions.add(f.getId()))
        ).exceptionally(ex -> {
            plugin.getLogger().warning("Error al cargar facciones raidable: " + ex.getMessage());
            return null;
        });
    }

    /**
     * Determina si el jugador cruzó la frontera entre dos chunks distintos.
     *
     * <p>Se usa como filtro rápido en {@link #onMove(PlayerMoveEvent)} para descartar
     * los movimientos dentro del mismo chunk sin procesar lógica adicional.</p>
     *
     * @param event evento de movimiento a evaluar
     * @return {@code true} si el chunk de origen y el de destino son diferentes
     */
    private boolean crossedChunkBorder(PlayerMoveEvent event) {
        return event.getFrom().getChunk().getX() != event.getTo().getChunk().getX()
                || event.getFrom().getChunk().getZ() != event.getTo().getChunk().getZ();
    }

    /**
     * Genera la clave compuesta usada para indexar claims en el caché en memoria.
     *
     * <p>El formato es {@code "mundo:chunkX:chunkZ"}, lo que garantiza unicidad global
     * entre mundos con coordenadas de chunk idénticas.</p>
     *
     * @param world  nombre del mundo de Minecraft
     * @param chunkX coordenada X del chunk
     * @param chunkZ coordenada Z del chunk
     * @return clave de caché en formato {@code "mundo:X:Z"}
     */
    private String chunkKey(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + ":" + chunkZ;
    }
}

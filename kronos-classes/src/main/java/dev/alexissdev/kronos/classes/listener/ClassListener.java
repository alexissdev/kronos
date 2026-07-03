package dev.alexissdev.kronos.classes.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.factions.service.FactionService;
import dev.alexissdev.kronos.players.domain.KitType;
import dev.alexissdev.kronos.timers.domain.TimerType;
import dev.alexissdev.kronos.classes.service.KitService;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Listener de Bukkit que aplica las habilidades pasivas y activas de todas las clases HCF.
 *
 * <p>Gestiona el ciclo de vida completo de las clases durante la sesión de cada jugador:</p>
 * <ul>
 *   <li>Detecta la clase activa al conectarse y la actualiza automáticamente cuando el
 *       jugador cambia de casco (el tipo de casco determina la clase).</li>
 *   <li>Aplica efectos pasivos en combate según la clase del atacante.</li>
 *   <li>Ejecuta la habilidad activa al hacer clic derecho con una {@code BLAZE_ROD},
 *       respetando el cooldown gestionado por {@link dev.alexissdev.kronos.timers.TimerApplicationService}.</li>
 *   <li>Ejecuta el aura periódica del Bardo para otorgar efectos a los aliados cercanos.</li>
 * </ul>
 *
 * <p>Mantiene un caché en memoria ({@code playerKitCache}) para evitar consultas asíncronas
 * a la base de datos durante eventos de alta frecuencia como el combate.</p>
 *
 * <p>Registrada como singleton por Guice a través de {@link dev.alexissdev.kronos.classes.ClassesModule}.</p>
 */
@Singleton
public class ClassListener implements Listener {

    private final KitService kitService;
    private final TimerApplicationService timerService;
    private final FactionService factionService;
    private final Plugin plugin;

    /** Caché que asocia el UUID de cada jugador conectado con su clase activa actual. */
    private final Map<UUID, KitType> playerKitCache = new ConcurrentHashMap<>();

    /**
     * Construye el listener inyectando sus dependencias e inicia el aura periódica del Bardo.
     *
     * @param kitService     servicio de clases para detectar y persistir el kit activo
     * @param timerService   servicio de timers para gestionar cooldowns de habilidades activas
     * @param factionService servicio de facciones para identificar aliados en el aura del Bardo
     * @param plugin         instancia del plugin principal, necesaria para programar tareas Bukkit
     */
    @Inject
    public ClassListener(KitService kitService, TimerApplicationService timerService,
                         FactionService factionService, Plugin plugin) {
        this.kitService = kitService;
        this.timerService = timerService;
        this.factionService = factionService;
        this.plugin = plugin;
        scheduleBardAura();
    }

    /**
     * Programa la tarea periódica del aura del Bardo, que se ejecuta cada 2 segundos (40 ticks).
     *
     * <p>Para cada jugador conectado con clase {@link KitType#BARD} activa, otorga los efectos
     * de Velocidad II y Regeneración I al propio Bardo y a todos los miembros de su facción
     * dentro de un radio de 15 bloques. La tarea se ejecuta de forma asíncrona y sólo aplica
     * los efectos en el hilo principal de Bukkit.</p>
     */
    private void scheduleBardAura() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, KitType> entry : playerKitCache.entrySet()) {
                    if (entry.getValue() != KitType.BARD) continue;
                    org.bukkit.entity.Player bard = plugin.getServer().getPlayer(entry.getKey());
                    if (bard == null || !bard.isOnline()) continue;

                    List<Player> nearby = bard.getNearbyEntities(15, 15, 15).stream()
                            .filter(e -> e instanceof Player)
                            .map(e -> (Player) e)
                            .collect(Collectors.toList());

                    factionService.getByPlayer(bard.getUniqueId()).thenAccept(bardFactionOpt -> {
                        java.util.Set<UUID> teammates = new java.util.HashSet<>();
                        bardFactionOpt.ifPresent(f -> teammates.addAll(f.getMembers().keySet()));

                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false);
                            PotionEffect regen = new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false);
                            if (bard.isOnline()) {
                                bard.addPotionEffect(speed, true);
                                bard.addPotionEffect(regen, true);
                            }
                            for (Player near : nearby) {
                                if (!near.isOnline()) continue;
                                if (!teammates.isEmpty() && teammates.contains(near.getUniqueId())) {
                                    near.addPotionEffect(speed, true);
                                    near.addPotionEffect(regen, true);
                                }
                            }
                        });
                    });
                }
            }
        }.runTaskTimerAsynchronously(plugin, 40L, 40L);
    }

    /**
     * Carga la clase activa del jugador en el caché local cuando se conecta al servidor.
     *
     * <p>La carga es asíncrona para no bloquear el hilo principal. Si el jugador no tiene
     * un kit asignado en su perfil, el caché no se inicializa con ninguna entrada para él.</p>
     *
     * @param event evento de conexión del jugador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        kitService.detectKit(event.getPlayer().getUniqueId())
                .thenAccept(opt -> opt.ifPresent(kit ->
                        playerKitCache.put(event.getPlayer().getUniqueId(), kit)));
    }

    /**
     * Elimina la clase del jugador del caché local cuando se desconecta.
     *
     * <p>Previene fugas de memoria asegurándose de que no permanezcan entradas obsoletas
     * en el caché de sesión.</p>
     *
     * @param event evento de desconexión del jugador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        playerKitCache.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Detecta cambios de clase cuando el jugador interactúa con su inventario relacionado con cascos.
     *
     * <p>Escucha clics en el inventario que puedan afectar la ranura de casco (ranura de armadura
     * o cualquier movimiento de un ítem de casco). Programa la actualización de clase en el
     * siguiente tick del servidor para que el cambio de equipo ya esté reflejado en el inventario.</p>
     *
     * @param event evento de clic en inventario de Bukkit
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean helmetInvolved = event.getSlotType() == InventoryType.SlotType.ARMOR
                || (current != null && isHelmetMaterial(current.getType()))
                || (cursor != null && isHelmetMaterial(cursor.getType()));

        if (!helmetInvolved) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> updateKit(player));
    }

    /**
     * Aumenta la velocidad de las flechas disparadas por jugadores con clase {@link KitType#ARCHER}.
     *
     * <p>Multiplica el vector de velocidad de la flecha por {@code 1.3}, incrementando su alcance
     * y dificultando esquivarla. Solo afecta a flechas (no otros proyectiles de arco).</p>
     *
     * @param event evento de disparo con arco de Bukkit
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player archer = (Player) event.getEntity();

        if (playerKitCache.getOrDefault(archer.getUniqueId(), KitType.DIAMOND) != KitType.ARCHER) return;
        if (!(event.getProjectile() instanceof Arrow)) return;

        Arrow arrow = (Arrow) event.getProjectile();
        arrow.setVelocity(arrow.getVelocity().multiply(1.3));
    }

    /**
     * Aplica los efectos pasivos de combate según la clase del jugador atacante.
     *
     * <p>Solo procesa eventos en los que tanto el atacante como la víctima son jugadores.
     * Delega la aplicación concreta de efectos en {@link #applyPassiveEffect(Player, Player, KitType)}.</p>
     *
     * @param event evento de daño entre entidades de Bukkit
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        KitType kit = playerKitCache.getOrDefault(attacker.getUniqueId(), KitType.DIAMOND);
        applyPassiveEffect(attacker, victim, kit);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (playerKitCache.getOrDefault(player.getUniqueId(), KitType.DIAMOND) != KitType.MINER) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 80, 1, false, false), true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType() != Material.BLAZE_ROD) return;
        Player player = event.getPlayer();

        if (timerService.hasActiveTimerSync(player.getUniqueId(), TimerType.CLASS_COOLDOWN)) {
            player.sendMessage(ChatColor.RED + "Tu habilidad está en cooldown.");
            return;
        }

        KitType kit = playerKitCache.getOrDefault(player.getUniqueId(), KitType.DIAMOND);
        if (kit == KitType.DIAMOND) return;

        kitService.activateClassAbility(player.getUniqueId(), kit);
        activateAbility(player, kit);
    }

    private void updateKit(Player player) {
        if (!player.isOnline()) return;
        KitType detected = detectKitFromHelmet(player.getInventory().getHelmet());
        KitType previous = playerKitCache.get(player.getUniqueId());
        if (detected == previous) return;
        playerKitCache.put(player.getUniqueId(), detected);
        kitService.updateActiveKit(player.getUniqueId(), detected);
        player.sendMessage(ChatColor.GOLD + "Clase activa: " + detected.name());
    }

    private boolean isHelmetMaterial(Material material) {
        switch (material) {
            case LEATHER_HELMET:
            case GOLD_HELMET:
            case CHAINMAIL_HELMET:
            case IRON_HELMET:
            case DIAMOND_HELMET:
                return true;
            default:
                return false;
        }
    }

    private KitType detectKitFromHelmet(ItemStack helmet) {
        if (helmet == null || helmet.getType() == Material.AIR) return KitType.DIAMOND;
        switch (helmet.getType()) {
            case LEATHER_HELMET:   return KitType.ARCHER;
            case GOLD_HELMET:      return KitType.BARD;
            case CHAINMAIL_HELMET: return KitType.ROGUE;
            case IRON_HELMET:      return KitType.MINER;
            case DIAMOND_HELMET:   return KitType.KNIGHT;
            default:               return KitType.DIAMOND;
        }
    }

    private void applyPassiveEffect(Player attacker, Player victim, KitType kit) {
        switch (kit) {
            case ARCHER:
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1, false, false));
                break;
            case ROGUE:
                attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false));
                break;
            case KNIGHT:
                attacker.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, 0, false, false));
                break;
            default:
                break;
        }
    }

    private void activateAbility(Player player, KitType kit) {
        switch (kit) {
            case ARCHER:
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 100, 1, false, false));
                break;
            case BARD:
                player.getNearbyEntities(15, 15, 15).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .forEach(p -> p.addPotionEffect(
                                new PotionEffect(PotionEffectType.SPEED, 200, 1, false, false)));
                break;
            case ROGUE:
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false));
                break;
            case KNIGHT:
                player.getNearbyEntities(5, 5, 5).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .forEach(p -> p.setVelocity(
                                p.getLocation().toVector()
                                        .subtract(player.getLocation().toVector())
                                        .normalize().multiply(2)));
                break;
            case MINER:
                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 300, 1, false, false));
                break;
            default:
                break;
        }
        player.sendMessage(ChatColor.GOLD + "¡Habilidad activada!");
    }
}

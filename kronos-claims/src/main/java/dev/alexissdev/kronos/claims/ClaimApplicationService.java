package dev.alexissdev.kronos.claims;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.claims.domain.Claim;
import dev.alexissdev.kronos.claims.domain.ClaimType;
import dev.alexissdev.kronos.factions.event.FactionClaimedDomainEvent;
import dev.alexissdev.kronos.claims.exception.ClaimConflictException;
import dev.alexissdev.kronos.factions.exception.FactionNotFoundException;
import dev.alexissdev.kronos.factions.exception.FactionPermissionException;
import dev.alexissdev.kronos.claims.repository.ClaimRepository;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.domain.FactionMember;
import dev.alexissdev.kronos.factions.domain.FactionRole;
import dev.alexissdev.kronos.factions.repository.FactionRepository;
import dev.alexissdev.kronos.claims.service.ClaimService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación principal del servicio de aplicación para territorios (claims) HCF.
 *
 * <p>Orquesta todas las reglas de negocio relacionadas con la reclamación, liberación y
 * conquista de territorios en el mundo de Minecraft. Opera como punto de entrada entre
 * los comandos de los jugadores y el modelo de dominio, coordinando los repositorios de
 * claims y facciones, y publicando eventos de dominio en el {@link EventBus} de Guava
 * para que otros módulos (como el listener de claims) reaccionen de forma desacoplada.</p>
 *
 * <p>Registrada como singleton por Guice a través de {@link ClaimsModule}.</p>
 */
@Singleton
public class ClaimApplicationService implements ClaimService {

    private final ClaimRepository claimRepository;
    private final FactionRepository factionRepository;
    private final EventBus eventBus;

    /**
     * Construye el servicio inyectando sus dependencias.
     *
     * @param claimRepository   repositorio de persistencia de claims (MongoDB)
     * @param factionRepository repositorio de facciones para validar permisos de los actores
     * @param eventBus          bus de eventos de Guava para publicar eventos de dominio
     */
    @Inject
    public ClaimApplicationService(ClaimRepository claimRepository,
                                   FactionRepository factionRepository,
                                   EventBus eventBus) {
        this.claimRepository = claimRepository;
        this.factionRepository = factionRepository;
        this.eventBus = eventBus;
    }

    /**
     * {@inheritDoc}
     *
     * <p>El proceso es:
     * <ol>
     *   <li>Verifica que la facción exista y que el actor sea {@code CAPTAIN} o superior.</li>
     *   <li>Comprueba en paralelo que ningún chunk del área esté reclamado.</li>
     *   <li>Crea y persiste el {@link Claim} de tipo {@link ClaimType#FACTION}.</li>
     *   <li>Publica un {@link dev.alexissdev.kronos.factions.event.FactionClaimedDomainEvent}.</li>
     * </ol></p>
     */
    @Override
    public CompletableFuture<Claim> claim(String factionId, UUID actorUuid, String world,
                                          int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            FactionMember actor = faction.getMember(actorUuid)
                    .orElseThrow(() -> new FactionPermissionException(actorUuid));
            if (!actor.getRole().isAtLeast(FactionRole.CAPTAIN)) {
                throw new FactionPermissionException(actorUuid);
            }
            List<CompletableFuture<Optional<Claim>>> checks = new ArrayList<>();
            for (int x = minChunkX; x <= maxChunkX; x++) {
                for (int z = minChunkZ; z <= maxChunkZ; z++) {
                    checks.add(claimRepository.findByChunk(world, x, z));
                }
            }
            return CompletableFuture.allOf(checks.toArray(new CompletableFuture[0]))
                    .thenCompose(v -> {
                        boolean conflict = checks.stream().anyMatch(f -> f.join().isPresent());
                        if (conflict) throw new ClaimConflictException("Ese territorio ya está reclamado");
                        String id = UUID.randomUUID().toString();
                        Claim claim = new Claim(id, factionId, ClaimType.FACTION, world,
                                minChunkX, minChunkZ, maxChunkX, maxChunkZ);
                        return claimRepository.save(claim).thenApply(saved -> {
                            eventBus.post(new FactionClaimedDomainEvent(factionId, saved.getId(), saved.getType().name(), saved.getWorld(), saved.getMinChunkX(), saved.getMinChunkZ(), saved.getMaxChunkX(), saved.getMaxChunkZ()));
                            return saved;
                        });
                    });
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Busca el claim en el chunk indicado y lo elimina solo si la facción solicitante
     * es su propietaria. Lanza {@link dev.alexissdev.kronos.factions.exception.FactionPermissionException}
     * si el chunk pertenece a otra facción.</p>
     */
    @Override
    public CompletableFuture<Void> unclaim(String factionId, UUID actorUuid, String world,
                                           int chunkX, int chunkZ) {
        return claimRepository.findByChunk(world, chunkX, chunkZ).thenCompose(opt -> {
            Claim claim = opt.orElseThrow(() -> new ClaimConflictException("No hay ningún claim aquí"));
            if (!factionId.equals(claim.getFactionId())) {
                throw new FactionPermissionException(actorUuid);
            }
            return claimRepository.delete(claim.getId());
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delega directamente en {@link ClaimRepository#deleteByFaction(String)} para
     * eliminar todos los claims de la facción en una sola operación de base de datos.</p>
     */
    @Override
    public CompletableFuture<Void> unclaimAll(String factionId) {
        return claimRepository.deleteByFaction(factionId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Consulta directamente el repositorio sin lógica adicional; útil para obtener
     * todos los metadatos del claim (propietario, tipo, límites) en un punto concreto.</p>
     */
    @Override
    public CompletableFuture<Optional<Claim>> getClaimAt(String world, int chunkX, int chunkZ) {
        return claimRepository.findByChunk(world, chunkX, chunkZ);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Si el chunk no tiene ningún claim registrado, retorna {@link ClaimType#WILDERNESS}
     * como valor predeterminado para indicar tierra libre.</p>
     */
    @Override
    public CompletableFuture<ClaimType> getClaimTypeAt(String world, int chunkX, int chunkZ) {
        return claimRepository.findByChunk(world, chunkX, chunkZ)
                .thenApply(opt -> opt.map(Claim::getType).orElse(ClaimType.WILDERNESS));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Devuelve todos los territorios actualmente registrados de la facción, útil
     * para mostrar estadísticas o para calcular el poder necesario para mantenerlos.</p>
     */
    @Override
    public CompletableFuture<List<Claim>> getFactionClaims(String factionId) {
        return claimRepository.findByFaction(factionId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Se usa principalmente durante el arranque del servidor para precargar el caché
     * en memoria de {@link dev.alexissdev.kronos.claims.listener.ClaimListener}.</p>
     */
    @Override
    public CompletableFuture<List<Claim>> getAllClaims() {
        return claimRepository.findAll();
    }

    /**
     * {@inheritDoc}
     *
     * <p>El flujo de conquista es:
     * <ol>
     *   <li>Verifica que el actor sea {@code CAPTAIN} o superior en su facción.</li>
     *   <li>Comprueba que exista un claim enemigo en el chunk objetivo.</li>
     *   <li>Valida que la facción defensora sea enemiga del atacante o esté en estado raidable.</li>
     *   <li>Delega en {@link #doOverclaim(String, Claim)} para reemplazar el claim.</li>
     * </ol></p>
     */
    @Override
    public CompletableFuture<Void> overclaim(String factionId, UUID actorUuid, String world,
                                             int chunkX, int chunkZ) {
        return factionRepository.findById(factionId).thenCompose(attackerOpt -> {
            Faction attacker = attackerOpt.orElseThrow(() -> new FactionNotFoundException(factionId));
            FactionMember actor = attacker.getMember(actorUuid)
                    .orElseThrow(() -> new FactionPermissionException(actorUuid));
            if (!actor.getRole().isAtLeast(FactionRole.CAPTAIN)) {
                throw new FactionPermissionException(actorUuid);
            }
            return claimRepository.findByChunk(world, chunkX, chunkZ).thenCompose(opt -> {
                Claim existing = opt.orElseThrow(() ->
                        new ClaimConflictException("No hay territorio enemigo aquí"));
                if (factionId.equals(existing.getFactionId())) {
                    throw new ClaimConflictException("Este territorio ya es tuyo");
                }
                // Only allow overclaiming faction claims that are raidable OR from an enemy faction
                if (existing.getType() == ClaimType.FACTION && existing.getFactionId() != null) {
                    boolean enemy = attacker.isEnemy(existing.getFactionId());
                    if (!enemy) {
                        return factionRepository.findById(existing.getFactionId()).thenCompose(defenderOpt -> {
                            boolean raidable = defenderOpt.map(Faction::isRaidable).orElse(false);
                            if (!raidable) {
                                throw new ClaimConflictException("Esa facción no es tu enemiga ni está siendo raideada");
                            }
                            return doOverclaim(factionId, existing);
                        });
                    }
                }
                return doOverclaim(factionId, existing);
            });
        });
    }

    /**
     * Ejecuta la sustitución efectiva de un claim existente por uno nuevo de la facción atacante.
     *
     * <p>Elimina el claim del defensor, crea un nuevo {@link Claim} de tipo
     * {@link ClaimType#FACTION} con los mismos límites geográficos y lo asocia a
     * {@code factionId}. Publica un
     * {@link dev.alexissdev.kronos.factions.event.FactionClaimedDomainEvent} para
     * actualizar el caché del listener de claims.</p>
     *
     * @param factionId identificador de la facción que conquista el territorio
     * @param existing  claim existente que será reemplazado
     * @return futuro que se completa cuando el nuevo claim ha sido persistido
     */
    private CompletableFuture<Void> doOverclaim(String factionId, Claim existing) {
        return claimRepository.delete(existing.getId()).thenCompose(v -> {
            String id = UUID.randomUUID().toString();
            Claim newClaim = new Claim(id, factionId, ClaimType.FACTION, existing.getWorld(),
                    existing.getMinChunkX(), existing.getMinChunkZ(),
                    existing.getMaxChunkX(), existing.getMaxChunkZ());
            return claimRepository.save(newClaim).thenAccept(saved ->
                    eventBus.post(new FactionClaimedDomainEvent(factionId, saved.getId(), saved.getType().name(),
                            saved.getWorld(), saved.getMinChunkX(), saved.getMinChunkZ(),
                            saved.getMaxChunkX(), saved.getMaxChunkZ())));
        });
    }
}

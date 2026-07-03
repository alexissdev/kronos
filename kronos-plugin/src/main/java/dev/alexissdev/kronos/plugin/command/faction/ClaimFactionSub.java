package dev.alexissdev.kronos.plugin.command.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import dev.alexissdev.kronos.claims.service.ClaimService;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.exception.HCFException;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Sub-comando {@code /f claim} que reclama el chunk en el que se encuentra
 * el jugador para su facción. El límite de chunks reclamables se calcula como
 * {@code maxClaimsPerMember × cantidad de miembros} en la facción, configurado
 * mediante la binding {@code @Named("faction.max-claims-per-member")}.
 */
@Singleton
public class ClaimFactionSub extends FactionSubCommand {

    private final FactionService factionService;
    private final ClaimService   claimService;
    private final MessagesConfig messages;
    private final Plugin         plugin;
    private final int            maxClaimsPerMember;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param factionService     servicio de facciones para obtener el perfil del ejecutor
     * @param claimService       servicio de claims para registrar el territorio
     * @param messages           configuración de mensajes localizados
     * @param plugin             instancia del plugin, usada para programar tareas en el hilo principal
     * @param maxClaimsPerMember número máximo de chunks reclamables por miembro de la facción
     */
    @Inject
    public ClaimFactionSub(FactionService factionService, ClaimService claimService,
                           MessagesConfig messages, Plugin plugin,
                           @Named("faction.max-claims-per-member") int maxClaimsPerMember) {
        this.factionService     = factionService;
        this.claimService       = claimService;
        this.messages           = messages;
        this.plugin             = plugin;
        this.maxClaimsPerMember = maxClaimsPerMember;
    }

    /** @return el nombre del sub-comando: {@code "claim"} */
    @Override public String name() { return "claim"; }

    /**
     * Verifica que el ejecutor pertenezca a una facción y que el límite de claims
     * no haya sido superado, luego registra el chunk actual como territorio de la facción.
     * Notifica al ejecutor con las coordenadas del chunk reclamado.
     *
     * @param sender ejecutor del comando; debe ser un {@link org.bukkit.entity.Player}
     * @param args   argumentos adicionales (no utilizados por este sub-comando)
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        int cx    = player.getLocation().getChunk().getX();
        int cz    = player.getLocation().getChunk().getZ();
        String world = player.getWorld().getName();

        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            Faction faction = opt.get();
            int maxClaims = faction.getMembers().size() * maxClaimsPerMember;
            return claimService.getFactionClaims(faction.getId()).thenCompose(existing -> {
                if (existing.size() >= maxClaims) {
                    throw new HCFException(messages.format("faction.cmd.claim-limit",
                            "current", existing.size(), "max", maxClaims));
                }
                return claimService.claim(faction.getId(), player.getUniqueId(), world, cx, cz, cx, cz);
            });
        }).thenAccept(claim -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.format("faction.cmd.claimed", "x", cx, "z", cz))))
                .exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin,
                        () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex)))); return null; });
    }
}

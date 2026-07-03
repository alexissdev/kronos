package dev.alexissdev.kronos.factions.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa a un jugador que pertenece a una facción, junto con su rol y
 * la fecha en que se unió.
 *
 * <p>Esta clase es parte del agregado {@link Faction}: las instancias viven
 * dentro del mapa interno de miembros de la facción y no se persisten de forma
 * independiente, sino embebidas en el documento de la facción en MongoDB.
 *
 * <p>El rol determina qué operaciones puede realizar el miembro sobre la facción
 * (invitar, expulsar, cambiar alias, retirar fondos, etc.). El rol puede ser
 * promovido o degradado en cualquier momento por un miembro con suficiente autoridad.
 */
public final class FactionMember {

    private final UUID uuid;
    private FactionRole role;
    private final Instant joinedAt;

    /**
     * Crea un nuevo miembro de facción.
     *
     * @param uuid     identificador único del jugador en Minecraft
     * @param role     rol inicial del miembro dentro de la facción
     * @param joinedAt momento en que el jugador se unió a la facción
     */
    public FactionMember(UUID uuid, FactionRole role, Instant joinedAt) {
        this.uuid = uuid;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    /**
     * Devuelve el UUID del jugador asociado a este miembro.
     *
     * @return UUID del jugador
     */
    public UUID getUuid() { return uuid; }

    /**
     * Devuelve el rol actual del miembro dentro de la facción.
     *
     * @return rol del miembro
     */
    public FactionRole getRole() { return role; }

    /**
     * Actualiza el rol del miembro dentro de la facción.
     *
     * <p>Solo debe invocarse desde el servicio de facciones tras verificar
     * que el actor tiene suficiente autoridad para promover o degradar al objetivo.
     *
     * @param role nuevo rol a asignar
     */
    public void setRole(FactionRole role) { this.role = role; }

    /**
     * Devuelve el instante exacto en que el jugador ingresó a la facción.
     *
     * @return marca temporal de ingreso
     */
    public Instant getJoinedAt() { return joinedAt; }
}

package dev.alexissdev.kronos.api.model;

import java.time.Instant;
import java.util.*;

/**
 * Immutable read-only snapshot of a faction in the HCF system.
 * <p>
 * Represents the state of a faction at the moment it was queried, including its member
 * composition with their roles, combat statistics, economic balance, and the remaining
 * Deaths To Kick (DTK) counter. Because it is immutable, instances are safe to share
 * across threads without synchronization.
 * </p>
 * <p>
 * This class does not reflect subsequent changes to the faction's state; for up-to-date
 * data, query again through {@link dev.alexissdev.kronos.api.facade.FactionApi}.
 * </p>
 * <p>
 * <b>DTK (Deaths To Kick) context:</b> every time a faction member dies at the hands of
 * an enemy, the DTK counter decreases. When it reaches zero the faction loses its territory
 * protection and becomes raidable.
 * </p>
 */
public final class FactionSnapshot {

    private final String id;
    private final String name;
    private final UUID leaderUuid;
    private final List<UUID> memberUuids;
    private final Map<UUID, String> memberRoles;
    private final int kills;
    private final int deaths;
    private final int dtkRemaining;
    private final double balance;
    private final Instant createdAt;

    /**
     * Construye una nueva instantánea de facción con todos sus atributos.
     * <p>
     * Las colecciones de miembros y roles se copian defensivamente y se envuelven
     * en vistas no modificables para garantizar la inmutabilidad de la instantánea.
     * </p>
     *
     * @param id           identificador único interno de la facción
     * @param name         nombre público de la facción tal como aparece en el servidor
     * @param leaderUuid   UUID del jugador líder de la facción
     * @param memberUuids  lista de UUIDs de todos los miembros actuales de la facción,
     *                     incluyendo al líder
     * @param memberRoles  mapa de UUID de miembro a nombre de su rol en la facción
     *                     (por ejemplo, {@code "LEADER"}, {@code "CAPTAIN"}, {@code "MEMBER"})
     * @param kills        número total de kills acumuladas por la facción
     * @param deaths       número total de deaths acumuladas por los miembros de la facción
     * @param dtkRemaining muertes restantes antes de que la facción pierda su protección (DTK)
     * @param balance      saldo económico actual de la facción en la moneda del servidor
     * @param createdAt    instante en que la facción fue creada originalmente
     */
    public FactionSnapshot(String id, String name, UUID leaderUuid,
                           List<UUID> memberUuids, Map<UUID, String> memberRoles,
                           int kills, int deaths, int dtkRemaining,
                           double balance, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.leaderUuid = leaderUuid;
        this.memberUuids = Collections.unmodifiableList(new ArrayList<>(memberUuids));
        this.memberRoles = Collections.unmodifiableMap(new LinkedHashMap<>(memberRoles));
        this.kills = kills;
        this.deaths = deaths;
        this.dtkRemaining = dtkRemaining;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    /**
     * Retorna el identificador único interno de la facción.
     * <p>
     * Este ID es generado al momento de la creación y no cambia durante el ciclo de vida
     * de la facción, incluso si su nombre es modificado posteriormente.
     * </p>
     *
     * @return ID único e inmutable de la facción
     */
    public String getId() { return id; }

    /**
     * Retorna el nombre público de la facción tal como aparece en el servidor.
     * <p>
     * El nombre puede ser modificado por el líder, pero el ID permanece constante.
     * </p>
     *
     * @return nombre actual de la facción
     */
    public String getName() { return name; }

    /**
     * Retorna el UUID del jugador que es el líder actual de la facción.
     * <p>
     * El líder tiene permisos completos sobre la facción, incluyendo la capacidad
     * de disolver la facción, gestionar alianzas y expulsar miembros.
     * </p>
     *
     * @return UUID del líder de la facción
     */
    public UUID getLeaderUuid() { return leaderUuid; }

    /**
     * Retorna una vista no modificable de los UUIDs de todos los miembros de la facción.
     * <p>
     * La lista incluye al líder y a todos los capitanes y miembros regulares.
     * Intentar modificar la lista lanzará {@link UnsupportedOperationException}.
     * </p>
     *
     * @return lista inmutable de UUIDs de todos los miembros de la facción
     */
    public List<UUID> getMemberUuids() { return memberUuids; }

    /**
     * Retorna una vista no modificable del mapa de roles de los miembros de la facción.
     * <p>
     * Las claves son los UUIDs de los miembros y los valores son los nombres de sus roles
     * (por ejemplo, {@code "LEADER"}, {@code "CAPTAIN"}, {@code "MEMBER"}).
     * Intentar modificar el mapa lanzará {@link UnsupportedOperationException}.
     * </p>
     *
     * @return mapa inmutable de UUID de miembro a nombre de rol
     */
    public Map<UUID, String> getMemberRoles() { return memberRoles; }

    /**
     * Retorna el número total de kills (eliminaciones a miembros de otras facciones)
     * acumuladas por todos los miembros de la facción.
     *
     * @return total de kills de la facción
     */
    public int getKills() { return kills; }

    /**
     * Retorna el número total de deaths (muertes a manos de enemigos) acumuladas
     * por todos los miembros de la facción.
     * <p>
     * Este valor está directamente relacionado con el sistema DTK: a medida que aumenta,
     * el contador {@link #getDtkRemaining()} disminuye.
     * </p>
     *
     * @return total de deaths de la facción
     */
    public int getDeaths() { return deaths; }

    /**
     * Retorna el número de muertes restantes que la facción puede absorber antes de perder
     * su protección de territorio (Deaths To Kick, DTK).
     * <p>
     * Cuando este valor llega a cero, la facción se vuelve raideable: sus territorios pueden
     * ser invadidos y sus cofres abiertos por facciones enemigas.
     * </p>
     *
     * @return contador DTK restante; cero indica que la facción ya es raideable
     */
    public int getDtkRemaining() { return dtkRemaining; }

    /**
     * Retorna el saldo económico actual de la facción en la moneda del servidor.
     * <p>
     * El balance de facción se usa para pagar algunos costos administrativos,
     * como reclamar territorios adicionales o establecer alianzas.
     * </p>
     *
     * @return saldo económico de la facción
     */
    public double getBalance() { return balance; }

    /**
     * Retorna el instante en que la facción fue creada originalmente en el servidor.
     * <p>
     * Puede usarse para calcular la antigüedad de la facción o implementar
     * mecánicas de protección por novedad.
     * </p>
     *
     * @return {@link Instant} de creación de la facción en UTC
     */
    public Instant getCreatedAt() { return createdAt; }
}

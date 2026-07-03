package dev.alexissdev.kronos.factions.domain;

import java.time.Instant;
import java.util.*;

/**
 * Agregado raíz que representa una facción en el sistema HCF (Hardcore Factions).
 *
 * <p>Una facción agrupa a un conjunto de jugadores bajo un nombre único y un líder.
 * Gestiona su economía interna (balance), relaciones con otras facciones (aliados y enemigos),
 * estadísticas de combate (kills y deaths), el contador DTK (Deaths To Kick) y el estado
 * de raideable que determina si sus claims pueden ser sobrereclamados por enemigos.
 *
 * <p>Reglas de negocio clave:
 * <ul>
 *   <li>Cuando {@code dtkRemaining} llega a 0, la facción se vuelve <em>raideable</em>.</li>
 *   <li>Una facción <em>congelada</em> no puede recibir nuevos miembros ni depósitos.</li>
 *   <li>Al acumular {@value #MAX_STRIKES} strikes la facción es disuelta automáticamente.</li>
 * </ul>
 *
 * <p>Las instancias de esta clase son persistidas en MongoDB mediante
 * {@link dev.alexissdev.kronos.factions.persistence.MongoFactionRepository}.
 */
public final class Faction {

    private static final int MAX_STRIKES = 3;

    private final String id;
    private String name;
    private UUID leaderId;
    private final Map<UUID, FactionMember> members;
    private final Set<String> allies;
    private final Set<String> enemies;
    private double balance;
    private int kills;
    private int deaths;
    private int dtkRemaining;
    private final int maxDtk;
    private final Instant createdAt;
    private FactionHome home;
    private int strikes;
    private boolean frozen;
    private boolean raidable;

    /**
     * Constructor de creación: inicializa una facción nueva con valores por defecto.
     *
     * <p>El contador DTK arranca en {@code maxDtk}, el balance en 0, sin aliados ni enemigos
     * y en estado normal (no congelada, no raideable).
     *
     * @param id        identificador único de la facción (UUID como cadena)
     * @param name      nombre visible de la facción
     * @param leaderId  UUID del jugador que crea y lidera la facción
     * @param maxDtk    número máximo de DTK (Deaths To Kick) que puede absorber la facción
     * @param createdAt instante de creación de la facción
     */
    public Faction(String id, String name, UUID leaderId, int maxDtk, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.maxDtk = maxDtk;
        this.dtkRemaining = maxDtk;
        this.members = new LinkedHashMap<>();
        this.allies = new HashSet<>();
        this.enemies = new HashSet<>();
        this.balance = 0.0;
        this.kills = 0;
        this.deaths = 0;
        this.strikes = 0;
        this.frozen = false;
        this.raidable = false;
        this.createdAt = createdAt;
    }

    /**
     * Constructor de reconstitución: restaura una facción desde la base de datos con todos
     * sus campos ya persistidos.
     *
     * <p>Este constructor es utilizado exclusivamente por
     * {@link dev.alexissdev.kronos.factions.persistence.MongoFactionRepository} al
     * deserializar documentos MongoDB, por lo que acepta el estado completo del agregado.
     *
     * @param id           identificador único de la facción
     * @param name         nombre de la facción
     * @param leaderId     UUID del líder actual
     * @param maxDtk       DTK máximo configurado al crear la facción
     * @param dtkRemaining DTK restantes en el momento de la persistencia
     * @param kills        total de kills acumuladas por la facción
     * @param deaths       total de muertes acumuladas por la facción
     * @param balance      balance económico actual de la facción
     * @param createdAt    instante de creación original
     * @param members      mapa de miembros (UUID → FactionMember) ya reconstituidos
     * @param allies       conjunto de IDs de facciones aliadas
     * @param enemies      conjunto de IDs de facciones enemigas
     * @param strikes      número de strikes acumulados
     * @param frozen       indica si la facción está congelada
     * @param raidable     indica si la facción es actualmente raideable
     */
    public Faction(String id, String name, UUID leaderId, int maxDtk, int dtkRemaining,
                   int kills, int deaths, double balance, Instant createdAt,
                   Map<UUID, FactionMember> members, Set<String> allies, Set<String> enemies,
                   int strikes, boolean frozen, boolean raidable) {
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.maxDtk = maxDtk;
        this.dtkRemaining = dtkRemaining;
        this.kills = kills;
        this.deaths = deaths;
        this.balance = balance;
        this.createdAt = createdAt;
        this.members = members;
        this.allies = allies;
        this.enemies = enemies;
        this.strikes = strikes;
        this.frozen = frozen;
        this.raidable = raidable;
    }

    /**
     * Agrega un miembro a la facción.
     *
     * <p>Si ya existía un miembro con el mismo UUID, este método lo reemplaza.
     *
     * @param member objeto que encapsula UUID, rol y fecha de ingreso del nuevo miembro
     */
    public void addMember(FactionMember member) {
        members.put(member.getUuid(), member);
    }

    /**
     * Elimina al miembro con el UUID indicado de la facción.
     *
     * <p>Si el UUID no pertenece a ningún miembro, la operación no tiene efecto.
     *
     * @param uuid UUID del jugador a eliminar
     */
    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    /**
     * Busca y devuelve el miembro de la facción con el UUID dado.
     *
     * @param uuid UUID del jugador a buscar
     * @return {@link Optional} con el {@link FactionMember} si existe, o vacío si no pertenece
     */
    public Optional<FactionMember> getMember(UUID uuid) {
        return Optional.ofNullable(members.get(uuid));
    }

    /**
     * Indica si el jugador con el UUID dado es miembro activo de esta facción.
     *
     * @param uuid UUID del jugador
     * @return {@code true} si el jugador pertenece a la facción
     */
    public boolean hasMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    /**
     * Incrementa en uno el contador de kills de la facción.
     *
     * <p>Se invoca cuando un miembro de esta facción mata a un jugador enemigo.
     */
    public void incrementKills() { kills++; }

    /**
     * Incrementa en uno el contador de muertes de la facción.
     *
     * <p>Se invoca cuando cualquier miembro de la facción muere, independientemente
     * de si esa muerte también consume un DTK.
     */
    public void incrementDeaths() { deaths++; }

    /**
     * Decrementa en uno el contador de DTK (Deaths To Kick) restantes.
     *
     * <p>El DTK es el número de muertes de miembros que la facción puede absorber antes de
     * volverse raideable. Si el contador ya está en 0, no se realiza ningún cambio.
     *
     * @return {@code true} si el decremento fue exitoso; {@code false} si el DTK ya era 0
     */
    public boolean decrementDtk() {
        if (dtkRemaining > 0) {
            dtkRemaining--;
            return true;
        }
        return false;
    }

    /**
     * Indica si la facción ha agotado todos sus DTK restantes.
     *
     * <p>Cuando este método devuelve {@code true}, el servicio de facciones marcará la
     * facción como raideable y publicará el evento {@link dev.alexissdev.kronos.factions.event.FactionRaidableDomainEvent}.
     *
     * @return {@code true} si {@code dtkRemaining} es 0 o menor
     */
    public boolean isAtDtk() { return dtkRemaining <= 0; }

    /**
     * Registra a la facción con el ID dado como aliada de esta facción.
     *
     * @param factionId ID de la facción aliada
     */
    public void addAlly(String factionId) { allies.add(factionId); }

    /**
     * Elimina a la facción con el ID dado del conjunto de aliados.
     *
     * @param factionId ID de la facción a desvincular
     */
    public void removeAlly(String factionId) { allies.remove(factionId); }

    /**
     * Indica si la facción con el ID dado es aliada de esta facción.
     *
     * @param factionId ID de la facción a consultar
     * @return {@code true} si ambas facciones son aliadas
     */
    public boolean isAlly(String factionId) { return allies.contains(factionId); }

    /**
     * Registra a la facción con el ID dado como enemiga de esta facción.
     *
     * @param factionId ID de la facción enemiga
     */
    public void addEnemy(String factionId) { enemies.add(factionId); }

    /**
     * Elimina a la facción con el ID dado del conjunto de enemigos.
     *
     * @param factionId ID de la facción a desvincular
     */
    public void removeEnemy(String factionId) { enemies.remove(factionId); }

    /**
     * Indica si la facción con el ID dado es enemiga de esta facción.
     *
     * @param factionId ID de la facción a consultar
     * @return {@code true} si ambas facciones son enemigas
     */
    public boolean isEnemy(String factionId) { return enemies.contains(factionId); }

    /**
     * Aumenta el balance de la facción en la cantidad indicada.
     *
     * <p>Este método no valida si la facción está congelada; dicha validación
     * es responsabilidad del servicio de facciones.
     *
     * @param amount cantidad a depositar (debe ser mayor a 0)
     */
    public void deposit(double amount) { balance += amount; }

    /**
     * Reduce el balance de la facción en la cantidad indicada.
     *
     * <p>No valida si el balance resultante sería negativo; la validación
     * previa es responsabilidad del servicio de facciones.
     *
     * @param amount cantidad a retirar (debe ser mayor a 0 y menor o igual al balance actual)
     */
    public void withdraw(double amount) { balance -= amount; }

    /**
     * Agrega un strike administrativo a la facción.
     *
     * <p>Al alcanzar {@value #MAX_STRIKES} strikes, el servicio de facciones
     * disuelve la facción automáticamente.
     */
    public void addStrike() { strikes++; }

    /**
     * Indica si la facción ha alcanzado el número máximo de strikes permitidos.
     *
     * @return {@code true} si los strikes acumulados son iguales o superiores a {@value #MAX_STRIKES}
     */
    public boolean isAtMaxStrikes() { return strikes >= MAX_STRIKES; }

    /**
     * Devuelve el identificador único de la facción.
     *
     * @return ID de la facción como cadena (representación de UUID)
     */
    public String getId() { return id; }

    /**
     * Devuelve el nombre visible de la facción.
     *
     * @return nombre de la facción
     */
    public String getName() { return name; }

    /**
     * Cambia el nombre visible de la facción.
     *
     * <p>El nombre nuevo debe ser único en el servidor; la validación de unicidad
     * es responsabilidad del servicio de facciones.
     *
     * @param name nuevo nombre de la facción
     */
    public void setName(String name) { this.name = name; }

    /**
     * Devuelve el UUID del líder actual de la facción.
     *
     * @return UUID del líder
     */
    public UUID getLeaderId() { return leaderId; }

    /**
     * Actualiza el líder de la facción.
     *
     * <p>Debe invocarse junto con los cambios de rol correspondientes en los miembros
     * afectados. Véase {@link dev.alexissdev.kronos.factions.FactionApplicationService#setLeader}.
     *
     * @param leaderId UUID del nuevo líder
     */
    public void setLeaderId(UUID leaderId) { this.leaderId = leaderId; }

    /**
     * Devuelve una vista inmutable del mapa de miembros de la facción.
     *
     * @return mapa UUID → FactionMember (no modificable)
     */
    public Map<UUID, FactionMember> getMembers() { return Collections.unmodifiableMap(members); }

    /**
     * Devuelve una vista inmutable del conjunto de IDs de facciones aliadas.
     *
     * @return conjunto de IDs de aliados (no modificable)
     */
    public Set<String> getAllies() { return Collections.unmodifiableSet(allies); }

    /**
     * Devuelve una vista inmutable del conjunto de IDs de facciones enemigas.
     *
     * @return conjunto de IDs de enemigos (no modificable)
     */
    public Set<String> getEnemies() { return Collections.unmodifiableSet(enemies); }

    /**
     * Devuelve el balance económico actual de la facción.
     *
     * @return balance en la moneda del servidor
     */
    public double getBalance() { return balance; }

    /**
     * Devuelve el total de kills acumuladas por los miembros de la facción.
     *
     * @return número de kills
     */
    public int getKills() { return kills; }

    /**
     * Devuelve el total de muertes acumuladas por los miembros de la facción.
     *
     * @return número de muertes
     */
    public int getDeaths() { return deaths; }

    /**
     * Devuelve los DTK restantes antes de que la facción se vuelva raideable.
     *
     * @return DTK restantes (0 indica estado raideable)
     */
    public int getDtkRemaining() { return dtkRemaining; }

    /**
     * Devuelve el número máximo de DTK con que la facción fue configurada.
     *
     * @return DTK máximo
     */
    public int getMaxDtk() { return maxDtk; }

    /**
     * Devuelve el instante en que la facción fue creada.
     *
     * @return marca temporal de creación
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * Devuelve el punto de hogar de la facción, o {@code null} si no se ha definido ninguno.
     *
     * @return ubicación del hogar, o {@code null}
     */
    public FactionHome getHome() { return home; }

    /**
     * Establece un nuevo punto de hogar para la facción.
     *
     * @param home nueva ubicación de hogar; puede ser {@code null} para eliminar el hogar
     */
    public void setHome(FactionHome home) { this.home = home; }

    /**
     * Elimina el hogar de la facción, dejando el campo en {@code null}.
     */
    public void clearHome() { this.home = null; }

    /**
     * Devuelve el número de strikes administrativos acumulados por la facción.
     *
     * @return número de strikes actuales
     */
    public int getStrikes() { return strikes; }

    /**
     * Devuelve el número máximo de strikes que puede acumular una facción antes de ser disuelta.
     *
     * @return límite de strikes ({@value #MAX_STRIKES})
     */
    public int getMaxStrikes() { return MAX_STRIKES; }

    /**
     * Indica si la facción está congelada.
     *
     * <p>Una facción congelada no puede recibir nuevos miembros ni depósitos económicos.
     *
     * @return {@code true} si la facción está congelada
     */
    public boolean isFrozen() { return frozen; }

    /**
     * Cambia el estado de congelamiento de la facción.
     *
     * @param frozen {@code true} para congelar la facción; {@code false} para descongelarla
     */
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    /**
     * Indica si la facción es actualmente raideable, es decir, si sus claims
     * pueden ser sobrereclamados por facciones enemigas.
     *
     * @return {@code true} si la facción es raideable
     */
    public boolean isRaidable() { return raidable; }

    /**
     * Cambia el estado de raideabilidad de la facción.
     *
     * <p>Este método se invoca cuando el DTK llega a 0 (para activar el estado)
     * o cuando un administrador restaura manualmente los DTK de la facción.
     *
     * @param raidable {@code true} para marcar la facción como raideable
     */
    public void setRaidable(boolean raidable) { this.raidable = raidable; }
}

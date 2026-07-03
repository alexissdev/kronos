package dev.alexissdev.kronos.koth.creation;

import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.koth.domain.KothZone;

/**
 * Objeto de sesión que almacena el estado temporal durante la creación interactiva
 * de una zona KOTH por parte de un administrador.
 *
 * <p>El proceso de creación está dividido en dos fases definidas por {@link Phase}:
 * <ol>
 *   <li><b>{@link Phase#CLAIM}</b>: el administrador selecciona las dos esquinas
 *       (posición 1 y posición 2) del territorio completo de claim.</li>
 *   <li><b>{@link Phase#CAPTURE}</b>: una vez completado el claim, el administrador
 *       selecciona las dos esquinas de la zona interior de captura.</li>
 * </ol>
 *
 * <p>Cuando todas las posiciones están definidas ({@link #isComplete()} retorna
 * {@code true}), se puede invocar {@link #build()} para construir la entidad
 * {@link dev.alexissdev.kronos.koth.domain.KothZone} lista para persistir.</p>
 *
 * <p>La sesión es gestionada por {@code KothCreationService} y {@code KothWandListener}.</p>
 */
public final class KothCreationSession {

    /**
     * Fases del flujo de creación interactiva de una zona KOTH.
     *
     * <ul>
     *   <li>{@code CLAIM} — fase inicial: el admin selecciona el territorio completo.</li>
     *   <li>{@code CAPTURE} — fase secundaria: el admin selecciona la zona de captura interior.</li>
     * </ul>
     */
    public enum Phase { CLAIM, CAPTURE }

    private final String kothName;
    private final int captureTimeSeconds;
    private Phase phase = Phase.CLAIM;

    private String worldName;

    private Integer claimX1, claimZ1;
    private Integer claimX2, claimZ2;
    private Integer capX1, capZ1;
    private Integer capX2, capZ2;

    /**
     * Construye una nueva sesión de creación para un KOTH con el nombre y tiempo dados.
     *
     * @param kothName           nombre que tendrá el KOTH una vez creado
     * @param captureTimeSeconds tiempo en segundos requerido para capturar el KOTH
     */
    public KothCreationSession(String kothName, int captureTimeSeconds) {
        this.kothName = kothName;
        this.captureTimeSeconds = captureTimeSeconds;
    }

    // ── setters ───────────────────────────────────────────────────────────

    /**
     * Establece la primera esquina del territorio de claim junto con el mundo donde se ubica.
     *
     * @param world nombre del mundo de Bukkit donde se seleccionó el bloque
     * @param x     coordenada X del bloque seleccionado como primera esquina del claim
     * @param z     coordenada Z del bloque seleccionado como primera esquina del claim
     */
    public void setClaimPos1(String world, int x, int z) {
        this.worldName = world;
        this.claimX1 = x;
        this.claimZ1 = z;
    }

    /**
     * Establece la segunda esquina del territorio de claim.
     * Requiere que {@link #setClaimPos1(String, int, int)} haya sido llamado previamente.
     *
     * @param x coordenada X del bloque seleccionado como segunda esquina del claim
     * @param z coordenada Z del bloque seleccionado como segunda esquina del claim
     */
    public void setClaimPos2(int x, int z) {
        this.claimX2 = x;
        this.claimZ2 = z;
    }

    /**
     * Establece la primera esquina de la zona de captura interior.
     * Solo debe invocarse durante la fase {@link Phase#CAPTURE}.
     *
     * @param x coordenada X del bloque seleccionado como primera esquina de captura
     * @param z coordenada Z del bloque seleccionado como primera esquina de captura
     */
    public void setCapturePos1(int x, int z) {
        this.capX1 = x;
        this.capZ1 = z;
    }

    /**
     * Establece la segunda esquina de la zona de captura interior.
     * Tras esta llamada, si el claim también está completo, {@link #isComplete()} retornará {@code true}.
     *
     * @param x coordenada X del bloque seleccionado como segunda esquina de captura
     * @param z coordenada Z del bloque seleccionado como segunda esquina de captura
     */
    public void setCapturePos2(int x, int z) {
        this.capX2 = x;
        this.capZ2 = z;
    }

    /**
     * Avanza la sesión de la fase {@link Phase#CLAIM} a la fase {@link Phase#CAPTURE}.
     * Debe invocarse después de que el territorio de claim esté completamente definido.
     */
    public void advanceToCapture() {
        this.phase = Phase.CAPTURE;
    }

    // ── state checks ──────────────────────────────────────────────────────

    /** @return {@code true} si ya se ha establecido la primera esquina del claim */
    public boolean hasClaimPos1()   { return claimX1 != null; }
    /** @return {@code true} si ambas esquinas del territorio de claim han sido seleccionadas */
    public boolean isClaimComplete() { return claimX1 != null && claimX2 != null; }
    /** @return {@code true} si ya se ha establecido la primera esquina de la zona de captura */
    public boolean hasCapturePos1() { return capX1 != null; }
    /**
     * @return {@code true} si tanto el claim como la zona de captura están completamente definidos
     *         y la sesión está lista para construir la {@link KothZone}
     */
    public boolean isComplete()     { return isClaimComplete() && capX1 != null && capX2 != null; }

    // ── getters ───────────────────────────────────────────────────────────

    /** @return fase actual del proceso de creación ({@link Phase#CLAIM} o {@link Phase#CAPTURE}) */
    public Phase getPhase()          { return phase; }
    /** @return nombre que tendrá el KOTH una vez creado */
    public String getKothName()      { return kothName; }
    /** @return tiempo en segundos requerido para capturar el KOTH */
    public int getCaptureTimeSeconds() { return captureTimeSeconds; }

    // ── build ─────────────────────────────────────────────────────────────

    /**
     * Construye la entidad {@link KothZone} a partir de las coordenadas registradas en la sesión.
     * Los límites mínimos y máximos se calculan automáticamente a partir de las dos esquinas,
     * por lo que no importa el orden en que fueron seleccionadas.
     *
     * <p>Este método debe invocarse únicamente cuando {@link #isComplete()} retorna {@code true}.</p>
     *
     * @return nueva instancia de {@link KothZone} lista para ser persistida
     * @throws NullPointerException si la sesión no está completa al momento de la llamada
     */
    public KothZone build() {
        return new KothZone(
                kothName, worldName,
                Math.min(claimX1, claimX2), Math.min(claimZ1, claimZ2),
                Math.max(claimX1, claimX2), Math.max(claimZ1, claimZ2),
                Math.min(capX1, capX2), Math.min(capZ1, capZ2),
                Math.max(capX1, capX2), Math.max(capZ1, capZ2),
                captureTimeSeconds, CrateType.KOTH
        );
    }
}

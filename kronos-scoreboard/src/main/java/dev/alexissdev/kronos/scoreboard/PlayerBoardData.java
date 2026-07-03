package dev.alexissdev.kronos.scoreboard;

import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Snapshot mutable de los datos de un jugador utilizado por {@link ScoreboardRenderer}
 * para generar las líneas del marcador lateral en cada tick.
 * <p>
 * Los campos están agrupados en tres categorías con frecuencias de actualización distintas:
 * </p>
 * <ul>
 *   <li><b>Estadísticas lentas</b> (kills, deaths, balance, facción, DTK): se refrescan de forma
 *       asíncrona cada 5 segundos desde {@link ScoreboardManager#refreshAllStats()}.</li>
 *   <li><b>Timers individuales</b> (combate, PvP, enderpearl, etc.): se actualizan de forma
 *       síncrona al recibir eventos del EventBus de Guava en {@link ScoreboardManager}.</li>
 *   <li><b>Contadores globales y KOTH</b> (SOTW, EOTW, captura de KOTH): se propagan en
 *       cada tick del hilo principal desde {@link ScoreboardManager#tickAll()}.</li>
 * </ul>
 */
final class PlayerBoardData {

    private volatile int kills;
    private volatile int deaths;
    private volatile double balance;
    private volatile String factionName;
    private volatile int dtkRemaining;

    // Maps TimerType → absolute epoch-ms when the timer expires.
    private final Map<TimerType, Long> timerExpiryMs = new EnumMap<>(TimerType.class);

    // KOTH capture progress for this player
    private volatile String capturingKothName;
    private volatile long   captureExpiresAt;

    // Global SOTW/EOTW countdown (same value for all players, updated each tick)
    private volatile long sotwRemainingMs = 0L;
    private volatile long eotwRemainingMs = 0L;

    // ── stats ─────────────────────────────────────────────────────────────

    /** @return número de kills acumulados del jugador en el servidor */
    int getKills()        { return kills; }

    /** @return número de muertes acumuladas del jugador en el servidor */
    int getDeaths()       { return deaths; }

    /** @return balance económico actual del jugador */
    double getBalance()   { return balance; }

    /**
     * @return nombre de la facción a la que pertenece el jugador,
     *         o {@code null} si no pertenece a ninguna facción
     */
    String getFactionName() { return factionName; }

    /**
     * @return cantidad de DTK (Deaths To Kick) restantes en la facción del jugador;
     *         {@code 0} si el jugador no tiene facción
     */
    int getDtkRemaining() { return dtkRemaining; }

    /**
     * Actualiza el número de kills del jugador.
     *
     * @param v nuevo valor de kills obtenido desde {@code PlayerService}
     */
    void setKills(int v)          { this.kills = v; }

    /**
     * Actualiza el número de muertes del jugador.
     *
     * @param v nuevo valor de muertes obtenido desde {@code PlayerService}
     */
    void setDeaths(int v)         { this.deaths = v; }

    /**
     * Actualiza el balance económico del jugador.
     *
     * @param v nuevo saldo obtenido desde {@code EconomyService}
     */
    void setBalance(double v)     { this.balance = v; }

    /**
     * Actualiza el nombre de la facción del jugador.
     *
     * @param v nombre de la facción, o {@code null} si el jugador no pertenece a ninguna
     */
    void setFactionName(String v) { this.factionName = v; }

    /**
     * Actualiza la cantidad de DTK restantes en la facción del jugador.
     *
     * @param v muertes restantes antes de que la facción sea expulsada del servidor
     */
    void setDtkRemaining(int v)   { this.dtkRemaining = v; }

    // ── timers ────────────────────────────────────────────────────────────

    /**
     * Registra o actualiza el tiempo de expiración de un timer individual del jugador.
     * <p>
     * Llamado desde {@link ScoreboardManager} al recibir un
     * {@code PlayerTimerStartedDomainEvent}. Este método es seguro para llamadas
     * concurrentes entre el hilo principal y los hilos del EventBus.
     * </p>
     *
     * @param type          tipo de timer que inicia (ej. {@code COMBAT_TAG}, {@code PVP_TIMER})
     * @param expiryEpochMs timestamp en milisegundos epoch en el que el timer expirará
     */
    synchronized void setTimer(TimerType type, long expiryEpochMs) {
        timerExpiryMs.put(type, expiryEpochMs);
    }

    /**
     * Elimina un timer activo del jugador al recibir su evento de expiración.
     * <p>
     * Llamado desde {@link ScoreboardManager} al recibir un
     * {@code PlayerTimerExpiredDomainEvent}. Este método es seguro para llamadas
     * concurrentes.
     * </p>
     *
     * @param type tipo de timer que ha expirado y debe eliminarse del marcador
     */
    synchronized void clearTimer(TimerType type) {
        timerExpiryMs.remove(type);
    }

    /**
     * Calcula el tiempo restante en milisegundos para el timer especificado.
     * <p>
     * Usado por {@link ScoreboardRenderer} en cada tick para mostrar la cuenta
     * regresiva actualizada. Este método es seguro para llamadas concurrentes.
     * </p>
     *
     * @param type tipo de timer a consultar
     * @return milisegundos restantes hasta que el timer expire, o {@code 0L}
     *         si el timer no está activo o ya ha expirado
     */
    synchronized long getRemainingMs(TimerType type) {
        Long expiry = timerExpiryMs.get(type);
        if (expiry == null) return 0L;
        return Math.max(0L, expiry - System.currentTimeMillis());
    }

    // ── KOTH capture ──────────────────────────────────────────────────────

    /**
     * Registra que el jugador está en proceso de capturar un KOTH y establece
     * el tiempo restante para completar la captura.
     * <p>
     * Llamado desde {@link ScoreboardManager#updateKothCapture(java.util.UUID, String, long)}
     * cuando el jugador permanece dentro de la zona de captura.
     * </p>
     *
     * @param kothName    nombre del KOTH que el jugador está capturando
     * @param remainingMs milisegundos restantes para completar la captura desde este momento
     */
    void setKothCapture(String kothName, long remainingMs) {
        this.capturingKothName = kothName;
        this.captureExpiresAt  = System.currentTimeMillis() + remainingMs;
    }

    /**
     * Limpia el estado de captura de KOTH del jugador, indicando que dejó de capturar
     * (salió de la zona o el KOTH finalizó).
     */
    void clearKothCapture() {
        this.capturingKothName = null;
        this.captureExpiresAt  = 0;
    }

    /**
     * @return nombre del KOTH que el jugador está capturando actualmente,
     *         o {@code null} si no está en proceso de captura
     */
    String getCapturingKothName()   { return capturingKothName; }

    /**
     * Calcula el tiempo restante para que el jugador complete la captura del KOTH actual.
     *
     * @return milisegundos restantes de captura, o {@code 0L} si el jugador no está
     *         capturando ningún KOTH o el tiempo de captura ya expiró
     */
    long getCaptureRemainingMs() {
        if (capturingKothName == null) return 0L;
        return Math.max(0L, captureExpiresAt - System.currentTimeMillis());
    }

    // ── SOTW / EOTW ───────────────────────────────────────────────────────

    /**
     * Actualiza el tiempo restante del periodo SOTW (Start Of The World).
     * Este valor es global —el mismo para todos los jugadores— y se propaga en
     * cada tick desde {@link ScoreboardManager#tickAll()}.
     *
     * @param ms milisegundos restantes de SOTW, o {@code 0L} si SOTW no está activo
     */
    void setSotwRemainingMs(long ms) { this.sotwRemainingMs = ms; }

    /**
     * Actualiza el tiempo restante del periodo EOTW (End Of The World).
     * Este valor es global —el mismo para todos los jugadores— y se propaga en
     * cada tick desde {@link ScoreboardManager#tickAll()}.
     *
     * @param ms milisegundos restantes de EOTW, o {@code 0L} si EOTW no está activo
     */
    void setEotwRemainingMs(long ms) { this.eotwRemainingMs = ms; }

    /**
     * @return milisegundos restantes del periodo SOTW, o {@code 0L} si no está activo
     */
    long getSotwRemainingMs()        { return sotwRemainingMs; }

    /**
     * @return milisegundos restantes del periodo EOTW, o {@code 0L} si no está activo
     */
    long getEotwRemainingMs()        { return eotwRemainingMs; }
}

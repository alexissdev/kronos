package dev.alexissdev.kronos.players.domain;

import java.util.UUID;

/**
 * Entidad de dominio que representa el perfil de un jugador dentro del sistema HCF.
 *
 * <p>Almacena todos los datos persistentes de un jugador: estadísticas de combate
 * (kills y muertes), número de vidas disponibles, el kit activo seleccionado,
 * el inventario guardado en formato JSON y el estado del timer de PvP.
 * Esta entidad se persiste en MongoDB a través de {@code PlayerRepository}.</p>
 *
 * <p>En el sistema HCF las vidas son un recurso limitado: cuando un jugador muere
 * con el timer de Deathban activo pierde una vida. Al llegar a cero vidas queda
 * baneado temporalmente del servidor (Deathban). Las vidas se regeneran
 * automáticamente con el paso del tiempo.</p>
 */
public final class HCFPlayer {

    private final UUID uuid;
    private String name;
    private int kills;
    private int deaths;
    private int lives;
    private boolean pvpTimerGiven;
    private KitType activeKit;
    private String savedInventoryJson;
    private long lastLifeRegenAt;

    /**
     * Crea un nuevo perfil de jugador con valores predeterminados para la primera
     * vez que el jugador se conecta al servidor.
     *
     * <p>Los valores iniciales son: 0 kills, 0 muertes, 3 vidas, sin timer de PvP
     * y kit {@link KitType#DIAMOND} como kit activo.</p>
     *
     * @param uuid UUID único del jugador de Minecraft
     * @param name nombre de usuario del jugador en el momento del registro
     */
    public HCFPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.kills = 0;
        this.deaths = 0;
        this.lives = 3;
        this.pvpTimerGiven = false;
        this.activeKit = KitType.DIAMOND;
        this.lastLifeRegenAt = System.currentTimeMillis();
    }

    /**
     * Reconstruye un perfil de jugador con todos sus datos desde la base de datos.
     * Este constructor es utilizado exclusivamente por la capa de persistencia
     * al deserializar un documento de MongoDB.
     *
     * @param uuid               UUID único del jugador
     * @param name               nombre de usuario actual del jugador
     * @param kills              número total de kills registradas
     * @param deaths             número total de muertes registradas
     * @param activeKit          kit activo seleccionado por el jugador
     * @param savedInventoryJson inventario guardado serializado en formato JSON, puede ser {@code null}
     * @param lives              número de vidas restantes del jugador
     * @param pvpTimerGiven      {@code true} si ya se le otorgó el timer de protección PvP al conectarse
     * @param lastLifeRegenAt    timestamp en milisegundos de la última regeneración de vida
     */
    public HCFPlayer(UUID uuid, String name, int kills, int deaths,
                     KitType activeKit, String savedInventoryJson, int lives,
                     boolean pvpTimerGiven, long lastLifeRegenAt) {
        this.uuid = uuid;
        this.name = name;
        this.kills = kills;
        this.deaths = deaths;
        this.activeKit = activeKit;
        this.savedInventoryJson = savedInventoryJson;
        this.lives = lives;
        this.pvpTimerGiven = pvpTimerGiven;
        this.lastLifeRegenAt = lastLifeRegenAt;
    }

    /**
     * Incrementa en uno el contador de kills del jugador.
     * Se invoca cuando este jugador mata a otro jugador en combate PvP.
     */
    public void incrementKills() { kills++; }

    /**
     * Incrementa en uno el contador de muertes del jugador.
     * Se invoca cuando este jugador es asesinado por otro jugador en combate PvP.
     */
    public void incrementDeaths() { deaths++; }

    /**
     * Decrementa en uno el número de vidas del jugador, sin bajar de cero.
     * Se llama cuando el jugador muere con el timer de Deathban activo.
     * Si el resultado es cero, el jugador quedará sujeto al Deathban en el
     * próximo ciclo de verificación.
     *
     * @return el número de vidas restantes tras el decremento (mínimo 0)
     */
    public int decrementLives() {
        if (lives > 0) lives--;
        return lives;
    }

    /**
     * Intenta regenerar una vida al jugador si se cumplen las condiciones necesarias.
     *
     * <p>La regeneración ocurre únicamente si: (1) el jugador tiene menos vidas
     * que el máximo permitido y (2) ha transcurrido el intervalo mínimo de tiempo
     * desde la última regeneración.</p>
     *
     * @param maxLives         número máximo de vidas que puede tener el jugador
     * @param regenIntervalMs  tiempo mínimo en milisegundos que debe transcurrir entre regeneraciones
     * @return {@code true} si se regeneró una vida exitosamente, {@code false} si no se cumplieron las condiciones
     */
    public boolean tryRegenLife(int maxLives, long regenIntervalMs) {
        if (lives >= maxLives) return false;
        if (System.currentTimeMillis() - lastLifeRegenAt < regenIntervalMs) return false;
        lives++;
        lastLifeRegenAt = System.currentTimeMillis();
        return true;
    }

    /**
     * Establece directamente el número de vidas del jugador.
     * Utilizado para restaurar vidas después de que expire un Deathban.
     *
     * @param lives nuevo número de vidas a asignar al jugador
     */
    public void setLives(int lives) { this.lives = lives; }

    /**
     * Obtiene el timestamp en milisegundos de la última vez que el jugador regeneró una vida.
     *
     * @return timestamp Unix en milisegundos de la última regeneración de vida
     */
    public long getLastLifeRegenAt()          { return lastLifeRegenAt; }

    /**
     * Actualiza el timestamp de la última regeneración de vida.
     *
     * @param t nuevo timestamp Unix en milisegundos
     */
    public void setLastLifeRegenAt(long t)    { this.lastLifeRegenAt = t; }

    /**
     * Indica si ya se le otorgó al jugador el timer de protección PvP al conectarse.
     * El timer PvP protege a los recién conectados de recibir daño por un tiempo determinado.
     *
     * @return {@code true} si el timer de PvP ya fue otorgado en la sesión actual
     */
    public boolean isPvpTimerGiven()          { return pvpTimerGiven; }

    /**
     * Establece el estado del timer de protección PvP del jugador.
     *
     * @param v {@code true} para marcar que el timer ya fue otorgado, {@code false} para reiniciarlo
     */
    public void setPvpTimerGiven(boolean v)   { this.pvpTimerGiven = v; }

    /**
     * Obtiene el UUID único de Minecraft del jugador.
     *
     * @return UUID del jugador
     */
    public UUID   getUuid()   { return uuid; }

    /**
     * Obtiene el nombre de usuario actual del jugador.
     *
     * @return nombre de usuario del jugador
     */
    public String getName()   { return name; }

    /**
     * Actualiza el nombre de usuario del jugador.
     * Se invoca cuando el jugador se reconecta con un nombre diferente al registrado.
     *
     * @param name nuevo nombre de usuario del jugador
     */
    public void   setName(String name) { this.name = name; }

    /**
     * Obtiene el total de kills registradas del jugador en el servidor.
     *
     * @return número total de kills
     */
    public int  getKills()  { return kills; }

    /**
     * Obtiene el total de muertes registradas del jugador en el servidor.
     *
     * @return número total de muertes
     */
    public int  getDeaths() { return deaths; }

    /**
     * Obtiene el número de vidas restantes del jugador.
     * Un valor de cero indica que el jugador está sujeto al sistema de Deathban.
     *
     * @return número de vidas restantes
     */
    public int  getLives()  { return lives; }

    /**
     * Obtiene el kit activo actualmente seleccionado por el jugador.
     *
     * @return tipo de kit activo del jugador
     */
    public KitType getActiveKit()                { return activeKit; }

    /**
     * Establece el kit activo del jugador.
     *
     * @param activeKit nuevo tipo de kit a activar
     */
    public void    setActiveKit(KitType activeKit) { this.activeKit = activeKit; }

    /**
     * Obtiene el inventario del jugador serializado en formato JSON.
     * Se usa para guardar y restaurar el inventario entre sesiones.
     *
     * @return cadena JSON con el inventario guardado, o {@code null} si no hay ninguno guardado
     */
    public String getSavedInventoryJson() { return savedInventoryJson; }

    /**
     * Establece el inventario del jugador serializado en formato JSON.
     *
     * @param savedInventoryJson cadena JSON con el inventario a guardar
     */
    public void setSavedInventoryJson(String savedInventoryJson) {
        this.savedInventoryJson = savedInventoryJson;
    }
}

package dev.alexissdev.kronos.plugin;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.plugin.guice.RootModule;
import dev.alexissdev.kronos.plugin.lifecycle.PluginDisableHandler;
import dev.alexissdev.kronos.plugin.lifecycle.PluginEnableHandler;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Clase principal del plugin KronosHCF.
 *
 * <p>Extiende {@link JavaPlugin} y actúa como punto de entrada del plugin en el ciclo de vida
 * de Bukkit. Se encarga de inicializar el contenedor de inyección de dependencias Guice a través
 * de {@link RootModule} y delega la lógica de arranque y apagado a
 * {@link PluginEnableHandler} y {@link PluginDisableHandler} respectivamente.
 */
public final class HCFPlugin extends JavaPlugin {

    private Injector injector;

    /**
     * Se invoca antes de que el servidor registre el plugin como activo.
     *
     * <p>Guarda la configuración predeterminada ({@code config.yml}) y el archivo de mensajes
     * ({@code messages.yml}) en la carpeta de datos del plugin si aún no existen.
     */
    @Override
    public void onLoad() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
    }

    /**
     * Punto de entrada principal del plugin al habilitarse.
     *
     * <p>Carga los mensajes, construye el inyector Guice con el {@link RootModule} raíz y
     * delega toda la lógica de inicialización (registro de comandos, listeners, servicios, etc.)
     * a {@link PluginEnableHandler#enable()}. Si ocurre cualquier excepción durante este proceso,
     * el plugin se deshabilita automáticamente para evitar un estado inconsistente.
     */
    @Override
    public void onEnable() {
        try {
            MessagesConfig messagesConfig = loadMessages();
            injector = Guice.createInjector(new RootModule(this, messagesConfig));
            PluginEnableHandler enableHandler = injector.getInstance(PluginEnableHandler.class);
            enableHandler.enable();
        } catch (Exception e) {
            getLogger().severe("Error al inicializar KronosHCF: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Se invoca cuando el plugin es deshabilitado por el servidor o por un error.
     *
     * <p>Si el inyector fue inicializado correctamente, delega el proceso de apagado
     * (cierre de conexiones de base de datos, desactivación de KOTHs, etc.) a
     * {@link PluginDisableHandler#disable()}.
     */
    @Override
    public void onDisable() {
        if (injector != null) {
            try {
                PluginDisableHandler disableHandler = injector.getInstance(PluginDisableHandler.class);
                disableHandler.disable();
            } catch (Exception e) {
                getLogger().severe("Error al deshabilitar KronosHCF: " + e.getMessage());
            }
        }
    }

    /**
     * Devuelve el inyector Guice creado durante {@link #onEnable()}.
     *
     * <p>Puede ser utilizado por componentes externos (por ejemplo, otros plugins o comandos
     * de administración) que necesiten obtener instancias gestionadas por Guice.
     *
     * @return el {@link Injector} de Guice, o {@code null} si el plugin no se inicializó correctamente
     */
    public Injector getInjector() {
        return injector;
    }

    /**
     * Lee el archivo {@code messages.yml} desde la carpeta de datos del plugin y construye
     * un {@link MessagesConfig} con todas las claves y valores encontrados.
     *
     * <p>Solo se procesan las claves que representan valores concretos (no secciones de
     * configuración), lo que garantiza que el mapa resultante contenga únicamente cadenas de texto.
     *
     * @return un {@link MessagesConfig} poblado con los mensajes definidos en {@code messages.yml}
     */
    private MessagesConfig loadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : yaml.getKeys(true)) {
            if (!yaml.isConfigurationSection(key)) {
                map.put(key, yaml.getString(key, ""));
            }
        }
        return new MessagesConfig(map);
    }
}

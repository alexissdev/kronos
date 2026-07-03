package dev.alexissdev.kronos.spawn.service;

import dev.alexissdev.kronos.spawn.domain.SpawnZone;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato de negocio para el sistema de zona de spawn del servidor HCF.
 *
 * <p>El spawn es una región protegida donde el PvP está deshabilitado y los jugadores
 * comienzan su sesión. Este servicio gestiona la {@link SpawnZone} activa, manteniendo
 * una copia en memoria para consultas síncronas frecuentes (p.ej. durante movimiento de
 * jugadores) sin necesidad de acceder a MongoDB en cada tick.</p>
 *
 * <p>La implementación por defecto es {@code SpawnApplicationService}, registrada como
 * singleton en {@code SpawnModule}.</p>
 */
public interface SpawnService {

    /**
     * Establece una nueva zona de spawn, actualizando tanto el caché en memoria como
     * la persistencia en MongoDB.
     *
     * @param zone la nueva zona de spawn a configurar
     * @return future que se completa cuando la zona ha sido guardada exitosamente
     */
    CompletableFuture<Void> setZone(SpawnZone zone);

    /**
     * Elimina la zona de spawn actual, borrándola del caché en memoria y de MongoDB.
     * Tras esta operación, {@link #getZone()} retornará un {@link Optional} vacío.
     *
     * @return future que se completa cuando la zona ha sido eliminada
     */
    CompletableFuture<Void> removeZone();

    /**
     * Devuelve la zona de spawn activa desde el caché en memoria de forma síncrona.
     * Esta operación no accede a la base de datos y es segura para usarla en el hilo
     * principal de Bukkit (p.ej. en listeners de movimiento).
     *
     * @return {@link Optional} con la zona de spawn si está configurada, o vacío si no hay ninguna
     */
    Optional<SpawnZone> getZone();

    /**
     * Carga la zona de spawn desde MongoDB y la almacena en el caché en memoria.
     * Debe invocarse durante la inicialización del plugin para que {@link #getZone()}
     * devuelva datos actualizados desde el primer momento.
     *
     * @return future que se completa cuando la zona ha sido cargada en memoria
     */
    CompletableFuture<Void> loadZone();
}

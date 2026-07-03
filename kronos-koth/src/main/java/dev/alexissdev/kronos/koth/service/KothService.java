package dev.alexissdev.kronos.koth.service;

import dev.alexissdev.kronos.koth.domain.KothZone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato principal de la lógica de negocio para el sistema KOTH (King of The Hill).
 *
 * <p>Define las operaciones de alto nivel disponibles sobre los eventos KOTH: iniciar, terminar,
 * capturar, crear y eliminar zonas. Todas las operaciones que acceden a persistencia son
 * asíncronas y devuelven un {@link CompletableFuture} para no bloquear el hilo principal
 * de Bukkit.</p>
 *
 * <p>La implementación por defecto es {@code KothApplicationService}, registrada en Guice
 * como singleton a través de {@code KothModule}.</p>
 */
public interface KothService {

    /**
     * Inicia un evento KOTH previamente creado, marcándolo como activo y publicando
     * el evento de dominio {@code KothStartedDomainEvent} para que otros módulos
     * puedan reaccionar (p.ej. mostrar un temporizador).
     *
     * @param name nombre único del KOTH a iniciar
     * @return future que se completa cuando el estado ha sido persistido y el evento publicado;
     *         completa excepcionalmente con {@code KothNotFoundException} si no existe,
     *         o con {@code IllegalStateException} si ya estaba activo
     */
    CompletableFuture<Void> startKoth(String name);

    /**
     * Finaliza un evento KOTH activo sin que ningún jugador lo haya capturado,
     * marcándolo como inactivo y publicando {@code KothEndedDomainEvent}.
     *
     * @param name nombre único del KOTH a finalizar
     * @return future que se completa cuando el estado ha sido persistido y el evento publicado;
     *         completa excepcionalmente con {@code KothNotFoundException} si no existe,
     *         o con {@code IllegalStateException} si no estaba activo
     */
    CompletableFuture<Void> endKoth(String name);

    /**
     * Busca una zona KOTH por su nombre.
     *
     * @param name nombre único del KOTH
     * @return future con un {@link Optional} que contiene la zona si existe, o vacío si no
     */
    CompletableFuture<Optional<KothZone>> getKoth(String name);

    /**
     * Devuelve la lista de todas las zonas KOTH que están actualmente activas.
     *
     * @return future con la lista de zonas activas; puede ser vacía si ningún KOTH está corriendo
     */
    CompletableFuture<List<KothZone>> getActiveKoths();

    /**
     * Devuelve la lista completa de zonas KOTH registradas, tanto activas como inactivas.
     *
     * @return future con todas las zonas KOTH persistidas en la base de datos
     */
    CompletableFuture<List<KothZone>> getAllKoths();

    /**
     * Registra la captura exitosa de un KOTH por un jugador. Marca la zona como inactiva
     * y publica {@code KothCapturedDomainEvent} con el UUID del captor para que el sistema
     * de recompensas pueda entregar el cofre correspondiente.
     *
     * @param name       nombre único del KOTH capturado
     * @param captorUuid UUID del jugador que realizó la captura
     * @return future que se completa cuando la persistencia y el evento han sido procesados
     */
    CompletableFuture<Void> captureKoth(String name, UUID captorUuid);

    /**
     * Persiste una nueva zona KOTH construida durante una sesión de creación administrativa.
     *
     * @param zone la zona KOTH ya configurada con sus límites de claim y captura
     * @return future que se completa cuando la zona ha sido guardada exitosamente
     */
    CompletableFuture<Void> createKoth(KothZone zone);

    /**
     * Elimina permanentemente una zona KOTH de la base de datos y publica
     * {@code KothDeletedDomainEvent} para que los sistemas dependientes puedan limpiar
     * su estado (p.ej. cancelar temporizadores activos).
     *
     * @param name nombre único del KOTH a eliminar
     * @return future que se completa cuando la eliminación y el evento han sido procesados
     */
    CompletableFuture<Void> deleteKoth(String name);
}

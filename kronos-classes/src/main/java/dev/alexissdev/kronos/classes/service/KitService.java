package dev.alexissdev.kronos.classes.service;

import dev.alexissdev.kronos.players.domain.KitType;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de aplicación que define las operaciones disponibles sobre las clases (kits) de jugadores.
 *
 * <p>Abstrae la lógica de detección del kit activo, la gestión del cooldown de habilidades y
 * la persistencia del kit seleccionado. La implementación principal es
 * {@link dev.alexissdev.kronos.classes.KitApplicationService}, inyectada por Guice.
 * Todas las operaciones son asíncronas.</p>
 */
public interface KitService {

    /**
     * Recupera el kit actualmente activo del jugador consultando su perfil persistido.
     *
     * <p>Se usa al conectar el jugador para inicializar el caché local de
     * {@link dev.alexissdev.kronos.classes.listener.ClassListener}.</p>
     *
     * @param playerUuid UUID del jugador
     * @return futuro con el {@link KitType} activo, o vacío si el jugador no tiene kit asignado
     */
    CompletableFuture<Optional<KitType>> detectKit(UUID playerUuid);

    /**
     * Registra la activación de la habilidad activa de una clase e inicia su cooldown.
     *
     * <p>Arranca el temporizador {@code CLASS_COOLDOWN} para el jugador, impidiendo que
     * la habilidad vuelva a usarse hasta que el cooldown expire.</p>
     *
     * @param playerUuid UUID del jugador que activa la habilidad
     * @param kitType    clase cuya habilidad se activó
     * @return futuro que se completa cuando el cooldown ha sido registrado
     */
    CompletableFuture<Void> activateClassAbility(UUID playerUuid, KitType kitType);

    /**
     * Comprueba si la habilidad activa del jugador está actualmente en cooldown.
     *
     * @param playerUuid UUID del jugador a consultar
     * @return futuro con {@code true} si el cooldown está activo; {@code false} en caso contrario
     */
    CompletableFuture<Boolean> isClassAbilityOnCooldown(UUID playerUuid);

    /**
     * Actualiza y persiste el kit activo del jugador.
     *
     * <p>Se invoca cuando el jugador cambia de casco para sincronizar el cambio de clase
     * detectado en tiempo real con el perfil almacenado en la base de datos.</p>
     *
     * @param playerUuid UUID del jugador
     * @param kitType    nuevo kit que el jugador ha equipado
     * @return futuro que se completa cuando el perfil ha sido actualizado
     * @throws dev.alexissdev.kronos.players.exception.PlayerNotFoundException si el jugador no tiene perfil registrado
     */
    CompletableFuture<Void> updateActiveKit(UUID playerUuid, KitType kitType);
}

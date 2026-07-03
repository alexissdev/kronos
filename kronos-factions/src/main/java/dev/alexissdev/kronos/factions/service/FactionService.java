package dev.alexissdev.kronos.factions.service;

import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.domain.FactionHome;
import dev.alexissdev.kronos.factions.domain.FactionRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Fachada de aplicación que expone todas las operaciones de negocio relacionadas
 * con las facciones del servidor HCF.
 *
 * <p>Define el contrato que deben implementar los servicios de facciones, de modo
 * que los comandos, listeners y otros módulos del plugin solo dependan de esta
 * interfaz y no de la implementación concreta.
 *
 * <p>La implementación principal es
 * {@link dev.alexissdev.kronos.factions.FactionApplicationService}, registrada en
 * el contenedor de Guice mediante {@link dev.alexissdev.kronos.factions.FactionsModule}.
 *
 * <p>Todos los métodos son asíncronos y devuelven {@link CompletableFuture} para
 * evitar bloquear el hilo principal del servidor Spigot durante operaciones de I/O.
 */
public interface FactionService {

    /**
     * Crea una nueva facción con el nombre dado y asigna al jugador indicado como líder.
     *
     * <p>Verifica que el jugador no pertenezca ya a otra facción y que el nombre
     * no esté en uso antes de persistir la nueva facción. Publica
     * {@link dev.alexissdev.kronos.factions.event.FactionCreatedDomainEvent} al completarse.
     *
     * @param name     nombre único para la nueva facción
     * @param leaderId UUID del jugador que creará y liderará la facción
     * @return futuro con la instancia de la {@link Faction} recién creada
     * @throws dev.alexissdev.kronos.common.exception.HCFException si el jugador ya está en una facción
     *         o si el nombre ya está en uso
     */
    CompletableFuture<Faction> createFaction(String name, UUID leaderId);

    /**
     * Disuelve permanentemente una facción.
     *
     * <p>Solo el líder puede disolver su propia facción. Publica
     * {@link dev.alexissdev.kronos.factions.event.FactionDisbandedDomainEvent} al completarse.
     *
     * @param factionId ID de la facción a disolver
     * @param actorUuid UUID del jugador que ejecuta la acción (debe ser el líder)
     * @return futuro que se completa cuando la facción ha sido eliminada
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException   si la facción no existe
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si el actor no es el líder
     */
    CompletableFuture<Void> disbandFaction(String factionId, UUID actorUuid);

    /**
     * Cambia el nombre visible de una facción.
     *
     * <p>Solo el líder puede renombrar la facción.
     *
     * @param factionId ID de la facción a renombrar
     * @param newName   nuevo nombre deseado
     * @param actorUuid UUID del jugador que ejecuta la acción (debe ser el líder)
     * @return futuro que se completa cuando el cambio ha sido persistido
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException   si la facción no existe
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si el actor no es el líder
     */
    CompletableFuture<Void> renameFaction(String factionId, String newName, UUID actorUuid);

    /**
     * Envía una invitación a un jugador para que se una a la facción.
     *
     * <p>La invitación queda pendiente en memoria hasta que el jugador la acepte
     * o expire. Valida que la facción no esté llena ni congelada, y que el jugador
     * no esté en cooldown de re-invitación.
     *
     * @param factionId   ID de la facción que extiende la invitación
     * @param inviteeUuid UUID del jugador invitado
     * @param actorUuid   UUID del miembro que envía la invitación (requiere al menos CAPTAIN)
     * @return futuro que se completa cuando la invitación ha sido registrada
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException   si la facción no existe
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si el actor no tiene rango suficiente
     * @throws dev.alexissdev.kronos.common.exception.HCFException si la facción está congelada, llena
     *         o el jugador ya está en una facción
     */
    CompletableFuture<Void> inviteMember(String factionId, UUID inviteeUuid, UUID actorUuid);

    /**
     * Acepta una invitación pendiente y une al jugador a la facción correspondiente.
     *
     * <p>Verifica que la invitación no haya expirado y que la facción aún tenga
     * espacio disponible. Publica
     * {@link dev.alexissdev.kronos.factions.event.PlayerJoinedFactionDomainEvent} al completarse.
     *
     * @param playerUuid UUID del jugador que acepta la invitación
     * @param factionId  ID de la facción cuya invitación se acepta
     * @return futuro que se completa cuando el jugador ha sido añadido como miembro
     * @throws dev.alexissdev.kronos.common.exception.HCFException si no hay invitación pendiente,
     *         la invitación expiró o la facción está llena
     */
    CompletableFuture<Void> acceptInvite(UUID playerUuid, String factionId);

    /**
     * Expulsa a un miembro de la facción.
     *
     * <p>Un CAPTAIN puede expulsar MEMBERS; para expulsar a un CAPTAIN o superior
     * se requiere rango CO_LEADER. Un miembro no puede expulsarse a sí mismo.
     * Publica {@link dev.alexissdev.kronos.factions.event.PlayerLeftFactionDomainEvent} al completarse.
     *
     * @param factionId  ID de la facción de la que se expulsará al miembro
     * @param targetUuid UUID del jugador a expulsar
     * @param actorUuid  UUID del miembro que ejecuta la expulsión
     * @return futuro que se completa cuando el miembro ha sido eliminado de la facción
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException   si la facción no existe
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si el actor no tiene rango suficiente
     * @throws dev.alexissdev.kronos.common.exception.HCFException si el actor intenta expulsarse a sí mismo
     */
    CompletableFuture<Void> kickMember(String factionId, UUID targetUuid, UUID actorUuid);

    /**
     * Permite a un jugador abandonar voluntariamente su facción actual.
     *
     * <p>El líder no puede usar este método; primero debe transferir el liderazgo
     * o disolver la facción. Publica
     * {@link dev.alexissdev.kronos.factions.event.PlayerLeftFactionDomainEvent} al completarse.
     *
     * @param playerUuid UUID del jugador que desea abandonar su facción
     * @return futuro que se completa cuando el jugador ha sido retirado de la facción
     * @throws dev.alexissdev.kronos.common.exception.HCFException si el jugador no está en ninguna
     *         facción o es el líder
     */
    CompletableFuture<Void> leaveFaction(UUID playerUuid);

    /**
     * Cambia el rol de un miembro dentro de la facción.
     *
     * <p>El actor debe ser CO_LEADER o superior, y no puede asignar un rol igual
     * o mayor al suyo propio.
     *
     * @param factionId  ID de la facción donde se realiza el cambio de rol
     * @param targetUuid UUID del miembro cuyo rol se cambiará
     * @param role       nuevo rol a asignar
     * @param actorUuid  UUID del miembro que ejecuta el cambio (requiere al menos CO_LEADER)
     * @return futuro que se completa cuando el cambio ha sido persistido
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException   si la facción no existe
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si el actor no tiene rango suficiente
     * @throws dev.alexissdev.kronos.common.exception.HCFException si el rol asignado es igual o superior al del actor
     */
    CompletableFuture<Void> setRole(String factionId, UUID targetUuid, FactionRole role, UUID actorUuid);

    /**
     * Establece una alianza mutua entre dos facciones.
     *
     * <p>Elimina cualquier relación de enemistad previa entre ambas y registra la alianza
     * de forma bidireccional. El actor debe ser CO_LEADER o superior en la facción A.
     *
     * @param factionAId ID de la facción que propone la alianza (el actor debe pertenecer a esta)
     * @param factionBId ID de la facción con la que se aliará
     * @param actorUuid  UUID del jugador que ejecuta la acción (requiere al menos CO_LEADER en factionA)
     * @return futuro que se completa cuando ambas facciones han sido actualizadas
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException   si alguna de las facciones no existe
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si el actor no tiene rango suficiente
     * @throws dev.alexissdev.kronos.common.exception.HCFException si las dos facciones son la misma
     */
    CompletableFuture<Void> setAlly(String factionAId, String factionBId, UUID actorUuid);

    /**
     * Declara enemistad mutua entre dos facciones.
     *
     * <p>Elimina cualquier relación de alianza previa y registra la enemistad
     * de forma bidireccional.
     *
     * @param factionAId ID de la facción que declara la enemistad (el actor debe pertenecer a esta)
     * @param factionBId ID de la facción objetivo de la declaración
     * @param actorUuid  UUID del jugador que ejecuta la acción (requiere al menos CO_LEADER en factionA)
     * @return futuro que se completa cuando ambas facciones han sido actualizadas
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException   si alguna de las facciones no existe
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si el actor no tiene rango suficiente
     * @throws dev.alexissdev.kronos.common.exception.HCFException si las dos facciones son la misma
     */
    CompletableFuture<Void> setEnemy(String factionAId, String factionBId, UUID actorUuid);

    /**
     * Elimina cualquier relación diplomática (alianza o enemistad) entre dos facciones.
     *
     * <p>Después de esta operación ambas facciones quedan en estado neutral entre sí.
     *
     * @param factionAId ID de la facción que inicia la neutralización (el actor debe pertenecer a esta)
     * @param factionBId ID de la facción con la que se rompe la relación
     * @param actorUuid  UUID del jugador que ejecuta la acción (requiere al menos CO_LEADER en factionA)
     * @return futuro que se completa cuando ambas facciones han sido actualizadas
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException   si alguna de las facciones no existe
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si el actor no tiene rango suficiente
     */
    CompletableFuture<Void> removeRelation(String factionAId, String factionBId, UUID actorUuid);

    /**
     * Transfiere dinero desde la cuenta personal del jugador al balance de la facción.
     *
     * <p>La facción no puede estar congelada para recibir depósitos. El dinero
     * se retira de la cuenta del jugador mediante el {@code EconomyService}.
     *
     * @param factionId  ID de la facción que recibirá el depósito
     * @param playerUuid UUID del jugador que realiza el depósito
     * @param amount     cantidad a depositar (debe ser mayor a 0)
     * @return futuro que se completa cuando el depósito ha sido procesado
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException si la facción no existe
     * @throws dev.alexissdev.kronos.common.exception.HCFException si la facción está congelada o el monto es inválido
     * @throws dev.alexissdev.kronos.economy.exception.InsufficientFundsException si el jugador no tiene saldo suficiente
     */
    CompletableFuture<Void> deposit(String factionId, UUID playerUuid, double amount);

    /**
     * Transfiere dinero desde el balance de la facción a la cuenta personal del jugador.
     *
     * <p>Solo los miembros con rango CO_LEADER o superior pueden retirar fondos.
     *
     * @param factionId  ID de la facción de la que se retiran fondos
     * @param playerUuid UUID del jugador que realiza el retiro (requiere al menos CO_LEADER)
     * @param amount     cantidad a retirar (debe ser mayor a 0 y no superar el balance)
     * @return futuro que se completa cuando el retiro ha sido procesado
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException   si la facción no existe
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si el jugador no tiene rango suficiente
     * @throws dev.alexissdev.kronos.economy.exception.InsufficientFundsException  si la facción no tiene saldo suficiente
     */
    CompletableFuture<Void> withdraw(String factionId, UUID playerUuid, double amount);

    /**
     * Busca la facción a la que pertenece un jugador dado.
     *
     * @param playerUuid UUID del jugador
     * @return futuro con la facción del jugador, o {@link Optional#empty()} si no está en ninguna
     */
    CompletableFuture<Optional<Faction>> getByPlayer(UUID playerUuid);

    /**
     * Busca una facción por su ID único.
     *
     * @param id ID de la facción
     * @return futuro con la facción si existe, o {@link Optional#empty()} si no
     */
    CompletableFuture<Optional<Faction>> getById(String id);

    /**
     * Busca una facción por su nombre (sin distinción de mayúsculas/minúsculas).
     *
     * @param name nombre de la facción
     * @return futuro con la facción si existe, o {@link Optional#empty()} si no
     */
    CompletableFuture<Optional<Faction>> getByName(String name);

    /**
     * Devuelve las facciones con más kills del servidor, limitadas por el parámetro dado.
     *
     * @param limit número máximo de facciones a devolver
     * @return futuro con la lista de facciones ordenada por kills descendente
     */
    CompletableFuture<List<Faction>> getTopFactions(int limit);

    /**
     * Devuelve todas las facciones que actualmente están en estado raideable.
     *
     * @return futuro con la lista de facciones raideables
     */
    CompletableFuture<List<Faction>> getRaidableFactions();

    /**
     * Notifica al sistema que un miembro de la facción ha muerto.
     *
     * <p>Incrementa el contador de muertes de la facción. Si el jugador muerto es
     * miembro activo, también decrementa el DTK. Si el DTK llega a 0, marca la facción
     * como raideable y publica {@link dev.alexissdev.kronos.factions.event.FactionRaidableDomainEvent}.
     * Siempre que el DTK cambia se publica {@link dev.alexissdev.kronos.factions.event.FactionDtkDecrementedDomainEvent}.
     *
     * @param factionId     ID de la facción afectada
     * @param deadMemberUuid UUID del jugador que murió
     * @return futuro que se completa cuando el estado de la facción ha sido actualizado
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException si la facción no existe
     */
    CompletableFuture<Void> notifyMemberDeath(String factionId, UUID deadMemberUuid);

    /**
     * Establece o actualiza el punto de hogar de la facción.
     *
     * <p>Los miembros con rango CAPTAIN o superior pueden mover el hogar de la facción.
     *
     * @param factionId ID de la facción cuyo hogar se actualizará
     * @param actorUuid UUID del jugador que establece el hogar (requiere al menos CAPTAIN)
     * @param home      nueva ubicación del hogar
     * @return futuro que se completa cuando el hogar ha sido guardado
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException   si la facción no existe
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si el actor no tiene rango suficiente
     */
    CompletableFuture<Void> setFactionHome(String factionId, UUID actorUuid, FactionHome home);

    /**
     * Elimina el punto de hogar de la facción.
     *
     * @param factionId ID de la facción cuyo hogar se eliminará
     * @param actorUuid UUID del jugador que elimina el hogar (requiere al menos CAPTAIN)
     * @return futuro que se completa cuando el hogar ha sido eliminado
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException   si la facción no existe
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si el actor no tiene rango suficiente
     */
    CompletableFuture<Void> clearFactionHome(String factionId, UUID actorUuid);

    /**
     * Agrega un strike administrativo a la facción.
     *
     * <p>Si la facción alcanza el número máximo de strikes, es disuelta automáticamente
     * y se publica {@link dev.alexissdev.kronos.factions.event.FactionDisbandedDomainEvent}.
     *
     * @param factionId ID de la facción que recibirá el strike
     * @param reason    motivo del strike (para registro de auditoría)
     * @param actorUuid UUID del administrador que aplica el strike
     * @return futuro que se completa cuando el strike ha sido procesado
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException si la facción no existe
     */
    CompletableFuture<Void> addStrike(String factionId, String reason, UUID actorUuid);

    /**
     * Congela una facción, impidiendo que reciba nuevos miembros o depósitos económicos.
     *
     * <p>Operación típicamente ejecutada por un administrador del servidor.
     *
     * @param factionId ID de la facción a congelar
     * @param actorUuid UUID del administrador que ejecuta la acción
     * @return futuro que se completa cuando la facción ha sido congelada
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException si la facción no existe
     */
    CompletableFuture<Void> freezeFaction(String factionId, UUID actorUuid);

    /**
     * Descongela una facción, restaurando su capacidad de recibir miembros y depósitos.
     *
     * @param factionId ID de la facción a descongelar
     * @param actorUuid UUID del administrador que ejecuta la acción
     * @return futuro que se completa cuando la facción ha sido descongelada
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException si la facción no existe
     */
    CompletableFuture<Void> unfreezeFaction(String factionId, UUID actorUuid);

    /**
     * Transfiere el liderazgo de la facción a otro miembro.
     *
     * <p>El antiguo líder desciende a rango CO_LEADER. Solo el líder actual puede
     * ejecutar esta transferencia.
     *
     * @param factionId      ID de la facción donde se realizará el cambio de liderazgo
     * @param newLeaderUuid  UUID del miembro que recibirá el rango de LEADER
     * @param actorUuid      UUID del líder actual que transfiere el mando
     * @return futuro que se completa cuando el liderazgo ha sido transferido y persistido
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException   si la facción no existe
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException si el actor no es el líder
     * @throws dev.alexissdev.kronos.common.exception.HCFException si el actor y el nuevo líder son la misma persona
     *         o el nuevo líder no es miembro de la facción
     */
    CompletableFuture<Void> setLeader(String factionId, UUID newLeaderUuid, UUID actorUuid);
}

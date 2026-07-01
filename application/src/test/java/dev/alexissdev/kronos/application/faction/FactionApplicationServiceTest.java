package dev.alexissdev.kronos.application.faction;

import com.google.common.eventbus.EventBus;
import dev.alexissdev.kronos.core.domain.*;
import dev.alexissdev.kronos.core.exception.FactionNotFoundException;
import dev.alexissdev.kronos.core.exception.FactionPermissionException;
import dev.alexissdev.kronos.core.repository.FactionRepository;
import dev.alexissdev.kronos.core.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FactionApplicationServiceTest {

    @Mock private FactionRepository factionRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private EventBus eventBus;

    private FactionApplicationService service;

    @BeforeEach
    void setUp() {
        service = new FactionApplicationService(factionRepository, playerRepository, eventBus);
    }

    @Test
    void createFaction_whenNameAvailable_shouldSaveFactionAndPostEvent() throws Exception {
        UUID leaderId = UUID.randomUUID();
        when(factionRepository.findByName("TestFaction"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(factionRepository.save(any(Faction.class)))
                .thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));

        Faction result = service.createFaction("TestFaction", leaderId).get();

        assertNotNull(result.getId());
        assertEquals("TestFaction", result.getName());
        assertEquals(leaderId, result.getLeaderId());
        assertTrue(result.hasMember(leaderId));
        verify(eventBus).post(any());
    }

    @Test
    void createFaction_whenNameTaken_shouldThrow() {
        UUID leaderId = UUID.randomUUID();
        Faction existing = new Faction("id", "TestFaction", leaderId, 20, Instant.now());
        when(factionRepository.findByName("TestFaction"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(existing)));

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> service.createFaction("TestFaction", leaderId).get());
        assertTrue(ex.getCause().getMessage().contains("already taken"));
    }

    @Test
    void disbandFaction_whenLeader_shouldDeleteAndPostEvent() throws Exception {
        UUID leaderId = UUID.randomUUID();
        Faction faction = createFactionWithLeader(leaderId);

        when(factionRepository.findById("factionId"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(faction)));
        when(factionRepository.delete("factionId"))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.disbandFaction("factionId", leaderId).get();

        verify(factionRepository).delete("factionId");
        verify(eventBus).post(any());
    }

    @Test
    void disbandFaction_whenNotLeader_shouldThrowPermissionException() {
        UUID leaderId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        Faction faction = createFactionWithLeader(leaderId);
        faction.addMember(new FactionMember(memberId, FactionRole.MEMBER, Instant.now()));

        when(factionRepository.findById("factionId"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(faction)));

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> service.disbandFaction("factionId", memberId).get());
        assertTrue(ex.getCause() instanceof FactionPermissionException);
    }

    @Test
    void disbandFaction_whenFactionNotFound_shouldThrow() {
        when(factionRepository.findById("missing"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> service.disbandFaction("missing", UUID.randomUUID()).get());
        assertTrue(ex.getCause() instanceof FactionNotFoundException);
    }

    @Test
    void acceptInvite_shouldAddMemberAndPostEvent() throws Exception {
        UUID leaderId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        Faction faction = createFactionWithLeader(leaderId);

        when(factionRepository.findById("factionId"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(faction)));
        when(factionRepository.save(any(Faction.class)))
                .thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));

        service.acceptInvite(inviteeId, "factionId").get();

        assertTrue(faction.hasMember(inviteeId));
        verify(eventBus).post(any());
    }

    private Faction createFactionWithLeader(UUID leaderId) {
        Faction faction = new Faction("factionId", "TestFaction", leaderId, 20, Instant.now());
        faction.addMember(new FactionMember(leaderId, FactionRole.LEADER, Instant.now()));
        return faction;
    }
}

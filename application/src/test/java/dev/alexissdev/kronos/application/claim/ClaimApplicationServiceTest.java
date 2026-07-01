package dev.alexissdev.kronos.application.claim;

import com.google.common.eventbus.EventBus;
import dev.alexissdev.kronos.core.domain.*;
import dev.alexissdev.kronos.core.exception.ClaimConflictException;
import dev.alexissdev.kronos.core.repository.ClaimRepository;
import dev.alexissdev.kronos.core.repository.FactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimApplicationServiceTest {

    @Mock private ClaimRepository claimRepository;
    @Mock private FactionRepository factionRepository;
    @Mock private EventBus eventBus;

    private ClaimApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ClaimApplicationService(claimRepository, factionRepository, eventBus);
    }

    @Test
    void claim_whenAreaFree_shouldSaveClaimAndPostEvent() throws Exception {
        UUID leaderId = UUID.randomUUID();
        Faction faction = new Faction("fId", "TestFaction", leaderId, 20, Instant.now());
        faction.addMember(new FactionMember(leaderId, FactionRole.LEADER, Instant.now()));

        when(factionRepository.findById("fId"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(faction)));
        when(claimRepository.findByChunk("world", 0, 0))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(claimRepository.save(any(Claim.class)))
                .thenAnswer(inv -> CompletableFuture.completedFuture(inv.getArgument(0)));

        Claim result = service.claim("fId", leaderId, "world", 0, 0, 2, 2).get();

        assertNotNull(result.getId());
        assertEquals("fId", result.getFactionId());
        assertEquals(ClaimType.FACTION, result.getType());
        verify(eventBus).post(any());
    }

    @Test
    void claim_whenAreaAlreadyClaimed_shouldThrowConflict() {
        UUID leaderId = UUID.randomUUID();
        Faction faction = new Faction("fId", "TestFaction", leaderId, 20, Instant.now());

        when(factionRepository.findById("fId"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(faction)));
        Claim existing = new Claim("cId", "other", ClaimType.FACTION, "world", 0, 0, 2, 2);
        when(claimRepository.findByChunk("world", 0, 0))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(existing)));

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> service.claim("fId", leaderId, "world", 0, 0, 2, 2).get());
        assertTrue(ex.getCause() instanceof ClaimConflictException);
    }

    @Test
    void getClaimTypeAt_whenNoClaim_shouldReturnWilderness() throws Exception {
        when(claimRepository.findByChunk("world", 5, 5))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        ClaimType type = service.getClaimTypeAt("world", 5, 5).get();

        assertEquals(ClaimType.WILDERNESS, type);
    }
}

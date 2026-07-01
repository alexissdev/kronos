package dev.alexissdev.kronos.application.timer;

import com.google.common.eventbus.EventBus;
import dev.alexissdev.kronos.core.domain.Timer;
import dev.alexissdev.kronos.core.domain.TimerType;
import dev.alexissdev.kronos.core.repository.TimerRepository;
import dev.alexissdev.kronos.application.timer.TimerCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimerApplicationServiceTest {

    @Mock private TimerRepository timerRepository;
    @Mock private TimerCache timerCache;
    @Mock private EventBus eventBus;

    private TimerApplicationService service;

    @BeforeEach
    void setUp() {
        service = new TimerApplicationService(timerRepository, timerCache, eventBus);
    }

    @Test
    void startTimer_shouldDelegateToRepository() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        when(timerRepository.saveTimer(any(Timer.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.startTimer(playerUuid, TimerType.COMBAT_TAG, 30_000L).get();

        verify(timerRepository).saveTimer(argThat(t ->
                t.getPlayerUuid().equals(playerUuid) && t.getType() == TimerType.COMBAT_TAG));
    }

    @Test
    void hasActiveTimer_whenTimerExists_shouldReturnTrue() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        Timer activeTimer = new Timer(playerUuid, TimerType.PVP_TIMER,
                Instant.now().plusSeconds(300));
        when(timerRepository.findTimer(playerUuid, TimerType.PVP_TIMER))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(activeTimer)));

        boolean result = service.hasActiveTimer(playerUuid, TimerType.PVP_TIMER).get();

        assertTrue(result);
    }

    @Test
    void hasActiveTimer_whenNoTimer_shouldReturnFalse() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        when(timerRepository.findTimer(playerUuid, TimerType.ENDERPEARL))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        boolean result = service.hasActiveTimer(playerUuid, TimerType.ENDERPEARL).get();

        assertFalse(result);
    }

    @Test
    void getRemainingMillis_whenActiveTimer_shouldReturnPositiveValue() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        Timer timer = new Timer(playerUuid, TimerType.HOME, Instant.now().plusSeconds(60));
        when(timerRepository.findTimer(playerUuid, TimerType.HOME))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(timer)));

        OptionalLong result = service.getRemainingMillis(playerUuid, TimerType.HOME).get();

        assertTrue(result.isPresent());
        assertTrue(result.getAsLong() > 0);
    }

    @Test
    void tagForCombat_shouldPostEventAndStartTimersForBothPlayers() throws Exception {
        UUID tagged = UUID.randomUUID();
        UUID tagger = UUID.randomUUID();
        when(timerRepository.saveTimer(any(Timer.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.tagForCombat(tagged, tagger).get();

        verify(timerRepository, times(2)).saveTimer(any(Timer.class));
        verify(eventBus).post(any());
    }

    @Test
    void cancelTimer_shouldDeleteFromRepositoryAndPostExpiredEvent() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        when(timerRepository.deleteTimer(playerUuid, TimerType.COMBAT_TAG))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.cancelTimer(playerUuid, TimerType.COMBAT_TAG).get();

        verify(timerRepository).deleteTimer(playerUuid, TimerType.COMBAT_TAG);
        verify(eventBus).post(any());
    }
}

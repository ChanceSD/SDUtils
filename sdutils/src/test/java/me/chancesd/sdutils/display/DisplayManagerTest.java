package me.chancesd.sdutils.display;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import me.chancesd.sdutils.scheduler.ScheduleUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DisplayManagerTest {

    private DisplayManager displayManager;
    private Player mockPlayer;
    private CountdownData mockCountdownData;
    private ScheduledExecutorService mockExecutor;
	private ScheduledFuture<?> mockFuture;

    @BeforeEach
    @SuppressWarnings("resource")
    void setUp() {
        displayManager = new DisplayManager();
        mockPlayer = Mockito.mock(Player.class);
        mockCountdownData = Mockito.mock(CountdownData.class);
        mockExecutor = Mockito.mock(ScheduledExecutorService.class);
        mockFuture = Mockito.mock(ScheduledFuture.class);

        ScheduleUtils.setExecutor(mockExecutor);
		Mockito.doReturn(mockFuture).when(mockExecutor).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testCreateCountdown() {
        displayManager.createCountdown(mockPlayer, mockCountdownData);
        assertTrue(displayManager.countdowns.containsKey(mockPlayer));
        assertTrue(displayManager.countdowns.get(mockPlayer).contains(mockCountdownData));
    }

    @Test
    void testCancelCountdown() {
        displayManager.createCountdown(mockPlayer, mockCountdownData);
        displayManager.cancelCountdown(mockPlayer, mockCountdownData);
        assertFalse(displayManager.countdowns.containsKey(mockPlayer));
    }

    @Test
    void testCancelAllCountdown() {
        displayManager.createCountdown(mockPlayer, mockCountdownData);
        displayManager.cancelAllCountdown(mockPlayer);
        assertFalse(displayManager.countdowns.containsKey(mockPlayer));
    }

    @Test
    void testRun() {
        when(mockCountdownData.update()).thenReturn(true);
        displayManager.createCountdown(mockPlayer, mockCountdownData);
        displayManager.run();
        assertFalse(displayManager.countdowns.containsKey(mockPlayer));
    }
}

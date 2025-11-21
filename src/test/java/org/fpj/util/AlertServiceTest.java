package org.fpj.util;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {

    @InjectMocks
    private AlertService underTest;

    @BeforeAll
    public static void initToolkit() {
        // JavaFX Toolkit initialisieren (falls noch nicht geschehen)
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit bereits initialisiert
        }
    }

    @Test
    public void testInfoCallsPlatformRunLater() {
        try (MockedStatic<Platform> platformMock = mockStatic(Platform.class)) {
            underTest.info("Test Title", "Test Header", "Test Message");

            platformMock.verify(() -> Platform.runLater(any(Runnable.class)), times(1));
        }
    }

    @Test
    public void testWarnCallsPlatformRunLater() {
        try (MockedStatic<Platform> platformMock = mockStatic(Platform.class)) {
            underTest.warn("Test Title", "Test Header", "Test Message");

            platformMock.verify(() -> Platform.runLater(any(Runnable.class)), times(1));
        }
    }

    @Test
    public void testErrorCallsPlatformRunLater() {
        try (MockedStatic<Platform> platformMock = mockStatic(Platform.class)) {
            underTest.error("Test Title", "Test Header", "Test Message");

            platformMock.verify(() -> Platform.runLater(any(Runnable.class)), times(1));
        }
    }
}

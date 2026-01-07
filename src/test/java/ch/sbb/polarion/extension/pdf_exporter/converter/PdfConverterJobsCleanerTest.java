package ch.sbb.polarion.extension.pdf_exporter.converter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfConverterJobsCleanerTest {

    @AfterEach
    void tearDown() {
        PdfConverterJobsCleaner.stopCleaningJob();
    }

    @Test
    void startCleaningJobShouldScheduleCleanup() {
        ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);

        try (MockedStatic<Executors> executorsMockedStatic = mockStatic(Executors.class);
             MockedConstruction<PropertiesUtility> propertiesUtilityMockedConstruction = mockConstruction(PropertiesUtility.class, (mock, context) ->
                     when(mock.getFinishedJobTimeout()).thenReturn(30)
             )) {

            executorsMockedStatic.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockExecutor);

            PdfConverterJobsCleaner.startCleaningJob();

            verify(mockExecutor).scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(30L), eq(TimeUnit.MINUTES));
        }
    }

    @Test
    void startCleaningJobShouldNotStartTwice() {
        ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);

        try (MockedStatic<Executors> executorsMockedStatic = mockStatic(Executors.class);
             MockedConstruction<PropertiesUtility> propertiesUtilityMockedConstruction = mockConstruction(PropertiesUtility.class, (mock, context) ->
                     when(mock.getFinishedJobTimeout()).thenReturn(30)
             )) {

            executorsMockedStatic.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockExecutor);

            PdfConverterJobsCleaner.startCleaningJob();
            PdfConverterJobsCleaner.startCleaningJob();

            // Should only be called once
            executorsMockedStatic.verify(Executors::newSingleThreadScheduledExecutor, times(1));
            verify(mockExecutor, times(1)).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        }
    }

    @Test
    void stopCleaningJobShouldShutdownExecutor() {
        ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);

        try (MockedStatic<Executors> executorsMockedStatic = mockStatic(Executors.class);
             MockedConstruction<PropertiesUtility> propertiesUtilityMockedConstruction = mockConstruction(PropertiesUtility.class, (mock, context) ->
                     when(mock.getFinishedJobTimeout()).thenReturn(30)
             )) {

            executorsMockedStatic.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockExecutor);

            PdfConverterJobsCleaner.startCleaningJob();
            PdfConverterJobsCleaner.stopCleaningJob();

            verify(mockExecutor).shutdown();
        }
    }

    @Test
    void stopCleaningJobShouldDoNothingWhenNotStarted() {
        // Should not throw any exception when called without starting
        assertDoesNotThrow(PdfConverterJobsCleaner::stopCleaningJob);
    }

    @Test
    void stopCleaningJobShouldAllowRestart() {
        ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);

        try (MockedStatic<Executors> executorsMockedStatic = mockStatic(Executors.class);
             MockedConstruction<PropertiesUtility> propertiesUtilityMockedConstruction = mockConstruction(PropertiesUtility.class, (mock, context) ->
                     when(mock.getFinishedJobTimeout()).thenReturn(30)
             )) {

            executorsMockedStatic.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockExecutor);

            PdfConverterJobsCleaner.startCleaningJob();
            PdfConverterJobsCleaner.stopCleaningJob();
            PdfConverterJobsCleaner.startCleaningJob();

            // Should be called twice (once for each start)
            executorsMockedStatic.verify(Executors::newSingleThreadScheduledExecutor, times(2));
            verify(mockExecutor, times(2)).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        }
    }
}

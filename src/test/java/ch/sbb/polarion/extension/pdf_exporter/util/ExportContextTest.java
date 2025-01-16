package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ExportContextTest {

    @BeforeEach
    void setUp() {
        ExportContext.clear();
    }

    @Test
    void addWorkItemIDsWithMissingAttachmentTest() {
        ExportContext.addWorkItemIDsWithMissingAttachment("WorkItem1");
        ExportContext.addWorkItemIDsWithMissingAttachment("WorkItem2");

        List<String> warnings = ExportContext.getWorkItemIDsWithMissingAttachment();

        assertNotNull(warnings);
        assertEquals(2, warnings.size());
        assertTrue(warnings.contains("WorkItem1"));
        assertTrue(warnings.contains("WorkItem2"));
    }

    @Test
    void clearTest() {
        ExportContext.addWorkItemIDsWithMissingAttachment("WorkItem1");
        ExportContext.clear();

        List<String> warnings = ExportContext.getWorkItemIDsWithMissingAttachment();

        assertNotNull(warnings);
        assertTrue(warnings.isEmpty());
    }

    @Test
    @SneakyThrows
    void threadLocalIsolationTest() {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        ExportContext.addWorkItemIDsWithMissingAttachment("MainThreadWorkItem");

        Future<List<String>> threadResult = executor.submit(() -> {
            ExportContext.addWorkItemIDsWithMissingAttachment("ThreadWorkItem1");
            ExportContext.addWorkItemIDsWithMissingAttachment("ThreadWorkItem2");
            return ExportContext.getWorkItemIDsWithMissingAttachment();
        });

        List<String> mainThreadWarnings = ExportContext.getWorkItemIDsWithMissingAttachment();

        List<String> otherThreadWarnings = threadResult.get();

        assertNotNull(mainThreadWarnings);
        assertEquals(1, mainThreadWarnings.size());
        assertTrue(mainThreadWarnings.contains("MainThreadWorkItem"));

        assertNotNull(otherThreadWarnings);
        assertEquals(2, otherThreadWarnings.size());
        assertTrue(otherThreadWarnings.contains("ThreadWorkItem1"));
        assertTrue(otherThreadWarnings.contains("ThreadWorkItem2"));

        executor.shutdown();
    }
}

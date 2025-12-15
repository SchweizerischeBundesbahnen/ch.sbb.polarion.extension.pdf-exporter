package ch.sbb.polarion.extension.pdf_exporter.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class HtmlLoggerTest {

    private static final Path TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir"));

    private final HtmlLogger htmlLogger = new HtmlLogger();

    @BeforeEach
    void setUp() throws IOException {
        // Clean up any leftover pdf-exporter directories from previous test runs
        cleanupPdfExporterDirs();
    }

    @Test
    void shouldCreateFilesWithOriginalAndProcessedHtml() {
        String originalHtml = "<div>Original</div>";
        String processedHtml = "<html><body><div>Processed</div></body></html>";
        String generationLog = "Generation log content";

        htmlLogger.log(originalHtml, processedHtml, generationLog);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Path> htmlFiles = findFilesInPdfExporterDirs("original-", ".html");
            assertFalse(htmlFiles.isEmpty(), "Original HTML file should be created");

            Path originalFile = htmlFiles.get(0);
            String content = Files.readString(originalFile);
            assertEquals(originalHtml, content);
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Path> processedFiles = findFilesInPdfExporterDirs("processed-", ".html");
            assertFalse(processedFiles.isEmpty(), "Processed HTML file should be created");

            Path processedFile = processedFiles.get(0);
            String content = Files.readString(processedFile);
            assertEquals(processedHtml, content);
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Path> logFiles = findFilesInPdfExporterDirs("timing-report-", ".txt");
            assertFalse(logFiles.isEmpty(), "Timing report file should be created");

            Path logFile = logFiles.get(0);
            String content = Files.readString(logFile);
            assertEquals(generationLog, content);
        });
    }

    @Test
    void shouldNotCreateLogFileWhenGenerationLogIsEmpty() {
        String originalHtml = "<div>Original</div>";
        String processedHtml = "<html><body>Processed</body></html>";

        htmlLogger.log(originalHtml, processedHtml, "");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Path> htmlFiles = findFilesInPdfExporterDirs("original-", ".html");
            assertFalse(htmlFiles.isEmpty(), "HTML files should be created");
        });

        // Wait and ensure no timing report files were created in any pdf-exporter directory
        await().pollDelay(1, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<Path> logFiles = findFilesInPdfExporterDirs("timing-report-", ".txt");
                    assertTrue(logFiles.isEmpty(), "No timing report file should be created");
                });
    }

    private void cleanupPdfExporterDirs() throws IOException {
        try (Stream<Path> paths = Files.list(TEMP_DIR)) {
            paths.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("pdf-exporter"))
                    .forEach(dir -> {
                        try {
                            deleteDirectory(dir);
                        } catch (IOException e) {
                            // Ignore cleanup failures
                        }
                    });
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted((p1, p2) -> -p1.compareTo(p2)) // Delete files before directories
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            // Ignore cleanup failures
                        }
                    });
        }
    }

    private List<Path> findFilesInPdfExporterDirs(String prefix, String suffix) throws IOException {
        try (Stream<Path> dirs = Files.list(TEMP_DIR)) {
            return dirs
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("pdf-exporter"))
                    .flatMap(dir -> {
                        try {
                            return Files.list(dir);
                        } catch (IOException e) {
                            return Stream.empty();
                        }
                    })
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String fileName = p.getFileName().toString();
                        return fileName.startsWith(prefix) && fileName.endsWith(suffix);
                    })
                    .toList();
        }
    }

}

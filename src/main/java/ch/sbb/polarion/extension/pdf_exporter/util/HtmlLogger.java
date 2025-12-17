package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Logger for debug HTML and timing reports.
 * <p>
 * When debug mode is enabled, this logger writes:
 * <ul>
 *     <li>original-*.html - Original HTML from Polarion</li>
 *     <li>processed-*.html - Final HTML after processing</li>
 *     <li>timing-report-*.txt - Detailed timing report for performance analysis</li>
 * </ul>
 */
public class HtmlLogger {

    private final Logger logger = Logger.getLogger(HtmlLogger.class);

    /**
     * Logs HTML content and timing report to temporary files.
     *
     * @param originalHtml  the original HTML from Polarion
     * @param processedHtml the processed HTML ready for PDF conversion
     * @param timingReport  the timing report (use {@link PdfGenerationLog#generateTimingReport(String)})
     */
    public void log(String originalHtml, String processedHtml, String timingReport) {
        if (!StringUtils.isEmpty(timingReport) && logger.isDebugEnabled()) {
            logger.debug(System.lineSeparator() + timingReport);
        }

        CompletableFuture.runAsync(() -> {
            try {
                Path tempDir = createTempDir("pdf-exporter");

                if (!StringUtils.isEmpty(originalHtml)) {
                    Path originalHtmlFile = createTempFile(tempDir, "original-", ".html");
                    Files.writeString(originalHtmlFile, originalHtml);
                    logger.info(String.format("Original HTML fragment provided by Polarion was stored in file %s", originalHtmlFile));
                }

                if (!StringUtils.isEmpty(processedHtml)) {
                    Path processedHtmlFile = createTempFile(tempDir, "processed-", ".html");
                    Files.writeString(processedHtmlFile, processedHtml);
                    logger.info(String.format("Final HTML page obtained as a result of PDF exporter processing was stored in file %s", processedHtmlFile));
                }

                if (!StringUtils.isEmpty(timingReport)) {
                    Path timingReportFile = createTempFile(tempDir, "timing-report-", ".txt");
                    Files.writeString(timingReportFile, timingReport);
                    logger.info(String.format("PDF generation timing report was stored in file %s", timingReportFile));
                }
            } catch (IOException e) {
                logger.error("Failed to log HTML to file system", e);
            }
        });
    }

    @SneakyThrows
    @SuppressWarnings({"SameParameterValue", "java:S2612", "java:S5443", "java:S1166"}) //need by design
    private Path createTempDir(String prefix) {
        Set<PosixFilePermission> folderPermissions = PosixFilePermissions.fromString("rwxr-xr-x");
        try {
            return Files.createTempDirectory(prefix, PosixFilePermissions.asFileAttribute(folderPermissions));
        } catch (UnsupportedOperationException e) { //windows doesn't like posix-related things
            return Files.createTempDirectory(prefix);
        }
    }

    @SneakyThrows
    @SuppressWarnings({"java:S2612", "java:S1166"}) //need by design
    private Path createTempFile(Path dir, String prefix, String suffix) {
        Set<PosixFilePermission> filePermissions = PosixFilePermissions.fromString("rw-r--r--");
        FileAttribute<?> fileAttribute = PosixFilePermissions.asFileAttribute(filePermissions);
        Path filePath;
        try {
            filePath = Files.createTempFile(dir, prefix, suffix, fileAttribute);
        } catch (UnsupportedOperationException e) { //windows doesn't like posix-related things
            filePath = Files.createTempFile(dir, prefix, suffix);
        }
        filePath.toFile().deleteOnExit();
        return filePath;
    }
}

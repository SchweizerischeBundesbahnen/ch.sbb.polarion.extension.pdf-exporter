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

public class HtmlLogger {

    private final Logger logger = Logger.getLogger(HtmlLogger.class);

    public void log(String originalHtml, String processedHtml, String generationLog) {
        CompletableFuture.runAsync(() -> {
            try {
                Path tempDir = createTempDir("pdf-exporter");

                Path originalHtmlFile = createTempFile(tempDir, "original-", ".html");
                Files.writeString(originalHtmlFile, originalHtml);
                logger.info(String.format("Original HTML fragment provided by Polarion was stored in file %s", originalHtmlFile));

                Path processedHtmlFile = createTempFile(tempDir, "processed-", ".html");
                Files.writeString(processedHtmlFile, processedHtml);
                logger.info(String.format("Final HTML page obtained as a result of PDF exporter processing was stored in file %s", processedHtmlFile));

                if (!StringUtils.isEmpty(generationLog)) {
                    Path generationLogFile = createTempFile(tempDir, "gen-log-", ".log");
                    Files.writeString(generationLogFile, generationLog);
                    logger.info(String.format("Generation log was stored in file %s", generationLogFile));
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

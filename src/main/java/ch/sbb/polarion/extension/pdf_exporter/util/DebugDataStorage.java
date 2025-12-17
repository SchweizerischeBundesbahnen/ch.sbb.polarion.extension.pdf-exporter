package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.model.DebugData;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.security.ISecurityService;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class DebugDataStorage {
    private static final Logger logger = Logger.getLogger(DebugDataStorage.class);
    private static final String UNKNOWN_DEBUG_DATA_MESSAGE = "Debug data not found for job: %s";

    private static final Map<String, DebugData> debugDataMap = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> currentJobId = new ThreadLocal<>();

    public void setCurrentJobId(@Nullable String jobId) {
        if (jobId != null) {
            currentJobId.set(jobId);
        } else {
            currentJobId.remove();
        }
    }

    @Nullable
    public String getCurrentJobId() {
        return currentJobId.get();
    }

    public void clearCurrentJobId() {
        currentJobId.remove();
    }

    public void store(@NotNull String jobId, @NotNull DebugData debugData) {
        debugDataMap.put(jobId, debugData);
        logger.debug("Stored debug data for job: " + jobId);
    }

    public void storeForCurrentJob(@Nullable String originalHtml,
                                   @Nullable String processedHtml,
                                   @Nullable String timingReport,
                                   @NotNull String user,
                                   @Nullable String documentTitle) {
        String jobId = getCurrentJobId();
        if (jobId == null) {
            logger.debug("No current job ID set, debug data will not be stored in job storage");
            return;
        }

        DebugData debugData = DebugData.builder()
                .originalHtml(originalHtml)
                .processedHtml(processedHtml)
                .timingReport(timingReport)
                .user(user)
                .createdAt(Instant.now())
                .documentTitle(documentTitle)
                .build();

        store(jobId, debugData);
    }

    @NotNull
    public DebugData get(@NotNull String jobId, @NotNull ISecurityService securityService) {
        DebugData debugData = debugDataMap.get(jobId);
        if (debugData == null) {
            throw new NoSuchElementException(String.format(UNKNOWN_DEBUG_DATA_MESSAGE, jobId));
        }

        String currentUser = securityService.getCurrentUser();
        if (!Objects.equals(debugData.user(), currentUser)) {
            throw new NoSuchElementException(String.format(UNKNOWN_DEBUG_DATA_MESSAGE, jobId));
        }

        return debugData;
    }

    public boolean exists(@NotNull String jobId) {
        return debugDataMap.containsKey(jobId);
    }

    public void remove(@NotNull String jobId) {
        debugDataMap.remove(jobId);
        logger.debug("Removed debug data for job: " + jobId);
    }

    public void cleanupExpired(int timeoutMinutes) {
        Instant cutoff = Instant.now().minus(timeoutMinutes, ChronoUnit.MINUTES);

        debugDataMap.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().createdAt().isBefore(cutoff);
            if (expired) {
                logger.debug("Cleaning up expired debug data for job: " + entry.getKey());
            }
            return expired;
        });
    }

    public void clear() {
        debugDataMap.clear();
        currentJobId.remove();
    }

    public int size() {
        return debugDataMap.size();
    }
}

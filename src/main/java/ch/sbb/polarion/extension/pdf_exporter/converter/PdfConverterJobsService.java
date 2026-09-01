package ch.sbb.polarion.extension.pdf_exporter.converter;

import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector;
import ch.sbb.polarion.extension.pdf_exporter.util.DebugDataStorage;
import ch.sbb.polarion.extension.pdf_exporter.util.ExportContext;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.security.ISecurityService;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class PdfConverterJobsService {
    private final Logger logger = Logger.getLogger(PdfConverterJobsService.class);
    // Static maps are necessary for per-request scoped InternalController and ApiController. In case of singletons static can be removed
    private static final Map<String, JobDetails> jobs = new ConcurrentHashMap<>();
    private static final Map<String, String> failedJobsReasons = new ConcurrentHashMap<>();
    private static final java.util.Set<String> cancelledJobIds = ConcurrentHashMap.newKeySet();
    private static final ExecutorService jobExecutor = Executors.newCachedThreadPool();
    private static final String UNKNOWN_JOB_MESSAGE = "Converter Job is unknown: %s";
    private static final String CANCELLED_BY_USER_MESSAGE = "Cancelled by user";

    private final PdfConverter pdfConverter;
    private final ISecurityService securityService;

    public PdfConverterJobsService(PdfConverter pdfConverter, ISecurityService securityService) {
        this.pdfConverter = pdfConverter;
        this.securityService = securityService;
    }

    public String startJob(ExportParams exportParams, int timeoutInMinutes) {
        return startJob(List.of(exportParams), timeoutInMinutes);
    }

    public String startJob(List<ExportParams> documentExportParams, int timeoutInMinutes) {
        String jobId = UUID.randomUUID().toString();
        Subject userSubject = securityService.getCurrentSubject();
        boolean isJobLogoutRequired = isJobLogoutRequired();
        final JobContext jobContext = JobContext.builder().workItemIDsWithMissingAttachment(new ArrayList<>()).build();
        ExportParams representativeParams = documentExportParams.isEmpty() ? null : documentExportParams.get(0);
        boolean isMerge = documentExportParams.size() > 1;

        // Store worker thread reference so we can interrupt it on timeout.
        // CompletableFuture.cancel(true) does NOT interrupt — we handle it manually.
        final java.util.concurrent.atomic.AtomicReference<Thread> workerThread = new java.util.concurrent.atomic.AtomicReference<>();

        CompletableFuture<byte[]> asyncJob = CompletableFuture.supplyAsync(() -> {
            workerThread.set(Thread.currentThread());
            try {
                DebugDataStorage.setCurrentJobId(jobId);
                return securityService.doAsUser(userSubject, (PrivilegedAction<byte[]>) () -> {
                    if (isMerge) {
                        BulkProcessingConnector.MergeResult mergeResult = pdfConverter.convertMergedToPdf(documentExportParams);
                        jobContext.failedDocumentCount()[0] = mergeResult.failedDocumentCount();
                        return mergeResult.pdfBytes();
                    } else {
                        return pdfConverter.convertToPdf(representativeParams, null);
                    }
                });
            } catch (Exception e) {
                String errorMessage = String.format("PDF conversion job '%s' failed with error: %s", jobId, e.getMessage());
                logger.error(errorMessage, e);
                // Only store the reason if not already set (e.g. by timeout handler)
                failedJobsReasons.putIfAbsent(jobId, StringUtils.getEmptyIfNull(e.getMessage()));
                throw e;
            } finally {
                workerThread.set(null);
                DebugDataStorage.clearCurrentJobId();
                jobContext.workItemIDsWithMissingAttachment.addAll(ExportContext.getWorkItemIDsWithMissingAttachment());
                ExportContext.clear();
                if ((userSubject != null) && isJobLogoutRequired) {
                    securityService.logout(userSubject);
                }
            }
        }, jobExecutor);

        asyncJob
                .orTimeout(timeoutInMinutes, TimeUnit.MINUTES)
                .exceptionally(e -> handleJobFailure(jobId, e, timeoutInMinutes, workerThread, asyncJob));

        JobDetails jobDetails = JobDetails.builder()
                .future(asyncJob)
                .user(securityService.getCurrentUser())
                .exportParams(representativeParams)
                .startingTime(Instant.now())
                .workerThread(workerThread)
                .jobContext(jobContext).build();
        jobs.put(jobId, jobDetails);
        return jobId;
    }

    private byte[] handleJobFailure(String jobId, Throwable e, int timeoutInMinutes,
                                    java.util.concurrent.atomic.AtomicReference<Thread> workerThread,
                                    CompletableFuture<byte[]> asyncJob) {
        // the future hands over a CompletionException whose message is "<class>: <text>",
        // and this text is what the export dialog shows, so the cause speaks for itself
        String failedReason = describeFailure(e);
        if (e instanceof TimeoutException) {
            failedReason = String.format("Timeout after %d min", timeoutInMinutes);
            // Interrupt the actual worker thread (unlike cancel(true) which is a no-op on CompletableFuture)
            Thread t = workerThread.get();
            if (t != null) {
                t.interrupt();
            }
            failedJobsReasons.put(jobId, failedReason);
        } else if (cancelledJobIds.contains(jobId)) {
            // User-initiated cancellation: keep the "Cancelled by user" reason set by cancelJob(),
            // don't overwrite it with the raw interruption exception message
            failedReason = failedJobsReasons.getOrDefault(jobId, CANCELLED_BY_USER_MESSAGE);
        } else {
            failedJobsReasons.put(jobId, failedReason);
        }
        logger.error(String.format("PDF conversion job '%s' failed with error: %s", jobId, failedReason), e);
        asyncJob.completeExceptionally(e);
        return null;
    }

    /**
     * Cancels a running job on user request. Unlike {@link CompletableFuture#cancel(boolean)} (which is a no-op on the
     * worker thread), this interrupts the actual worker thread so long-running conversions (e.g. merge export) can
     * observe the interruption, stop and clean up remote resources.
     */
    public void cancelJob(String jobId) {
        JobDetails jobDetails = getJobDetails(jobId);
        if (jobDetails.future().isDone()) {
            return;
        }
        // Mark as user-cancelled and record the reason before interrupting so neither the worker's own error
        // handling nor the exceptionally stage overwrites it
        cancelledJobIds.add(jobId);
        failedJobsReasons.put(jobId, CANCELLED_BY_USER_MESSAGE);
        Thread workerThread = jobDetails.workerThread() == null ? null : jobDetails.workerThread().get();
        if (workerThread != null) {
            workerThread.interrupt();
        }
        jobDetails.future().cancel(true);
    }

    public JobState getJobState(String jobId) {
        CompletableFuture<byte[]> future = getJobDetails(jobId).future();
        return JobState.builder()
                .isDone(future.isDone())
                .isCompletedExceptionally(future.isCompletedExceptionally())
                .isCancelled(future.isCancelled())
                .errorMessage(failedJobsReasons.get(jobId)).build();
    }

    public Optional<byte[]> getJobResult(String jobId) {
        CompletableFuture<byte[]> future = getJobDetails(jobId).future();
        if (!future.isDone()) {
            return Optional.empty();
        }
        if (future.isCancelled() || future.isCompletedExceptionally()) {
            throw new IllegalStateException("Job was cancelled or failed: " + failedJobsReasons.get(jobId));
        }
        try {
            return Optional.of(future.get());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cannot extract result for job " + jobId + " :" + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot extract result for job " + jobId + " :" + e.getMessage(), e);
        }
    }

    public ExportParams getJobParams(String jobId) {
        return getJobDetails(jobId).exportParams;
    }

    public JobContext getJobContext(String jobId) {
        return getJobDetails(jobId).jobContext;
    }

    public Map<String, JobState> getAllJobsStates() {
        return jobs.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue().user, securityService.getCurrentUser()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getJobState(entry.getKey())));
    }

    public static void cleanupExpiredJobs(int timeout) {
        Instant currentTime = Instant.now();

        jobs.entrySet().stream()
                .filter(entry -> entry.getValue().future.isDone()
                        && entry.getValue().startingTime.plus(timeout, ChronoUnit.MINUTES).isBefore(currentTime))
                .map(Map.Entry::getKey)
                .forEach(PdfConverterJobsService::removeKeyFromJobMaps);

        // Also cleanup expired debug data
        DebugDataStorage.cleanupExpired(timeout);
    }

    private static void removeKeyFromJobMaps(String id) {
        jobs.remove(id);
        failedJobsReasons.remove(id);
        cancelledJobIds.remove(id);
        DebugDataStorage.remove(id);
    }

    @VisibleForTesting
    void cancelJobsAndCleanMap() {
        jobs.values().forEach(j -> j.future().cancel(true));
        jobs.clear();
        failedJobsReasons.clear();
        cancelledJobIds.clear();
    }

    @Builder
    public record JobDetails(
            CompletableFuture<byte[]> future,
            String user,
            ExportParams exportParams,
            Instant startingTime,
            java.util.concurrent.atomic.AtomicReference<Thread> workerThread,
            JobContext jobContext) {
    }

    @Builder
    public record JobContext(
            List<String> workItemIDsWithMissingAttachment,
            java.util.concurrent.atomic.AtomicInteger failedDocumentCount) {

        public static class JobContextBuilder {
            private java.util.concurrent.atomic.AtomicInteger failedDocumentCount = new java.util.concurrent.atomic.AtomicInteger();
        }
    }

    @Builder
    public record JobState(
            boolean isDone,
            boolean isCompletedExceptionally,
            boolean isCancelled,
            String errorMessage) {
    }

    private JobDetails getJobDetails(String jobId) {
        JobDetails jobDetails = jobs.get(jobId);
        if (jobDetails == null || !Objects.equals(jobDetails.user, securityService.getCurrentUser())) {
            throw new NoSuchElementException(String.format(UNKNOWN_JOB_MESSAGE, jobId));
        }
        return jobDetails;
    }

    private boolean isJobLogoutRequired() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            if (requestAttributes.getAttribute(LogoutFilter.XSRF_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST) == Boolean.TRUE) {
                return false;
            }
            return requestAttributes.getAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST) == Boolean.TRUE;
        }
        return false;
    }

    /**
     * @return what the export dialog shows: the message of the failure itself, or its class where it
     *         carries no message, since an empty reason tells the reader nothing
     */
    @VisibleForTesting
    static @NotNull String describeFailure(@NotNull Throwable thrown) {
        Throwable reason = rootReason(thrown);
        String message = StringUtils.getEmptyIfNull(reason.getMessage());
        return message.isBlank() ? reason.getClass().getName() : message;
    }

    /**
     * @return the failure worth showing: a future wraps what was thrown, and the wrapper says only
     *         which class it was
     */
    @VisibleForTesting
    static @NotNull Throwable rootReason(@NotNull Throwable thrown) {
        Throwable reason = thrown;
        while ((reason instanceof CompletionException || reason instanceof ExecutionException) && reason.getCause() != null) {
            reason = reason.getCause();
        }
        return reason;
    }
}

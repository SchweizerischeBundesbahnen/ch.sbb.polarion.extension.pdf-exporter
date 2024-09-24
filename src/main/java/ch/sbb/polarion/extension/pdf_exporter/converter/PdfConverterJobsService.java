package ch.sbb.polarion.extension.pdf_exporter.converter;

import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentFileNameHelper;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.security.ISecurityService;
import lombok.Builder;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PdfConverterJobsService {
    private final Logger logger = Logger.getLogger(PdfConverterJobsService.class);
    // Static maps are necessary for per-request scoped InternalController and ApiController. In case of singletons static can be removed
    private static final Map<String, JobDetails> jobs = new ConcurrentHashMap<>();
    private static final Map<String, String> failedJobsReasons = new ConcurrentHashMap<>();

    private final PdfConverter pdfConverter;
    private final ISecurityService securityService;

    public PdfConverterJobsService(PdfConverter pdfConverter, ISecurityService securityService) {
        this.pdfConverter = pdfConverter;
        this.securityService = securityService;
    }

    public String startJob(ExportParams exportParams, int timeoutInMinutes) {
        String jobId = UUID.randomUUID().toString();
        Subject userSubject = securityService.getCurrentSubject();
        boolean isJobLogoutRequired = isJobLogoutRequired();

        CompletableFuture<byte[]> asyncConversionJob = CompletableFuture.supplyAsync(() -> {
            try {
                return securityService.doAsUser(userSubject, (PrivilegedAction<byte[]>) () -> pdfConverter.convertToPdf(exportParams, null));
            } catch (Exception e) {
                String errorMessage = String.format("PDF conversion job '%s' is failed with error: %s", jobId, e.getMessage());
                logger.error(errorMessage, e);
                failedJobsReasons.put(jobId, e.getMessage());
                throw e;
            } finally {
                if ((userSubject != null) && isJobLogoutRequired) {
                    securityService.logout(userSubject);
                }
            }
        }, Executors.newSingleThreadExecutor());
        asyncConversionJob
                .orTimeout(timeoutInMinutes, TimeUnit.MINUTES)
                .exceptionally(e -> {
                    String failedReason = e.getMessage();
                    if (e instanceof TimeoutException) {
                        failedReason = String.format("Timeout after %d min", timeoutInMinutes);
                    }
                    failedJobsReasons.put(jobId, failedReason);
                    logger.error(String.format("PDF conversion job '%s' is failed with error: %s", jobId, failedReason), e);
                    asyncConversionJob.completeExceptionally(e);
                    return null;
                });
        JobDetails jobDetails = JobDetails.builder()
                .future(asyncConversionJob)
                .exportParams(exportParams)
                .startingTime(Instant.now()).build();
        jobs.put(jobId, jobDetails);
        return jobId;
    }

    public JobState getJobState(String jobId) {
        JobDetails jobDetails = jobs.get(jobId);
        if (jobDetails == null) {
            throw new NoSuchElementException("Converter Job is unknown: " + jobId);
        }
        CompletableFuture<byte[]> future = jobDetails.future();
        return JobState.builder()
                .isDone(future.isDone())
                .isCompletedExceptionally(future.isCompletedExceptionally())
                .isCancelled(future.isCancelled())
                .errorMessage(failedJobsReasons.get(jobId)).build();
    }

    public Optional<byte[]> getJobResult(String jobId) {
        JobDetails jobDetails = jobs.get(jobId);
        if (jobDetails == null) {
            throw new NoSuchElementException("Converter Job is unknown: " + jobId);
        }
        CompletableFuture<byte[]> future = jobDetails.future();
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
        JobDetails jobDetails = jobs.get(jobId);
        if (jobDetails == null) {
            throw new NoSuchElementException("Converter Job is unknown: " + jobId);
        }
        return jobDetails.exportParams;
    }

    public Map<String, JobState> getAllJobsStates() {
        return jobs.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), this::getJobState));
    }

    public static void cleanupExpiredJobs(int timeout) {
        Instant currentTime = Instant.now();

        jobs.entrySet().stream()
                .filter(entry -> entry.getValue().future.isDone()
                        && entry.getValue().startingTime.plus(timeout, ChronoUnit.MINUTES).isBefore(currentTime))
                .map(Map.Entry::getKey)
                .forEach(PdfConverterJobsService::removeKeyFromJobMaps);
    }

    private static void removeKeyFromJobMaps(String id) {
        jobs.remove(id);
        failedJobsReasons.remove(id);
    }

    @VisibleForTesting
    void cancelJobsAndCleanMap() {
        jobs.values().forEach(j -> j.future().cancel(true));
        jobs.clear();
    }

    @Builder
    public record JobDetails(
            CompletableFuture<byte[]> future,
            ExportParams exportParams,
            Instant startingTime) {
    }

    @Builder
    public record JobState(
            boolean isDone,
            boolean isCompletedExceptionally,
            boolean isCancelled,
            String errorMessage) {
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
}

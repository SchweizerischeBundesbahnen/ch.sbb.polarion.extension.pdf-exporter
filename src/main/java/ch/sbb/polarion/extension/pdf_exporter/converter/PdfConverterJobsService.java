package ch.sbb.polarion.extension.pdf_exporter.converter;

import ch.sbb.polarion.extension.generic.jobs.AsyncJobsService;
import ch.sbb.polarion.extension.generic.jobs.JobsProperties;
import ch.sbb.polarion.extension.generic.jobs.JobsRegistry;
import ch.sbb.polarion.extension.generic.jobs.TimeoutPolicy;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.util.DebugDataStorage;
import ch.sbb.polarion.extension.pdf_exporter.util.ExportContext;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.BulkProcessingConnector;
import com.polarion.platform.security.ISecurityService;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs PDF conversions in the background, of one document or of several merged into one PDF. The job mechanics are
 * generic's {@link AsyncJobsService}; this class adds the debug data and the export context of a conversion.
 * <p>
 * A cancelled or timed-out conversion has its worker thread interrupted, so a long merge export can observe the
 * interruption, stop and clean up its remote resources.
 */
public class PdfConverterJobsService extends AsyncJobsService<PdfConverterJobsService.JobPayload, byte[]> {

    public static final String JOBS_PROPERTIES_FILE = "/pdf-converter-jobs.properties";

    // Static, so that the jobs survive the controller instance which started them
    private static final JobsRegistry<JobPayload, byte[]> REGISTRY = registryBuilder().build();

    private final PdfConverter pdfConverter;

    public PdfConverterJobsService(@NotNull PdfConverter pdfConverter, @NotNull ISecurityService securityService) {
        this(pdfConverter, securityService, REGISTRY);
    }

    @VisibleForTesting
    PdfConverterJobsService(@NotNull PdfConverter pdfConverter, @NotNull ISecurityService securityService,
                            @NotNull JobsRegistry<JobPayload, byte[]> registry) {
        super(registry, securityService);
        this.pdfConverter = pdfConverter;
    }

    /**
     * @return the job timeouts of this extension
     */
    public static @NotNull JobsProperties jobsProperties() {
        return new JobsProperties(PdfConverterJobsService.class, JOBS_PROPERTIES_FILE);
    }

    /**
     * Starts dropping finished conversions, and their debug data, once they are older than the finished job timeout.
     */
    public static void startCleaner() {
        REGISTRY.startCleaner(jobsProperties().getFinishedJobTimeout());
    }

    /**
     * Stops the cleaner and the conversion threads. Called when the bundle stops.
     */
    public static void shutdown() {
        REGISTRY.shutdown();
    }

    public @NotNull String startJob(@NotNull ExportParams exportParams, int timeoutInMinutes) {
        return startJob(List.of(exportParams), timeoutInMinutes);
    }

    /**
     * Starts a conversion of one document, or of several documents merged into one PDF.
     * The first document stands for the whole job, for example in its file name.
     */
    public @NotNull String startJob(@NotNull List<ExportParams> documentExportParams, int timeoutInMinutes) {
        ExportParams representativeParams = documentExportParams.isEmpty() ? null : documentExportParams.get(0);
        boolean isMerge = documentExportParams.size() > 1;
        JobContext jobContext = JobContext.builder()
                .workItemIDsWithMissingAttachment(new ArrayList<>())
                .blockedResources(new ArrayList<>())
                .failedDocumentCount(new AtomicInteger())
                .build();
        return startJob(new JobPayload(representativeParams, jobContext), timeoutInMinutes, control -> {
            try {
                DebugDataStorage.setCurrentJobId(control.jobId());
                if (isMerge) {
                    BulkProcessingConnector.MergeResult mergeResult = pdfConverter.convertMergedToPdf(documentExportParams);
                    jobContext.failedDocumentCount().set(mergeResult.failedDocumentCount());
                    return mergeResult.pdfBytes();
                } else {
                    return pdfConverter.convertToPdf(Objects.requireNonNull(representativeParams, "No document to convert"), null);
                }
            } finally {
                DebugDataStorage.clearCurrentJobId();
                jobContext.workItemIDsWithMissingAttachment().addAll(ExportContext.getWorkItemIDsWithMissingAttachment());
                jobContext.blockedResources().addAll(ExportContext.getBlockedResources());
                ExportContext.clear();
            }
        });
    }

    public @Nullable ExportParams getJobParams(@NotNull String jobId) {
        return payload(jobId).exportParams();
    }

    public @NotNull JobContext getJobContext(@NotNull String jobId) {
        return payload(jobId).jobContext();
    }

    private @NotNull JobPayload payload(@NotNull String jobId) {
        return Objects.requireNonNull(getJobPayload(jobId), "Job payload is always set by startJob");
    }

    /**
     * A conversion only reads, so it is declared over at its timeout and its thread is interrupted.
     * Its debug data goes together with the job.
     */
    @VisibleForTesting
    static @NotNull JobsRegistry.Builder<JobPayload, byte[]> registryBuilder() {
        return JobsRegistry.<JobPayload, byte[]>builder("PDF conversion")
                .timeoutPolicy(TimeoutPolicy.INTERRUPT)
                .onJobRemoved(DebugDataStorage::remove)
                .onCleanup(DebugDataStorage::cleanupExpired);
    }

    /**
     * What a conversion keeps next to its result: the parameters of the document which stands for it, and what its
     * export context collected.
     */
    public record JobPayload(@Nullable ExportParams exportParams, @NotNull JobContext jobContext) {
    }

    @Builder
    public record JobContext(
            List<String> workItemIDsWithMissingAttachment,
            List<ExportContext.BlockedResource> blockedResources,
            AtomicInteger failedDocumentCount) {
    }
}

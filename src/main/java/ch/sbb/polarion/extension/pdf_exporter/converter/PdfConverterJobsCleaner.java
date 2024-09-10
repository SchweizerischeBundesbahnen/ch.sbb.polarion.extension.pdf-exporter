package ch.sbb.polarion.extension.pdf_exporter.converter;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class PdfConverterJobsCleaner {
    private static Future<?> cleaningJob;

    private PdfConverterJobsCleaner() {
    }

    public static synchronized void startCleaningJob() {
        if (cleaningJob != null) {
            return;
        }
        PropertiesUtility propertiesUtility = new PropertiesUtility();

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        cleaningJob = executorService.scheduleWithFixedDelay(
                () -> PdfConverterJobsService.cleanupExpiredJobs(propertiesUtility.getFinishedJobTimeout()),
                0,
                propertiesUtility.getFinishedJobTimeout(),
                TimeUnit.MINUTES);
    }
}

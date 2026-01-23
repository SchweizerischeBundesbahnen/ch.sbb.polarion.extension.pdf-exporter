package ch.sbb.polarion.extension.pdf_exporter.converter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class PdfConverterJobsCleaner {
    private static ScheduledExecutorService executorService;

    private PdfConverterJobsCleaner() {
    }

    public static synchronized void startCleaningJob() {
        if (executorService != null) {
            return;
        }
        PropertiesUtility propertiesUtility = new PropertiesUtility();

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(
                () -> PdfConverterJobsService.cleanupExpiredJobs(propertiesUtility.getFinishedJobTimeout()),
                0,
                propertiesUtility.getFinishedJobTimeout(),
                TimeUnit.MINUTES);
    }

    public static synchronized void stopCleaningJob() {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }
}

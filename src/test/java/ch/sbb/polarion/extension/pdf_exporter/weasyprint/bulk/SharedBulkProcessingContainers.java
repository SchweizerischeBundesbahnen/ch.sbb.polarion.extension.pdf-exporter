package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * The bulk processing service and the WeasyPrint it renders with, in containers on one network, started once for all the
 * tests of a run as {@link ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.SharedWeasyPrintContainer} is.
 * TestContainers removes them when the JVM exits.
 */
public final class SharedBulkProcessingContainers {

    private static final String WEASYPRINT_IMAGE = "ghcr.io/schweizerischebundesbahnen/weasyprint-service:latest";
    private static final String BULK_PROCESSING_IMAGE = "ghcr.io/schweizerischebundesbahnen/bulk-processing-service:latest";
    private static final String WEASYPRINT_ALIAS = "weasyprint-service";
    private static final int WEASYPRINT_PORT = 9080;
    private static final int BULK_PROCESSING_PORT = 9070;

    private SharedBulkProcessingContainers() {
    }

    /** Started on first access, thread safe without synchronization, as a holder class is initialized once. */
    private static final class Holder {
        private static final GenericContainer<?> WEASYPRINT;
        private static final GenericContainer<?> BULK_PROCESSING;

        static {
            @SuppressWarnings("resource") // closed with the containers when the JVM exits
            Network network = Network.newNetwork();
            WEASYPRINT = new GenericContainer<>(WEASYPRINT_IMAGE)
                    .withNetwork(network)
                    .withNetworkAliases(WEASYPRINT_ALIAS)
                    .withExposedPorts(WEASYPRINT_PORT)
                    .waitingFor(Wait.forHttp("/version").forPort(WEASYPRINT_PORT).forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)));
            WEASYPRINT.start();
            BULK_PROCESSING = new GenericContainer<>(BULK_PROCESSING_IMAGE)
                    .withNetwork(network)
                    .withEnv("WEASYPRINT_SERVICE_URL", "http://" + WEASYPRINT_ALIAS + ":" + WEASYPRINT_PORT)
                    .withExposedPorts(BULK_PROCESSING_PORT)
                    .waitingFor(Wait.forHttp("/ready").forPort(BULK_PROCESSING_PORT).forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)));
            BULK_PROCESSING.start();
        }
    }

    public static @NotNull GenericContainer<?> bulkProcessing() {
        return Holder.BULK_PROCESSING;
    }

    public static @NotNull String bulkProcessingUrl() {
        return "http://" + Holder.BULK_PROCESSING.getHost() + ":" + Holder.BULK_PROCESSING.getMappedPort(BULK_PROCESSING_PORT);
    }

    public static @NotNull String weasyPrintUrl() {
        return "http://" + Holder.WEASYPRINT.getHost() + ":" + Holder.WEASYPRINT.getMappedPort(WEASYPRINT_PORT);
    }
}

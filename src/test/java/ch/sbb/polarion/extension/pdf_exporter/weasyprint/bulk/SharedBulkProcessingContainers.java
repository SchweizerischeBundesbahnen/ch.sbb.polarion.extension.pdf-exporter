package ch.sbb.polarion.extension.pdf_exporter.weasyprint.bulk;

import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.SharedWeasyPrintContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * The bulk processing service and the WeasyPrint it renders with, in containers on one network, started once for all the
 * tests of a run as {@link ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.SharedWeasyPrintContainer} is.
 * TestContainers removes them when the JVM exits. Where {@value #BULK_PROCESSING_SERVICE_URL_PROPERTY} names a service
 * started elsewhere, a candidate for example, the tests merge through that one and start no container.
 */
final class SharedBulkProcessingContainers {

    /** The system property naming a running bulk processing service, which the tests then use instead of a container. */
    static final String BULK_PROCESSING_SERVICE_URL_PROPERTY = "bulk-processing.service.url";
    /** The system property naming the bulk processing image the tests start. */
    static final String BULK_PROCESSING_IMAGE_PROPERTY = "bulk-processing.image";
    private static final String DEFAULT_BULK_PROCESSING_IMAGE = "ghcr.io/schweizerischebundesbahnen/bulk-processing-service:latest";
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
            WEASYPRINT = new GenericContainer<>(SharedWeasyPrintContainer.weasyPrintImage())
                    .withNetwork(network)
                    .withNetworkAliases(WEASYPRINT_ALIAS)
                    .withExposedPorts(WEASYPRINT_PORT)
                    .waitingFor(Wait.forHttp("/version").forPort(WEASYPRINT_PORT).forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)));
            WEASYPRINT.start();
            BULK_PROCESSING = new GenericContainer<>(SharedWeasyPrintContainer.imageNamedBy(BULK_PROCESSING_IMAGE_PROPERTY, DEFAULT_BULK_PROCESSING_IMAGE))
                    .withNetwork(network)
                    .withEnv("WEASYPRINT_SERVICE_URL", "http://" + WEASYPRINT_ALIAS + ":" + WEASYPRINT_PORT)
                    .withExposedPorts(BULK_PROCESSING_PORT)
                    .waitingFor(Wait.forHttp("/ready").forPort(BULK_PROCESSING_PORT).forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)));
            BULK_PROCESSING.start();
        }
    }

    static @NotNull GenericContainer<?> bulkProcessing() {
        return Holder.BULK_PROCESSING;
    }

    /** Whether the tests started the service themselves, so that they can look into its container. */
    static boolean startedByTheTests() {
        return externalBulkProcessingUrl() == null;
    }

    static @NotNull String bulkProcessingUrl() {
        String externalUrl = externalBulkProcessingUrl();
        if (externalUrl != null) {
            return externalUrl;
        }
        return "http://" + Holder.BULK_PROCESSING.getHost() + ":" + Holder.BULK_PROCESSING.getMappedPort(BULK_PROCESSING_PORT);
    }

    /**
     * The WeasyPrint address the connector is given. The connector keeps it and the service renders with its own, so for
     * a service started elsewhere the address {@code weasyprint.service.url} names is given, and no container started.
     */
    static @NotNull String weasyPrintUrl() {
        if (!startedByTheTests()) {
            return BaseWeasyPrintTest.stripTrailingSlashes(System.getProperty(BaseWeasyPrintTest.WEASYPRINT_SERVICE_URL_PROPERTY, "").trim());
        }
        return "http://" + Holder.WEASYPRINT.getHost() + ":" + Holder.WEASYPRINT.getMappedPort(WEASYPRINT_PORT);
    }

    private static @Nullable String externalBulkProcessingUrl() {
        String url = System.getProperty(BULK_PROCESSING_SERVICE_URL_PROPERTY, "").trim();
        return url.isEmpty() ? null : BaseWeasyPrintTest.stripTrailingSlashes(url);
    }
}

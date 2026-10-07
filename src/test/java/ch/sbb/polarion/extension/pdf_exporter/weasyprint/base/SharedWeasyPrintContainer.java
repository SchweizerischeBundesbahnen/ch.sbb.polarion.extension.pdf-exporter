package ch.sbb.polarion.extension.pdf_exporter.weasyprint.base;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * Singleton container holder for WeasyPrint service.
 * Uses proper lazy initialization with thread safety.
 */
public final class SharedWeasyPrintContainer {

    private static final Logger logger = LoggerFactory.getLogger(SharedWeasyPrintContainer.class);
    /** The system property naming the WeasyPrint image the tests start, for example a candidate built from a branch. */
    public static final String WEASYPRINT_IMAGE_PROPERTY = "weasyprint.image";
    private static final String DEFAULT_WEASYPRINT_IMAGE = "ghcr.io/schweizerischebundesbahnen/weasyprint-service:latest";

    private SharedWeasyPrintContainer() {
        // Private constructor to prevent instantiation
    }

    /**
     * Bill Pugh Singleton Implementation using inner static helper class.
     * Thread-safe without synchronization overhead.
     */
    private static class ContainerHolder {
        private static final GenericContainer<?> INSTANCE = createAndStartContainer();

        private static GenericContainer<?> createAndStartContainer() {
            try {
                GenericContainer<?> container = new GenericContainer<>(weasyPrintImage())
                        .withExposedPorts(9080)
                        .waitingFor(
                                Wait.forHttp("/version").forPort(9080)
                                        .forStatusCode(200)
                                        .withStartupTimeout(Duration.ofMinutes(2))
                        );

                container.start();

                if (!container.isRunning()) {
                    throw new IllegalStateException("Container failed to start");
                }

                return container;
            } catch (Exception e) {
                logger.error("Failed to start WeasyPrint container", e);
                throw new RuntimeException("Failed to start WeasyPrint container", e);
            }
        }
    }

    /** The WeasyPrint image every container of the tests is started from. */
    public static @NotNull String weasyPrintImage() {
        return imageNamedBy(WEASYPRINT_IMAGE_PROPERTY, DEFAULT_WEASYPRINT_IMAGE);
    }

    /** The image the system property names, or the default one where it names none. */
    public static @NotNull String imageNamedBy(@NotNull String property, @NotNull String defaultImage) {
        String image = System.getProperty(property, "").trim();
        return image.isEmpty() ? defaultImage : image;
    }

    /**
     * Get the shared container instance.
     * Container is started on first access (lazy initialization).
     *
     * @return the shared WeasyPrint container
     */
    public static GenericContainer<?> getInstance() {
        return ContainerHolder.INSTANCE;
    }

    /**
     * Explicitly stop the container if needed.
     * Note: TestContainers will automatically clean up containers when JVM exits.
     */
    public static void stopIfRunning() {
        try {
            GenericContainer<?> container = ContainerHolder.INSTANCE;
            if (container.isRunning()) {
                logger.info("Stopping shared WeasyPrint container...");
                container.stop();
            }
        } catch (Exception e) {
            logger.warn("Error stopping container: {}", e.getMessage());
        }
    }
}

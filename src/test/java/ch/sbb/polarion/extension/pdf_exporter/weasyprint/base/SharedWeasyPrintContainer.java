package ch.sbb.polarion.extension.pdf_exporter.weasyprint.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.images.PullPolicy;

import java.time.Duration;

/**
 * Singleton container holder for WeasyPrint service.
 * Uses proper lazy initialization with thread safety.
 */
public final class SharedWeasyPrintContainer {

    private static final Logger logger = LoggerFactory.getLogger(SharedWeasyPrintContainer.class);
    private static final String DOCKER_IMAGE_NAME = "ghcr.io/schweizerischebundesbahnen/weasyprint-service:latest";

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
                GenericContainer<?> container = new GenericContainer<>(DOCKER_IMAGE_NAME)
                        .withExposedPorts(9080)
                        .waitingFor(
                                Wait.forHttp("/version").forPort(9080)
                                        .forStatusCode(200)
                                        .withStartupTimeout(Duration.ofMinutes(2))
                        )
                        .withCreateContainerCmdModifier(createContainerCmd ->
                                createContainerCmd.getHostConfig()
                                        .withNanoCPUs(1_000_000_000L) // 1.0 vCPU
                                        .withMemory(4 * 1024 * 1024 * 1024L)); // 4 GB

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

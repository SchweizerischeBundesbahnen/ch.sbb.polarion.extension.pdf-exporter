package ch.sbb.polarion.extension.pdf.exporter.weasyprint.exporter;

import com.github.dockerjava.api.exception.NotFoundException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Waits for the file appearance in the container at specified path.
 */
@Getter
public class FileWaitStrategy implements WaitStrategy {
    private static final Logger logger = LoggerFactory.getLogger(FileWaitStrategy.class);

    private static final long CHECK_INTERVAL_MILLIS = 500;
    private static final long WAIT_TIMEOUT_SECONDS = 10;

    private final String filePath;

    public FileWaitStrategy(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
        Awaitility.given().ignoreException(NotFoundException.class)
                .await().atMost(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .pollInterval(CHECK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    logger.info("PDF file {} is ready", filePath);
                });
    }

    @Override
    public WaitStrategy withStartupTimeout(Duration startupTimeout) {
        throw new UnsupportedOperationException("This strategy cannot be configured from outside");
    }
}

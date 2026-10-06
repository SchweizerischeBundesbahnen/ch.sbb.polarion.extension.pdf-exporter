package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The bulk processing service of the performance tests, which renders with the WeasyPrint the single exports are timed
 * against, through a proxy which adds up how long WeasyPrint takes. A merge is then timed in three parts: the exporter,
 * WeasyPrint, and the service itself, which uploads, keeps and merges the documents. Started once for the run;
 * TestContainers removes the container when the JVM exits.
 */
final class TimedBulkProcessingService {

    private static final String BULK_PROCESSING_IMAGE = "ghcr.io/schweizerischebundesbahnen/bulk-processing-service:latest";
    private static final int BULK_PROCESSING_PORT = 9070;

    /** The headers the client of the proxy sets itself, which it refuses to be given. */
    private static final Set<String> RESTRICTED_HEADERS = Set.of("connection", "content-length", "host", "expect", "upgrade");

    /** Kept outside of the holder: the proxy adds to it while the holder starts the service, which already calls WeasyPrint. */
    private static final AtomicLong WEASYPRINT_NANOS = new AtomicLong();

    private TimedBulkProcessingService() {
    }

    /** Started on first access, thread safe without synchronization, as a holder class is initialized once. */
    private static final class Holder {
        private static final String URL;
        private static final long NETWORK_MS;

        static {
            NETWORK_MS = NetworkCalibration.measure(BULK_PROCESSING_IMAGE);
            int proxyPort = startProxy(BaseWeasyPrintTest.getWeasyPrintServiceUrl());
            Testcontainers.exposeHostPorts(proxyPort);
            @SuppressWarnings("resource") // removed when the JVM exits
            GenericContainer<?> bulkProcessing = new GenericContainer<>(BULK_PROCESSING_IMAGE)
                    .withEnv("WEASYPRINT_SERVICE_URL", "http://host.testcontainers.internal:" + proxyPort)
                    .withExposedPorts(BULK_PROCESSING_PORT)
                    .waitingFor(Wait.forHttp("/ready").forPort(BULK_PROCESSING_PORT).forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)));
            bulkProcessing.start();
            URL = "http://" + bulkProcessing.getHost() + ":" + bulkProcessing.getMappedPort(BULK_PROCESSING_PORT);
        }
    }

    static @NotNull String url() {
        return Holder.URL;
    }

    /** How long a document of a merge takes on the network between the JVM and the containers, as {@link NetworkCalibration} measured it. */
    static long networkMs() {
        return Holder.NETWORK_MS;
    }

    /** How long WeasyPrint has taken for the service since the run started, in milliseconds. */
    static long weasyPrintMs() {
        return WEASYPRINT_NANOS.get() / 1_000_000;
    }

    /** Forwards every request to WeasyPrint as it is and adds how long WeasyPrint took to answer it. */
    @SneakyThrows
    private static int startProxy(@NotNull String weasyPrintUrl) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> forward(client, weasyPrintUrl, exchange));
        server.setExecutor(Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "weasyprint-timing-proxy");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        return server.getAddress().getPort();
    }

    private static void forward(@NotNull HttpClient client, @NotNull String weasyPrintUrl, @NotNull HttpExchange exchange) throws IOException {
        try (exchange) {
            byte[] body = exchange.getRequestBody().readAllBytes();
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(weasyPrintUrl + exchange.getRequestURI()))
                    .timeout(Duration.ofMinutes(10))
                    .method(exchange.getRequestMethod(), HttpRequest.BodyPublishers.ofByteArray(body));
            exchange.getRequestHeaders().forEach((name, values) -> {
                if (!RESTRICTED_HEADERS.contains(name.toLowerCase())) {
                    values.forEach(value -> request.header(name, value));
                }
            });

            long start = System.nanoTime();
            HttpResponse<byte[]> response = client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            WEASYPRINT_NANOS.addAndGet(System.nanoTime() - start);

            response.headers().map().forEach((name, values) -> {
                if (!RESTRICTED_HEADERS.contains(name.toLowerCase()) && !"transfer-encoding".equalsIgnoreCase(name)) {
                    exchange.getResponseHeaders().put(name, values);
                }
            });
            byte[] answer = response.body();
            exchange.sendResponseHeaders(response.statusCode(), answer.length == 0 ? -1 : answer.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(answer);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while forwarding to WeasyPrint", e);
        }
    }
}

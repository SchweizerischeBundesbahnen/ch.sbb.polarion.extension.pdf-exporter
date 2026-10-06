package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.concurrent.Executors;

/**
 * Times how long a document of a merge takes on the network of this machine, as the bulk processing service of the run
 * sends it: from the JVM into a container, and from a container back to the JVM through the tunnel of Testcontainers,
 * where the proxy of WeasyPrint listens. Docker in a VM, as on Windows or a Mac, takes ten times longer for each than
 * Docker on Linux, which no other part of an export shows.
 */
final class NetworkCalibration {

    /** About the HTML of one document of a merge, most of it the fonts the export embeds. */
    private static final int PAYLOAD_BYTES = 5 * 1024 * 1024;
    private static final int RUNS = 5;
    private static final int SINK_PORT = 9000;

    /** Reads a request whole and answers it, in the container. */
    private static final String SINK = """
            import http.server
            class Sink(http.server.BaseHTTPRequestHandler):
                def do_POST(self):
                    self.rfile.read(int(self.headers['Content-Length']))
                    self.send_response(200)
                    self.send_header('Content-Length', '0')
                    self.end_headers()
                def log_message(self, *args):
                    pass
            http.server.ThreadingHTTPServer(('0.0.0.0', %d), Sink).serve_forever()
            """.formatted(SINK_PORT);

    /** Sends the payload to the URL once untimed and then RUNS times, printing the median in milliseconds. */
    private static final String CLIENT = """
            import statistics, sys, time, urllib.request
            url, size, runs = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
            body = b'A' * size
            times = []
            for run in range(runs + 1):
                start = time.perf_counter()
                urllib.request.urlopen(urllib.request.Request(url, data=body, method='POST'), timeout=120).read()
                times.append((time.perf_counter() - start) * 1000)
            print(round(statistics.median(times[1:])))
            """;

    private NetworkCalibration() {
    }

    /**
     * The median time of one payload into a container plus that of one out of it, in milliseconds. The container runs the
     * image of the bulk processing service, which the run pulls anyway, with the sink in place of the service.
     */
    @SneakyThrows
    static long measure(@NotNull String image) {
        HttpServer hostSink = HttpServer.create(new InetSocketAddress(0), 0);
        hostSink.createContext("/", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                exchange.sendResponseHeaders(200, -1);
            }
        });
        hostSink.setExecutor(Executors.newCachedThreadPool());
        hostSink.start();
        int hostPort = hostSink.getAddress().getPort();
        // Before the container starts: one started earlier does not know host.testcontainers.internal
        Testcontainers.exposeHostPorts(hostPort);
        try (GenericContainer<?> container = new GenericContainer<>(image)
                .withCreateContainerCmdModifier(command -> command.withEntrypoint("python3", "-c", SINK))
                .withExposedPorts(SINK_PORT)) {
            container.start();
            long intoContainerMs = postFromHost("http://" + container.getHost() + ":" + container.getMappedPort(SINK_PORT) + "/");
            long outOfContainerMs = postFromContainer(container, "http://host.testcontainers.internal:" + hostPort + "/");
            return intoContainerMs + outOfContainerMs;
        } finally {
            hostSink.stop(0);
        }
    }

    @SneakyThrows
    private static long postFromHost(@NotNull String url) {
        HttpClient client = HttpClient.newHttpClient();
        byte[] body = new byte[PAYLOAD_BYTES];
        Arrays.fill(body, (byte) 'A');
        long[] times = new long[RUNS];
        for (int run = 0; run <= RUNS; run++) {
            long start = System.nanoTime();
            HttpResponse<Void> response = client.send(HttpRequest.newBuilder(URI.create(url)).POST(HttpRequest.BodyPublishers.ofByteArray(body)).build(),
                    HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("The sink in the container answered " + response.statusCode());
            }
            // The first request warms the forwarder up. Each request connects anew, as the connector does for each document
            if (run > 0) {
                times[run - 1] = (System.nanoTime() - start) / 1_000_000;
            }
        }
        Arrays.sort(times);
        return times[RUNS / 2];
    }

    @SneakyThrows
    private static long postFromContainer(@NotNull GenericContainer<?> container, @NotNull String url) {
        Container.ExecResult result = container.execInContainer("python3", "-c", CLIENT, url, String.valueOf(PAYLOAD_BYTES), String.valueOf(RUNS));
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Sending from the container to the host failed: " + result.getStderr());
        }
        return Long.parseLong(result.getStdout().trim());
    }
}

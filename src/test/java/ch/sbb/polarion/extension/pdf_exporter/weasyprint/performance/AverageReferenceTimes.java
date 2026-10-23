package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Averages the times of runs of the performance tests on one architecture into its reference times, which it prints.
 * Each argument is the {@code performance-reference-times.properties} a run writes, and CI uploads. Each reference time is
 * the geometric mean of the runs, as {@link PerformanceRun} scales by ratios: the small document by the fixed piece of
 * work, the bulk processing service of a merge by the network, every other export by the basket of its run.
 * <p>
 * It needs no build: {@code java src/test/java/ch/sbb/polarion/extension/pdf_exporter/weasyprint/performance/AverageReferenceTimes.java runs/*.properties}
 * </p>
 */
public final class AverageReferenceTimes {

    private static final String CALIBRATION = "machine.calibration";
    private static final String NETWORK_CALIBRATION = "network.calibration";

    private AverageReferenceTimes() {
    }

    @SuppressWarnings("java:S106") // A command line tool, whose output is the file it makes
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: AverageReferenceTimes <performance-reference-times.properties>...");
            System.exit(1);
        }
        List<Properties> runs = new ArrayList<>();
        for (String arg : args) {
            Properties run = new Properties();
            try (Reader reader = Files.newBufferedReader(Path.of(arg), StandardCharsets.UTF_8)) {
                run.load(reader);
            }
            runs.add(run);
        }
        System.out.print(average(runs));
    }

    /** The reference times of the given runs, in the form of {@code reference-times-<architecture>.properties}. */
    static String average(List<Properties> runs) {
        // The mean of the logarithm of each time, over the runs which have it: the logarithm of its geometric mean
        Map<String, Double> logs = new TreeMap<>();
        runs.forEach(run -> run.stringPropertyNames().forEach(key -> logs.computeIfAbsent(key, k -> runs.stream()
                .filter(other -> other.containsKey(k)).mapToDouble(other -> Math.log(value(other, k))).average().orElseThrow())));
        Map<String, String> references = new TreeMap<>();
        logs.forEach((key, log) -> references.put(key, String.valueOf(Math.round(Math.exp(log)))));
        StringBuilder text = new StringBuilder()
                .append("# The reference times of the performance tests on one architecture, in ms: the geometric mean of ").append(runs.size())
                .append(" runs, made by AverageReferenceTimes.\n")
                .append("# Each test reads <export>.exporter and <export>.weasyprint, a merge <export>.bps too. The small document is scaled by the fixed piece of work of\n")
                .append("# machine.calibration, every other export by the basket of its run,\n")
                .append("# the bps of a merge by network.calibration where it is given.\n")
                .append(CALIBRATION).append('=').append(references.remove(CALIBRATION)).append('\n');
        String network = references.remove(NETWORK_CALIBRATION);
        if (network != null) {
            text.append(NETWORK_CALIBRATION).append('=').append(network).append('\n');
        }
        references.forEach((key, value) -> text.append(key).append('=').append(value).append('\n'));
        return text.toString();
    }

    private static double value(Properties run, String key) {
        String value = run.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("A run has no " + key);
        }
        return Double.parseDouble(value.trim());
    }
}

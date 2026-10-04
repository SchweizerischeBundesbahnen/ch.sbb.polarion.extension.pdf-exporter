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
import java.util.TreeSet;

/**
 * Averages the times of runs of the performance tests into reference times, which it prints. Each argument is a
 * {@code performance-reference-times.properties} a run of CI uploads. Runs on machines of different speed average as
 * {@link PerformanceRun} reads them: the small document by its time against the fixed piece of work, every other export
 * by its time against the small document of the same run.
 * <p>
 * It needs no build: {@code java src/test/java/ch/sbb/polarion/extension/pdf_exporter/weasyprint/performance/AverageReferenceTimes.java runs/*.properties}
 * </p>
 */
public final class AverageReferenceTimes {

    private static final String CALIBRATION = "machine.calibration";
    private static final String SMALL_DOCUMENT = "smallDocument.";
    private static final String[] PARTS = {"exporter", "weasyprint"};

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

    /** The reference times of the given runs, in the form of {@code reference-times.properties}. */
    static String average(List<Properties> runs) {
        double calibration = runs.stream().mapToDouble(run -> value(run, CALIBRATION)).average().orElseThrow();
        Map<String, Long> references = new TreeMap<>();
        for (String part : PARTS) {
            String smallKey = SMALL_DOCUMENT + part;
            double small = calibration * runs.stream().mapToDouble(run -> value(run, smallKey) / value(run, CALIBRATION)).average().orElseThrow();
            references.put(smallKey, Math.round(small));
            for (String key : keysOf(runs, part)) {
                if (!key.equals(smallKey)) {
                    references.put(key, Math.round(small * runs.stream().mapToDouble(run -> value(run, key) / value(run, smallKey)).average().orElseThrow()));
                }
            }
        }
        StringBuilder text = new StringBuilder()
                .append("# The reference times of the performance tests, in ms: the average of ").append(runs.size()).append(" runs of CI, made by AverageReferenceTimes.\n")
                .append("# Each test reads <export>.exporter and <export>.weasyprint. The small document is scaled by the fixed piece of work of\n")
                .append("# machine.calibration, every other export by the small document of its run.\n")
                .append(CALIBRATION).append('=').append(Math.round(calibration)).append('\n');
        references.forEach((key, value) -> text.append(key).append('=').append(value).append('\n'));
        return text.toString();
    }

    private static TreeSet<String> keysOf(List<Properties> runs, String part) {
        TreeSet<String> keys = new TreeSet<>();
        runs.forEach(run -> run.stringPropertyNames().stream().filter(key -> key.endsWith("." + part)).forEach(keys::add));
        return keys;
    }

    private static double value(Properties run, String key) {
        String value = run.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("A run has no " + key);
        }
        return Double.parseDouble(value.trim());
    }
}

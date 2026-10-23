package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Averages the times of runs of the performance tests on one architecture into its reference times, which it prints.
 * Each argument is the {@code performance-reference-times.properties} a run writes, and CI uploads. Each reference time is
 * the geometric mean of the runs, as {@link PerformanceRun} scales by ratios: the small document by the fixed piece of
 * work, the bulk processing service of a merge by the network, every other export by the basket of its run.
 * <p>
 * Given at least {@link #ELASTICITY_RUNS} runs, it also fits the elasticity of each part of each export but the small
 * document: how far its time moves when the basket of its run moves, over the runs. A layout which takes a slower
 * machine longer than the basket does has an elasticity above 1.
 * </p>
 * <p>
 * It needs no build: {@code java src/test/java/ch/sbb/polarion/extension/pdf_exporter/weasyprint/performance/AverageReferenceTimes.java runs/*.properties}
 * </p>
 */
public final class AverageReferenceTimes {

    private static final String CALIBRATION = "machine.calibration";
    private static final String NETWORK_CALIBRATION = "network.calibration";
    private static final String SMALL_DOCUMENT = "smallDocument";
    private static final String[] PARTS = {"exporter", "weasyprint"};

    /** The exports of the basket, as BasePerformanceTest times them: this program runs without the classes of the tests. */
    private static final String[] BASKET = {SMALL_DOCUMENT, "largeTable", "pageBreakSections", "longCells"};

    /** The runs an elasticity needs: fewer tell the elasticity of an export from the noise of its runs too badly. */
    static final int ELASTICITY_RUNS = 10;

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
        if (runs.size() >= ELASTICITY_RUNS) {
            for (String key : logs.keySet()) {
                String export = key.substring(0, key.indexOf('.'));
                String part = key.substring(key.indexOf('.') + 1);
                if (List.of(PARTS).contains(part) && !SMALL_DOCUMENT.equals(export)) {
                    references.put(key + ".elasticity", String.format(Locale.ROOT, "%.2f", elasticity(runs, logs, export, part)));
                }
            }
        }
        StringBuilder text = new StringBuilder()
                .append("# The reference times of the performance tests on one architecture, in ms: the geometric mean of ").append(runs.size())
                .append(" runs, made by AverageReferenceTimes.\n")
                .append("# Each test reads <export>.exporter and <export>.weasyprint, a merge <export>.bps too. The small document is scaled by the fixed piece of work of\n")
                .append("# machine.calibration, every other export by the basket of its run to the power of its <key>.elasticity, 1 where none is given,\n")
                .append("# the bps of a merge by network.calibration where it is given.\n")
                .append(CALIBRATION).append('=').append(references.remove(CALIBRATION)).append('\n');
        String network = references.remove(NETWORK_CALIBRATION);
        if (network != null) {
            text.append(NETWORK_CALIBRATION).append('=').append(network).append('\n');
        }
        references.forEach((key, value) -> text.append(key).append('=').append(value).append('\n'));
        return text.toString();
    }

    /**
     * The slope of the logarithm of the time of a part of an export over that of the basket of its run, each against its
     * mean: the basket of a run being the mean over the exports of the basket, the export itself left out.
     */
    private static double elasticity(List<Properties> runs, Map<String, Double> logs, String export, String part) {
        String key = export + "." + part;
        double products = 0;
        double squares = 0;
        for (Properties run : runs) {
            if (!run.containsKey(key)) {
                continue;
            }
            double basket = 0;
            int count = 0;
            for (String member : BASKET) {
                if (!member.equals(export)) {
                    String memberKey = member + "." + part;
                    basket += Math.log(value(run, memberKey)) - logs.get(memberKey);
                    count++;
                }
            }
            basket /= count;
            products += basket * (Math.log(value(run, key)) - logs.get(key));
            squares += basket * basket;
        }
        return squares == 0 ? 1 : products / squares;
    }

    private static double value(Properties run, String key) {
        String value = run.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("A run has no " + key);
        }
        return Double.parseDouble(value.trim());
    }
}

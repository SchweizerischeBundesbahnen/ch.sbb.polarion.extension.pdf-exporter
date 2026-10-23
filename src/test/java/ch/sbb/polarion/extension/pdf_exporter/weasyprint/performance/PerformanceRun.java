package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * One run of the performance tests, across all their classes. It tells how long each part of an export is expected to
 * take in this run, and writes a report of every export after the last test, the reference times first and the results
 * under them, and the times of the run in the form of the reference times.
 * <p>
 * The reference times are the geometric mean of runs of the tests on a machine of the architecture of this one: the arm64
 * of a Mac, or the amd64 of a runner of CI, each in {@code performance/reference-times-<architecture>.properties}, as
 * WeasyPrint runs in a container of the same architecture and lays documents out at a different pace on each. Each part of
 * an export is expected to take its reference time scaled by how the basket went in this run: a few exports of different
 * shapes, timed first, which whatever makes this machine or this moment faster or slower makes so too. One document alone
 * tells it worse: a small document spends its time on what every export costs, a large one on layout, and a runner can
 * be slower at one than at the other. The scale of an export is the geometric mean of how much longer than its reference
 * time each export of the basket took, the export itself left out.
 * </p>
 * <p>
 * A change which slows every export would slow the basket as well and go unseen that way, so the small document is scaled
 * by a fixed piece of JDK work instead, which runs no code of the exporter.
 * </p>
 * <p>
 * The bulk processing service of a merge is scaled by the network instead: each document crosses it twice, into the
 * service and from it to WeasyPrint, and Docker in a VM, as on Windows or a Mac, takes ten times as long there as Docker
 * on Linux, which neither the small document nor the fixed work shows. {@link NetworkCalibration} times it.
 * </p>
 * <p>
 * JUnit keeps the run in the store of the root context, which it closes once every test has run.
 * </p>
 */
public final class PerformanceRun implements AutoCloseable {

    static final String EXPORTER = "exporter";
    static final String WEASYPRINT = "WeasyPrint";

    /**
     * The bulk processing service of a merge, WeasyPrint aside: the upload of each document, what the service does with it
     * and the merge. A service as WeasyPrint is, it is judged as WeasyPrint, but scaled by the network the documents
     * cross, where the reference times hold a network.calibration.
     */
    static final String BPS = "BPS";

    /** The export of the basket which is scaled by the fixed piece of work itself. */
    static final String SMALL_DOCUMENT = "smallDocument";

    /** How the exporter may deviate from its expected time: it is our code, and varies by a few percent in CI. */
    private static final Tolerance EXPORTER_TOLERANCE = new Tolerance(1.2, 1.5);

    /** How WeasyPrint may deviate from its expected time: scaled by the basket, it varies by a few percent in CI, as the exporter. */
    private static final Tolerance WEASYPRINT_TOLERANCE = new Tolerance(1.2, 1.5);

    /** The reference times of the architecture of this machine, as runs of the tests on such a machine averaged them. */
    private static final String REFERENCE_TIMES = "/performance/reference-times-%s.properties";
    private static final String CALIBRATION_KEY = "machine.calibration";
    private static final String NETWORK_CALIBRATION_KEY = "network.calibration";

    private static final String REPORTS = "target/surefire-reports/";
    private static final String SUMMARY_FILE = REPORTS + "performance-summary.md";
    private static final String TIMES_FILE = REPORTS + "performance-reference-times.properties";

    private static final Pattern WORD = Pattern.compile("word(\\d)");

    private static PerformanceRun current;

    private final String architecture;
    private final String referenceTimesFile;
    private final Properties referenceTimes;
    private final long referenceCalibrationMs;
    private final long calibrationMs;
    private final double machineFactor;
    /** Null in reference times taken before the network was timed: the bulk processing service then follows WeasyPrint. */
    private final Long referenceNetworkMs;
    private Long networkMs;
    /** The time of each part of each export of the basket in this run, in the order the exports were timed. */
    private final Map<String, Map<String, Long>> basketMs = new LinkedHashMap<>();
    private final List<Row> rows = new ArrayList<>();

    /**
     * How many times its expected time a part may take: above the warning it is marked in the report and the log, above
     * the limit it fails.
     */
    private record Tolerance(double warning, double limit) {
    }

    /** One part of one export, as the report shows it. */
    private record Row(@NotNull String export, @NotNull String part, long referenceMs, double scale, long expectedMs, long timeMs, long warningMs, long limitMs) {
        @NotNull String result() {
            if (timeMs > limitMs) {
                return "over the limit";
            }
            return timeMs > warningMs ? "warning" : "ok";
        }

        /** How far the time is from the expected one, in whole percent. */
        @NotNull String difference() {
            long percent = expectedMs == 0 ? 0 : Math.round(((double) timeMs / expectedMs - 1) * 100);
            return percent == 0 ? "0 %" : "%+d %%".formatted(percent);
        }
    }

    /** Starts the run before the first test of a class which needs it: the store of the root context keeps one. */
    public static final class Extension implements BeforeAllCallback {
        @Override
        public void beforeAll(@NotNull ExtensionContext context) {
            current = context.getRoot().getStore(ExtensionContext.Namespace.create(PerformanceRun.class))
                    .computeIfAbsent(PerformanceRun.class);
        }
    }

    public PerformanceRun() {
        architecture = architecture(System.getProperty("os.arch"));
        referenceTimesFile = REFERENCE_TIMES.formatted(architecture);
        referenceTimes = readReferenceTimes(referenceTimesFile);
        referenceCalibrationMs = milliseconds(CALIBRATION_KEY);
        referenceNetworkMs = referenceTimes.containsKey(NETWORK_CALIBRATION_KEY) ? milliseconds(NETWORK_CALIBRATION_KEY) : null;
        calibrate();
        long best = Long.MAX_VALUE;
        for (int run = 0; run < 3; run++) {
            best = Math.min(best, calibrate());
        }
        calibrationMs = best;
        machineFactor = (double) calibrationMs / referenceCalibrationMs;
        log("Performance tests: the reference times of %s, from %s. The fixed work took %d ms here and %d ms where they were taken, so the small document is expected to take %.2f times its reference time"
                .formatted(architecture, referenceTimesFile, calibrationMs, referenceCalibrationMs, machineFactor));
    }

    /** The run of the tests, which the extension started. */
    static @NotNull PerformanceRun current() {
        if (current == null) {
            throw new IllegalStateException("A performance test runs without " + Extension.class.getName());
        }
        return current;
    }

    /** Records how an export of the basket went in this run, which every other export is scaled by. */
    synchronized void baseline(@NotNull String export, long exporterMs, long weasyPrintMs) {
        basketMs.put(export, Map.of(EXPORTER, exporterMs, WEASYPRINT, weasyPrintMs));
        log("Performance tests: %s of the basket took %d ms in the exporter and %d ms in WeasyPrint, %.2f and %.2f times its reference times"
                .formatted(export, exporterMs, weasyPrintMs, basketRatio(export, EXPORTER), basketRatio(export, WEASYPRINT)));
    }

    /** Records how long a document of a merge took on the network in this run, which the bulk processing service is scaled by. */
    synchronized void network(long ms) {
        if (networkMs != null) {
            return;
        }
        networkMs = ms;
        log(referenceNetworkMs == null
                ? "Performance tests: a document of a merge took %d ms on the network here; %s has no %s, so the bulk processing service is scaled as WeasyPrint"
                .formatted(ms, referenceTimesFile, NETWORK_CALIBRATION_KEY)
                : "Performance tests: a document of a merge took %d ms on the network here and %d ms where the reference times were taken, so the bulk processing service is expected to take %.2f times its reference times"
                .formatted(ms, referenceNetworkMs, scale("", BPS)));
    }

    /** The reference time of a part of an export, from the reference times of this architecture. */
    long reference(@NotNull String export, @NotNull String part) {
        return milliseconds(key(export, part));
    }

    /**
     * What the reference time of a part is multiplied by in this run: for the small document the factor of the fixed
     * work, for the bulk processing service the factor of the network, for any other export the geometric mean of how
     * much longer than its reference time each other export of the basket took here.
     */
    double scale(@NotNull String export, @NotNull String part) {
        if (SMALL_DOCUMENT.equals(export)) {
            return machineFactor;
        }
        if (BPS.equals(part) && networkMs != null && referenceNetworkMs != null) {
            return (double) networkMs / referenceNetworkMs;
        }
        double logs = 0;
        int count = 0;
        for (String member : basketMs.keySet()) {
            if (!member.equals(export)) {
                logs += Math.log(basketRatio(member, basketPart(part)));
                count++;
            }
        }
        if (count == 0) {
            throw new IllegalStateException("The basket was not timed before " + export);
        }
        return Math.exp(logs / count);
    }

    /** How many times its reference time a part of an export of the basket took in this run. */
    private double basketRatio(@NotNull String export, @NotNull String part) {
        return (double) basketMs.get(export).get(part) / reference(export, part);
    }

    /** The time a part of an export is expected to take in this run: its reference time times its scale. */
    long expected(@NotNull String export, @NotNull String part) {
        return Math.round(reference(export, part) * scale(export, part));
    }

    /** The time a part of an export may take in this run before it fails. */
    long limit(@NotNull String export, @NotNull String part) {
        return Math.round(expected(export, part) * tolerance(part).limit());
    }

    /** Records a part of an export for the report, before it is checked against its limit. */
    synchronized void add(@NotNull String export, @NotNull String part, long timeMs) {
        long expected = expected(export, part);
        rows.add(new Row(export, part, reference(export, part), scale(export, part), expected, timeMs,
                Math.round(expected * tolerance(part).warning()), limit(export, part)));
    }

    @Override
    public void close() {
        String report = report();
        log(System.lineSeparator() + report);
        write(SUMMARY_FILE, report);
        write(TIMES_FILE, times());
        // A line of this form becomes an annotation of the run in GitHub Actions, and is plain text anywhere else
        rows.stream().filter(row -> "warning".equals(row.result())).forEach(row -> log("::warning title=Performance::%s, %s: %d ms, %s against the %d ms expected"
                .formatted(row.export(), row.part(), row.timeMs(), row.difference(), row.expectedMs())));
        current = null;
    }

    /** The report in Markdown: the reference times and what this run makes of them first, the results under them. */
    private @NotNull String report() {
        StringBuilder report = new StringBuilder()
                .append("### Performance tests%n%n#### Reference times%n%n".formatted())
                .append(("The reference times are those of %s, the geometric mean of runs on that architecture. The small document is expected at its reference time times %.2f: "
                        + "the fixed work took %d ms here and %d ms there. The other exports are expected at their reference times scaled by the basket of this run: "
                        + "the geometric mean of how much longer than its reference time each export of the basket took, the export itself left out.%n%n")
                        .formatted(architecture, machineFactor, calibrationMs, referenceCalibrationMs))
                .append(basketReport())
                .append(networkReport())
                .append("| Export | Part | Reference, ms | Scaled by | Expected here, ms |%n|---|---|---:|---:|---:|%n".formatted());
        for (Row row : rows) {
            report.append("| %s | %s | %d | %.2f | %d |%n".formatted(row.export(), row.part(), row.referenceMs(), row.scale(), row.expectedMs()));
        }
        report.append("%n#### Results%n%n".formatted())
                .append(("Each time is the median of %d exports. The exporter is a warning above %s times its expected time and fails above %s times, "
                        + "WeasyPrint and the bulk processing service of a merge a warning above %s times and fails above %s times.%n%n")
                        .formatted(BasePerformanceTest.RUNS, times(EXPORTER_TOLERANCE.warning()), times(EXPORTER_TOLERANCE.limit()),
                                times(WEASYPRINT_TOLERANCE.warning()), times(WEASYPRINT_TOLERANCE.limit())))
                .append("| Export | Part | Expected, ms | Time, ms | Against expected | Warning above, ms | Limit, ms | Result |%n|---|---|---:|---:|---:|---:|---:|---|%n".formatted());
        for (Row row : rows) {
            report.append("| %s | %s | %d | %d | %s | %d | %d | %s |%n".formatted(row.export(), row.part(), row.expectedMs(), row.timeMs(),
                    row.difference(), row.warningMs(), row.limitMs(), row.result()));
        }
        return report.toString();
    }

    /** The exports of the basket in this run against their reference times, for the report. */
    private @NotNull String basketReport() {
        StringBuilder report = new StringBuilder("| Basket | Exporter, ms | Reference, ms | WeasyPrint, ms | Reference, ms |%n|---|---:|---:|---:|---:|%n".formatted());
        basketMs.forEach((export, timesMs) -> report.append("| %s | %d | %d | %d | %d |%n".formatted(export, timesMs.get(EXPORTER), reference(export, EXPORTER),
                timesMs.get(WEASYPRINT), reference(export, WEASYPRINT))));
        return report.append("%n".formatted()).toString();
    }

    /** How the bulk processing service is scaled, for the report: empty when no merge ran. */
    private @NotNull String networkReport() {
        if (networkMs == null) {
            return "";
        }
        if (referenceNetworkMs == null) {
            return "A document of a merge took %d ms on the network here. The reference times have no %s, so the bulk processing service is scaled as WeasyPrint.%n%n"
                    .formatted(networkMs, NETWORK_CALIBRATION_KEY);
        }
        return "The bulk processing service is expected at its reference times times %.2f: a document of a merge took %d ms on the network here and %d ms there.%n%n"
                .formatted(scale("", BPS), networkMs, referenceNetworkMs);
    }

    /** The times of this run in the form of the reference times, which {@link AverageReferenceTimes} averages over runs. */
    private @NotNull String times() {
        Map<String, Long> sorted = new TreeMap<>();
        // The basket of the run is written whether its own tests ran or not, as every other export is scaled by it
        basketMs.forEach((export, timesMs) -> timesMs.forEach((part, timeMs) -> sorted.put(key(export, part), timeMs)));
        rows.forEach(row -> sorted.put(key(row.export(), row.part()), row.timeMs()));
        StringBuilder times = new StringBuilder("# The times of a run of the performance tests on %s, in the form of %s%n".formatted(architecture, referenceTimesFile.substring(1)))
                .append("%s=%d%n".formatted(CALIBRATION_KEY, calibrationMs));
        if (networkMs != null) {
            times.append("%s=%d%n".formatted(NETWORK_CALIBRATION_KEY, networkMs));
        }
        sorted.forEach((key, value) -> times.append("%s=%d%n".formatted(key, value)));
        return times.toString();
    }

    private static @NotNull Tolerance tolerance(@NotNull String part) {
        return EXPORTER.equals(part) ? EXPORTER_TOLERANCE : WEASYPRINT_TOLERANCE;
    }

    /** The part of the basket a part is scaled by: the bulk processing service, a service too, follows WeasyPrint. */
    private static @NotNull String basketPart(@NotNull String part) {
        return BPS.equals(part) ? WEASYPRINT : part;
    }

    private static @NotNull String key(@NotNull String export, @NotNull String part) {
        return export + "." + switch (part) {
            case EXPORTER -> "exporter";
            case BPS -> "bps";
            default -> "weasyprint";
        };
    }

    private static @NotNull String times(double tolerance) {
        return tolerance == Math.rint(tolerance) ? String.valueOf((long) tolerance) : String.valueOf(tolerance);
    }

    /** A time of the reference times, which fails naming the key where it is missing or no number. */
    private long milliseconds(@NotNull String key) {
        String value = referenceTimes.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("No time " + key + " in " + referenceTimesFile);
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("The time " + key + " in " + referenceTimesFile + " is no number of milliseconds: '" + value + "'", e);
        }
    }

    /**
     * The architecture whose reference times this machine is judged by: arm64 for a Mac, amd64 for a runner of CI. WeasyPrint
     * runs in a container of the same architecture, and lays documents out at a different pace on each, which no measure of
     * the JVM alone tells.
     */
    static @NotNull String architecture(@NotNull String osArch) {
        return switch (osArch) {
            case "aarch64", "arm64" -> "arm64";
            case "amd64", "x86_64" -> "amd64";
            default -> throw new IllegalStateException("No reference times for the architecture " + osArch + ": only arm64 and amd64 have them");
        };
    }

    @SneakyThrows
    private static @NotNull Properties readReferenceTimes(@NotNull String file) {
        Properties properties = new Properties();
        try (InputStream stream = PerformanceRun.class.getResourceAsStream(file)) {
            properties.load(Objects.requireNonNull(stream, file));
        }
        return properties;
    }

    @SneakyThrows
    private static void write(@NotNull String file, @NotNull String text) {
        Path path = Path.of(file);
        Files.createDirectories(path.getParent());
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("java:S106") // The report belongs in the log of the build, which is what a performance run is read in
    private static void log(@NotNull String text) {
        System.out.println(text);
    }

    /**
     * Times a fixed piece of work of the JDK alone, which no change of the exporter or of its libraries makes slower. It
     * tells how fast this machine is, for the small document. Besides hashing and sorting, it builds and walks many small
     * objects and runs a regular expression over a long text, as an export does with the DOM of a document: a machine
     * whose memory is slower than its processor is slower at that.
     */
    @SneakyThrows
    private static long calibrate() {
        long start = System.nanoTime();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] block = new byte[1 << 20];
        new Random(42).nextBytes(block);
        for (int round = 0; round < 100; round++) {
            digest.update(block);
        }
        int[] numbers = new Random(42).ints(2_000_000).toArray();
        Arrays.sort(numbers);
        Map<String, Integer> words = new HashMap<>();
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < 1_500_000; index++) {
            String word = "word" + index % 50_000;
            words.merge(word, 1, Integer::sum);
            text.append(word).append(' ');
        }
        String shortened = WORD.matcher(text).replaceAll("w$1");
        consume(digest.digest(), numbers, words, shortened);
        return (System.nanoTime() - start) / 1_000_000;
    }

    /** Keeps the result of the work, so that the JIT cannot drop the work. */
    private static void consume(byte @NotNull [] hash, int @NotNull [] numbers, @NotNull Map<String, Integer> words, @NotNull String text) {
        if (hash.length != 32 || numbers.length == 0 || words.size() != 50_000 || text.isEmpty()) {
            throw new IllegalStateException("The fixed work did not run");
        }
    }
}

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
 * The reference times are the average of runs of the tests on a machine of the architecture of this one: the arm64 of a
 * Mac, or the amd64 of a runner of CI, each in {@code performance/reference-times-<architecture>.properties}, as WeasyPrint
 * runs in a container of the same architecture and lays documents out at a different pace on each. Each part of an export is expected to
 * take its reference time scaled by how the small document went in this run: the small document is exported first, and
 * whatever makes this machine or this moment faster or slower makes it so too. A change which slows every export would
 * slow the small document as well and go unseen that way, so the small document itself is scaled by a fixed piece of JDK
 * work instead, which runs no code of the exporter.
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
     * and the merge. A service as WeasyPrint is, it is scaled and judged as WeasyPrint.
     */
    static final String BPS = "BPS";

    /** The export which the others are scaled by, and which is scaled by the fixed piece of work itself. */
    static final String SMALL_DOCUMENT = "smallDocument";

    /** How the exporter may deviate from its expected time: it is our code, and varies by a few percent in CI. */
    private static final Tolerance EXPORTER_TOLERANCE = new Tolerance(1.2, 1.5);

    /** How WeasyPrint may deviate from its expected time: a service of its own, which varies by up to a fifth in CI. */
    private static final Tolerance WEASYPRINT_TOLERANCE = new Tolerance(1.35, 2);

    /** The reference times of the architecture of this machine, as runs of the tests on such a machine averaged them. */
    private static final String REFERENCE_TIMES = "/performance/reference-times-%s.properties";
    private static final String CALIBRATION_KEY = "machine.calibration";

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
    private final Map<String, Long> baselineMs = new HashMap<>();
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

    /** Records how the small document went in this run, which every other export is scaled by. */
    synchronized void baseline(long exporterMs, long weasyPrintMs) {
        baselineMs.put(EXPORTER, exporterMs);
        baselineMs.put(WEASYPRINT, weasyPrintMs);
        log("Performance tests: the small document took %d ms in the exporter and %d ms in WeasyPrint, so the other exports are expected to take %.2f and %.2f times their reference times"
                .formatted(exporterMs, weasyPrintMs, scale("", EXPORTER), scale("", WEASYPRINT)));
    }

    /** The reference time of a part of an export, from the reference times of this architecture. */
    long reference(@NotNull String export, @NotNull String part) {
        return milliseconds(key(export, part));
    }

    /**
     * What the reference time of a part is multiplied by in this run: for the small document the factor of the fixed
     * work, for any other export how much longer or shorter than its own reference the small document took here.
     */
    double scale(@NotNull String export, @NotNull String part) {
        if (SMALL_DOCUMENT.equals(export)) {
            return machineFactor;
        }
        Long baseline = baselineMs.get(baselinePart(part));
        if (baseline == null) {
            throw new IllegalStateException("The small document was not timed before " + export);
        }
        return (double) baseline / reference(SMALL_DOCUMENT, baselinePart(part));
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
                .append(("The reference times are those of %s, the average of runs on that architecture. The small document is expected at its reference time times %.2f: "
                        + "the fixed work took %d ms here and %d ms there. The other exports are expected at their reference times scaled by the small document of this run: "
                        + "%d ms in the exporter and %d ms in WeasyPrint, against its reference times of %d ms and %d ms.%n%n")
                        .formatted(architecture, machineFactor, calibrationMs, referenceCalibrationMs, baselineMs.getOrDefault(EXPORTER, 0L), baselineMs.getOrDefault(WEASYPRINT, 0L),
                                reference(SMALL_DOCUMENT, EXPORTER), reference(SMALL_DOCUMENT, WEASYPRINT)))
                .append("| Export | Part | Reference, ms | Scaled by | Expected here, ms |%n|---|---|---:|---:|---:|%n".formatted());
        for (Row row : rows) {
            report.append("| %s | %s | %d | %.2f | %d |%n".formatted(row.export(), row.part(), row.referenceMs(), row.scale(), row.expectedMs()));
        }
        report.append("%n#### Results%n%n".formatted())
                .append(("Each time is the average of %d exports. The exporter is a warning above %s times its expected time and fails above %s times, "
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

    /** The times of this run in the form of the reference times, which {@link AverageReferenceTimes} averages over runs. */
    private @NotNull String times() {
        Map<String, Long> sorted = new TreeMap<>();
        // The small document of the run is written whether its own test ran or not, as every other time is averaged against it
        baselineMs.forEach((part, timeMs) -> sorted.put(key(SMALL_DOCUMENT, part), timeMs));
        rows.forEach(row -> sorted.put(key(row.export(), row.part()), row.timeMs()));
        StringBuilder times = new StringBuilder("# The times of a run of the performance tests on %s, in the form of %s%n".formatted(architecture, referenceTimesFile.substring(1)))
                .append("%s=%d%n".formatted(CALIBRATION_KEY, calibrationMs));
        sorted.forEach((key, value) -> times.append("%s=%d%n".formatted(key, value)));
        return times.toString();
    }

    private static @NotNull Tolerance tolerance(@NotNull String part) {
        return EXPORTER.equals(part) ? EXPORTER_TOLERANCE : WEASYPRINT_TOLERANCE;
    }

    /** The part of the small document a part is scaled by: the bulk processing service, a service too, follows WeasyPrint. */
    private static @NotNull String baselinePart(@NotNull String part) {
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

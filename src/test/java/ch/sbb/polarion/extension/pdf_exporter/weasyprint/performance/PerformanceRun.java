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
 * One run of the performance tests, across all their classes. Before the first test it measures how fast this machine
 * is against the machine of the reference times and writes that factor to the log. Each part of an export is expected
 * to take its reference time times the factor here, and is judged against that expected time. After the last test it
 * writes a report to the log and to a file, the reference times first and the results under them, and the times of the
 * run in the form of the reference times.
 * <p>
 * The reference times are in {@value #REFERENCE_TIMES}. JUnit keeps the run in the store of the root context, which it
 * closes once every test has run.
 * </p>
 */
public final class PerformanceRun implements AutoCloseable {

    /** How many times its expected time the exporter may take before it fails: it is our code. */
    static final double EXPORTER_TOLERANCE = 2;

    /** How many times its expected time WeasyPrint may take before it fails: a service of its own, which varies more. */
    static final double WEASYPRINT_TOLERANCE = 3;

    /** How many times its expected time a part may take before the report and the log mark it, without failing. */
    static final double WARNING_TOLERANCE = 1.5;

    static final String EXPORTER = "exporter";
    static final String WEASYPRINT = "WeasyPrint";

    private static final String REFERENCE_TIMES = "/performance/reference-times.properties";
    private static final String CALIBRATION_KEY = "machine.calibration";

    private static final String REPORTS = "target/surefire-reports/";
    private static final String SUMMARY_FILE = REPORTS + "performance-summary.md";
    private static final String TIMES_FILE = REPORTS + "performance-reference-times.properties";

    private static final Pattern WORD = Pattern.compile("word(\\d)");

    private static PerformanceRun current;

    private final Properties referenceTimes;
    private final long referenceCalibrationMs;
    private final long calibrationMs;
    private final double factor;
    private final List<Row> rows = new ArrayList<>();

    /** One part of one export, as the report shows it. */
    private record Row(@NotNull String export, @NotNull String part, long referenceMs, long expectedMs, long timeMs, long warningMs, long limitMs) {
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
        referenceTimes = readReferenceTimes();
        referenceCalibrationMs = milliseconds(CALIBRATION_KEY);
        calibrate();
        long best = Long.MAX_VALUE;
        for (int run = 0; run < 3; run++) {
            best = Math.min(best, calibrate());
        }
        calibrationMs = best;
        factor = (double) calibrationMs / referenceCalibrationMs;
        log("Performance tests: the fixed work took %d ms here and %d ms on the machine of the reference times, so each export is expected to take %.2f times its reference time"
                .formatted(calibrationMs, referenceCalibrationMs, factor));
    }

    /** The run of the tests, which the extension started. */
    static @NotNull PerformanceRun current() {
        if (current == null) {
            throw new IllegalStateException("A performance test runs without " + Extension.class.getName());
        }
        return current;
    }

    /** How much slower than the machine of the reference times this one is, below one where it is faster. */
    double factor() {
        return factor;
    }

    /** The reference time of a part of an export, from {@value #REFERENCE_TIMES}. */
    long reference(@NotNull String export, @NotNull String part) {
        return milliseconds(key(export, part));
    }

    /** A time of {@value #REFERENCE_TIMES}, which fails naming the key where it is missing or no number. */
    private long milliseconds(@NotNull String key) {
        String value = referenceTimes.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("No time " + key + " in " + REFERENCE_TIMES);
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("The time " + key + " in " + REFERENCE_TIMES + " is no number of milliseconds: '" + value + "'", e);
        }
    }

    /** The time a part of an export is expected to take on this machine: its reference time times the factor. */
    long expected(@NotNull String export, @NotNull String part) {
        return Math.round(reference(export, part) * factor);
    }

    /** The time a part of an export may take on this machine, at the given multiple of its expected time. */
    long limit(@NotNull String export, @NotNull String part, double tolerance) {
        return Math.round(expected(export, part) * tolerance);
    }

    /** Records a part of an export for the report, before it is checked against its limit. */
    synchronized void add(@NotNull String export, @NotNull String part, long timeMs, double tolerance) {
        rows.add(new Row(export, part, reference(export, part), expected(export, part), timeMs,
                limit(export, part, WARNING_TOLERANCE), limit(export, part, tolerance)));
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

    /** The report in Markdown: the reference times and what they make of this machine first, the results under them. */
    private @NotNull String report() {
        StringBuilder report = new StringBuilder()
                .append("### Performance tests%n%n#### Reference times%n%n".formatted())
                .append("The fixed work took %d ms here and %d ms on the machine of the reference times: a factor of %.2f. Expected is the reference time times the factor.%n%n"
                        .formatted(calibrationMs, referenceCalibrationMs, factor))
                .append("| Export | Part | Reference, ms | Expected here, ms |%n|---|---|---:|---:|%n".formatted());
        for (Row row : rows) {
            report.append("| %s | %s | %d | %d |%n".formatted(row.export(), row.part(), row.referenceMs(), row.expectedMs()));
        }
        report.append("%n#### Results%n%n".formatted())
                .append("Each time is the average of %d exports. The exporter fails at %s times its expected time, WeasyPrint at %s times, and either is marked as a warning above %s times.%n%n"
                        .formatted(BasePerformanceTest.RUNS, times(EXPORTER_TOLERANCE), times(WEASYPRINT_TOLERANCE), times(WARNING_TOLERANCE)))
                .append("| Export | Part | Expected, ms | Time, ms | Against expected | Warning above, ms | Limit, ms | Result |%n|---|---|---:|---:|---:|---:|---:|---|%n".formatted());
        for (Row row : rows) {
            report.append("| %s | %s | %d | %d | %s | %d | %d | %s |%n".formatted(row.export(), row.part(), row.expectedMs(), row.timeMs(),
                    row.difference(), row.warningMs(), row.limitMs(), row.result()));
        }
        return report.toString();
    }

    /** The times of this run in the form of the reference times, to take as new reference times when this is the machine. */
    private @NotNull String times() {
        Map<String, Long> sorted = new TreeMap<>();
        rows.forEach(row -> sorted.put(key(row.export(), row.part()), row.timeMs()));
        StringBuilder times = new StringBuilder("# The times of a run of the performance tests, in the form of %s%n".formatted(REFERENCE_TIMES.substring(1)))
                .append("%s=%d%n".formatted(CALIBRATION_KEY, calibrationMs));
        sorted.forEach((key, value) -> times.append("%s=%d%n".formatted(key, value)));
        return times.toString();
    }

    private static @NotNull String key(@NotNull String export, @NotNull String part) {
        return export + "." + (EXPORTER.equals(part) ? "exporter" : "weasyprint");
    }

    private static @NotNull String times(double tolerance) {
        return tolerance == Math.rint(tolerance) ? String.valueOf((long) tolerance) : String.valueOf(tolerance);
    }

    @SneakyThrows
    private static @NotNull Properties readReferenceTimes() {
        Properties properties = new Properties();
        try (InputStream stream = PerformanceRun.class.getResourceAsStream(REFERENCE_TIMES)) {
            properties.load(Objects.requireNonNull(stream, REFERENCE_TIMES));
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
     * tells how fast this machine is, for the reference times. Besides hashing and sorting, it builds and walks many small
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

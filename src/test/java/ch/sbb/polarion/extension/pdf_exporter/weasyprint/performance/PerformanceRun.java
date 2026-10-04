package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * One run of the performance tests, across all their classes. Before the first test it measures how fast the machine
 * is and writes the factor which scales every reference time to the log. After the last test it writes a table of every
 * export: its time, its reference time, its limit and how far it is from the reference.
 * <p>
 * JUnit keeps it in the store of the root context, which it closes once every test has run.
 * </p>
 */
public final class PerformanceRun implements AutoCloseable {

    /** What the fixed piece of work takes on the machine the reference times were taken on, an arm64 Mac, in ms. */
    static final long CALIBRATION_MS = 213;

    /** How many times its reference time an export may take on this machine before it fails. */
    static final int TOLERANCE = 3;

    private static final Pattern WORD = Pattern.compile("word(\\d)");

    private static final String SUMMARY_FILE = "target/surefire-reports/performance-summary.md";

    /** How far above its reference an export is worth a look, in percent: a third of the way to its limit. */
    private static final long NOTABLE_DIFFERENCE = 100;

    private static final String RED = "\u001B[1;31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RESET = "\u001B[0m";

    private static PerformanceRun current;

    private final long calibrationMs;
    private final double scale;
    private final List<Row> rows = new ArrayList<>();

    /** One part of one export, as the table shows it. */
    private record Row(@NotNull String export, @NotNull String part, long timeMs, long referenceMs, long limitMs) {
        boolean withinLimit() {
            return timeMs <= limitMs;
        }

        /** How far the time is from the reference, in whole percent. */
        long difference() {
            return referenceMs == 0 ? 0 : Math.round(((double) timeMs / referenceMs - 1) * 100);
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
        calibrate();
        long best = Long.MAX_VALUE;
        for (int run = 0; run < 3; run++) {
            best = Math.min(best, calibrate());
        }
        calibrationMs = best;
        scale = Math.max(1d, (double) best / CALIBRATION_MS);
        log("Performance tests: the fixed work took %d ms, %d ms on the machine of the reference times, so the reference times are scaled by %.2f"
                .formatted(calibrationMs, CALIBRATION_MS, scale));
    }

    /** The run of the tests, which the extension started. */
    static @NotNull PerformanceRun current() {
        if (current == null) {
            throw new IllegalStateException("A performance test runs without " + Extension.class.getName());
        }
        return current;
    }

    /** How much slower than the machine of the reference times this one is, never less than one. */
    double scale() {
        return scale;
    }

    /** The reference time of a part of an export, scaled to this machine. */
    long reference(long referenceMs) {
        return Math.round(referenceMs * scale);
    }

    /** The time a part of an export may take on this machine. */
    long limit(long referenceMs) {
        return reference(referenceMs) * TOLERANCE;
    }

    /** Records a part of an export for the table, before it is checked against its limit. */
    synchronized void add(@NotNull String export, @NotNull String part, long timeMs, long referenceMs) {
        rows.add(new Row(export, part, timeMs, reference(referenceMs), limit(referenceMs)));
    }

    @Override
    public void close() {
        log(System.lineSeparator() + table(colored()));
        write(table(false));
        current = null;
    }

    /**
     * The table in Markdown. Colored, it marks only what needs a look: an export over its limit in red, one far above its
     * reference in yellow.
     */
    private @NotNull String table(boolean colored) {
        StringBuilder table = new StringBuilder()
                .append("### Performance tests%n%n".formatted())
                .append("The fixed work took %d ms, %d ms on the machine of the reference times: reference times scaled by %.2f, limit %d times the reference.%n%n"
                        .formatted(calibrationMs, CALIBRATION_MS, scale, TOLERANCE))
                .append("| Export | Part | Time, ms | Reference, ms | Limit, ms | Against the reference | Result |%n".formatted())
                .append("|---|---|---:|---:|---:|---:|---|%n".formatted());
        for (Row row : rows) {
            String difference = row.difference() == 0 ? "0 %" : "%+d %%".formatted(row.difference());
            String result = row.withinLimit() ? "ok" : "over the limit";
            if (colored && !row.withinLimit()) {
                difference = RED + difference + RESET;
                result = RED + result + RESET;
            } else if (colored && row.difference() > NOTABLE_DIFFERENCE) {
                difference = YELLOW + difference + RESET;
            }
            table.append("| %s | %s | %d | %d | %d | %s | %s |%n".formatted(row.export(), row.part(), row.timeMs(), row.referenceMs(), row.limitMs(),
                    difference, result));
        }
        return table.toString();
    }

    /** Whether the log is colored: it is, unless {@code NO_COLOR} says otherwise, as https://no-color.org asks. */
    private static boolean colored() {
        String noColor = System.getenv("NO_COLOR");
        return noColor == null || noColor.isEmpty();
    }

    @SneakyThrows
    private static void write(@NotNull String table) {
        Path file = Path.of(SUMMARY_FILE);
        Files.createDirectories(file.getParent());
        Files.writeString(file, table, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("java:S106") // The table belongs in the log of the build, which is what a performance run is read in
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

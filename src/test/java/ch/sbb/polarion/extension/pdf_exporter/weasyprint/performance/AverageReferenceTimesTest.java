package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class AverageReferenceTimesTest {

    @Test
    void takesTheGeometricMeanOfTheRuns() {
        String references = AverageReferenceTimes.average(List.of(
                run("machine.calibration", "100", "smallDocument.exporter", "100"),
                run("machine.calibration", "400", "smallDocument.exporter", "900")));

        assertThat(references).contains("machine.calibration=200\n", "smallDocument.exporter=300\n");
    }

    /** The bulk processing service of a merge is scaled by the network, so its time and the network come from the same runs. */
    @Test
    void takesTheTimesOfAMergeOnlyFromTheRunsWhichMeasuredTheNetwork() {
        String references = AverageReferenceTimes.average(List.of(
                run("machine.calibration", "100", "network.calibration", "50", "mergeLargeTables.bps", "1000", "mergeLargeTables.exporter", "100"),
                run("machine.calibration", "100", "mergeLargeTables.bps", "4000", "mergeLargeTables.exporter", "400")));

        assertThat(references).contains("network.calibration=50\n", "mergeLargeTables.bps=1000\n", "mergeLargeTables.exporter=200\n");
    }

    @Test
    void takesTheTimesOfAMergeFromEveryRunWhereNoneMeasuredTheNetwork() {
        String references = AverageReferenceTimes.average(List.of(
                run("machine.calibration", "100", "mergeLargeTables.bps", "1000"),
                run("machine.calibration", "100", "mergeLargeTables.bps", "4000")));

        assertThat(references).contains("mergeLargeTables.bps=2000\n").doesNotContain("network.calibration=");
    }

    private static @NotNull Properties run(@NotNull String... keysAndValues) {
        Properties run = new Properties();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            run.setProperty(keysAndValues[i], keysAndValues[i + 1]);
        }
        return run;
    }
}

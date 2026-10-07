package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolarionTokenIssuerTest {

    @Test
    void shouldGiveTheTokenPolarionSigned() {
        PolarionTokenIssuer issuer = new PolarionTokenIssuer() {
            @Override
            protected @NotNull String sign(@NotNull String user, @Nullable String jobId) {
                return "signed-for-" + user + "-" + jobId;
            }
        };

        assertThat(issuer.issue("alice", "job-1")).isEqualTo("signed-for-alice-job-1");
        assertThat(issuer.issue("alice", null)).isEqualTo("signed-for-alice-null");
    }

    @Test
    void shouldGiveNoTokenWhenPolarionCannotSign() {
        PolarionTokenIssuer failing = new PolarionTokenIssuer() {
            @Override
            protected @NotNull String sign(@NotNull String user, @Nullable String jobId) {
                throw new IllegalStateException("no key yet for " + user);
            }
        };

        assertThat(failing.issue("alice", "job-1")).isNull();
    }

    @Test
    void shouldGiveNoTokenWhenPolarionHasNoTokenProvider() {
        PolarionTokenIssuer missing = new PolarionTokenIssuer() {
            @Override
            protected @NotNull String sign(@NotNull String user, @Nullable String jobId) {
                // what an older Polarion does: the class it is asked for is not there
                throw new NoClassDefFoundError("com/polarion/platform/security/auth/JwtTokenProvider");
            }
        };

        assertThat(missing.issue("alice", null)).isNull();
    }

    @Test
    void shouldFixTheClaimNames() {
        // the key also signs Polarion's own license and cluster tokens: the claims are ours, and their names are fixed
        assertThat(PolarionTokenIssuer.SERVICE_CLAIM).isEqualTo("svc");
        assertThat(PolarionTokenIssuer.JOB_CLAIM).isEqualTo("job");
        assertThat(PolarionTokenIssuer.SERVICE_NAME).isEqualTo("bulk-processing-service");
        assertThat(PolarionTokenIssuer.LIFETIME_MILLIS).isEqualTo(5 * 60 * 1000L);
    }
}

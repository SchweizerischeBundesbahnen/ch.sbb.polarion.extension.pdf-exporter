package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import com.polarion.core.util.logging.Logger;
import com.polarion.platform.security.auth.JwtTokenProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Asks Polarion for the token which tells the bulk processing service who a merge is made for.
 * <p>
 * Polarion signs it with the key it publishes as a JWKS, so the service verifies it against that key set and no secret is
 * shared with it. The key never reaches the extension: Polarion signs, the extension only names the claims:
 * <ul>
 *   <li>{@code sub}: the user the merge is made for;</li>
 *   <li>{@code job}: the job the token is for, absent for the call which starts one;</li>
 *   <li>{@code svc}: the service it is for, so a token cannot be used for another one;</li>
 *   <li>{@code exp}: it lives for minutes, and a token is made for every call.</li>
 * </ul>
 * The key signs tokens of Polarion's own too (those of its license and cluster handling), which are told apart by their
 * claims. The claim names are therefore fixed here and never taken from a caller, and only values the extension
 * itself works out go into them.
 * <p>
 * A Polarion without {@link JwtTokenProvider}, or one which cannot sign right now, gives no token and the call goes on
 * without: a service which does not check tokens does not need one, and one which does answers 401 with a message which says so.
 */
public class PolarionTokenIssuer {
    private static final Logger logger = Logger.getLogger(PolarionTokenIssuer.class);

    @VisibleForTesting
    static final String SERVICE_CLAIM = "svc";
    @VisibleForTesting
    static final String JOB_CLAIM = "job";
    @VisibleForTesting
    static final String SERVICE_NAME = "bulk-processing-service";
    @VisibleForTesting
    static final Duration LIFETIME = Duration.ofMinutes(5);

    /**
     * @param user  the user the merge is made for
     * @param jobId the job the token is for, {@code null} for the call which starts one
     * @return the token, or {@code null} where Polarion cannot give one
     */
    public @Nullable String issue(@NotNull String user, @Nullable String jobId) {
        try {
            return sign(user, jobId);
        } catch (Exception | LinkageError e) {
            // never the user or the claims: only what failed
            logger.warn("Polarion could not issue a token for the bulk processing service: " + e.getClass().getName());
            return null;
        }
    }

    @VisibleForTesting
    protected @NotNull String sign(@NotNull String user, @Nullable String jobId) {
        JwtTokenProvider.TokenBuilder builder = JwtTokenProvider.provideToken().withClaim(SERVICE_CLAIM, SERVICE_NAME);
        if (jobId != null) {
            builder = builder.withClaim(JOB_CLAIM, jobId);
        }
        return builder.withExpiration(Date.from(Instant.now().plus(LIFETIME))).signFor(user);
    }
}

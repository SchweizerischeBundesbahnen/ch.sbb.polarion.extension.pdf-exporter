package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.core.util.vault.PolarionSecretsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Supplies the API key of the WeasyPrint service.
 * <p>
 * The key itself is never configured in {@code polarion.properties}. The property
 * {@code weasyprint.apiKeySecret} holds the <em>name</em> of a Polarion secret, and the value behind
 * that name is read here, so neither the properties file nor the About page carries the credential.
 * <p>
 * An unset name means the service needs no key, which is how the service is configured by default.
 */
public class ApiKeyProvider {

    /**
     * Reads the API key configured for the WeasyPrint service.
     * <p>
     * Where a name is configured but no value can be read for it, or the value is empty, this fails
     * with a user friendly exception, so the reason survives the catch-all of the conversion paths
     * and reaches the export dialog, where the person who can fix the configuration reads it.
     *
     * @return the key, or {@code null} when no secret name is configured
     */
    public @Nullable String getApiKey() {
        String configured = PdfExporterExtensionConfiguration.getInstance().getWeasyPrintApiKeySecret();
        String secretName = configured == null ? "" : configured.trim();
        if (secretName.isEmpty()) {
            return null;
        }

        String apiKey;
        try {
            apiKey = readSecret(secretName);
        } catch (Exception e) {
            // The message names the secret and what failed, never the value and never the text of the
            // original failure: a secrets manager is free to quote the credential in its own message,
            // and a cause carries that text into every log which prints the chain.
            throw new UserFriendlyRuntimeException(String.format(
                    "Could not read the WeasyPrint API key from the Polarion secret '%s' (%s)", secretName, e.getClass().getName()));
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new UserFriendlyRuntimeException(String.format("The Polarion secret '%s', configured as the WeasyPrint API key, is empty or does not exist", secretName));
        }
        // Returned as stored: a credential is not ours to reshape.
        return apiKey;
    }

    @VisibleForTesting
    protected @Nullable String readSecret(@NotNull String secretName) {
        return PolarionSecretsManager.getInstance().readSecret(secretName);
    }
}

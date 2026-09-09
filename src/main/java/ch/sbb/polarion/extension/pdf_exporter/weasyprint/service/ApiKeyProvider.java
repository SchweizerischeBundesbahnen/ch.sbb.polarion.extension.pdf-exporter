package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.core.util.vault.PolarionSecretsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.function.Supplier;

/**
 * Supplies the API key of a downstream service (WeasyPrint or the bulk processing service).
 * <p>
 * The key itself is never configured in {@code polarion.properties}. A property such as
 * {@code weasyprint.apiKeySecret} holds the <em>name</em> of a Polarion secret, and the value behind
 * that name is read here, so neither the properties file nor the About page carries the credential.
 * <p>
 * An unset name means the service needs no key, which is how the service is configured by default.
 */
public class ApiKeyProvider {

    private final @NotNull Supplier<String> secretNameSupplier;
    private final @NotNull String serviceLabel;

    /**
     * Reads the API key of the WeasyPrint service, from {@code weasyprint.apiKeySecret}.
     */
    public ApiKeyProvider() {
        this(() -> PdfExporterExtensionConfiguration.getInstance().getWeasyPrintApiKeySecret(), "WeasyPrint");
    }

    /**
     * @param secretNameSupplier reads the configured name of the Polarion secret holding the key
     * @param serviceLabel       names the service in the user facing messages, e.g. {@code "WeasyPrint"}
     */
    public ApiKeyProvider(@NotNull Supplier<String> secretNameSupplier, @NotNull String serviceLabel) {
        this.secretNameSupplier = secretNameSupplier;
        this.serviceLabel = serviceLabel;
    }

    /**
     * Reads the API key configured for the service.
     * <p>
     * Where a name is configured but no value can be read for it, or the value is empty, this fails
     * with a user friendly exception, so the reason survives the catch-all of the conversion paths
     * and reaches the export dialog, where the person who can fix the configuration reads it.
     *
     * @return the key, or {@code null} when no secret name is configured
     */
    public @Nullable String getApiKey() {
        String configured = secretNameSupplier.get();
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
                    "Could not read the %s API key from the Polarion secret '%s' (%s)", serviceLabel, secretName, e.getClass().getName()));
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new UserFriendlyRuntimeException(String.format("The Polarion secret '%s', configured as the %s API key, is empty or does not exist", secretName, serviceLabel));
        }
        // Returned as stored: a credential is not ours to reshape.
        return apiKey;
    }

    @VisibleForTesting
    protected @Nullable String readSecret(@NotNull String secretName) {
        return PolarionSecretsManager.getInstance().readSecret(secretName);
    }
}

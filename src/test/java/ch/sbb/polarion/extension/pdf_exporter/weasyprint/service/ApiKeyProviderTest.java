package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyProviderTest {

    private static final String SECRET_NAME = "weasyprint-api-key";
    private static final String API_KEY = "s3cr3t";

    /**
     * Runs the body with the extension configuration reporting the given secret name.
     */
    private static void withConfiguredSecretName(@Nullable String secretName, @NotNull Consumer<Void> body) {
        PdfExporterExtensionConfiguration configuration = mock(PdfExporterExtensionConfiguration.class);
        when(configuration.getWeasyPrintApiKeySecret()).thenReturn(secretName);
        try (MockedStatic<PdfExporterExtensionConfiguration> configurationMock = mockStatic(PdfExporterExtensionConfiguration.class)) {
            configurationMock.when(PdfExporterExtensionConfiguration::getInstance).thenReturn(configuration);
            body.accept(null);
        }
    }

    private static ApiKeyProvider providerReading(@Nullable String secretValue) {
        return new ApiKeyProvider() {
            @Override
            protected @Nullable String readSecret(@NotNull String secretName) {
                assertThat(secretName).isEqualTo(SECRET_NAME);
                return secretValue;
            }
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldReturnNoKeyWhenNoSecretNameConfigured(String secretName) {
        withConfiguredSecretName(secretName, unused -> assertThat(providerReading(API_KEY).getApiKey()).isNull());
    }

    @Test
    void shouldReturnNoKeyWhenPropertyUnset() {
        withConfiguredSecretName(null, unused -> assertThat(providerReading(API_KEY).getApiKey()).isNull());
    }

    @Test
    void shouldReadKeyFromConfiguredSecret() {
        withConfiguredSecretName(SECRET_NAME, unused -> assertThat(providerReading(API_KEY).getApiKey()).isEqualTo(API_KEY));
    }

    @Test
    void shouldTrimConfiguredSecretName() {
        withConfiguredSecretName("  " + SECRET_NAME + "  ", unused -> assertThat(providerReading(API_KEY).getApiKey()).isEqualTo(API_KEY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void shouldRejectEmptySecretValue(String secretValue) {
        withConfiguredSecretName(SECRET_NAME, unused -> assertThatThrownBy(() -> providerReading(secretValue).getApiKey())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(SECRET_NAME)
                .hasMessageContaining("empty or does not exist"));
    }

    @Test
    void shouldRejectMissingSecret() {
        withConfiguredSecretName(SECRET_NAME, unused -> assertThatThrownBy(() -> providerReading(null).getApiKey())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(SECRET_NAME));
    }

    @Test
    void shouldReportUnreadableSecretWithoutLeakingIt() {
        ApiKeyProvider provider = new ApiKeyProvider() {
            @Override
            protected @Nullable String readSecret(@NotNull String secretName) {
                throw new IllegalArgumentException("no secrets manager configured, key was " + API_KEY);
            }
        };

        withConfiguredSecretName(SECRET_NAME, unused -> assertThatThrownBy(provider::getApiKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not read the WeasyPrint API key")
                .hasMessageContaining(SECRET_NAME)
                .hasMessageNotContaining(API_KEY));
    }
}

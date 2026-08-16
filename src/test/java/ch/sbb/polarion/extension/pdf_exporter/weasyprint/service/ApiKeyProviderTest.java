package ch.sbb.polarion.extension.pdf_exporter.weasyprint.service;

import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyProviderTest {

    private static final String SECRET_NAME = "weasyprint-api-key";
    private static final String API_KEY = "s3cr3t";

    private MockedStatic<PdfExporterExtensionConfiguration> configurationMock;
    private PdfExporterExtensionConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = mock(PdfExporterExtensionConfiguration.class);
        configurationMock = mockStatic(PdfExporterExtensionConfiguration.class);
        configurationMock.when(PdfExporterExtensionConfiguration::getInstance).thenReturn(configuration);
    }

    @AfterEach
    void tearDown() {
        configurationMock.close();
    }

    private void configureSecretName(@Nullable String secretName) {
        when(configuration.getWeasyPrintApiKeySecret()).thenReturn(secretName);
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
        configureSecretName(secretName);

        assertThat(providerReading(API_KEY).getApiKey()).isNull();
    }

    @Test
    void shouldReturnNoKeyWhenPropertyUnset() {
        configureSecretName(null);

        assertThat(providerReading(API_KEY).getApiKey()).isNull();
    }

    @Test
    void shouldReadKeyFromConfiguredSecret() {
        configureSecretName(SECRET_NAME);

        assertThat(providerReading(API_KEY).getApiKey()).isEqualTo(API_KEY);
    }

    @Test
    void shouldTrimConfiguredSecretName() {
        configureSecretName("  " + SECRET_NAME + "  ");

        assertThat(providerReading(API_KEY).getApiKey()).isEqualTo(API_KEY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void shouldRejectEmptySecretValue(String secretValue) {
        configureSecretName(SECRET_NAME);
        ApiKeyProvider provider = providerReading(secretValue);

        assertThatThrownBy(provider::getApiKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(SECRET_NAME)
                .hasMessageContaining("empty or does not exist");
    }

    @Test
    void shouldRejectMissingSecret() {
        configureSecretName(SECRET_NAME);
        ApiKeyProvider provider = providerReading(null);

        assertThatThrownBy(provider::getApiKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(SECRET_NAME);
    }

    @Test
    void shouldReportUnreadableSecretWithoutLeakingIt() {
        configureSecretName(SECRET_NAME);
        ApiKeyProvider provider = new ApiKeyProvider() {
            @Override
            protected @Nullable String readSecret(@NotNull String secretName) {
                throw new IllegalArgumentException("no secrets manager configured, key was " + API_KEY);
            }
        };

        assertThatThrownBy(provider::getApiKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not read the WeasyPrint API key")
                .hasMessageContaining(SECRET_NAME)
                .hasMessageNotContaining(API_KEY);
    }
}

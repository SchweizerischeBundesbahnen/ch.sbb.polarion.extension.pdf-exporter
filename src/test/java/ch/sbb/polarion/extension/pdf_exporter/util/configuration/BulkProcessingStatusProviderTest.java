package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.VersionUtils;
import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.BulkProcessingServiceConnector;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model.BulkProcessingServiceInfo;
import jakarta.ws.rs.ProcessingException;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static ch.sbb.polarion.extension.pdf_exporter.util.exporter.Constants.VERSION_FILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, PdfExporterExtensionConfigurationExtension.class})
class BulkProcessingStatusProviderTest {

    private static final String SERVICE_NAME = "Bulk Processing Service";
    private static final String PYTHON_NAME = "Bulk Processing Service: Python";
    private static final String API_VERSION_KEY = "bulk-processing-service.api-version";
    private static final String TIMESTAMP = "2026-01-01T00:00:00Z";

    private static BulkProcessingServiceInfo info(@Nullable Integer apiVersion, @Nullable String python, @Nullable String service, @Nullable String timestamp) {
        BulkProcessingServiceInfo info = new BulkProcessingServiceInfo();
        info.setApiVersion(apiVersion);
        info.setPython(python);
        info.setBulkProcessingService(service);
        info.setTimestamp(timestamp);
        return info;
    }

    private static void configureUrl(@Nullable String url) {
        when(PdfExporterExtensionConfiguration.getInstance().getBulkProcessingService()).thenReturn(url);
    }

    private static List<ConfigurationStatus> statuses(BulkProcessingServiceConnector connector) {
        return new BulkProcessingStatusProvider(connector).getStatuses(ConfigurationStatusProvider.Context.builder().build());
    }

    @Test
    void reportsDisabledWhenNoUrlConfigured() {
        configureUrl("");
        BulkProcessingServiceConnector connector = mock(BulkProcessingServiceConnector.class);

        assertThat(statuses(connector)).containsExactly(
                new ConfigurationStatus(SERVICE_NAME, Status.OK, "Disabled (no URL configured)"));
    }

    @Test
    void reportsDisabledWhenUrlIsNull() {
        configureUrl(null);
        BulkProcessingServiceConnector connector = mock(BulkProcessingServiceConnector.class);

        assertThat(statuses(connector)).containsExactly(
                new ConfigurationStatus(SERVICE_NAME, Status.OK, "Disabled (no URL configured)"));
    }

    @Test
    void reportsOkWhenApiVersionMatches() {
        configureUrl("http://localhost:9070");
        BulkProcessingServiceConnector connector = mock(BulkProcessingServiceConnector.class);
        when(connector.getVersionInfo()).thenReturn(info(1, "3.12.5", "1.0.0", TIMESTAMP));

        try (MockedStatic<VersionUtils> versions = mockStatic(VersionUtils.class)) {
            versions.when(() -> VersionUtils.getValueFromProperties(VERSION_FILE, API_VERSION_KEY)).thenReturn("1");

            assertThat(statuses(connector)).containsExactlyInAnyOrder(
                    new ConfigurationStatus(SERVICE_NAME, Status.OK, "1.0.0 (" + TIMESTAMP + ")"),
                    new ConfigurationStatus(PYTHON_NAME, Status.OK, "3.12.5"));
        }
    }

    @Test
    void warnsOnIncompatibleApiVersion() {
        configureUrl("http://localhost:9070");
        BulkProcessingServiceConnector connector = mock(BulkProcessingServiceConnector.class);
        when(connector.getVersionInfo()).thenReturn(info(2, "3.12.5", "1.0.0", TIMESTAMP));

        try (MockedStatic<VersionUtils> versions = mockStatic(VersionUtils.class)) {
            versions.when(() -> VersionUtils.getValueFromProperties(VERSION_FILE, API_VERSION_KEY)).thenReturn("1");

            assertThat(statuses(connector)).containsExactlyInAnyOrder(
                    new ConfigurationStatus(SERVICE_NAME, Status.WARNING, "1.0.0 (" + TIMESTAMP + "): <span style='color: red;'>incompatible API version 2, expected 1</span>"),
                    new ConfigurationStatus(PYTHON_NAME, Status.OK, "3.12.5"));
        }
    }

    @Test
    void reportsErrorWhenApiVersionUnknown() {
        configureUrl("http://localhost:9070");
        BulkProcessingServiceConnector connector = mock(BulkProcessingServiceConnector.class);
        when(connector.getVersionInfo()).thenReturn(info(null, "3.12.5", "1.0.0", TIMESTAMP));

        try (MockedStatic<VersionUtils> versions = mockStatic(VersionUtils.class)) {
            versions.when(() -> VersionUtils.getValueFromProperties(VERSION_FILE, API_VERSION_KEY)).thenReturn("1");

            assertThat(statuses(connector)).containsExactlyInAnyOrder(
                    new ConfigurationStatus(SERVICE_NAME, Status.ERROR, "1.0.0 (" + TIMESTAMP + "): <span style='color: red;'>API version unknown, please upgrade bulk-processing-service</span>"),
                    new ConfigurationStatus(PYTHON_NAME, Status.OK, "3.12.5"));
        }
    }

    @Test
    void warnsWhenExpectedApiVersionNotConfigured() {
        configureUrl("http://localhost:9070");
        BulkProcessingServiceConnector connector = mock(BulkProcessingServiceConnector.class);
        when(connector.getVersionInfo()).thenReturn(info(1, "3.12.5", "1.0.0", TIMESTAMP));

        try (MockedStatic<VersionUtils> versions = mockStatic(VersionUtils.class)) {
            versions.when(() -> VersionUtils.getValueFromProperties(VERSION_FILE, API_VERSION_KEY)).thenReturn(null);

            assertThat(statuses(connector)).containsExactlyInAnyOrder(
                    new ConfigurationStatus(SERVICE_NAME, Status.WARNING, "1.0.0 (" + TIMESTAMP + "): <span style='color: orange;'>expected API version not configured</span>"),
                    new ConfigurationStatus(PYTHON_NAME, Status.OK, "3.12.5"));
        }
    }

    @Test
    void reportsErrorOnInvalidExpectedApiVersionConfiguration() {
        configureUrl("http://localhost:9070");
        BulkProcessingServiceConnector connector = mock(BulkProcessingServiceConnector.class);
        when(connector.getVersionInfo()).thenReturn(info(1, "3.12.5", "1.0.0", TIMESTAMP));

        try (MockedStatic<VersionUtils> versions = mockStatic(VersionUtils.class)) {
            versions.when(() -> VersionUtils.getValueFromProperties(VERSION_FILE, API_VERSION_KEY)).thenReturn("not-a-number");

            assertThat(statuses(connector)).containsExactly(
                    new ConfigurationStatus(SERVICE_NAME, Status.ERROR, "Invalid configuration for 'bulk-processing-service.api-version': 'not-a-number' is not a valid integer."));
        }
    }

    @Test
    void reportsErrorWhenConnectorFails() {
        configureUrl("http://localhost:9070");
        BulkProcessingServiceConnector connector = mock(BulkProcessingServiceConnector.class);
        when(connector.getVersionInfo()).thenThrow(new ProcessingException("Connection refused"));

        assertThat(statuses(connector)).containsExactly(
                new ConfigurationStatus(SERVICE_NAME, Status.ERROR, "Connection refused"));
    }

    @Test
    void reportsPythonUnknownWhenPythonMissing() {
        configureUrl("http://localhost:9070");
        BulkProcessingServiceConnector connector = mock(BulkProcessingServiceConnector.class);
        when(connector.getVersionInfo()).thenReturn(info(1, null, "1.0.0", TIMESTAMP));

        try (MockedStatic<VersionUtils> versions = mockStatic(VersionUtils.class)) {
            versions.when(() -> VersionUtils.getValueFromProperties(VERSION_FILE, API_VERSION_KEY)).thenReturn("1");

            assertThat(statuses(connector)).containsExactlyInAnyOrder(
                    new ConfigurationStatus(SERVICE_NAME, Status.OK, "1.0.0 (" + TIMESTAMP + ")"),
                    new ConfigurationStatus(PYTHON_NAME, Status.ERROR, "Unknown"));
        }
    }

    @Test
    void formatsUnknownVersionWithoutTimestamp() {
        configureUrl("http://localhost:9070");
        BulkProcessingServiceConnector connector = mock(BulkProcessingServiceConnector.class);
        when(connector.getVersionInfo()).thenReturn(info(1, "3.12.5", null, null));

        try (MockedStatic<VersionUtils> versions = mockStatic(VersionUtils.class)) {
            versions.when(() -> VersionUtils.getValueFromProperties(VERSION_FILE, API_VERSION_KEY)).thenReturn("1");

            assertThat(statuses(connector)).containsExactlyInAnyOrder(
                    new ConfigurationStatus(SERVICE_NAME, Status.OK, "Unknown"),
                    new ConfigurationStatus(PYTHON_NAME, Status.OK, "3.12.5"));
        }
    }

    @Test
    void defaultConstructorCreatesOwnConnector() {
        assertThat(new BulkProcessingStatusProvider()).isNotNull();
    }
}

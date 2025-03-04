package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.util.VersionUtils;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.model.WeasyPrintInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.ProcessingException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static ch.sbb.polarion.extension.pdf_exporter.util.exporter.Constants.VERSION_FILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class WeasyPrintStatusProviderTest {

    @Test
    void testHappyPath() {
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT);
        WeasyPrintInfo weasyPrintInfo = WeasyPrintInfo.builder()
                .chromium("129.0.6668.58")
                .python("3.12.5")
                .timestamp(timestamp)
                .weasyprint("62.3")
                .weasyprintService("62.4.6")
                .build();

        WeasyPrintServiceConnector weasyPrintServiceConnector = mock(WeasyPrintServiceConnector.class);
        when(weasyPrintServiceConnector.getWeasyPrintInfo()).thenReturn(weasyPrintInfo);
        WeasyPrintStatusProvider weasyPrintStatusProvider = new WeasyPrintStatusProvider(weasyPrintServiceConnector);

        try (MockedStatic<VersionUtils> versionsUtilsMockedStatic = mockStatic(VersionUtils.class)) {
            versionsUtilsMockedStatic.when(() -> VersionUtils.getValueFromProperties(VERSION_FILE, "weasyprint-service.version")).thenReturn("62.4.6");

            List<ConfigurationStatus> configurationStatuses = weasyPrintStatusProvider.getStatuses(ConfigurationStatusProvider.Context.builder().build());

            assertEquals(4, configurationStatuses.size());
            assertThat(configurationStatuses).containsExactlyInAnyOrder(
                    new ConfigurationStatus("WeasyPrint Service", Status.OK, "62.4.6"),
                    new ConfigurationStatus("WeasyPrint Service: Chromium", Status.OK, "129.0.6668.58"),
                    new ConfigurationStatus("WeasyPrint Service: Python", Status.OK, "3.12.5"),
                    new ConfigurationStatus("WeasyPrint Service: WeasyPrint", Status.OK, "62.3")
            );
        }
    }

    @Test
    void testConnectionRefused() {
        WeasyPrintServiceConnector weasyPrintServiceConnector = mock(WeasyPrintServiceConnector.class);
        when(weasyPrintServiceConnector.getWeasyPrintInfo()).thenThrow(new ProcessingException("java.net.ConnectException: Connection refused"));
        WeasyPrintStatusProvider weasyPrintStatusProvider = new WeasyPrintStatusProvider(weasyPrintServiceConnector);

        List<ConfigurationStatus> configurationStatuses = weasyPrintStatusProvider.getStatuses(ConfigurationStatusProvider.Context.builder().build());

        assertEquals(1, configurationStatuses.size());
        assertThat(configurationStatuses).containsExactlyInAnyOrder(
                new ConfigurationStatus("WeasyPrint Service", Status.ERROR, "java.net.ConnectException: Connection refused")
        );
    }

    @Test
    void testUpdateWeasyPrintRequired() {
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT);
        WeasyPrintInfo weasyPrintInfo = WeasyPrintInfo.builder()
                .chromium("129.0.6668.58")
                .python("3.12.5")
                .timestamp(timestamp)
                .weasyprint("62.3")
                .weasyprintService("62.4.5")
                .build();

        WeasyPrintServiceConnector weasyPrintServiceConnector = mock(WeasyPrintServiceConnector.class);
        when(weasyPrintServiceConnector.getWeasyPrintInfo()).thenReturn(weasyPrintInfo);
        WeasyPrintStatusProvider weasyPrintStatusProvider = new WeasyPrintStatusProvider(weasyPrintServiceConnector);

        try (MockedStatic<VersionUtils> versionsUtilsMockedStatic = mockStatic(VersionUtils.class)) {
            versionsUtilsMockedStatic.when(() -> VersionUtils.getValueFromProperties(VERSION_FILE, "weasyprint-service.version")).thenReturn("62.4.6");

            List<ConfigurationStatus> configurationStatuses = weasyPrintStatusProvider.getStatuses(ConfigurationStatusProvider.Context.builder().build());

            assertEquals(4, configurationStatuses.size());
            assertThat(configurationStatuses).containsExactlyInAnyOrder(
                    new ConfigurationStatus("WeasyPrint Service", Status.WARNING, "62.4.5 (" + timestamp + "): <span style='color: red;'>use latest compatible</span> <a href='https://github.com/SchweizerischeBundesbahnen/weasyprint-service/releases/tag/v62.4.6' target='_blank'>62.4.6</a>"),
                    new ConfigurationStatus("WeasyPrint Service: Chromium", Status.OK, "129.0.6668.58"),
                    new ConfigurationStatus("WeasyPrint Service: Python", Status.OK, "3.12.5"),
                    new ConfigurationStatus("WeasyPrint Service: WeasyPrint", Status.OK, "62.3")
            );
        }
    }

    @Test
    void testUnknownWeasyPrintServiceVersion() {
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT);
        WeasyPrintInfo weasyPrintInfo = WeasyPrintInfo.builder()
                .chromium("129.0.6668.58")
                .python("3.12.5")
                .timestamp(timestamp)
                .weasyprint("62.3")
                .weasyprintService(null)
                .build();

        WeasyPrintServiceConnector weasyPrintServiceConnector = mock(WeasyPrintServiceConnector.class);
        when(weasyPrintServiceConnector.getWeasyPrintInfo()).thenReturn(weasyPrintInfo);
        WeasyPrintStatusProvider weasyPrintStatusProvider = new WeasyPrintStatusProvider(weasyPrintServiceConnector);

        try (MockedStatic<VersionUtils> versionsUtilsMockedStatic = mockStatic(VersionUtils.class)) {
            versionsUtilsMockedStatic.when(() -> VersionUtils.getValueFromProperties(VERSION_FILE, "weasyprint-service.version")).thenReturn("62.4.6");

            List<ConfigurationStatus> configurationStatuses = weasyPrintStatusProvider.getStatuses(ConfigurationStatusProvider.Context.builder().build());

            assertEquals(4, configurationStatuses.size());
            assertThat(configurationStatuses).containsExactlyInAnyOrder(
                    new ConfigurationStatus("WeasyPrint Service", Status.ERROR, "Unknown (" + timestamp + "): <span style='color: red;'>use latest compatible</span> <a href='https://github.com/SchweizerischeBundesbahnen/weasyprint-service/releases/tag/v62.4.6' target='_blank'>62.4.6</a>"),
                    new ConfigurationStatus("WeasyPrint Service: Chromium", Status.OK, "129.0.6668.58"),
                    new ConfigurationStatus("WeasyPrint Service: Python", Status.OK, "3.12.5"),
                    new ConfigurationStatus("WeasyPrint Service: WeasyPrint", Status.OK, "62.3")
            );
        }
    }

    @Test
    void testUnknownWeasyPrintServiceVersionNoTimestamp() {
        WeasyPrintInfo weasyPrintInfo = WeasyPrintInfo.builder()
                .chromium("129.0.6668.58")
                .python("3.12.5")
                .timestamp(null)
                .weasyprint("62.3")
                .weasyprintService(null)
                .build();

        WeasyPrintServiceConnector weasyPrintServiceConnector = mock(WeasyPrintServiceConnector.class);
        when(weasyPrintServiceConnector.getWeasyPrintInfo()).thenReturn(weasyPrintInfo);
        WeasyPrintStatusProvider weasyPrintStatusProvider = new WeasyPrintStatusProvider(weasyPrintServiceConnector);

        try (MockedStatic<VersionUtils> versionsUtilsMockedStatic = mockStatic(VersionUtils.class)) {
            versionsUtilsMockedStatic.when(() -> VersionUtils.getValueFromProperties(VERSION_FILE, "weasyprint-service.version")).thenReturn("62.4.6");

            List<ConfigurationStatus> configurationStatuses = weasyPrintStatusProvider.getStatuses(ConfigurationStatusProvider.Context.builder().build());

            assertEquals(4, configurationStatuses.size());
            assertThat(configurationStatuses).containsExactlyInAnyOrder(
                    new ConfigurationStatus("WeasyPrint Service", Status.ERROR, "Unknown: <span style='color: red;'>use latest compatible</span> <a href='https://github.com/SchweizerischeBundesbahnen/weasyprint-service/releases/tag/v62.4.6' target='_blank'>62.4.6</a>"),
                    new ConfigurationStatus("WeasyPrint Service: Chromium", Status.OK, "129.0.6668.58"),
                    new ConfigurationStatus("WeasyPrint Service: Python", Status.OK, "3.12.5"),
                    new ConfigurationStatus("WeasyPrint Service: WeasyPrint", Status.OK, "62.3")
            );
        }
    }

    @Test
    void testNoTimestamp() {
        WeasyPrintInfo weasyPrintInfo = WeasyPrintInfo.builder()
                .chromium("129.0.6668.58")
                .python("3.12.5")
                .timestamp("")
                .weasyprint("62.3")
                .weasyprintService("62.4.5")
                .build();

        WeasyPrintServiceConnector weasyPrintServiceConnector = mock(WeasyPrintServiceConnector.class);
        when(weasyPrintServiceConnector.getWeasyPrintInfo()).thenReturn(weasyPrintInfo);
        WeasyPrintStatusProvider weasyPrintStatusProvider = new WeasyPrintStatusProvider(weasyPrintServiceConnector);

        try (MockedStatic<VersionUtils> versionsUtilsMockedStatic = mockStatic(VersionUtils.class)) {
            versionsUtilsMockedStatic.when(() -> VersionUtils.getValueFromProperties(VERSION_FILE, "weasyprint-service.version")).thenReturn("62.4.6");

            List<ConfigurationStatus> configurationStatuses = weasyPrintStatusProvider.getStatuses(ConfigurationStatusProvider.Context.builder().build());

            assertEquals(4, configurationStatuses.size());
            assertThat(configurationStatuses).containsExactlyInAnyOrder(
                    new ConfigurationStatus("WeasyPrint Service", Status.WARNING, "62.4.5: <span style='color: red;'>use latest compatible</span> <a href='https://github.com/SchweizerischeBundesbahnen/weasyprint-service/releases/tag/v62.4.6' target='_blank'>62.4.6</a>"),
                    new ConfigurationStatus("WeasyPrint Service: Chromium", Status.OK, "129.0.6668.58"),
                    new ConfigurationStatus("WeasyPrint Service: Python", Status.OK, "3.12.5"),
                    new ConfigurationStatus("WeasyPrint Service: WeasyPrint", Status.OK, "62.3")
            );
        }
    }

    @Test
    void testNoChromiumVersion() {
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT);
        WeasyPrintInfo weasyPrintInfo = WeasyPrintInfo.builder()
                .chromium("")
                .python("3.12.5")
                .timestamp(timestamp)
                .weasyprint("62.3")
                .weasyprintService("62.4.6")
                .build();

        WeasyPrintServiceConnector weasyPrintServiceConnector = mock(WeasyPrintServiceConnector.class);
        when(weasyPrintServiceConnector.getWeasyPrintInfo()).thenReturn(weasyPrintInfo);
        WeasyPrintStatusProvider weasyPrintStatusProvider = new WeasyPrintStatusProvider(weasyPrintServiceConnector);

        try (MockedStatic<VersionUtils> versionsUtilsMockedStatic = mockStatic(VersionUtils.class)) {
            versionsUtilsMockedStatic.when(() -> VersionUtils.getValueFromProperties(VERSION_FILE, "weasyprint-service.version")).thenReturn("62.4.6");

            List<ConfigurationStatus> configurationStatuses = weasyPrintStatusProvider.getStatuses(ConfigurationStatusProvider.Context.builder().build());

            assertEquals(4, configurationStatuses.size());
            assertThat(configurationStatuses).containsExactlyInAnyOrder(
                    new ConfigurationStatus("WeasyPrint Service", Status.OK, "62.4.6"),
                    new ConfigurationStatus("WeasyPrint Service: Chromium", Status.ERROR, "Unknown"),
                    new ConfigurationStatus("WeasyPrint Service: Python", Status.OK, "3.12.5"),
                    new ConfigurationStatus("WeasyPrint Service: WeasyPrint", Status.OK, "62.3")
            );
        }
    }

    @Test
    void testWeasyPrintVersionIsHigherThanRequired() {
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT);
        WeasyPrintInfo weasyPrintInfo = WeasyPrintInfo.builder()
                .chromium("129.0.6668.58")
                .python("3.12.5")
                .timestamp(timestamp)
                .weasyprint("62.3")
                .weasyprintService("62.4.7")
                .build();

        WeasyPrintServiceConnector weasyPrintServiceConnector = mock(WeasyPrintServiceConnector.class);
        when(weasyPrintServiceConnector.getWeasyPrintInfo()).thenReturn(weasyPrintInfo);
        WeasyPrintStatusProvider weasyPrintStatusProvider = new WeasyPrintStatusProvider(weasyPrintServiceConnector);

        try (MockedStatic<VersionUtils> versionsUtilsMockedStatic = mockStatic(VersionUtils.class)) {
            versionsUtilsMockedStatic.when(() -> VersionUtils.getValueFromProperties(VERSION_FILE, "weasyprint-service.version")).thenReturn("62.4.6");

            List<ConfigurationStatus> configurationStatuses = weasyPrintStatusProvider.getStatuses(ConfigurationStatusProvider.Context.builder().build());

            assertEquals(4, configurationStatuses.size());
            assertThat(configurationStatuses).containsExactlyInAnyOrder(
                    new ConfigurationStatus("WeasyPrint Service", Status.WARNING, "62.4.7 (" + timestamp + "): <span style='color: red;'>use latest compatible</span> <a href='https://github.com/SchweizerischeBundesbahnen/weasyprint-service/releases/tag/v62.4.6' target='_blank'>62.4.6</a>"),
                    new ConfigurationStatus("WeasyPrint Service: Chromium", Status.OK, "129.0.6668.58"),
                    new ConfigurationStatus("WeasyPrint Service: Python", Status.OK, "3.12.5"),
                    new ConfigurationStatus("WeasyPrint Service: WeasyPrint", Status.OK, "62.3")
            );
        }
    }

}

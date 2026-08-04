package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import com.polarion.alm.projects.properties.internal.ScriptInjectionPropertiesProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

class LiveReportMainHeadStatusProviderTest {

    // Recommended single-tag Live Reports loader.
    public static final String CONFIG_NEW = "<script src=\"/polarion/pdf-exporter/js/live-reports.js\"></script>";
    // Deprecated form (loads starter.js).
    public static final String CONFIG_DEPRECATED = "<script src=\"/polarion/pdf-exporter/js/starter.js\"></script>";

    public static Stream<Arguments> provideConfigurationStatus() {
        ConfigurationStatus notConfigured = ConfigurationStatus.builder()
                .name(LiveReportMainHeadStatusProvider.LIVE_REPORT_BUTTON)
                .status(Status.WARNING)
                .details(LiveReportMainHeadStatusProvider.NOT_CONFIGURED)
                .build();
        ConfigurationStatus ok = ConfigurationStatus.builder()
                .name(LiveReportMainHeadStatusProvider.LIVE_REPORT_BUTTON)
                .status(Status.OK)
                .details("")
                .build();
        ConfigurationStatus deprecated = ConfigurationStatus.builder()
                .name(LiveReportMainHeadStatusProvider.LIVE_REPORT_BUTTON)
                .status(Status.WARNING)
                .details(LiveReportMainHeadStatusProvider.DEPRECATED_DETAILS)
                .build();

        return Stream.of(
                Arguments.of("", "", notConfigured),
                Arguments.of(null, null, notConfigured),

                // Recommended single-tag form → OK.
                Arguments.of(CONFIG_NEW, "", ok),
                Arguments.of("", CONFIG_NEW, ok),
                Arguments.of(CONFIG_NEW, CONFIG_NEW, ok),
                Arguments.of("<script></script> <script src=\"/polarion/pdf-exporter/js/live-reports.js\"></script> <script></script>", "", ok),
                // Recommended form with the data-expand-tools opt-in attribute → still OK.
                Arguments.of("<script src=\"/polarion/pdf-exporter/js/live-reports.js\" data-expand-tools=\"true\"></script>", "", ok),

                // Deprecated starter.js form → WARNING with deprecation hint.
                Arguments.of(CONFIG_DEPRECATED, "", deprecated),
                Arguments.of("", CONFIG_DEPRECATED, deprecated),
                Arguments.of(CONFIG_DEPRECATED, CONFIG_DEPRECATED, deprecated),

                // The recommended form on either source wins over a deprecated form.
                Arguments.of(CONFIG_NEW, CONFIG_DEPRECATED, ok),
                Arguments.of(CONFIG_DEPRECATED, CONFIG_NEW, ok)
        );
    }

    @ParameterizedTest
    @MethodSource("provideConfigurationStatus")
    void testConfigurationStatus(String scriptInjectionSystemProperties, String scripInjectionRuntimeProperties, ConfigurationStatus expectedConfigurationStatus) {
        LiveReportMainHeadStatusProvider liveReportMainHeadStatusProvider = new LiveReportMainHeadStatusProvider();

        try (MockedStatic<ScriptInjectionPropertiesProvider> scriptInjectionPropertiesProviderMockedStatic = mockStatic(ScriptInjectionPropertiesProvider.class, Mockito.RETURNS_DEEP_STUBS)) {
            scriptInjectionPropertiesProviderMockedStatic.when(() -> ScriptInjectionPropertiesProvider.getScriptInjectionSystemProperties().mainHead()).thenReturn(scriptInjectionSystemProperties);
            scriptInjectionPropertiesProviderMockedStatic.when(() -> ScriptInjectionPropertiesProvider.getScripInjectionRuntimeProperties().mainHead()).thenReturn(scripInjectionRuntimeProperties);

            ConfigurationStatusProvider.Context context = ConfigurationStatusProvider.Context.builder().scope("").build();
            ConfigurationStatus configurationStatus = liveReportMainHeadStatusProvider.getStatus(context);
            assertEquals(expectedConfigurationStatus, configurationStatus);
        }
    }
}

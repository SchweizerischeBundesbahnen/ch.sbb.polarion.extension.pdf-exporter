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

    public static final String CONFIG = "<script src=\"/polarion/pdf-exporter/js/starter.js\"></script>";

    public static Stream<Arguments> provideConfigurationStatus() {
        ConfigurationStatus configurationStatusNotConfigured = ConfigurationStatus.builder()
                .name(LiveReportMainHeadStatusProvider.LIVE_REPORT_BUTTON)
                .status(Status.WARNING)
                .details("Not configured")
                .build();
        ConfigurationStatus configurationStatusOk = ConfigurationStatus.builder()
                .name(LiveReportMainHeadStatusProvider.LIVE_REPORT_BUTTON)
                .status(Status.OK)
                .details("")
                .build();

        return Stream.of(
                Arguments.of("", "", configurationStatusNotConfigured),

                Arguments.of(CONFIG, "", configurationStatusOk),
                Arguments.of("", CONFIG, configurationStatusOk),
                Arguments.of(CONFIG, CONFIG, configurationStatusOk),

                Arguments.of("<script> </script> <script src=\"/polarion/pdf-exporter/js/starter.js\"></script> <script></script>", "", configurationStatusOk)
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

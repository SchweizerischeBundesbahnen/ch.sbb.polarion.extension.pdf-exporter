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

class DleToolbarStatusProviderTest {

    // The single-tag injector, the only supported form.
    public static final String CONFIG_NEW = "<script src=\"/polarion/pdf-exporter/js/dle-toolbar.js\"></script>";

    public static Stream<Arguments> provideConfigurationStatus() {
        ConfigurationStatus notConfigured = ConfigurationStatus.builder()
                .name(DleToolbarStatusProvider.DLE_TOOLBAR)
                .status(Status.WARNING)
                .details(DleToolbarStatusProvider.NOT_CONFIGURED)
                .build();
        ConfigurationStatus ok = ConfigurationStatus.builder()
                .name(DleToolbarStatusProvider.DLE_TOOLBAR)
                .status(Status.OK)
                .details("")
                .build();

        return Stream.of(
                Arguments.of("", "", notConfigured),
                Arguments.of(null, null, notConfigured),

                // The single-tag form → OK.
                Arguments.of(CONFIG_NEW, "", ok),
                Arguments.of("", CONFIG_NEW, ok),
                Arguments.of(CONFIG_NEW, CONFIG_NEW, ok),
                Arguments.of("<script></script> <script src=\"/polarion/pdf-exporter/js/dle-toolbar.js\"></script> <script></script>", "", ok)
        );
    }

    @ParameterizedTest
    @MethodSource("provideConfigurationStatus")
    void testConfigurationStatus(String scriptInjectionSystemProperties, String scripInjectionRuntimeProperties, ConfigurationStatus expectedConfigurationStatus) {
        DleToolbarStatusProvider dleToolbarStatusProvider = new DleToolbarStatusProvider();

        try (MockedStatic<ScriptInjectionPropertiesProvider> scriptInjectionPropertiesProviderMockedStatic = mockStatic(ScriptInjectionPropertiesProvider.class, Mockito.RETURNS_DEEP_STUBS)) {
            scriptInjectionPropertiesProviderMockedStatic.when(() -> ScriptInjectionPropertiesProvider.getScriptInjectionSystemProperties().dleEditorHead()).thenReturn(scriptInjectionSystemProperties);
            scriptInjectionPropertiesProviderMockedStatic.when(() -> ScriptInjectionPropertiesProvider.getScripInjectionRuntimeProperties().dleEditorHead()).thenReturn(scripInjectionRuntimeProperties);

            ConfigurationStatusProvider.Context context = ConfigurationStatusProvider.Context.builder().scope("").build();
            ConfigurationStatus configurationStatus = dleToolbarStatusProvider.getStatus(context);
            assertEquals(expectedConfigurationStatus, configurationStatus);
        }
    }
}

package ch.sbb.polarion.extension.pdf_exporter.properties;

import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import com.polarion.core.config.impl.SystemValueReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class,  CurrentContextExtension.class})
class PdfExporterExtensionConfigurationTest {

    private MockedStatic<SystemValueReader> systemValueReaderMockedStatic;
    private SystemValueReader systemValueReader;
    private PdfExporterExtensionConfiguration configuration;

    @BeforeEach
    void setUp() {
        systemValueReader = mock(SystemValueReader.class);
        systemValueReaderMockedStatic = mockStatic(SystemValueReader.class);
        systemValueReaderMockedStatic.when(SystemValueReader::getInstance).thenReturn(systemValueReader);
        configuration = new PdfExporterExtensionConfiguration();
    }

    @AfterEach
    void tearDown() {
        systemValueReaderMockedStatic.close();
    }

    @Test
    void getWeasyPrintServiceReturnsConfiguredValue() {
        when(systemValueReader.readString(anyString(), anyString())).thenReturn("http://custom:8080");
        assertEquals("http://custom:8080", configuration.getWeasyPrintService());
    }

    @Test
    void getWeasyPrintServiceDescriptionReturnsConstant() {
        assertEquals(PdfExporterExtensionConfiguration.WEASYPRINT_SERVICE_DESCRIPTION, configuration.getWeasyPrintServiceDescription());
    }

    @Test
    void getWeasyPrintServiceDefaultValueReturnsConstant() {
        assertEquals(PdfExporterExtensionConfiguration.WEASYPRINT_SERVICE_DEFAULT_VALUE, configuration.getWeasyPrintServiceDefaultValue());
    }

    @Test
    void getWebhooksEnabledReturnsConfiguredValue() {
        when(systemValueReader.readBoolean(anyString(), anyBoolean())).thenReturn(true);
        assertTrue(configuration.getWebhooksEnabled());
    }

    @Test
    void getWebhooksEnabledDescriptionReturnsConstant() {
        assertEquals(PdfExporterExtensionConfiguration.WEBHOOKS_ENABLED_DESCRIPTION, configuration.getWebhooksEnabledDescription());
    }

    @Test
    void getWebhooksEnabledDefaultValueReturnsStringFalse() {
        assertEquals("false", configuration.getWebhooksEnabledDefaultValue());
    }

    @Test
    void getDebugDescriptionReturnsConstant() {
        assertEquals(PdfExporterExtensionConfiguration.DEBUG_DESCRIPTION, configuration.getDebugDescription());
    }

    @Test
    void getRenderableImageExtensionsReturnsDefaultSet() {
        String defaultValue = String.join(", ", PdfExporterExtensionConfiguration.RENDERABLE_IMAGE_EXTENSIONS_DEFAULT_VALUE);
        when(systemValueReader.readString(anyString(), anyString())).thenReturn(defaultValue);

        Set<String> extensions = configuration.getRenderableImageExtensions();

        assertEquals(PdfExporterExtensionConfiguration.RENDERABLE_IMAGE_EXTENSIONS_DEFAULT_VALUE, extensions);
    }

    static Stream<Arguments> renderableImageExtensionsParsingProvider() {
        return Stream.of(
                Arguments.of("png, svg, webp", Set.of("png", "svg", "webp")),
                Arguments.of("  PNG ,  SVG  , JpEg ", Set.of("png", "svg", "jpeg")),
                Arguments.of("png,,svg, ,jpg", Set.of("png", "svg", "jpg"))
        );
    }

    @ParameterizedTest
    @MethodSource("renderableImageExtensionsParsingProvider")
    void getRenderableImageExtensionsParsesInput(String input, Set<String> expected) {
        when(systemValueReader.readString(anyString(), anyString())).thenReturn(input);

        assertEquals(expected, configuration.getRenderableImageExtensions());
    }

    @Test
    void getRenderableImageExtensionsValueReturnsRawString() {
        when(systemValueReader.readString(anyString(), anyString())).thenReturn("png, svg, webp");

        assertEquals("png, svg, webp", configuration.getRenderableImageExtensionsValue());
    }

    @Test
    void getRenderableImageExtensionsDescriptionReturnsConstant() {
        assertEquals(PdfExporterExtensionConfiguration.RENDERABLE_IMAGE_EXTENSIONS_DESCRIPTION, configuration.getRenderableImageExtensionsDescription());
    }

    @Test
    void getRenderableImageExtensionsDefaultValueReturnsCommaSeparatedString() {
        String defaultValue = configuration.getRenderableImageExtensionsDefaultValue();

        assertFalse(defaultValue.startsWith("["));
        assertFalse(defaultValue.endsWith("]"));
        for (String ext : PdfExporterExtensionConfiguration.RENDERABLE_IMAGE_EXTENSIONS_DEFAULT_VALUE) {
            assertTrue(defaultValue.contains(ext));
        }
    }

    @Test
    void getExternalResourcesPolicyReturnsConfiguredValue() {
        when(systemValueReader.readString(anyString(), anyString())).thenReturn("allowlistOnly");

        assertEquals("allowlistOnly", configuration.getExternalResourcesPolicy());
    }

    @Test
    void getExternalResourcesAllowedHostsReturnsConfiguredValue() {
        when(systemValueReader.readString(anyString(), anyString())).thenReturn("cdn.intranet");

        assertEquals("cdn.intranet", configuration.getExternalResourcesAllowedHosts());
    }

    @Test
    void getExternalResourcesMaxSizeMBReturnsConfiguredValue() {
        when(systemValueReader.readInt(anyString(), anyInt())).thenReturn(32);

        assertEquals(32, configuration.getExternalResourcesMaxSizeMB());
    }

    @Test
    void getExternalResourcesDescriptionsAndDefaultsReturnConstants() {
        assertEquals(PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_POLICY_DESCRIPTION, configuration.getExternalResourcesPolicyDescription());
        assertEquals(PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_POLICY_DEFAULT_VALUE, configuration.getExternalResourcesPolicyDefaultValue());
        assertEquals(PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_ALLOWED_HOSTS_DESCRIPTION, configuration.getExternalResourcesAllowedHostsDescription());
        assertEquals(PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_ALLOWED_HOSTS_DEFAULT_VALUE, configuration.getExternalResourcesAllowedHostsDefaultValue());
        assertEquals(PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_MAX_SIZE_MB_DESCRIPTION, configuration.getExternalResourcesMaxSizeMBDescription());
        assertEquals("16", configuration.getExternalResourcesMaxSizeMBDefaultValue());
    }

    @Test
    void getSupportedPropertiesContainsAllProperties() {
        var properties = configuration.getSupportedProperties();

        assertTrue(properties.contains(PdfExporterExtensionConfiguration.WEASYPRINT_SERVICE));
        assertTrue(properties.contains(PdfExporterExtensionConfiguration.WEBHOOKS_ENABLED));
        assertTrue(properties.contains(PdfExporterExtensionConfiguration.RENDERABLE_IMAGE_EXTENSIONS));
        assertTrue(properties.contains(PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_POLICY));
        assertTrue(properties.contains(PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_ALLOWED_HOSTS));
        assertTrue(properties.contains(PdfExporterExtensionConfiguration.EXTERNAL_RESOURCES_MAX_SIZE_MB));
    }
}

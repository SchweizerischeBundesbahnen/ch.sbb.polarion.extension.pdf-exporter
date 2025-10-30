package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.pdf_exporter.settings.CoverPageSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultSettingsStatusProviderTest {

    @BeforeEach
    void beforeEach() {
        NamedSettingsRegistry.INSTANCE.getAll().clear();
    }

    @AfterEach
    void afterEach() {
        NamedSettingsRegistry.INSTANCE.getAll().clear();
    }

    @Test
    void testGetStatus() {
        CoverPageSettings coverPageSettings = mock(CoverPageSettings.class);
        when(coverPageSettings.getFeatureName()).thenReturn(CoverPageSettings.FEATURE_NAME);
        NamedSettingsRegistry.INSTANCE.register(List.of(coverPageSettings));

        DefaultSettingsStatusProvider provider = new DefaultSettingsStatusProvider();
        ConfigurationStatusProvider.Context context = mock(ConfigurationStatusProvider.Context.class);
        when(context.getScope()).thenReturn("testScope");
        ConfigurationStatus status = provider.getStatus(context);
        assertEquals(Status.OK, status.getStatus());

        when(coverPageSettings.readNames(anyString())).thenThrow(new RuntimeException("Test exception"));
        status = provider.getStatus(context);
        assertEquals(Status.ERROR, status.getStatus());
        assertEquals("Test exception", status.getDetails());
    }

}

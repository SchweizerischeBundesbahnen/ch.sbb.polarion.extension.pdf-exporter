package ch.sbb.polarion.extension.pdf_exporter.util.configuration;

import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus;
import ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider;
import ch.sbb.polarion.extension.generic.configuration.Status;
import com.polarion.core.config.Configuration;
import com.polarion.core.config.IConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CORSStatusProviderTest {

    @Test
    void testRestEnabledWithCorsAllowedOrigins() {
        CORSStatusProvider corsStatusProvider = new CORSStatusProvider();

        try (MockedStatic<Configuration> configurationMock = mockStatic(Configuration.class)) {
            IConfiguration configInstance = mock(IConfiguration.class, RETURNS_DEEP_STUBS);

            configurationMock.when(Configuration::getInstance).thenReturn(configInstance);
            when(configInstance.rest().enabled()).thenReturn(true);
            when(configInstance.rest().corsAllowedOrigins()).thenReturn(Set.of("https://example.com", "https://another.com"));

            ConfigurationStatusProvider.Context context = ConfigurationStatusProvider.Context.builder().build();
            ConfigurationStatus status = corsStatusProvider.getStatus(context);

            assertEquals(CORSStatusProvider.CORS, status.getName());
            assertEquals(Status.OK, status.getStatus());
            assertTrue(status.getDetails().contains("https://example.com"));
            assertTrue(status.getDetails().contains("https://another.com"));
            assertTrue(status.getDetails().startsWith("CORS allowed origins: ["));
        }
    }

    @Test
    void testRestEnabledWithoutCorsAllowedOrigins() {
        CORSStatusProvider corsStatusProvider = new CORSStatusProvider();

        try (MockedStatic<Configuration> configurationMock = mockStatic(Configuration.class)) {
            IConfiguration configInstance = mock(IConfiguration.class, RETURNS_DEEP_STUBS);

            configurationMock.when(Configuration::getInstance).thenReturn(configInstance);
            when(configInstance.rest().enabled()).thenReturn(true);
            when(configInstance.rest().corsAllowedOrigins()).thenReturn(Collections.emptySet());

            ConfigurationStatusProvider.Context context = ConfigurationStatusProvider.Context.builder().build();
            ConfigurationStatus status = corsStatusProvider.getStatus(context);

            assertEquals(CORSStatusProvider.CORS, status.getName());
            assertEquals(Status.WARNING, status.getStatus());
            assertEquals("CORS allowed origins are not configured", status.getDetails());
        }
    }

    @Test
    void testRestDisabled() {
        CORSStatusProvider corsStatusProvider = new CORSStatusProvider();

        try (MockedStatic<Configuration> configurationMock = mockStatic(Configuration.class)) {
            IConfiguration configInstance = mock(IConfiguration.class, RETURNS_DEEP_STUBS);

            configurationMock.when(Configuration::getInstance).thenReturn(configInstance);
            when(configInstance.rest().enabled()).thenReturn(false);

            ConfigurationStatusProvider.Context context = ConfigurationStatusProvider.Context.builder().build();
            ConfigurationStatus status = corsStatusProvider.getStatus(context);

            assertEquals(CORSStatusProvider.CORS, status.getName());
            assertEquals(Status.WARNING, status.getStatus());
            assertEquals("Polarion REST API is not enabled, so CORS is not enabled", status.getDetails());
        }
    }

    @Test
    void testRestEnabledWithSingleCorsOrigin() {
        CORSStatusProvider corsStatusProvider = new CORSStatusProvider();

        try (MockedStatic<Configuration> configurationMock = mockStatic(Configuration.class)) {
            IConfiguration configInstance = mock(IConfiguration.class, RETURNS_DEEP_STUBS);

            configurationMock.when(Configuration::getInstance).thenReturn(configInstance);
            when(configInstance.rest().enabled()).thenReturn(true);
            when(configInstance.rest().corsAllowedOrigins()).thenReturn(Set.of("https://single-origin.com"));

            ConfigurationStatusProvider.Context context = ConfigurationStatusProvider.Context.builder().build();
            ConfigurationStatus status = corsStatusProvider.getStatus(context);

            assertEquals(CORSStatusProvider.CORS, status.getName());
            assertEquals(Status.OK, status.getStatus());
            assertEquals("CORS allowed origins: [https://single-origin.com]", status.getDetails());
        }
    }

}

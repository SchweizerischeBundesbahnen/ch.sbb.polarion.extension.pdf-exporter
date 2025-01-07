package ch.sbb.polarion.extension.pdf_exporter;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.guice.internal.GuicePlatform;
import com.polarion.platform.security.ISecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfExporterInternalModuleTest {

    @Mock
    private ITestManagementService testManagementService;

    @BeforeEach
    void setUp() throws Exception {
        Injector injector = Guice.createInjector(new TestModule(testManagementService));
        Field field = GuicePlatform.class.getDeclaredField("globalInjector");
        field.setAccessible(true);
        field.set(null, injector);
    }

    @Test
    void shouldBindClassImplementations() {
        try (MockedStatic<PlatformContext> platformContext = mockStatic(PlatformContext.class)) {
            IPlatform platform = mock(IPlatform.class);
            platformContext.when(PlatformContext::getPlatform).thenReturn(platform);
            ISecurityService securityService = mock(ISecurityService.class);
            when(platform.lookupService(ISecurityService.class)).thenReturn(securityService);

            Injector injector = Guice.createInjector(new PdfExporterInternalModule());
            assertThat(injector.getInstance(ISecurityService.class)).isEqualTo(securityService);
            assertThat(injector.getInstance(ITestManagementService.class)).isEqualTo(testManagementService);
        }
    }

    public static class TestModule extends AbstractModule {
        private final ITestManagementService testManagementService;

        public TestModule(ITestManagementService testManagementService) {
            this.testManagementService = testManagementService;
        }

        @Override
        protected void configure() {
            bind(ITestManagementService.class).toInstance(testManagementService);
        }
    }
}
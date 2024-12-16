package ch.sbb.polarion.extension.pdf_exporter.test_extensions.generic;

import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.security.ILoginPolicy;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.portal.internal.server.navigation.TestManagementServiceAccessor;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

public class PlatformContextMockExtension implements BeforeEachCallback, AfterEachCallback {

    private MockedConstruction<TestManagementServiceAccessor> testManagementServiceAccessorMockedConstruction;
    private MockedStatic<PlatformContext> platformContextMockedStatic;

    private ITrackerService trackerServiceMock;
    private IProjectService projectService;
    private ISecurityService securityService;
    private IPlatformService platformService;
    private IRepositoryService repositoryService;
    private IDataService dataService;
    private ILoginPolicy loginPolicy;
    private ITestManagementService testManagementService;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        IPlatform platformMock = mock(IPlatform.class);

        trackerServiceMock = mock(ITrackerService.class);
        projectService = mock(IProjectService.class);
        securityService = mock(ISecurityService.class);
        platformService = mock(IPlatformService.class);
        repositoryService = mock(IRepositoryService.class);
        dataService = mock(IDataService.class);
        loginPolicy = mock(ILoginPolicy.class);
        testManagementService = mock(ITestManagementService.class);

        lenient().when(platformMock.lookupService(ITrackerService.class)).thenReturn(trackerServiceMock);
        lenient().when(platformMock.lookupService(IProjectService.class)).thenReturn(projectService);
        lenient().when(platformMock.lookupService(ISecurityService.class)).thenReturn(securityService);
        lenient().when(platformMock.lookupService(IPlatformService.class)).thenReturn(platformService);
        lenient().when(platformMock.lookupService(IRepositoryService.class)).thenReturn(repositoryService);
        lenient().when(platformMock.lookupService(IDataService.class)).thenReturn(dataService);
        lenient().when(platformMock.lookupService(ILoginPolicy.class)).thenReturn(loginPolicy);

        platformContextMockedStatic = mockStatic(PlatformContext.class);
        platformContextMockedStatic.when(PlatformContext::getPlatform).thenReturn(platformMock);

        CustomExtensionMockInjector.inject(context, trackerServiceMock);
        CustomExtensionMockInjector.inject(context, projectService);
        CustomExtensionMockInjector.inject(context, securityService);
        CustomExtensionMockInjector.inject(context, platformService);
        CustomExtensionMockInjector.inject(context, repositoryService);
        CustomExtensionMockInjector.inject(context, dataService);
        CustomExtensionMockInjector.inject(context, loginPolicy);

        testManagementServiceAccessorMockedConstruction = mockConstruction(TestManagementServiceAccessor.class, (testManagementServiceAccessor, mockedContructionContext) -> {
            lenient().when(testManagementServiceAccessor.getTestingService()).thenReturn(testManagementService);
        });
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (testManagementServiceAccessorMockedConstruction != null) {
            testManagementServiceAccessorMockedConstruction.close();
        }
        if (platformContextMockedStatic != null) {
            platformContextMockedStatic.close();
        }
    }

}

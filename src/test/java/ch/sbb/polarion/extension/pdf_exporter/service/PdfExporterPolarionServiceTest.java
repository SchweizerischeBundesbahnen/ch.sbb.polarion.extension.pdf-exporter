package ch.sbb.polarion.extension.pdf_exporter.service;

import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.util.PObjectListStub;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.attachments.TestRunAttachment;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.collections.CollectionItem;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageWeightInfo;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageSettings;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollection;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.ITestRunAttachment;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollectionElement;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PdfExporterPolarionServiceTest {

    private final ITrackerService trackerService = mock(ITrackerService.class);
    private final ITestManagementService testManagementService = mock(ITestManagementService.class);
    private StylePackageSettings stylePackageSettings;
    private final PdfExporterPolarionService service = new PdfExporterPolarionService(
            trackerService,
            mock(IProjectService.class),
            mock(ISecurityService.class),
            mock(IPlatformService.class),
            mock(IRepositoryService.class),
            testManagementService
    );

    @BeforeEach
    void setUp() {
        stylePackageSettings = mock(StylePackageSettings.class);
        when(stylePackageSettings.getFeatureName()).thenReturn("style-package");
        NamedSettingsRegistry.INSTANCE.getAll().clear();
        NamedSettingsRegistry.INSTANCE.register(List.of(stylePackageSettings));
    }

    @Test
    void testGetProjectFromScope_ValidScope() {
        String scope = "project/validProjectId/";
        String expectedProjectId = "validProjectId";
        ITrackerProject mockProject = mock(ITrackerProject.class);

        PdfExporterPolarionService polarionService = mock(PdfExporterPolarionService.class);
        when(polarionService.getProjectFromScope(anyString())).thenCallRealMethod();
        when(polarionService.getTrackerProject(anyString())).thenReturn(mockProject);

        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            mockScopeUtils.when(() -> ScopeUtils.getProjectFromScope(anyString())).thenReturn(expectedProjectId);

            ITrackerProject result = polarionService.getProjectFromScope(scope);

            assertNotNull(result);
            verify(polarionService, times(1)).getTrackerProject(expectedProjectId);
        }
    }

    @Test
    void testGetProjectFromScope_InvalidScope() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            mockScopeUtils.when(() -> ScopeUtils.getProjectFromScope(anyString())).thenReturn(null);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.getProjectFromScope("invalidScope"));

            assertEquals("Wrong scope format: invalidScope. Should be of form 'project/{projectId}/'", exception.getMessage());
        }
    }

    @Test
    void testGetStylePackagesWeights() {
        String scope = "project/someProjectId";
        Collection<SettingName> settingNames = List.of(
                SettingName.builder().id("id1").name("name1").scope(scope).build(),
                SettingName.builder().id("id2").name("name2").scope(scope).build()
        );
        StylePackageModel mockModel1 = mock(StylePackageModel.class);
        StylePackageModel mockModel2 = mock(StylePackageModel.class);

        when(stylePackageSettings.readNames(scope)).thenReturn(settingNames);
        when(stylePackageSettings.read(eq(scope), any(SettingId.class), isNull()))
                .thenReturn(mockModel1)
                .thenReturn(mockModel2);
        when(mockModel1.getWeight()).thenReturn(1.0f);
        when(mockModel2.getWeight()).thenReturn(2.0f);

        Collection<StylePackageWeightInfo> result = service.getStylePackagesWeights(scope);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(stylePackageSettings, times(2)).read(anyString(), any(SettingId.class), isNull());
    }

    @Test
    void testUpdateStylePackagesWeights() {
        List<StylePackageWeightInfo> weightInfos = new ArrayList<>();
        weightInfos.add(new StylePackageWeightInfo("Package1", "project/someProjectId", 1.0f));
        weightInfos.add(new StylePackageWeightInfo("Package2", "project/someProjectId", 2.0f));

        StylePackageModel mockModel = mock(StylePackageModel.class);

        when(stylePackageSettings.read(anyString(), any(SettingId.class), isNull())).thenReturn(mockModel);

        service.updateStylePackagesWeights(weightInfos);

        verify(stylePackageSettings, times(2)).read(anyString(), any(SettingId.class), isNull());
        verify(stylePackageSettings, times(2)).save(anyString(), any(SettingId.class), any(StylePackageModel.class));
    }

    @Test
    void testGetSuitableStylePackages() {
        String projectId = "someProjectId";
        String spaceId = "someSpaceId";
        String documentName = "documentName";
        Collection<SettingName> settingNames = List.of(
                SettingName.builder().id("id1").name("name1").scope("project/someProjectId/").build(),
                SettingName.builder().id("id4").name("name4").scope("project/someProjectId/").build(),
                SettingName.builder().id("id2").name("name2").scope("project/someProjectId/").build(),
                SettingName.builder().id("id5").name("name5").scope("project/someProjectId/").build(),
                SettingName.builder().id("id3").name("name3").scope("project/someProjectId/").build()
        );
        StylePackageModel mockModel1 = mock(StylePackageModel.class);
        when(mockModel1.getWeight()).thenReturn(0.5f);
        StylePackageModel mockModel2 = mock(StylePackageModel.class);
        when(mockModel2.getWeight()).thenReturn(50.1f);
        StylePackageModel mockModel3 = mock(StylePackageModel.class);
        when(mockModel3.getWeight()).thenReturn(50.0f);
        StylePackageModel mockModel4 = mock(StylePackageModel.class);
        when(mockModel4.getWeight()).thenReturn(50.0f);
        StylePackageModel mockModel5 = mock(StylePackageModel.class);
        when(mockModel5.getWeight()).thenReturn(50.0f);

        when(stylePackageSettings.readNames(ScopeUtils.getScopeFromProject(projectId))).thenReturn(settingNames);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name1")), isNull())).thenReturn(mockModel1);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name2")), isNull())).thenReturn(mockModel2);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name3")), isNull())).thenReturn(mockModel3);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name4")), isNull())).thenReturn(mockModel4);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name5")), isNull())).thenReturn(mockModel5);

        Collection<SettingName> result = service.getSuitableStylePackages(projectId, spaceId, documentName);

        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals(List.of("name2", "name3", "name4", "name5", "name1"), result.stream().map(SettingName::getName).toList());
    }

    @Test
    void testGetTestRun() {
        when(testManagementService.getTestRun("testProjectId", "testTestRunId", null)).thenReturn(mock(ITestRun.class));
        when(testManagementService.getTestRun("testProjectId", "testTestRunId", "1234")).thenReturn(mock(ITestRun.class));

        ITestRun testRun = service.getTestRun("testProjectId", "testTestRunId", null);
        assertNotNull(testRun);
        ITestRun testRunWithRevision = service.getTestRun("testProjectId", "testTestRunId", "1234");
        assertNotNull(testRunWithRevision);

        ITestRun nonExistingTestRun = mock(ITestRun.class);
        when(nonExistingTestRun.isUnresolvable()).thenReturn(true);
        when(testManagementService.getTestRun("testProjectId", "nonExistingTestRun", null)).thenReturn(nonExistingTestRun);
        assertThrows(IllegalArgumentException.class, () -> service.getTestRun("testProjectId", "nonExistingTestRun", null));
    }

    @Test
    void testGetTestRunAttachments() {
        PObjectListStub<ITestRunAttachment> attachments = new PObjectListStub<>();
        ITestRunAttachment testRunAttachment1 = mock(ITestRunAttachment.class);
        when(testRunAttachment1.getFileName()).thenReturn("test1.pdf");
        attachments.add(testRunAttachment1);
        ITestRunAttachment testRunAttachment2 = mock(ITestRunAttachment.class);
        when(testRunAttachment2.getFileName()).thenReturn("test2.txt");
        attachments.add(testRunAttachment2);

        ITestRun testRun = mock(ITestRun.class);
        when(testRun.isUnresolvable()).thenReturn(false);
        when(testRun.getAttachments()).thenReturn(attachments);
        when(testManagementService.getTestRun("testProjectId", "testTestRunId", null)).thenReturn(testRun);

        List<TestRunAttachment> testRunAttachments = service.getTestRunAttachments("testProjectId", "testTestRunId", null, null);
        assertNotNull(testRunAttachments);
        assertEquals(2, testRunAttachments.size());

        List<TestRunAttachment> testRunAttachmentsFilteredPdf = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.pdf");
        assertNotNull(testRunAttachmentsFilteredPdf);
        assertEquals(1, testRunAttachmentsFilteredPdf.size());

        List<TestRunAttachment> testRunAttachmentsFilteredTxt = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.pdf");
        assertNotNull(testRunAttachmentsFilteredTxt);
        assertEquals(1, testRunAttachmentsFilteredTxt.size());

        List<TestRunAttachment> testRunAttachmentsFilteredAll = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.*");
        assertNotNull(testRunAttachmentsFilteredAll);
        assertEquals(2, testRunAttachmentsFilteredAll.size());

        List<TestRunAttachment> testRunAttachmentsFilteredNone = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.png");
        assertNotNull(testRunAttachmentsFilteredNone);
        assertEquals(0, testRunAttachmentsFilteredNone.size());
    }

    @Test
    void testGetTestRunAttachment() {
        ITestRun testRun = mock(ITestRun.class);
        when(testRun.isUnresolvable()).thenReturn(false);
        when(testRun.getAttachment("testAttachmentId")).thenReturn(mock(ITestRunAttachment.class));
        when(testManagementService.getTestRun("testProjectId", "testTestRunId", null)).thenReturn(testRun);
        when(testManagementService.getTestRun("testProjectId", "testTestRunId", "1234")).thenReturn(testRun);

        ITestRunAttachment testRunAttachment = service.getTestRunAttachment("testProjectId", "testTestRunId", "testAttachmentId", null);
        assertNotNull(testRunAttachment);
        ITestRunAttachment testRunAttachmentWithRevision = service.getTestRunAttachment("testProjectId", "testTestRunId", "testAttachmentId", "1234");
        assertNotNull(testRunAttachmentWithRevision);

        assertThrows(IllegalArgumentException.class, () -> service.getTestRunAttachment("testProjectId", "testTestRunId", "nonExistingAttachmentId", null));
    }

    @Test
    void testGetCollectionItems() {
        // Mock dependencies
        String projectId = "testProjectId";
        String collectionId = "testCollectionId";

        IBaselineCollection mockCollection = mock(IBaselineCollection.class);
        BaselineCollectionReference mockReference = mock(BaselineCollectionReference.class);

        // Mock collection elements
        IModule mockModule1 = mock(IModule.class);
        IModule mockModule2 = mock(IModule.class);

        when(mockModule1.getModuleNameWithSpace()).thenReturn("space1/Module1");
        when(mockModule1.getLastRevision()).thenReturn("1");

        when(mockModule2.getModuleNameWithSpace()).thenReturn("space2/Module2");
        when(mockModule2.getLastRevision()).thenReturn("2");

        IBaselineCollectionElement mockElement1 = mock(IBaselineCollectionElement.class);
        IBaselineCollectionElement mockElement2 = mock(IBaselineCollectionElement.class);

        when(mockElement1.getObjectWithRevision()).thenReturn(mockModule1);
        when(mockElement2.getObjectWithRevision()).thenReturn(mockModule2);

        BaselineCollection baselineCollection = mock(BaselineCollection.class);
        when(mockCollection.getElements()).thenReturn(List.of(mockElement1, mockElement2));
        when(mockReference.get(Mockito.any())).thenReturn(baselineCollection);
        when(baselineCollection.getOldApi()).thenReturn(mockCollection);

        try (MockedConstruction<BaselineCollectionReference> mockedStaticReference = mockConstruction(BaselineCollectionReference.class, (mock, context) -> {
            when(mock.get(Mockito.any())).thenReturn(baselineCollection);
        })) {
            List<CollectionItem> result = service.getCollectionItems(projectId, collectionId, mock(ReadOnlyTransaction.class));

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("space1/Module1", result.get(0).getModuleNameWithSpace());
            assertEquals("1", result.get(0).getRevision());

            assertEquals("space2/Module2", result.get(1).getModuleNameWithSpace());
            assertEquals("2", result.get(1).getRevision());
        }
    }
}

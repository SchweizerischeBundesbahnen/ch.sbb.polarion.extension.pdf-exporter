package ch.sbb.polarion.extension.pdf_exporter.service;

import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.attachments.TestRunAttachment;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.collections.DocumentCollectionEntry;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.DocIdentifier;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageWeightInfo;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentFileNameHelper;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollection;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestRecord;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.ITestRunAttachment;
import com.polarion.alm.tracker.model.ITestStepResult;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollectionElement;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.persistence.spi.PObjectList;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({PlatformContextMockExtension.class, TransactionalExecutorExtension.class})
class PdfExporterPolarionServiceTest {

    private final ITrackerService trackerService = mock(ITrackerService.class);
    private final ITestManagementService testManagementService = mock(ITestManagementService.class);
    private StylePackageSettings stylePackageSettings;
    private final DocumentFileNameHelper documentFileNameHelper = mock(DocumentFileNameHelper.class);

    private final PdfExporterPolarionService service = new PdfExporterPolarionService(
            trackerService,
            mock(IProjectService.class),
            mock(ISecurityService.class),
            mock(IPlatformService.class),
            mock(IRepositoryService.class),
            testManagementService,
            documentFileNameHelper
    );

    @BeforeEach
    void setUp() {
        stylePackageSettings = mock(StylePackageSettings.class);
        when(stylePackageSettings.getFeatureName()).thenReturn("style-package");
        NamedSettingsRegistry.INSTANCE.getAll().clear();
        NamedSettingsRegistry.INSTANCE.register(List.of(stylePackageSettings));
        when(documentFileNameHelper.getDocumentFileName(any())).thenReturn("testDocumentName.pdf");
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
        Collection<SettingName> result = service.getSuitableStylePackages(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());

        String projectId = "someProjectId";
        String spaceId = "someSpaceId";
        String documentName = "documentName";
        Collection<SettingName> defaultSettingNames = List.of(
                SettingName.builder().id("d1").name("default1").scope("").build(),
                SettingName.builder().id("d2").name("default2").scope("").build()
        );
        StylePackageModel defaultMockModel1 = mock(StylePackageModel.class);
        when(defaultMockModel1.getWeight()).thenReturn(10f);
        StylePackageModel defaultMockModel2 = mock(StylePackageModel.class);
        when(defaultMockModel2.getWeight()).thenReturn(16f);
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

        when(stylePackageSettings.readNames("")).thenReturn(defaultSettingNames);
        when(stylePackageSettings.readNames(ScopeUtils.getScopeFromProject(projectId))).thenReturn(settingNames);
        when(stylePackageSettings.read(eq(""), eq(SettingId.fromName("default1")), isNull())).thenReturn(defaultMockModel1);
        when(stylePackageSettings.read(eq(""), eq(SettingId.fromName("default2")), isNull())).thenReturn(defaultMockModel2);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name1")), isNull())).thenReturn(mockModel1);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name2")), isNull())).thenReturn(mockModel2);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name3")), isNull())).thenReturn(mockModel3);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name4")), isNull())).thenReturn(mockModel4);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name5")), isNull())).thenReturn(mockModel5);

        result = service.getSuitableStylePackages(List.of(new DocIdentifier(projectId, spaceId, documentName)));

        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals(List.of("name2", "name3", "name4", "name5", "name1"), result.stream().map(SettingName::getName).toList());

        result = service.getSuitableStylePackages(List.of(new DocIdentifier(projectId, spaceId, documentName), new DocIdentifier("anotherProject", spaceId, documentName)));
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(List.of("default2", "default1"), result.stream().map(SettingName::getName).toList());
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
    @SuppressWarnings("unchecked")
    void testGetTestRunAttachments() {

        ITestRunAttachment rootAttachment = mock(ITestRunAttachment.class); // an attachment on the test run itself
        when(rootAttachment.getFileName()).thenReturn("rootAttachment.txt");

        ITestRecord record1 = mock(ITestRecord.class);
        ITestRunAttachment record1Attachment = mock(ITestRunAttachment.class);
        when(record1Attachment.getFileName()).thenReturn("record1.pdf");
        when(record1.getAttachments()).thenReturn(List.of(record1Attachment));

        ITestRecord record2 = mock(ITestRecord.class);
        ITestRunAttachment record2Attachment = mock(ITestRunAttachment.class);
        when(record2Attachment.getFileName()).thenReturn("record2.txt");
        when(record2.getAttachments()).thenReturn(List.of(record2Attachment));

        ITestRunAttachment record2step1Attachment1 = mock(ITestRunAttachment.class);
        when(record2step1Attachment1.getFileName()).thenReturn("record2step1Attachment1.txt");
        ITestRunAttachment record2step1Attachment2 = mock(ITestRunAttachment.class);
        when(record2step1Attachment2.getFileName()).thenReturn("record2step1Attachment2.txt");
        ITestRunAttachment record2step2Attachment1 = mock(ITestRunAttachment.class);
        when(record2step2Attachment1.getFileName()).thenReturn("record2step2Attachment1.txt");

        ITestStepResult record2step1 = mock(ITestStepResult.class);
        when(record2step1.getAttachments()).thenReturn(List.of(record2step1Attachment1, record2step1Attachment2));
        ITestStepResult record2step2 = mock(ITestStepResult.class);
        when(record2step2.getAttachments()).thenReturn(List.of(record2step2Attachment1));
        when(record2.getTestStepResults()).thenReturn(List.of(record2step1, record2step2));

        ITestRun testRun = mock(ITestRun.class);
        when(testRun.isUnresolvable()).thenReturn(false);
        when(testRun.getAllRecords()).thenReturn(List.of(record1, record2));
        when(testRun.getAttachments()).thenReturn(new PObjectList(null,
                List.of(rootAttachment, record1Attachment, record2Attachment, record2step1Attachment1, record2step1Attachment2, record2step2Attachment1)));
        when(testManagementService.getTestRun("testProjectId", "testTestRunId", null)).thenReturn(testRun);

        List<TestRunAttachment> testRunAttachments = service.getTestRunAttachments("testProjectId", "testTestRunId", null, null, null);
        assertNotNull(testRunAttachments);
        assertEquals(6, testRunAttachments.size());

        List<TestRunAttachment> testRunAttachmentsFilteredPdf = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.pdf", null);
        assertNotNull(testRunAttachmentsFilteredPdf);
        assertEquals(1, testRunAttachmentsFilteredPdf.size());

        List<TestRunAttachment> testRunAttachmentsFilteredTxt = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.pdf", null);
        assertNotNull(testRunAttachmentsFilteredTxt);
        assertEquals(1, testRunAttachmentsFilteredTxt.size());

        List<TestRunAttachment> testRunAttachmentsFilteredAll = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.*", null);
        assertNotNull(testRunAttachmentsFilteredAll);
        assertEquals(6, testRunAttachmentsFilteredAll.size());

        List<TestRunAttachment> testRunAttachmentsFilteredNone = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.png", null);
        assertNotNull(testRunAttachmentsFilteredNone);
        assertEquals(0, testRunAttachmentsFilteredNone.size());

        IWorkItem workItem1 = mock(IWorkItem.class);
        IWorkItem workItem2 = mock(IWorkItem.class);
        when(record1.getTestCase()).thenReturn(workItem1);
        when(record2.getTestCase()).thenReturn(workItem2);

        when(testRun.getValue("someBooleanField")).thenReturn(true);
        when(workItem1.getValue("someBooleanField")).thenReturn(null);
        when(workItem2.getValue("someBooleanField")).thenReturn(null);
        List<TestRunAttachment> result = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.*", "someBooleanField");
        assertEquals(6, result.size());

        List<TestRunAttachment> testRunAttachmentsFilteredWithNullValue = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.*", "someBooleanField");
        assertNotNull(testRunAttachmentsFilteredWithNullValue);
        assertEquals(6, testRunAttachmentsFilteredWithNullValue.size());

        when(workItem1.getValue("someBooleanField")).thenReturn("true");
        List<TestRunAttachment> testRunAttachmentsFilteredWithWrongTypeValue = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.*", "someBooleanField");
        assertNotNull(testRunAttachmentsFilteredWithWrongTypeValue);
        assertEquals(5, testRunAttachmentsFilteredWithWrongTypeValue.size());

        when(workItem1.getValue("someBooleanField")).thenReturn(false);
        List<TestRunAttachment> testRunAttachmentsFilteredWithFalseValue = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.*", "someBooleanField");
        assertNotNull(testRunAttachmentsFilteredWithFalseValue);
        assertEquals(5, testRunAttachmentsFilteredWithFalseValue.size());

        when(workItem1.getValue("someBooleanField")).thenReturn(true);
        when(workItem2.getValue("someBooleanField")).thenReturn(false);
        result = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.*", "someBooleanField");
        assertEquals(2, result.size());

        when(testRun.getValue("someBooleanField")).thenReturn(false);
        when(workItem1.getValue("someBooleanField")).thenReturn(true);
        when(workItem2.getValue("someBooleanField")).thenReturn(true);
        result = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.*", "someBooleanField");
        assertEquals(6, result.size());

        when(workItem1.getValue("someBooleanField")).thenReturn(false);
        when(workItem2.getValue("someBooleanField")).thenReturn(false);
        result = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.*", "someBooleanField");
        assertEquals(1, result.size());

        when(testRun.getValue("someBooleanField")).thenReturn(true);
        when(workItem1.getValue("someBooleanField")).thenReturn(null);
        when(workItem2.getValue("someBooleanField")).thenReturn(false);
        result = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.*", "someBooleanField");
        assertEquals(2, result.size());

        when(testRun.getValue("someBooleanField")).thenReturn(null);
        when(workItem1.getValue("someBooleanField")).thenReturn(false);
        when(workItem2.getValue("someBooleanField")).thenReturn(null);
        result = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.*", "someBooleanField");
        assertEquals(1, result.size());

        when(testRun.getValue("someBooleanField")).thenReturn(false);
        when(workItem1.getValue("someBooleanField")).thenReturn("notBoolean");
        when(workItem2.getValue("someBooleanField")).thenReturn(null);
        result = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.*", "someBooleanField");
        assertEquals(1, result.size());

        when(workItem1.getValue("someBooleanField")).thenReturn(true);
        when(workItem2.getValue("someBooleanField")).thenReturn(true);
        result = service.getTestRunAttachments("testProjectId", "testTestRunId", null, "*.*", null);
        assertEquals(6, result.size());
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
    @SuppressWarnings("unused")
    void testGetDocumentsFromCollection() {
        String projectId = "testProjectId";
        String collectionId = "testCollectionId";

        IBaselineCollection mockCollection = mock(IBaselineCollection.class);
        BaselineCollectionReference mockBaselineCollectionReference = mock(BaselineCollectionReference.class);

        IModule mockModule1 = mock(IModule.class);
        IModule mockModule2 = mock(IModule.class);
        IModule mockModule3 = mock(IModule.class);

        when(mockModule1.getProjectId()).thenReturn(projectId);
        when(mockModule1.getModuleFolder()).thenReturn("space 1");
        when(mockModule1.getModuleName()).thenReturn("test Module1");
        ILocation mockLocation1 = mock(ILocation.class);
        when(mockModule1.getModuleLocation()).thenReturn(mockLocation1);
        when(mockLocation1.getLocationPath()).thenReturn("space 1");
        when(mockModule1.getRevision()).thenReturn("1");

        when(mockModule2.getProjectId()).thenReturn(projectId);
        when(mockModule2.getModuleFolder()).thenReturn("_default");
        when(mockModule2.getModuleName()).thenReturn("test Module2");
        ILocation mockLocation2 = mock(ILocation.class);
        when(mockModule2.getModuleLocation()).thenReturn(mockLocation2);
        when(mockLocation2.getLocationPath()).thenReturn("space 2");
        when(mockModule2.getRevision()).thenReturn("2");

        when(mockModule3.getProjectId()).thenReturn(projectId);
        when(mockModule3.getModuleFolder()).thenReturn("upstream_space");
        when(mockModule3.getModuleName()).thenReturn("upstream Module");
        ILocation mockLocation3 = mock(ILocation.class);
        when(mockModule3.getModuleLocation()).thenReturn(mockLocation3);
        when(mockLocation3.getLocationPath()).thenReturn("space 3");
        when(mockModule3.getRevision()).thenReturn("3");

        IBaselineCollectionElement mockElement1 = mock(IBaselineCollectionElement.class);
        IBaselineCollectionElement mockElement2 = mock(IBaselineCollectionElement.class);
        IBaselineCollectionElement mockUpstreamElement = mock(IBaselineCollectionElement.class);

        when(mockElement1.getObjectWithRevision()).thenReturn(mockModule1);
        when(mockElement2.getObjectWithRevision()).thenReturn(mockModule2);
        when(mockUpstreamElement.getObjectWithRevision()).thenReturn(mockModule3);

        IBaselineCollection mockUpstreamCollection = mock(IBaselineCollection.class);
        when(mockUpstreamCollection.getElements()).thenReturn(List.of(mockUpstreamElement));
        when(mockCollection.getElements()).thenReturn(List.of(mockElement1, mockElement2));
        when(mockCollection.getUpstreamCollections()).thenReturn(List.of(mockUpstreamCollection));

        BaselineCollection baselineCollection = mock(BaselineCollection.class);
        when(mockBaselineCollectionReference.get(Mockito.any())).thenReturn(baselineCollection);
        when(baselineCollection.getOldApi()).thenReturn(mockCollection);

        try (MockedConstruction<BaselineCollectionReference> mockedStaticReference = mockConstruction(BaselineCollectionReference.class, (mock, context) -> {
            when(mock.get(Mockito.any())).thenReturn(baselineCollection);
            when(mock.getWithRevision(Mockito.anyString())).thenReturn(mock);
        })) {
            List<DocumentCollectionEntry> result = service.getDocumentsFromCollection(projectId, collectionId, null, mock(ReadOnlyTransaction.class));

            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals("space 1", result.get(0).getSpaceId());
            assertEquals("test Module1", result.get(0).getDocumentName());
            assertEquals("1", result.get(0).getRevision());

            assertEquals("_default", result.get(1).getSpaceId());
            assertEquals("test Module2", result.get(1).getDocumentName());
            assertEquals("2", result.get(1).getRevision());

            assertEquals("upstream_space", result.get(2).getSpaceId());
            assertEquals("upstream Module", result.get(2).getDocumentName());
            assertEquals("3", result.get(2).getRevision());

            List<DocumentCollectionEntry> resultWithRevision = service.getDocumentsFromCollection(projectId, collectionId, "1234", mock(ReadOnlyTransaction.class));

            assertNotNull(resultWithRevision);
            assertEquals(3, resultWithRevision.size());
            assertEquals("space 1", resultWithRevision.get(0).getSpaceId());
            assertEquals("test Module1", resultWithRevision.get(0).getDocumentName());
            assertEquals("1", resultWithRevision.get(0).getRevision());

            assertEquals("_default", resultWithRevision.get(1).getSpaceId());
            assertEquals("test Module2", resultWithRevision.get(1).getDocumentName());
            assertEquals("2", resultWithRevision.get(1).getRevision());

            assertEquals("upstream_space", resultWithRevision.get(2).getSpaceId());
            assertEquals("upstream Module", resultWithRevision.get(2).getDocumentName());
            assertEquals("3", resultWithRevision.get(2).getRevision());
        }
    }

}

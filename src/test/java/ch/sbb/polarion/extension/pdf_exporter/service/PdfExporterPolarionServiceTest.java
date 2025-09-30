package ch.sbb.polarion.extension.pdf_exporter.service;

import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.attachments.TestRunAttachment;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.DocIdentifier;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageWeightInfo;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageSettings;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestRecord;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.ITestRunAttachment;
import com.polarion.alm.tracker.model.ITestStepResult;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.persistence.spi.PObjectList;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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
    void testGetMostSuitableStylePackageModel() {
        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);
        IModule module = mock(IModule.class);
        when(module.getProjectId()).thenReturn("someProjectId");
        ILocation location = mock(ILocation.class);
        when(module.getModuleLocation()).thenReturn(location);
        when(location.getLocationPath()).thenReturn("someSpaceId/documentName");
        IPObjectList matchingDocuments = new PObjectList(dataService, List.of(module));
        when(dataService.searchInstances(IModule.PROTO, "matching", "name")).thenReturn(matchingDocuments);
        IPObjectList notMatchingDocuments = new PObjectList(dataService, List.of());
        when(dataService.searchInstances(IModule.PROTO, "not_matching", "name")).thenReturn(notMatchingDocuments);

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
        StylePackageModel model1 = StylePackageModel.builder().weight(0.5f).matchingQuery("not_matching").build();
        StylePackageModel model2 = StylePackageModel.builder().weight(50.1f).matchingQuery("not_matching").build();
        StylePackageModel model3 = StylePackageModel.builder().weight(55.0f).matchingQuery("matching").build();
        StylePackageModel model4 = StylePackageModel.builder().weight(60.0f).matchingQuery("matching").build();
        StylePackageModel model5 = StylePackageModel.builder().weight(50.0f).build();

        when(stylePackageSettings.readNames("")).thenReturn(defaultSettingNames);
        when(stylePackageSettings.readNames(ScopeUtils.getScopeFromProject(projectId))).thenReturn(settingNames);
        when(stylePackageSettings.read(eq(""), eq(SettingId.fromName("default1")), isNull())).thenReturn(defaultMockModel1);
        when(stylePackageSettings.read(eq(""), eq(SettingId.fromName("default2")), isNull())).thenReturn(defaultMockModel2);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name1")), isNull())).thenReturn(model1);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name2")), isNull())).thenReturn(model2);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name3")), isNull())).thenReturn(model3);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name4")), isNull())).thenReturn(model4);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name5")), isNull())).thenReturn(model5);

        StylePackageModel result = service.getMostSuitableStylePackageModel(new DocIdentifier(projectId, spaceId, documentName));

        assertNotNull(result);
        assertEquals(60.0f, result.getWeight());
        assertEquals("matching", result.getMatchingQuery());
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
}

package ch.sbb.polarion.extension.pdf_exporter.service;

import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.attachments.TestRunAttachment;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.authorization.AuthorizationModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.DocIdentifier;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageWeightInfo;
import ch.sbb.polarion.extension.pdf_exporter.settings.AuthorizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageSettings;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
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
import com.polarion.subterra.base.data.identification.IContextId;
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
    @SuppressWarnings("rawtypes")
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

        IRichPage page = mock(IRichPage.class);
        when(page.getProjectId()).thenReturn("someProjectId");
        when(page.getSpaceId()).thenReturn("someSpaceId");
        when(page.getPageName()).thenReturn("pageName");
        IPObjectList matchingPages = new PObjectList(dataService, List.of(page));
        when(dataService.searchInstances(IRichPage.PROTO, "matching", "name")).thenReturn(matchingPages);
        IPObjectList notMatchingPages = new PObjectList(dataService, List.of());
        when(dataService.searchInstances(IRichPage.PROTO, "not_matching", "name")).thenReturn(notMatchingPages);

        IPObjectList emptyTestRuns = new PObjectList(dataService, List.of());
        when(dataService.searchInstances(ITestRun.PROTO, "matching", "name")).thenReturn(emptyTestRuns);
        when(dataService.searchInstances(ITestRun.PROTO, "not_matching", "name")).thenReturn(emptyTestRuns);

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
        StylePackageModel model1Page = StylePackageModel.builder().weight(88f).matchingQuery("matching").build();

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
        assertEquals(60.0f, result.getWeight());
        assertEquals("matching", result.getMatchingQuery());

        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("name1")), isNull())).thenReturn(model1Page);
        result = service.getMostSuitableStylePackageModel(new DocIdentifier(projectId, spaceId, "pageName"));
        assertEquals(88f, result.getWeight());
        assertEquals("matching", result.getMatchingQuery());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testStylePackageSuitabilityWithUnresolvableDocuments() {
        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);

        // Create an unresolvable module that would throw on location access
        IModule unresolvableModule = mock(IModule.class);
        when(unresolvableModule.isUnresolvable()).thenReturn(true);
        when(unresolvableModule.getModuleLocation()).thenThrow(new IllegalStateException("Cannot access location of unresolvable object"));

        // Create a resolvable module that matches the document
        IModule resolvableModule = mock(IModule.class);
        when(resolvableModule.isUnresolvable()).thenReturn(false);
        when(resolvableModule.getProjectId()).thenReturn("someProjectId");
        ILocation location = mock(ILocation.class);
        when(resolvableModule.getModuleLocation()).thenReturn(location);
        when(location.getLocationPath()).thenReturn("someSpaceId/documentName");

        // Search returns both unresolvable and resolvable documents
        IPObjectList mixedDocuments = new PObjectList(dataService, List.of(unresolvableModule, resolvableModule));
        when(dataService.searchInstances(IModule.PROTO, "mixed_query", "name")).thenReturn(mixedDocuments);
        when(dataService.searchInstances(IRichPage.PROTO, "mixed_query", "name")).thenReturn(new PObjectList(dataService, List.of()));
        when(dataService.searchInstances(ITestRun.PROTO, "mixed_query", "name")).thenReturn(new PObjectList(dataService, List.of()));

        // Search returns only unresolvable documents
        IPObjectList unresolvableOnly = new PObjectList(dataService, List.of(unresolvableModule));
        when(dataService.searchInstances(IModule.PROTO, "unresolvable_only", "name")).thenReturn(unresolvableOnly);
        when(dataService.searchInstances(IRichPage.PROTO, "unresolvable_only", "name")).thenReturn(new PObjectList(dataService, List.of()));
        when(dataService.searchInstances(ITestRun.PROTO, "unresolvable_only", "name")).thenReturn(new PObjectList(dataService, List.of()));

        String projectId = "someProjectId";
        String spaceId = "someSpaceId";
        String documentName = "documentName";

        Collection<SettingName> settingNames = List.of(
                SettingName.builder().id("id1").name("mixedPackage").scope("project/someProjectId/").build(),
                SettingName.builder().id("id2").name("unresolvablePackage").scope("project/someProjectId/").build()
        );

        // Package with query returning mixed (unresolvable + resolvable) documents - should be suitable
        StylePackageModel mixedModel = StylePackageModel.builder().weight(10f).matchingQuery("mixed_query").build();
        // Package with query returning only unresolvable documents - should NOT be suitable
        StylePackageModel unresolvableModel = StylePackageModel.builder().weight(20f).matchingQuery("unresolvable_only").build();

        when(stylePackageSettings.readNames("")).thenReturn(List.of());
        when(stylePackageSettings.readNames(ScopeUtils.getScopeFromProject(projectId))).thenReturn(settingNames);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("mixedPackage")), isNull())).thenReturn(mixedModel);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("unresolvablePackage")), isNull())).thenReturn(unresolvableModel);
        when(stylePackageSettings.defaultValues()).thenReturn(StylePackageModel.builder().weight(0f).build());

        // When only unresolvable documents match, the package with higher weight should be skipped,
        // and the one with resolvable matches should be selected
        StylePackageModel result = service.getMostSuitableStylePackageModel(new DocIdentifier(projectId, spaceId, documentName));

        // mixedPackage should be selected (weight 10) because unresolvablePackage (weight 20)
        // has no resolvable matching documents
        assertEquals(10f, result.getWeight());
        assertEquals("mixed_query", result.getMatchingQuery());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testStylePackageSuitabilityForTestRunWithNullSpaceId() {
        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);

        // A matching test run
        ITestRun testRun = mock(ITestRun.class);
        when(testRun.isUnresolvable()).thenReturn(false);
        when(testRun.getProjectId()).thenReturn("someProjectId");
        when(testRun.getId()).thenReturn("TestRun-123");

        // Also return a module and rich page from the same query — these should NOT match
        // when spaceId is null (covers null-safe branches in sameDocument)
        IModule module = mock(IModule.class);
        when(module.isUnresolvable()).thenReturn(false);
        when(module.getProjectId()).thenReturn("someProjectId");
        ILocation location = mock(ILocation.class);
        when(module.getModuleLocation()).thenReturn(location);
        when(location.getLocationPath()).thenReturn("space/SomeDoc");

        IRichPage page = mock(IRichPage.class);
        when(page.isUnresolvable()).thenReturn(false);
        when(page.getProjectId()).thenReturn("someProjectId");
        when(page.getSpaceId()).thenReturn("space");
        when(page.getPageName()).thenReturn("SomePage");

        when(dataService.searchInstances(ITestRun.PROTO, "testrun_query", "name")).thenReturn(new PObjectList(dataService, List.of(testRun)));
        when(dataService.searchInstances(IModule.PROTO, "testrun_query", "name")).thenReturn(new PObjectList(dataService, List.of(module)));
        when(dataService.searchInstances(IRichPage.PROTO, "testrun_query", "name")).thenReturn(new PObjectList(dataService, List.of(page)));

        String projectId = "someProjectId";

        Collection<SettingName> settingNames = List.of(
                SettingName.builder().id("id1").name("withQuery").scope("project/someProjectId/").build(),
                SettingName.builder().id("id2").name("noQuery").scope("project/someProjectId/").build()
        );

        StylePackageModel modelWithQuery = StylePackageModel.builder().weight(10f).matchingQuery("testrun_query").build();
        StylePackageModel modelNoQuery = StylePackageModel.builder().weight(5f).build();

        when(stylePackageSettings.readNames("")).thenReturn(List.of());
        when(stylePackageSettings.readNames(ScopeUtils.getScopeFromProject(projectId))).thenReturn(settingNames);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("withQuery")), isNull())).thenReturn(modelWithQuery);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("noQuery")), isNull())).thenReturn(modelNoQuery);

        // Test Run with null spaceId matching by ID — should find style package with matchingQuery
        DocIdentifier testRunDoc = new DocIdentifier(projectId, null, "TestRun-123");
        Collection<SettingName> result = service.getSuitableStylePackages(List.of(testRunDoc));
        assertEquals(2, result.size());
        assertEquals(List.of("withQuery", "noQuery"), result.stream().map(SettingName::getName).toList());

        // Test Run with non-matching ID — style package with matchingQuery should be excluded
        DocIdentifier nonMatchingTestRun = new DocIdentifier(projectId, null, "TestRun-999");
        result = service.getSuitableStylePackages(List.of(nonMatchingTestRun));
        assertEquals(1, result.size());
        assertEquals("noQuery", result.iterator().next().getName());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testStylePackageSuitabilityMixedDocumentAndTestRun() {
        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);

        // Module matching query
        IModule module = mock(IModule.class);
        when(module.isUnresolvable()).thenReturn(false);
        when(module.getProjectId()).thenReturn("someProjectId");
        ILocation location = mock(ILocation.class);
        when(module.getModuleLocation()).thenReturn(location);
        when(location.getLocationPath()).thenReturn("space/Doc1");
        when(dataService.searchInstances(IModule.PROTO, "doc_query", "name")).thenReturn(new PObjectList(dataService, List.of(module)));
        when(dataService.searchInstances(IRichPage.PROTO, "doc_query", "name")).thenReturn(new PObjectList(dataService, List.of()));
        when(dataService.searchInstances(ITestRun.PROTO, "doc_query", "name")).thenReturn(new PObjectList(dataService, List.of()));

        String projectId = "someProjectId";
        Collection<SettingName> settingNames = List.of(
                SettingName.builder().id("id1").name("docOnly").scope("project/someProjectId/").build(),
                SettingName.builder().id("id2").name("generic").scope("project/someProjectId/").build()
        );

        StylePackageModel docOnlyModel = StylePackageModel.builder().weight(10f).matchingQuery("doc_query").build();
        StylePackageModel genericModel = StylePackageModel.builder().weight(5f).build();

        when(stylePackageSettings.readNames("")).thenReturn(List.of());
        when(stylePackageSettings.readNames(ScopeUtils.getScopeFromProject(projectId))).thenReturn(settingNames);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("docOnly")), isNull())).thenReturn(docOnlyModel);
        when(stylePackageSettings.read(eq("project/someProjectId/"), eq(SettingId.fromName("generic")), isNull())).thenReturn(genericModel);

        // Mix: Module (matches query) + TestRun (does NOT match query, spaceId=null)
        // allMatch requires all docs to match → "docOnly" should be excluded
        DocIdentifier moduleDoc = new DocIdentifier(projectId, "space", "Doc1");
        DocIdentifier testRunDoc = new DocIdentifier(projectId, null, "TestRun-456");
        Collection<SettingName> result = service.getSuitableStylePackages(List.of(moduleDoc, testRunDoc));

        assertEquals(1, result.size());
        assertEquals("generic", result.iterator().next().getName());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testSearchInstancesSafeSwallowsExceptionForUnsupportedPrototype() {
        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);

        // An unknown document type — covers the default branch in sameDocument
        IUniqueObject unknownDoc = mock(IUniqueObject.class);
        when(unknownDoc.isUnresolvable()).thenReturn(false);
        when(unknownDoc.getProjectId()).thenReturn("proj");

        // Module and RichPage return non-matching results, so stream must reach ITestRun
        when(dataService.searchInstances(IModule.PROTO, "failing_query", "name")).thenReturn(new PObjectList(dataService, List.of(unknownDoc)));
        when(dataService.searchInstances(IRichPage.PROTO, "failing_query", "name")).thenReturn(new PObjectList(dataService, List.of()));

        // ITestRun search throws — query not compatible with this prototype
        when(dataService.searchInstances(ITestRun.PROTO, "failing_query", "name")).thenThrow(new RuntimeException("Unsupported field for TestRun"));

        Collection<SettingName> settingNames = List.of(
                SettingName.builder().id("id1").name("pkg").scope("project/proj/").build()
        );
        StylePackageModel modelWithQuery = StylePackageModel.builder().weight(10f).matchingQuery("failing_query").build();

        when(stylePackageSettings.readNames("")).thenReturn(List.of());
        when(stylePackageSettings.readNames(ScopeUtils.getScopeFromProject("proj"))).thenReturn(settingNames);
        when(stylePackageSettings.read(eq("project/proj/"), eq(SettingId.fromName("pkg")), isNull())).thenReturn(modelWithQuery);
        when(stylePackageSettings.defaultValues()).thenReturn(StylePackageModel.builder().weight(0f).build());

        // No documents match (unknownDoc hits default->false, RichPage empty, TestRun throws)
        // but the exception should not propagate — package simply doesn't match
        DocIdentifier doc = new DocIdentifier("proj", "space", "Doc");
        Collection<SettingName> result = service.getSuitableStylePackages(List.of(doc));
        assertEquals(0, result.size());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testSearchInstancesSafeRecoversAndFindsMatch() {
        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);

        // IModule throws
        when(dataService.searchInstances(IModule.PROTO, "mixed_fail_query", "name")).thenThrow(new RuntimeException("Bad query for Module"));
        // IRichPage returns empty
        when(dataService.searchInstances(IRichPage.PROTO, "mixed_fail_query", "name")).thenReturn(new PObjectList(dataService, List.of()));
        // ITestRun returns a match
        ITestRun testRun = mock(ITestRun.class);
        when(testRun.isUnresolvable()).thenReturn(false);
        when(testRun.getProjectId()).thenReturn("proj");
        when(testRun.getId()).thenReturn("TR-1");
        when(dataService.searchInstances(ITestRun.PROTO, "mixed_fail_query", "name")).thenReturn(new PObjectList(dataService, List.of(testRun)));

        Collection<SettingName> settingNames = List.of(
                SettingName.builder().id("id1").name("pkg").scope("project/proj/").build()
        );
        StylePackageModel modelWithQuery = StylePackageModel.builder().weight(10f).matchingQuery("mixed_fail_query").build();

        when(stylePackageSettings.readNames("")).thenReturn(List.of());
        when(stylePackageSettings.readNames(ScopeUtils.getScopeFromProject("proj"))).thenReturn(settingNames);
        when(stylePackageSettings.read(eq("project/proj/"), eq(SettingId.fromName("pkg")), isNull())).thenReturn(modelWithQuery);

        // Module search fails, but TestRun match should still be found
        DocIdentifier doc = new DocIdentifier("proj", null, "TR-1");
        Collection<SettingName> result = service.getSuitableStylePackages(List.of(doc));
        assertEquals(1, result.size());
        assertEquals("pkg", result.iterator().next().getName());
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
    void validateWorkItemsQueryAcceptsNullAndEmpty() {
        assertDoesNotThrow(() -> service.validateWorkItemsQuery(null));
        assertDoesNotThrow(() -> service.validateWorkItemsQuery(""));
        verifyNoInteractions(trackerService);
    }

    @Test
    void validateWorkItemsQueryDelegatesToDataService() {
        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);

        service.validateWorkItemsQuery("type:requirement");

        verify(dataService).searchInstances(IWorkItem.PROTO, "type:requirement", null, 1);
    }

    @Test
    void validateWorkItemsQueryWrapsParserException() {
        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);
        when(dataService.searchInstances(IWorkItem.PROTO, "broken syntax !@#", null, 1))
                .thenThrow(new RuntimeException("Syntax error at position 5"));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.validateWorkItemsQuery("broken syntax !@#"));
        assertTrue(thrown.getMessage().contains("Invalid work items query"));
        assertTrue(thrown.getMessage().contains("Syntax error"));
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
    void userAuthorizedForExportWhenUnrestricted() {
        // No roles configured => export available to everyone, no role lookup needed
        registerAuthorizationModel(new AuthorizationModel(List.of(), List.of()));
        assertTrue(service.userAuthorizedForExport("proj"));
    }

    @Test
    void userAuthorizedForExportWhenUserHasAllowedGlobalRole() {
        ISecurityService securityService = mock(ISecurityService.class);
        PdfExporterPolarionService svc = authorizationService(securityService);
        registerAuthorizationModel(new AuthorizationModel(List.of("admin"), List.of()));

        when(securityService.getCurrentUser()).thenReturn("user1");
        when(securityService.getRolesForUser("user1")).thenReturn(List.of("developer", "admin"));

        assertTrue(svc.userAuthorizedForExport("proj"));
    }

    @Test
    void userAuthorizedForExportWhenUserHasAllowedProjectRole() {
        ISecurityService securityService = mock(ISecurityService.class);
        PdfExporterPolarionService svc = authorizationService(securityService);
        registerAuthorizationModel(new AuthorizationModel(List.of("globalOnly"), List.of("reviewer")));

        ITrackerProject trackerProject = mock(ITrackerProject.class);
        IContextId contextId = mock(IContextId.class);
        when(trackerProject.getContextId()).thenReturn(contextId);
        doReturn(trackerProject).when(svc).getTrackerProject("proj");

        when(securityService.getCurrentUser()).thenReturn("user1");
        when(securityService.getRolesForUser("user1")).thenReturn(List.of("developer"));
        when(securityService.getRolesForUser("user1", contextId)).thenReturn(List.of("reviewer"));

        assertTrue(svc.userAuthorizedForExport("proj"));
    }

    @Test
    void userNotAuthorizedForExportWhenNoRoleMatches() {
        ISecurityService securityService = mock(ISecurityService.class);
        PdfExporterPolarionService svc = authorizationService(securityService);
        registerAuthorizationModel(new AuthorizationModel(List.of("admin"), List.of("reviewer")));

        ITrackerProject trackerProject = mock(ITrackerProject.class);
        IContextId contextId = mock(IContextId.class);
        when(trackerProject.getContextId()).thenReturn(contextId);
        doReturn(trackerProject).when(svc).getTrackerProject("proj");

        when(securityService.getCurrentUser()).thenReturn("user1");
        when(securityService.getRolesForUser("user1")).thenReturn(List.of("developer"));
        when(securityService.getRolesForUser("user1", contextId)).thenReturn(List.of("developer"));

        assertFalse(svc.userAuthorizedForExport("proj"));
    }

    @Test
    void userNotAuthorizedForExportWhenProjectIdNullAndNoGlobalRole() {
        ISecurityService securityService = mock(ISecurityService.class);
        PdfExporterPolarionService svc = authorizationService(securityService);
        registerAuthorizationModel(new AuthorizationModel(List.of("admin"), List.of()));

        when(securityService.getCurrentUser()).thenReturn("user1");
        when(securityService.getRolesForUser("user1")).thenReturn(List.of("developer"));

        // No project → only global roles apply; none match → denied (no project-context lookup).
        assertFalse(svc.userAuthorizedForExport(null));
    }

    private PdfExporterPolarionService authorizationService(ISecurityService securityService) {
        return spy(new PdfExporterPolarionService(
                trackerService,
                mock(IProjectService.class),
                securityService,
                mock(IPlatformService.class),
                mock(IRepositoryService.class),
                testManagementService
        ));
    }

    private void registerAuthorizationModel(AuthorizationModel model) {
        AuthorizationSettings authorizationSettings = mock(AuthorizationSettings.class);
        when(authorizationSettings.getFeatureName()).thenReturn(AuthorizationSettings.FEATURE_NAME);
        when(authorizationSettings.read(nullable(String.class), any(SettingId.class), isNull())).thenReturn(model);
        NamedSettingsRegistry.INSTANCE.register(List.of(authorizationSettings));
    }
}

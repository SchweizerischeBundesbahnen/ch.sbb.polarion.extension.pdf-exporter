package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.util.exporter.ModifiedDocumentRenderer;
import ch.sbb.polarion.extension.pdf_exporter.util.exporter.WikiRenderer;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.server.api.model.document.ProxyDocument;
import com.polarion.alm.server.api.model.rp.ProxyRichPage;
import com.polarion.alm.server.api.model.tr.ProxyTestRun;
import com.polarion.alm.shared.api.Scope;
import com.polarion.alm.shared.api.model.rp.RichPageReference;
import com.polarion.alm.shared.api.model.tr.TestRunReference;
import com.polarion.alm.shared.api.services.internal.InternalServices;
import com.polarion.alm.shared.api.transaction.RunnableInReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.rpe.RpeModelAspect;
import com.polarion.alm.shared.rpe.RpeRenderer;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IBaseline;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWikiPage;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.tracker.model.ipi.IInternalBaselinesManager;
import com.polarion.alm.tracker.spi.model.IInternalModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class DocumentDataTest {

    @Mock
    private IInternalBaselinesManager internalBaselinesManager;
    @Mock
    private InternalReadOnlyTransaction internalReadOnlyTransactionMock;
    @Mock
    private IInternalModule module;
    @Mock
    private IRichPage richPageInDefaultRepo;
    @Mock
    private IRichPage richPageInProject;
    @Mock
    private ITestRun testRun;
    @Mock
    private IWikiPage wikiPageInDefaultRepo;
    @Mock
    private IWikiPage wikiPageInProject;
    @Mock
    private IBaselineCollection baselineCollection;

    private MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic;

    @BeforeEach
    void setUp() {
        transactionalExecutorMockedStatic = mockStatic(TransactionalExecutor.class);
        transactionalExecutorMockedStatic.when(() -> TransactionalExecutor.executeSafelyInReadOnlyTransaction(any()))
                .thenAnswer(invocation -> {
                    RunnableInReadOnlyTransaction<?> runnable = invocation.getArgument(0);
                    return runnable.run(internalReadOnlyTransactionMock);
                });

        InternalServices internalServicesMock = mock(InternalServices.class);
        ITrackerService trackerServiceMock = mock(ITrackerService.class);
        ITrackerProject trackerProjectMock = mock(ITrackerProject.class);
        lenient().when(trackerProjectMock.getBaselinesManager()).thenReturn(internalBaselinesManager);
        lenient().when(trackerServiceMock.getTrackerProject((IProject) any())).thenReturn(trackerProjectMock);
        lenient().when(internalServicesMock.trackerService()).thenReturn(trackerServiceMock);
        lenient().when(internalReadOnlyTransactionMock.services()).thenReturn(internalServicesMock);

        mockDocumentBaseline(module);
        mockDocumentBaseline(richPageInDefaultRepo);
        mockDocumentBaseline(richPageInProject);
        mockDocumentBaseline(testRun);
        mockDocumentBaseline(wikiPageInDefaultRepo);
        mockDocumentBaseline(wikiPageInProject);
    }

    private void mockDocumentBaseline(IUniqueObject uniqueObject) {
        IBaseline documentBaselineName = mock(IBaseline.class);
        lenient().when(documentBaselineName.getName()).thenReturn("documentBaselineName");
        lenient().when(internalBaselinesManager.getRevisionBaseline(uniqueObject, "12345")).thenReturn(documentBaselineName);
        IBaseline projectBaselineName = mock(IBaseline.class);
        lenient().when(projectBaselineName.getName()).thenReturn("projectBaselineName");
        lenient().when(internalBaselinesManager.getRevisionBaseline("12345")).thenReturn(projectBaselineName);
    }

    @AfterEach
    void tearDown() {
        transactionalExecutorMockedStatic.close();
    }

    @Test
    void testLiveDocDocumentData() {
        when(module.getId()).thenReturn("module id");
        when(module.getLastRevision()).thenReturn("12345");
        ITrackerProject project = mock(ITrackerProject.class);
        when(project.getId()).thenReturn("project id");
        when(project.getName()).thenReturn("project name");
        when(module.getProject()).thenReturn(project);
        when(module.getModuleFolder()).thenReturn("_default");
        when(module.getTitleOrName()).thenReturn("module title");

        UniqueObjectConverter uniqueObjectConverter = new UniqueObjectConverter(module);
        DocumentData<IModule> documentData = uniqueObjectConverter.toDocumentData();

        assertEquals("project id", documentData.getId().getDocumentProject().getId());
        assertEquals("project name", documentData.getId().getDocumentProject().getName());
        assertEquals("_default", ((LiveDocId) documentData.getId()).getSpaceId());
        assertEquals("module id", documentData.getId().getDocumentId());
        assertEquals(DocumentType.LIVE_DOC, documentData.getType());
        assertEquals("module title", documentData.getTitle());
        assertNull(documentData.getContent());
        assertNotNull(documentData.getBaseline());
        assertEquals("pb. projectBaselineName | db. documentBaselineName", documentData.getBaseline().asPlaceholder());
        assertNotNull(uniqueObjectConverter.getUniqueObjectAdapter().getDocumentBaseline());
        assertEquals("pb. projectBaselineName | db. documentBaselineName", uniqueObjectConverter.getUniqueObjectAdapter().getDocumentBaseline().asPlaceholder());
        assertNull(documentData.getRevision());
        assertEquals("12345", documentData.getLastRevision());
        assertNull(documentData.getContent());

        ExportParams exportParams = ExportParams.builder()
                .locationPath("location path")
                .build();

        try (
                MockedConstruction<ProxyDocument> proxyDocumentMockedConstruction = mockConstruction(ProxyDocument.class, (mock, context) -> {
                    when(mock.getHomePageContentHtml()).thenReturn("");
                });
                MockedConstruction<ModifiedDocumentRenderer> modifiedDocumentRendererMockedConstruction = mockConstruction(ModifiedDocumentRenderer.class, (mock, context) -> {
                    when(mock.render(anyString())).thenReturn("test content");
                })
        ) {
            DocumentData<IModule> documentDataWithContent = uniqueObjectConverter
                    .withContent(true)
                    .withExportParams(exportParams)
                    .toDocumentData();
            assertEquals("test content", documentDataWithContent.getContent());
        }
    }

    @Test
    void testLiveReportDocumentData() {
        when(richPageInProject.getId()).thenReturn("rich page id");
        when(richPageInProject.getLastRevision()).thenReturn("12345");
        ITrackerProject project = mock(ITrackerProject.class);
        when(project.getId()).thenReturn("project id");
        when(project.getName()).thenReturn("project name");
        when(richPageInProject.getProject()).thenReturn(project);
        when(richPageInProject.getTitleOrName()).thenReturn("rich page title");

        UniqueObjectConverter uniqueObjectConverter = new UniqueObjectConverter(richPageInProject);
        DocumentData<IRichPage> documentData = uniqueObjectConverter.toDocumentData();

        assertEquals("project id", documentData.getId().getDocumentProject().getId());
        assertEquals("project name", documentData.getId().getDocumentProject().getName());
        assertEquals("rich page id", documentData.getId().getDocumentId());
        assertEquals(DocumentType.LIVE_REPORT, documentData.getType());
        assertEquals("rich page title", documentData.getTitle());
        assertNull(documentData.getContent());
        assertNotNull(documentData.getBaseline());
        assertEquals("pb. projectBaselineName | db. documentBaselineName", documentData.getBaseline().asPlaceholder());
        assertNotNull(uniqueObjectConverter.getUniqueObjectAdapter().getDocumentBaseline());
        assertEquals("pb. projectBaselineName | db. documentBaselineName", uniqueObjectConverter.getUniqueObjectAdapter().getDocumentBaseline().asPlaceholder());
        assertNull(documentData.getRevision());
        assertEquals("12345", documentData.getLastRevision());
        assertNull(documentData.getContent());

        ExportParams exportParams = ExportParams.builder()
                .locationPath("location path")
                .build();

        try (
                MockedStatic<RpeModelAspect> rpeModelAspectMockedStatic = mockStatic(RpeModelAspect.class);
                MockedConstruction<ProxyRichPage> proxyRichPageMockedConstruction = mockConstruction(ProxyRichPage.class, (mock, context) -> {
                    RichPageReference referenceRichPageMock = mock(RichPageReference.class);
                    when(mock.getReference()).thenReturn(referenceRichPageMock);
                    when(referenceRichPageMock.scope()).thenReturn(mock(Scope.class));
                });

                MockedConstruction<RpeRenderer> modifiedDocumentRendererMockedConstruction = mockConstruction(RpeRenderer.class, (mock, context) -> {
                    when(mock.render(null)).thenReturn("test content");
                })
        ) {
            DocumentData<IModule> documentDataWithContent = uniqueObjectConverter
                    .withContent(true)
                    .withExportParams(exportParams)
                    .toDocumentData();
            assertEquals("test content", documentDataWithContent.getContent());
        }
    }

    @Test
    void testLiveReportInDefaultRepoDocumentData() {
        when(richPageInDefaultRepo.getId()).thenReturn("rich page id");
        when(richPageInDefaultRepo.getLastRevision()).thenReturn("12345");
        when(richPageInDefaultRepo.getProject()).thenReturn(null);
        when(richPageInDefaultRepo.getTitleOrName()).thenReturn("rich page title");

        UniqueObjectConverter uniqueObjectConverter = new UniqueObjectConverter(richPageInDefaultRepo);
        DocumentData<IRichPage> documentData = uniqueObjectConverter.toDocumentData();

        assertNull(documentData.getId().getDocumentProject());
        assertEquals("rich page id", documentData.getId().getDocumentId());
        assertEquals(DocumentType.LIVE_REPORT, documentData.getType());
        assertEquals("rich page title", documentData.getTitle());
        assertNull(documentData.getContent());
        assertNotNull(documentData.getBaseline());
        assertEquals("pb. projectBaselineName | db. documentBaselineName", documentData.getBaseline().asPlaceholder());
        assertNotNull(uniqueObjectConverter.getUniqueObjectAdapter().getDocumentBaseline());
        assertEquals("pb. projectBaselineName | db. documentBaselineName", uniqueObjectConverter.getUniqueObjectAdapter().getDocumentBaseline().asPlaceholder());
        assertNull(documentData.getRevision());
        assertEquals("12345", documentData.getLastRevision());
        assertNull(documentData.getContent());

        ExportParams exportParams = ExportParams.builder()
                .locationPath("location path")
                .build();

        try (
                MockedStatic<RpeModelAspect> rpeModelAspectMockedStatic = mockStatic(RpeModelAspect.class);
                MockedConstruction<ProxyRichPage> proxyRichPageMockedConstruction = mockConstruction(ProxyRichPage.class, (mock, context) -> {
                    RichPageReference referenceRichPageMock = mock(RichPageReference.class);
                    when(mock.getReference()).thenReturn(referenceRichPageMock);
                    when(referenceRichPageMock.scope()).thenReturn(mock(Scope.class));
                });

                MockedConstruction<RpeRenderer> modifiedDocumentRendererMockedConstruction = mockConstruction(RpeRenderer.class, (mock, context) -> {
                    when(mock.render(null)).thenReturn("test content");
                })
        ) {
            DocumentData<IModule> documentDataWithContent = uniqueObjectConverter
                    .withContent(true)
                    .withExportParams(exportParams)
                    .toDocumentData();
            assertEquals("test content", documentDataWithContent.getContent());
        }
    }

    @Test
    void testTestRunDocumentData() {
        when(testRun.getId()).thenReturn("test run id");
        when(testRun.getLastRevision()).thenReturn("12345");
        ITrackerProject project = mock(ITrackerProject.class);
        when(project.getId()).thenReturn("project id");
        when(project.getName()).thenReturn("project name");
        when(testRun.getProject()).thenReturn(project);
        when(testRun.getLabel()).thenReturn("test run title");

        UniqueObjectConverter uniqueObjectConverter = new UniqueObjectConverter(testRun);
        DocumentData<IRichPage> documentData = uniqueObjectConverter.toDocumentData();

        assertEquals("project id", documentData.getId().getDocumentProject().getId());
        assertEquals("project name", documentData.getId().getDocumentProject().getName());
        assertEquals("test run id", documentData.getId().getDocumentId());
        assertEquals(DocumentType.TEST_RUN, documentData.getType());
        assertEquals("test run title", documentData.getTitle());
        assertNull(documentData.getContent());
        assertNotNull(documentData.getBaseline());
        assertEquals("pb. projectBaselineName | db. documentBaselineName", documentData.getBaseline().asPlaceholder());
        assertNotNull(uniqueObjectConverter.getUniqueObjectAdapter().getDocumentBaseline());
        assertEquals("pb. projectBaselineName | db. documentBaselineName", uniqueObjectConverter.getUniqueObjectAdapter().getDocumentBaseline().asPlaceholder());
        assertNull(documentData.getRevision());
        assertEquals("12345", documentData.getLastRevision());
        assertNull(documentData.getContent());

        ExportParams exportParams = ExportParams.builder()
                .locationPath("location path")
                .build();

        try (
                MockedStatic<RpeModelAspect> rpeModelAspectMockedStatic = mockStatic(RpeModelAspect.class);
                MockedConstruction<ProxyTestRun> proxyTestRunMockedConstruction = mockConstruction(ProxyTestRun.class, (mock, context) -> {
                    TestRunReference referenceRichPageMock = mock(TestRunReference.class);
                    when(mock.getReference()).thenReturn(referenceRichPageMock);
                    when(referenceRichPageMock.scope()).thenReturn(mock(Scope.class));
                });

                MockedConstruction<RpeRenderer> modifiedDocumentRendererMockedConstruction = mockConstruction(RpeRenderer.class, (mock, context) -> {
                    when(mock.render(null)).thenReturn("test content");
                })
        ) {
            DocumentData<ITestRun> documentDataWithContent = uniqueObjectConverter
                    .withContent(true)
                    .withExportParams(exportParams)
                    .toDocumentData();
            assertEquals("test content", documentDataWithContent.getContent());
        }
    }

    @Test
    void testWikiPageInProjectDocumentData() {
        when(wikiPageInProject.getId()).thenReturn("wiki page id");
        when(wikiPageInProject.getLastRevision()).thenReturn("12345");
        ITrackerProject project = mock(ITrackerProject.class);
        when(project.getId()).thenReturn("project id");
        when(project.getName()).thenReturn("project name");
        when(wikiPageInProject.getProject()).thenReturn(project);
        when(wikiPageInProject.getTitleOrName()).thenReturn("wiki page title");

        UniqueObjectConverter uniqueObjectConverter = new UniqueObjectConverter(wikiPageInProject);
        DocumentData<IWikiPage> documentData = uniqueObjectConverter.toDocumentData();

        assertEquals("project id", documentData.getId().getDocumentProject().getId());
        assertEquals("project name", documentData.getId().getDocumentProject().getName());
        assertEquals("wiki page id", documentData.getId().getDocumentId());
        assertEquals(DocumentType.WIKI_PAGE, documentData.getType());
        assertEquals("wiki page title", documentData.getTitle());
        assertNull(documentData.getContent());
        assertNotNull(documentData.getBaseline());
        assertEquals("pb. projectBaselineName | db. documentBaselineName", documentData.getBaseline().asPlaceholder());
        assertNotNull(uniqueObjectConverter.getUniqueObjectAdapter().getDocumentBaseline());
        assertEquals("pb. projectBaselineName | db. documentBaselineName", uniqueObjectConverter.getUniqueObjectAdapter().getDocumentBaseline().asPlaceholder());
        assertNull(documentData.getRevision());
        assertEquals("12345", documentData.getLastRevision());
        assertNull(documentData.getContent());

        ExportParams exportParams = ExportParams.builder()
                .locationPath("location path")
                .build();

        try (
                MockedConstruction<WikiRenderer> wikiRendererMockedConstruction = mockConstruction(WikiRenderer.class, (mock, context) -> {
                    when(mock.render(any(), any(), any())).thenReturn("test content");
                })
        ) {
            DocumentData<IWikiPage> documentDataWithContent = uniqueObjectConverter
                    .withContent(true)
                    .withExportParams(exportParams)
                    .toDocumentData();
            assertEquals("test content", documentDataWithContent.getContent());
        }
    }

    @Test
    void testWikiPageInDefaultRepoDocumentData() {
        when(wikiPageInDefaultRepo.getId()).thenReturn("wiki page id");
        when(wikiPageInDefaultRepo.getLastRevision()).thenReturn("12345");
        when(wikiPageInDefaultRepo.getProject()).thenReturn(null);
        when(wikiPageInDefaultRepo.getTitleOrName()).thenReturn("wiki page title");

        UniqueObjectConverter uniqueObjectConverter = new UniqueObjectConverter(wikiPageInDefaultRepo);
        DocumentData<IWikiPage> documentData = uniqueObjectConverter.toDocumentData();

        assertNull(documentData.getId().getDocumentProject());
        assertEquals("wiki page id", documentData.getId().getDocumentId());
        assertEquals(DocumentType.WIKI_PAGE, documentData.getType());
        assertEquals("wiki page title", documentData.getTitle());
        assertNull(documentData.getContent());
        assertNotNull(documentData.getBaseline());
        assertEquals("pb. projectBaselineName | db. documentBaselineName", documentData.getBaseline().asPlaceholder());
        assertNotNull(uniqueObjectConverter.getUniqueObjectAdapter().getDocumentBaseline());
        assertEquals("pb. projectBaselineName | db. documentBaselineName", uniqueObjectConverter.getUniqueObjectAdapter().getDocumentBaseline().asPlaceholder());
        assertNull(documentData.getRevision());
        assertEquals("12345", documentData.getLastRevision());
        assertNull(documentData.getContent());

        ExportParams exportParams = ExportParams.builder()
                .locationPath("location path")
                .build();

        try (
                MockedConstruction<WikiRenderer> wikiRendererMockedConstruction = mockConstruction(WikiRenderer.class, (mock, context) -> {
                    when(mock.render(any(), any(), any())).thenReturn("test content");
                })
        ) {
            DocumentData<IWikiPage> documentDataWithContent = uniqueObjectConverter
                    .withContent(true)
                    .withExportParams(exportParams)
                    .toDocumentData();
            assertEquals("test content", documentDataWithContent.getContent());
        }
    }

    @Test
    void testBaselineCollectionDocumentData() {
        assertThrows(IllegalArgumentException.class, () -> new UniqueObjectConverter(baselineCollection));
    }
}

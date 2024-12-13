package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.util.exporter.ModifiedDocumentRenderer;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.server.api.model.document.ProxyDocument;
import com.polarion.alm.shared.api.services.internal.InternalServices;
import com.polarion.alm.shared.api.transaction.RunnableInReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IBaseline;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ipi.IInternalBaselinesManager;
import com.polarion.alm.tracker.spi.model.IInternalModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentDataTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IInternalBaselinesManager internalBaselinesManager;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    InternalReadOnlyTransaction internalReadOnlyTransactionMock;

    MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic;

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
    }

    @AfterEach
    void tearDown() {
        transactionalExecutorMockedStatic.close();
    }

    @Test
    void testLiveDocDocumentData() {
        IInternalModule module = mock(IInternalModule.class);
        when(module.getId()).thenReturn("module id");
        when(module.getLastRevision()).thenReturn("12345");
        ITrackerProject project = mock(ITrackerProject.class);
        when(project.getId()).thenReturn("project id");
        when(project.getName()).thenReturn("project name");
        when(module.getProject()).thenReturn(project);
        when(module.getModuleFolder()).thenReturn("_default");
        when(module.getTitleOrName()).thenReturn("module title");

        IBaseline documentBaselineName = mock(IBaseline.class);
        when(documentBaselineName.getName()).thenReturn("documentBaselineName");
        when(internalBaselinesManager.getRevisionBaseline(module, "12345")).thenReturn(documentBaselineName);
        IBaseline projectBaselineName = mock(IBaseline.class);
        when(projectBaselineName.getName()).thenReturn("projectBaselineName");
        when(internalBaselinesManager.getRevisionBaseline("12345")).thenReturn(projectBaselineName);

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
        assertNotNull(uniqueObjectConverter.getModuleAdapter().getDocumentBaseline());
        assertEquals("pb. projectBaselineName | db. documentBaselineName", uniqueObjectConverter.getModuleAdapter().getDocumentBaseline().asPlaceholder());
        assertNull(documentData.getRevision());
        assertEquals("12345", documentData.getLastRevision());
        assertNull(documentData.getContent());

        ExportParams exportParams = ExportParams.builder().build();
        try (
                MockedConstruction<ProxyDocument> proxyDocumentMockedConstruction = mockConstruction(ProxyDocument.class, (mock, context) -> {
                    when(mock.getHomePageContentHtml()).thenReturn("");
                });
                MockedConstruction<ModifiedDocumentRenderer> modifiedDocumentRendererMockedConstruction = mockConstruction(ModifiedDocumentRenderer.class, (mock, context) -> {
                    when(mock.render(anyString())).thenReturn("test content");
                });
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

    }

    @Test
    void testTestRunDocumentData() {

    }

    @Test
    void testWikiPageDocumentData() {

    }

}

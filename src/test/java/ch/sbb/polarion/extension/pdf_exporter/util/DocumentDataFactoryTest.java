package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.ModelObjectProvider;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.generic.test_extensions.CustomExtensionMock;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.shared.api.model.document.Document;
import com.polarion.alm.shared.api.model.document.DocumentSelector;
import com.polarion.alm.shared.api.model.document.internal.InternalDocuments;
import com.polarion.alm.shared.api.services.internal.InternalServices;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ipi.IInternalBaselinesManager;
import com.polarion.alm.tracker.spi.model.IInternalModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, TransactionalExecutorExtension.class, PlatformContextMockExtension.class})
class DocumentDataFactoryTest {

    @CustomExtensionMock
    private InternalReadOnlyTransaction internalReadOnlyTransactionMock;

    @CustomExtensionMock
    private IProjectService projectServiceMock;

    @Mock
    PdfExporterPolarionService pdfExporterPolarionServiceMock;

    @BeforeEach
    void setUp() {
        IProject projectMock = mock(IProject.class);
        when(projectMock.getId()).thenReturn("testProjectId");
        when(pdfExporterPolarionServiceMock.getProject("testProjectId")).thenReturn(projectMock);

        InternalDocuments internalDocumentsMock = mock(InternalDocuments.class);
        DocumentSelector documentSelectorMock = mock(DocumentSelector.class);
        when(documentSelectorMock.revision(ArgumentMatchers.any())).thenReturn(documentSelectorMock);
        Document documentMock = mock(Document.class);
        when(documentSelectorMock.spaceReferenceAndName(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(documentMock);
        when(internalDocumentsMock.getBy()).thenReturn(documentSelectorMock);
        when(internalReadOnlyTransactionMock.documents()).thenReturn(internalDocumentsMock);
    }

    @Test
    void testGetDocumentData() {
        IInternalModule moduleMock = mock(IInternalModule.class);
        when(moduleMock.getId()).thenReturn("testModuleId");
        when(moduleMock.getModuleFolder()).thenReturn("testModuleFolder");
        when(moduleMock.getTitleOrName()).thenReturn("testModuleTitle");
        ITrackerProject projectMock = mock(ITrackerProject.class);
        when(projectMock.getId()).thenReturn("testProjectId");
        when(moduleMock.getProject()).thenReturn(projectMock);
        when(moduleMock.getLastRevision()).thenReturn("12345");

        IInternalBaselinesManager internalBaselinesManager = mock(IInternalBaselinesManager.class);
        when(projectMock.getBaselinesManager()).thenReturn(internalBaselinesManager);
        when(internalBaselinesManager.getRevisionBaseline("12345")).thenReturn(null);

        InternalServices internalServicesMock = mock(InternalServices.class);
        ITrackerService trackerService = mock(ITrackerService.class);
        when(trackerService.getTrackerProject(any(IProject.class))).thenReturn(projectMock);
        lenient().when(internalServicesMock.trackerService()).thenReturn(trackerService);
        lenient().when(internalReadOnlyTransactionMock.services()).thenReturn(internalServicesMock);

        Document documentMock = mock(Document.class);
        when(documentMock.getOldApi()).thenReturn(moduleMock);

        when(projectServiceMock.getProject("testProjectId")).thenReturn(projectMock);

        ExportParams exportParamsMock = ExportParams.builder()
                .projectId("testProjectId")
                .locationPath("testModuleFolder/testLocationPath")
                .documentType(DocumentType.LIVE_DOC)
                .build();
        when(new ModelObjectProvider(exportParamsMock, pdfExporterPolarionServiceMock).getModelObject(internalReadOnlyTransactionMock))
                .thenReturn(documentMock);

        DocumentData<IUniqueObject> actualDocumentData = DocumentDataFactory.getDocumentData(exportParamsMock, false);

        assertNotNull(actualDocumentData);
        assertEquals("testProjectId", actualDocumentData.getId().getDocumentProject().getId());
        assertEquals("testModuleFolder", ((LiveDocId) actualDocumentData.getId()).getSpaceId());
        assertEquals("testModuleId", actualDocumentData.getId().getDocumentId());
        assertEquals(DocumentType.LIVE_DOC, actualDocumentData.getType());
        assertEquals("testModuleTitle", actualDocumentData.getTitle());
        assertNull(actualDocumentData.getRevision());
        assertEquals("12345", actualDocumentData.getLastRevision());
        assertEquals("12345", actualDocumentData.getRevisionPlaceholder());
        assertEquals("", actualDocumentData.getBaseline().asPlaceholder());
        assertNull(actualDocumentData.getContent());
    }
}

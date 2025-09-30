package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.collections.DocumentCollectionEntry;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollection;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollectionElement;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith({PlatformContextMockExtension.class, TransactionalExecutorExtension.class})
class DocumentCollectionHelperTest {

    @Test
    @SuppressWarnings("unused")
    void testGetDocumentsFromCollection() {
        DocumentFileNameHelper documentFileNameHelper = mock(DocumentFileNameHelper.class);

        DocumentCollectionHelper helper = new DocumentCollectionHelper(documentFileNameHelper);
        String projectId = "testProjectId";
        String secondProjectId = "testProject2Id";
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

        when(mockModule3.getProjectId()).thenReturn(secondProjectId);
        when(mockModule3.getModuleFolder()).thenReturn("upstream_space");
        when(mockModule3.getModuleName()).thenReturn("upstream Module");
        ILocation mockLocation3 = mock(ILocation.class);
        when(mockModule3.getModuleLocation()).thenReturn(mockLocation3);
        when(mockLocation3.getLocationPath()).thenReturn("space 3");
        when(mockModule3.getRevision()).thenReturn("3");

        when(documentFileNameHelper.getDocumentFileName(any())).thenThrow(new IllegalStateException("Unexpected call to documentFileNameHelper"));
        doReturn("testFileName1").when(documentFileNameHelper).getDocumentFileName(
                ExportParams.builder()
                        .projectId(projectId)
                        .documentType(DocumentType.LIVE_DOC)
                        .locationPath("space 1")
                        .revision("1")
                        .build());
        doReturn("testFileName2").when(documentFileNameHelper).getDocumentFileName(
                ExportParams.builder()
                        .projectId(projectId)
                        .documentType(DocumentType.LIVE_DOC)
                        .locationPath("space 2")
                        .revision("2")
                        .build());
        doReturn("testFileName3").when(documentFileNameHelper).getDocumentFileName(
                ExportParams.builder()
                        .projectId(secondProjectId)
                        .documentType(DocumentType.LIVE_DOC)
                        .locationPath("space 3")
                        .revision("3")
                        .build());

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
            List<DocumentCollectionEntry> result = helper.getDocumentsFromCollection(projectId, collectionId, null, mock(ReadOnlyTransaction.class));

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

            List<DocumentCollectionEntry> resultWithRevision = helper.getDocumentsFromCollection(projectId, collectionId, "1234", mock(ReadOnlyTransaction.class));

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

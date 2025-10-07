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
import com.polarion.alm.tracker.model.IRichPage;
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
        doReturn("reportFileName1").when(documentFileNameHelper).getDocumentFileName(
                ExportParams.builder()
                        .projectId(projectId)
                        .documentType(DocumentType.LIVE_REPORT)
                        .locationPath("Reports/TestReport")
                        .revision("10")
                        .build());
        doReturn("reportFileName2").when(documentFileNameHelper).getDocumentFileName(
                ExportParams.builder()
                        .projectId(secondProjectId)
                        .documentType(DocumentType.LIVE_REPORT)
                        .locationPath("_default/LiveReport")
                        .revision("5")
                        .build());

        IBaselineCollectionElement mockElement1 = mock(IBaselineCollectionElement.class);
        IBaselineCollectionElement mockElement2 = mock(IBaselineCollectionElement.class);
        IBaselineCollectionElement mockUpstreamElement = mock(IBaselineCollectionElement.class);

        when(mockElement1.getObjectWithRevision()).thenReturn(mockModule1);
        when(mockElement2.getObjectWithRevision()).thenReturn(mockModule2);
        when(mockUpstreamElement.getObjectWithRevision()).thenReturn(mockModule3);

        IRichPage mockPage1 = mock(IRichPage.class, RETURNS_DEEP_STUBS);
        IRichPage mockPage2 = mock(IRichPage.class, RETURNS_DEEP_STUBS);

        when(mockPage1.getProjectId()).thenReturn(projectId);
        when(mockPage1.getPageNameWithSpace()).thenReturn("Reports/TestReport");
        when(mockPage1.getPageName()).thenReturn("TestReport");
        when(mockPage1.getRevision()).thenReturn("10");
        when(mockPage1.getFolder().getName()).thenReturn("Reports");

        when(mockPage2.getProjectId()).thenReturn(secondProjectId);
        when(mockPage2.getPageNameWithSpace()).thenReturn("_default/LiveReport");
        when(mockPage2.getPageName()).thenReturn("LiveReport");
        when(mockPage2.getRevision()).thenReturn("5");
        when(mockPage2.getFolder().getName()).thenReturn("_default");

        IBaselineCollection mockUpstreamCollection = mock(IBaselineCollection.class);
        when(mockUpstreamCollection.getElements()).thenReturn(List.of(mockUpstreamElement));
        when(mockCollection.getElements()).thenReturn(List.of(mockElement1, mockElement2));
        when(mockCollection.getUpstreamCollections()).thenReturn(List.of(mockUpstreamCollection));
        when(mockCollection.getRichPages()).thenReturn(List.of(mockPage1, mockPage2));

        BaselineCollection baselineCollection = mock(BaselineCollection.class);
        when(mockBaselineCollectionReference.get(Mockito.any())).thenReturn(baselineCollection);
        when(baselineCollection.getOldApi()).thenReturn(mockCollection);

        try (MockedConstruction<BaselineCollectionReference> mockedStaticReference = mockConstruction(BaselineCollectionReference.class, (mock, context) -> {
            when(mock.get(Mockito.any())).thenReturn(baselineCollection);
            when(mock.getWithRevision(Mockito.anyString())).thenReturn(mock);
        })) {
            List<DocumentCollectionEntry> result = helper.getDocumentsFromCollection(projectId, collectionId, null, mock(ReadOnlyTransaction.class));

            assertNotNull(result);
            assertEquals(5, result.size());

            // Modules
            assertEquals("space 1", result.get(0).getSpaceId());
            assertEquals("test Module1", result.get(0).getDocumentName());
            assertEquals(DocumentType.LIVE_DOC, result.get(0).getDocumentType());
            assertEquals("1", result.get(0).getRevision());
            assertEquals("testFileName1", result.get(0).getFileName());

            assertEquals("_default", result.get(1).getSpaceId());
            assertEquals("test Module2", result.get(1).getDocumentName());
            assertEquals(DocumentType.LIVE_DOC, result.get(1).getDocumentType());
            assertEquals("2", result.get(1).getRevision());
            assertEquals("testFileName2", result.get(1).getFileName());

            assertEquals("upstream_space", result.get(2).getSpaceId());
            assertEquals("upstream Module", result.get(2).getDocumentName());
            assertEquals(DocumentType.LIVE_DOC, result.get(2).getDocumentType());
            assertEquals("3", result.get(2).getRevision());
            assertEquals("testFileName3", result.get(2).getFileName());

            // Rich Pages
            assertEquals("Reports", result.get(3).getSpaceId());
            assertEquals("TestReport", result.get(3).getDocumentName());
            assertEquals(DocumentType.LIVE_REPORT, result.get(3).getDocumentType());
            assertEquals("10", result.get(3).getRevision());
            assertEquals("reportFileName1", result.get(3).getFileName());

            assertEquals("_default", result.get(4).getSpaceId());
            assertEquals("LiveReport", result.get(4).getDocumentName());
            assertEquals(DocumentType.LIVE_REPORT, result.get(4).getDocumentType());
            assertEquals("5", result.get(4).getRevision());
            assertEquals("reportFileName2", result.get(4).getFileName());

            List<DocumentCollectionEntry> resultWithRevision = helper.getDocumentsFromCollection(projectId, collectionId, "1234", mock(ReadOnlyTransaction.class));

            assertNotNull(resultWithRevision);
            assertEquals(5, resultWithRevision.size());

            // Modules with revision
            assertEquals("space 1", resultWithRevision.get(0).getSpaceId());
            assertEquals("test Module1", resultWithRevision.get(0).getDocumentName());
            assertEquals(DocumentType.LIVE_DOC, resultWithRevision.get(0).getDocumentType());
            assertEquals("1", resultWithRevision.get(0).getRevision());

            assertEquals("_default", resultWithRevision.get(1).getSpaceId());
            assertEquals("test Module2", resultWithRevision.get(1).getDocumentName());
            assertEquals(DocumentType.LIVE_DOC, resultWithRevision.get(1).getDocumentType());
            assertEquals("2", resultWithRevision.get(1).getRevision());

            assertEquals("upstream_space", resultWithRevision.get(2).getSpaceId());
            assertEquals("upstream Module", resultWithRevision.get(2).getDocumentName());
            assertEquals(DocumentType.LIVE_DOC, resultWithRevision.get(2).getDocumentType());
            assertEquals("3", resultWithRevision.get(2).getRevision());

            // Rich Pages with revision
            assertEquals("Reports", resultWithRevision.get(3).getSpaceId());
            assertEquals("TestReport", resultWithRevision.get(3).getDocumentName());
            assertEquals(DocumentType.LIVE_REPORT, resultWithRevision.get(3).getDocumentType());
            assertEquals("10", resultWithRevision.get(3).getRevision());

            assertEquals("_default", resultWithRevision.get(4).getSpaceId());
            assertEquals("LiveReport", resultWithRevision.get(4).getDocumentName());
            assertEquals(DocumentType.LIVE_REPORT, resultWithRevision.get(4).getDocumentType());
            assertEquals("5", resultWithRevision.get(4).getRevision());
        }
    }
}

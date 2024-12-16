package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents;

import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.test_extensions.CustomExtensionMock;
import ch.sbb.polarion.extension.pdf_exporter.test_extensions.TransactionalExecutorExtension;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.document.Document;
import com.polarion.alm.shared.api.model.document.DocumentSelector;
import com.polarion.alm.shared.api.model.document.internal.InternalDocuments;
import com.polarion.alm.shared.api.model.rp.RichPage;
import com.polarion.alm.shared.api.model.rp.RichPageSelector;
import com.polarion.alm.shared.api.model.rp.internal.InternalRichPages;
import com.polarion.alm.shared.api.model.tr.TestRun;
import com.polarion.alm.shared.api.model.tr.TestRunSelector;
import com.polarion.alm.shared.api.model.tr.internal.InternalTestRuns;
import com.polarion.alm.shared.api.model.wiki.WikiPage;
import com.polarion.alm.shared.api.model.wiki.WikiPageSelector;
import com.polarion.alm.shared.api.model.wiki.WikiPages;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, TransactionalExecutorExtension.class})
class ModelObjectProviderTest {

    @CustomExtensionMock
    private InternalReadOnlyTransaction internalReadOnlyTransactionMock;

    @Mock
    private PdfExporterPolarionService pdfExporterPolarionService;

    @BeforeEach
    void setUp() {
        IProject projectMock = mock(IProject.class);
        lenient().when(projectMock.getId()).thenReturn("testProjectId");
        lenient().when(pdfExporterPolarionService.getProject(eq("testProjectId"))).thenReturn(projectMock);
        lenient().when(pdfExporterPolarionService.getProject(eq("nonExistingProjectId"))).thenThrow(new ObjectNotFoundException("Project not found"));
    }

    public static Stream<Arguments> paramsForModelObjectProviderGetDocument() {
        return Stream.of(
                Arguments.of(ExportParams.builder()
                        .projectId("testProjectId")
                        .locationPath("_default/testLocationPath")
                        .documentType(DocumentType.LIVE_DOC)
                        .build()),
                Arguments.of(ExportParams.builder()
                        .projectId("testProjectId")
                        .locationPath("_default/testLocationPath")
                        .revision("12345")
                        .documentType(DocumentType.LIVE_DOC)
                        .build())
        );
    }

    @ParameterizedTest
    @MethodSource("paramsForModelObjectProviderGetDocument")
    void testModelObjectProviderGetDocument(@NotNull ExportParams exportParams, ExtensionContext extensionContext) {
        InternalDocuments internalDocumentsMock = mock(InternalDocuments.class);
        DocumentSelector documentSelectorMock = mock(DocumentSelector.class);
        when(documentSelectorMock.revision(any())).thenReturn(documentSelectorMock);
        Document documentMock = mock(Document.class);
        when(documentSelectorMock.spaceReferenceAndName(any(), any())).thenReturn(documentMock);
        when(internalDocumentsMock.getBy()).thenReturn(documentSelectorMock);
        when(internalReadOnlyTransactionMock.documents()).thenReturn(internalDocumentsMock);

        ModelObjectProvider modelObjectProvider = new ModelObjectProvider(exportParams, pdfExporterPolarionService);
        ModelObject modelObject = TransactionalExecutor.executeSafelyInReadOnlyTransaction(modelObjectProvider::getModelObject);

        assertEquals(documentMock, modelObject);
    }

    public static Stream<Arguments> paramsForModelObjectProviderGetRichPage() {
        return Stream.of(
                Arguments.of(ExportParams.builder()
                        .projectId("testProjectId")
                        .locationPath("testLocationPath")
                        .documentType(DocumentType.LIVE_REPORT)
                        .build()),
                Arguments.of(ExportParams.builder()
                        .locationPath("testLocationPath")
                        .documentType(DocumentType.LIVE_REPORT)
                        .build()),
                Arguments.of(ExportParams.builder()
                        .locationPath("testLocationPath")
                        .revision("12345")
                        .documentType(DocumentType.LIVE_REPORT)
                        .build())
        );
    }

    @ParameterizedTest
    @MethodSource("paramsForModelObjectProviderGetRichPage")
    void testModelObjectProviderGetRichPage(@NotNull ExportParams exportParams, ExtensionContext extensionContext) {
        InternalRichPages richPagesMock = mock(InternalRichPages.class);
        RichPageSelector richPagesSelectorMock = mock(RichPageSelector.class);
        when(richPagesSelectorMock.revision(any())).thenReturn(richPagesSelectorMock);
        RichPage richPageMock = mock(RichPage.class);
        when(richPagesSelectorMock.spaceReferenceAndName(any(), any())).thenReturn(richPageMock);
        when(richPagesMock.getBy()).thenReturn(richPagesSelectorMock);
        when(internalReadOnlyTransactionMock.richPages()).thenReturn(richPagesMock);

        ModelObjectProvider modelObjectProvider = new ModelObjectProvider(exportParams, pdfExporterPolarionService);
        ModelObject modelObject = TransactionalExecutor.executeSafelyInReadOnlyTransaction(modelObjectProvider::getModelObject);

        assertEquals(richPageMock, modelObject);
    }

    public static Stream<Arguments> paramsForModelObjectProviderGetTestRun() {
        return Stream.of(
                Arguments.of(ExportParams.builder()
                        .projectId("testProjectId")
                        .urlQueryParameters(Map.of("id", "testRunId"))
                        .documentType(DocumentType.TEST_RUN)
                        .build()),
                Arguments.of(ExportParams.builder()
                        .projectId("testProjectId")
                        .urlQueryParameters(Map.of("id", "testRunId"))
                        .revision("12345")
                        .documentType(DocumentType.TEST_RUN)
                        .build())
        );
    }

    @ParameterizedTest
    @MethodSource("paramsForModelObjectProviderGetTestRun")
    void testModelObjectProviderGetTestRun(@NotNull ExportParams exportParams, ExtensionContext extensionContext) {
        InternalTestRuns testRunsMock = mock(InternalTestRuns.class);
        TestRunSelector testRunsSelectorMock = mock(TestRunSelector.class);
        when(testRunsSelectorMock.revision(any())).thenReturn(testRunsSelectorMock);
        TestRun testRunMock = mock(TestRun.class);
        when(testRunsSelectorMock.ids(any(), any())).thenReturn(testRunMock);
        when(testRunsMock.getBy()).thenReturn(testRunsSelectorMock);
        when(internalReadOnlyTransactionMock.testRuns()).thenReturn(testRunsMock);

        ModelObjectProvider modelObjectProvider = new ModelObjectProvider(exportParams, pdfExporterPolarionService);
        ModelObject modelObject = TransactionalExecutor.executeSafelyInReadOnlyTransaction(modelObjectProvider::getModelObject);

        assertEquals(testRunMock, modelObject);
    }

    public static Stream<Arguments> paramsForModelObjectProviderGetWikiPage() {
        return Stream.of(
                Arguments.of(ExportParams.builder()
                        .projectId("testProjectId")
                        .locationPath("_default/testLocationPath")
                        .documentType(DocumentType.WIKI_PAGE)
                        .build()),
                Arguments.of(ExportParams.builder()
                        .locationPath("_default/testLocationPath")
                        .documentType(DocumentType.WIKI_PAGE)
                        .build()),
                Arguments.of(ExportParams.builder()
                        .projectId("testProjectId")
                        .locationPath("_default/testLocationPath")
                        .revision("12345")
                        .documentType(DocumentType.WIKI_PAGE)
                        .build())
        );
    }

    @ParameterizedTest
    @MethodSource("paramsForModelObjectProviderGetWikiPage")
    void testModelObjectProviderGetWikiPage(@NotNull ExportParams exportParams, ExtensionContext extensionContext) {
        WikiPages wikiPagesMock = mock(WikiPages.class);
        WikiPageSelector wikiPagesSelectorMock = mock(WikiPageSelector.class);
        when(wikiPagesSelectorMock.revision(any())).thenReturn(wikiPagesSelectorMock);
        WikiPage wikiPageMock = mock(WikiPage.class);
        when(wikiPagesSelectorMock.spaceReferenceAndName(any(), any())).thenReturn(wikiPageMock);
        when(wikiPagesMock.getBy()).thenReturn(wikiPagesSelectorMock);
        when(internalReadOnlyTransactionMock.wikiPages()).thenReturn(wikiPagesMock);

        ModelObjectProvider modelObjectProvider = new ModelObjectProvider(exportParams, pdfExporterPolarionService);
        ModelObject modelObject = TransactionalExecutor.executeSafelyInReadOnlyTransaction(modelObjectProvider::getModelObject);

        assertEquals(wikiPageMock, modelObject);
    }

    public static Stream<Arguments> paramsForModelObjectProviderShouldFail() {
        return Stream.of(
                Arguments.of(ExportParams.builder()
                        .projectId("nonExistingProjectId")
                        .locationPath("_default/testLocationPath")
                        .documentType(DocumentType.LIVE_DOC)
                        .build(), ObjectNotFoundException.class),
                Arguments.of(ExportParams.builder()
                        .projectId("testProjectId")
                        .documentType(DocumentType.LIVE_DOC)
                        .build(), IllegalArgumentException.class),
//                Arguments.of(ExportParams.builder()
//                        .projectId("testProjectId")
//                        .locationPath("wrongLocationPath")
//                        .documentType(DocumentType.LIVE_DOC)
//                        .build(), IllegalArgumentException.class),
//                Arguments.of(ExportParams.builder()
//                        .projectId("testProjectId")
//                        .locationPath("_default/nonExistingLocationPath")
//                        .documentType(DocumentType.LIVE_DOC)
//                        .build(), IllegalArgumentException.class),

                Arguments.of(ExportParams.builder()
                        .projectId("nonExistingProjectId")
                        .locationPath("_default/testLocationPath")
                        .documentType(DocumentType.LIVE_REPORT)
                        .build(), ObjectNotFoundException.class),
                Arguments.of(ExportParams.builder()
                        .projectId("testProjectId")
                        .documentType(DocumentType.LIVE_REPORT)
                        .build(), IllegalArgumentException.class),

                Arguments.of(ExportParams.builder()
                        .projectId("nonExistingProjectId")
                        .urlQueryParameters(Map.of("id", "testRunId"))
                        .documentType(DocumentType.TEST_RUN)
                        .build(), ObjectNotFoundException.class),
                Arguments.of(ExportParams.builder()
                        .projectId("testProjectId")
                        .documentType(DocumentType.TEST_RUN)
                        .build(), IllegalArgumentException.class),
                Arguments.of(ExportParams.builder()
                        .projectId("testProjectId")
                        .urlQueryParameters(Map.of())
                        .documentType(DocumentType.TEST_RUN)
                        .build(), IllegalArgumentException.class),

                Arguments.of(ExportParams.builder()
                        .projectId("nonExistingProjectId")
                        .locationPath("_default/testLocationPath")
                        .documentType(DocumentType.WIKI_PAGE)
                        .build(), ObjectNotFoundException.class),
                Arguments.of(ExportParams.builder()
                        .projectId("testProjectId")
                        .documentType(DocumentType.WIKI_PAGE)
                        .build(), IllegalArgumentException.class)
        );

    }

    @ParameterizedTest
    @MethodSource("paramsForModelObjectProviderShouldFail")
    void testModelObjectProviderShouldFail(@NotNull ExportParams exportParams, @NotNull Class<? extends Exception> expectedExceptionClass) {
        ModelObjectProvider modelObjectProvider = new ModelObjectProvider(exportParams, pdfExporterPolarionService);
        assertThrows(expectedExceptionClass, () -> TransactionalExecutor.executeSafelyInReadOnlyTransaction(modelObjectProvider::getModelObject));
    }

    @Test
    void testModelObjectProviderGetBaselineCollection() {
        ExportParams exportParams = ExportParams.builder()
                .documentType(DocumentType.BASELINE_COLLECTION)
                .build();
        ModelObjectProvider modelObjectProvider = new ModelObjectProvider(exportParams, pdfExporterPolarionService);

        assertThrows(IllegalArgumentException.class, () -> TransactionalExecutor.executeSafelyInReadOnlyTransaction(modelObjectProvider::getModelObject));
    }
}

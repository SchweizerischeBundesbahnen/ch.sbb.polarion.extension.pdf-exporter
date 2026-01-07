package ch.sbb.polarion.extension.pdf_exporter.util.velocity;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentProject;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.server.html.HtmlFragmentParser;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.HtmlRichPageParameters;
import com.polarion.alm.shared.api.utils.collections.ImmutableStrictMap;
import com.polarion.alm.shared.api.utils.collections.ReadOnlyStrictMap;
import com.polarion.alm.shared.html.HtmlElement;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWikiPage;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class VelocityEvaluatorTest {

    private VelocityEvaluator velocityEvaluator;

    @Mock
    private DocumentData<IUniqueObject> documentData;

    @Mock
    private IUniqueObject documentObject;

    @Mock
    private DocumentId documentId;

    private HtmlElement htmlElement;

    @Mock
    private ReadOnlyStrictMap readOnlyStrictMap;

    @Mock
    private ImmutableStrictMap immutableStrictMap;

    private MockedConstruction<HtmlFragmentParser> mockedHtmlFragmentParserConstruction;
    private MockedConstruction<HtmlRichPageParameters> mockedHtmlRichPageParametersConstruction;
    private MockedStatic<HtmlRichPageParameters> htmlRichPageParametersMockedStatic;

    @BeforeEach
    void setUp() {
        velocityEvaluator = new VelocityEvaluator();
        lenient().when(documentData.getDocumentObject()).thenReturn(documentObject);
        lenient().when(documentData.getId()).thenReturn(documentId);
        lenient().when(readOnlyStrictMap.toImmutable()).thenReturn(immutableStrictMap);

        mockedHtmlFragmentParserConstruction = mockConstruction(HtmlFragmentParser.class, (mock, context) -> {
        });
        mockedHtmlRichPageParametersConstruction = mockConstruction(HtmlRichPageParameters.class, (mock, context) -> when(mock.get(isNull())).thenReturn(readOnlyStrictMap));

        htmlRichPageParametersMockedStatic = mockStatic(HtmlRichPageParameters.class);
        htmlRichPageParametersMockedStatic.when(() -> HtmlRichPageParameters.findElement(any())).thenAnswer((Answer<HtmlElement>) invocationOnMock -> htmlElement);
    }

    @AfterEach
    void tearDown() {
        mockedHtmlFragmentParserConstruction.close();
        mockedHtmlRichPageParametersConstruction.close();
        htmlRichPageParametersMockedStatic.close();
    }

    @Test
    void getPageParametersEmptyWhenNoContent() {
        when(documentData.getContent()).thenReturn("");
        ImmutableStrictMap<String, RichPageParameter> result = velocityEvaluator.getPageParameters(documentData);
        assertTrue(result.isEmpty());

        when(documentData.getContent()).thenReturn(null);
        result = velocityEvaluator.getPageParameters(documentData);
        assertTrue(result.isEmpty());
    }

    @Test
    void getPageParametersEmptyWhenNullHtmlElement() {
        when(documentData.getContent()).thenReturn("<div>Some content without parameters</div>");
        ImmutableStrictMap<String, RichPageParameter> result = velocityEvaluator.getPageParameters(documentData);
        assertTrue(result.isEmpty());
    }

    @Test
    void getPageParametersReturnsProperMap() {
        when(documentData.getContent()).thenReturn("<div>Some content</div>");
        htmlElement = mock(HtmlElement.class);
        ImmutableStrictMap<String, RichPageParameter> result = velocityEvaluator.getPageParameters(documentData);
        assertEquals(immutableStrictMap, result);
    }

    @Test
    void updateVelocityContextForLiveDoc() throws Exception {
        IModule module = mock(IModule.class);
        DocumentData<IModule> liveDocData = createDocumentData(module, DocumentType.LIVE_DOC, "Test Project");

        VelocityContext velocityContext = new VelocityContext();
        invokeUpdateVelocityContext(velocityContext, liveDocData);

        assertEquals(module, velocityContext.get("document"));
        assertEquals("Test Project", velocityContext.get("projectName"));
        assertNull(velocityContext.get("page"));
        assertNull(velocityContext.get("testrun"));
        assertNull(velocityContext.get("collection"));
    }

    @Test
    void updateVelocityContextForLiveReport() throws Exception {
        IRichPage richPage = mock(IRichPage.class);
        DocumentData<IRichPage> liveReportData = createDocumentData(richPage, DocumentType.LIVE_REPORT, "Test Project");

        VelocityContext velocityContext = new VelocityContext();
        invokeUpdateVelocityContext(velocityContext, liveReportData);

        assertEquals(richPage, velocityContext.get("page"));
        assertEquals("Test Project", velocityContext.get("projectName"));
        assertNull(velocityContext.get("document"));
        assertNull(velocityContext.get("testrun"));
        assertNull(velocityContext.get("collection"));
    }

    @Test
    void updateVelocityContextForTestRun() throws Exception {
        ITestRun testRun = mock(ITestRun.class);
        DocumentData<ITestRun> testRunData = createDocumentData(testRun, DocumentType.TEST_RUN, "Test Project");

        VelocityContext velocityContext = new VelocityContext();
        invokeUpdateVelocityContext(velocityContext, testRunData);

        assertEquals(testRun, velocityContext.get("testrun"));
        assertEquals("Test Project", velocityContext.get("projectName"));
        assertNull(velocityContext.get("document"));
        assertNull(velocityContext.get("page"));
        assertNull(velocityContext.get("collection"));
    }

    @Test
    void updateVelocityContextForWikiPage() throws Exception {
        IWikiPage wikiPage = mock(IWikiPage.class);
        DocumentData<IWikiPage> wikiPageData = createDocumentData(wikiPage, DocumentType.WIKI_PAGE, "Test Project");

        VelocityContext velocityContext = new VelocityContext();
        invokeUpdateVelocityContext(velocityContext, wikiPageData);

        assertEquals(wikiPage, velocityContext.get("page"));
        assertEquals("Test Project", velocityContext.get("projectName"));
        assertNull(velocityContext.get("document"));
        assertNull(velocityContext.get("testrun"));
        assertNull(velocityContext.get("collection"));
    }

    @Test
    void updateVelocityContextForBaselineCollection() throws Exception {
        IBaselineCollection collection = mock(IBaselineCollection.class);
        DocumentData<IBaselineCollection> collectionData = createDocumentData(collection, DocumentType.BASELINE_COLLECTION, "Test Project");

        VelocityContext velocityContext = new VelocityContext();
        invokeUpdateVelocityContext(velocityContext, collectionData);

        assertEquals(collection, velocityContext.get("collection"));
        assertEquals("Test Project", velocityContext.get("projectName"));
        assertNull(velocityContext.get("document"));
        assertNull(velocityContext.get("page"));
        assertNull(velocityContext.get("testrun"));
    }

    @Test
    void updateVelocityContextWithNullProject() throws Exception {
        IModule module = mock(IModule.class);
        DocumentData<IModule> liveDocData = createDocumentData(module, DocumentType.LIVE_DOC, null);

        VelocityContext velocityContext = new VelocityContext();
        invokeUpdateVelocityContext(velocityContext, liveDocData);

        assertEquals(module, velocityContext.get("document"));
        assertEquals("", velocityContext.get("projectName"));
    }

    @Test
    void updateVelocityContextWithUnknownDocumentType() throws Exception {
        // Test with document object that doesn't match its document type (e.g., IModule with TEST_RUN type)
        IModule module = mock(IModule.class);
        DocumentData<IModule> mismatchedData = createDocumentData(module, DocumentType.TEST_RUN, "Test Project");

        VelocityContext velocityContext = new VelocityContext();
        invokeUpdateVelocityContext(velocityContext, mismatchedData);

        // Should fall through to default case, not adding any document-specific variable
        assertNull(velocityContext.get("document"));
        assertNull(velocityContext.get("page"));
        assertNull(velocityContext.get("testrun"));
        assertNull(velocityContext.get("collection"));
        assertEquals("Test Project", velocityContext.get("projectName"));
    }

    @SuppressWarnings("unchecked")
    private <T extends IUniqueObject> DocumentData<T> createDocumentData(T documentObject, DocumentType type, String projectName) {
        DocumentId documentId = mock(DocumentId.class);
        if (projectName != null) {
            DocumentProject documentProject = mock(DocumentProject.class);
            when(documentProject.getName()).thenReturn(projectName);
            when(documentId.getDocumentProject()).thenReturn(documentProject);
        } else {
            when(documentId.getDocumentProject()).thenReturn(null);
        }

        DocumentData<T> data = mock(DocumentData.class);
        when(data.getDocumentObject()).thenReturn(documentObject);
        when(data.getType()).thenReturn(type);
        when(data.getId()).thenReturn(documentId);

        return data;
    }

    private void invokeUpdateVelocityContext(VelocityContext velocityContext, DocumentData<? extends IUniqueObject> documentData) throws Exception {
        Method method = VelocityEvaluator.class.getDeclaredMethod("updateVelocityContext", VelocityContext.class, DocumentData.class);
        method.setAccessible(true);
        method.invoke(velocityEvaluator, velocityContext, documentData);
    }
}

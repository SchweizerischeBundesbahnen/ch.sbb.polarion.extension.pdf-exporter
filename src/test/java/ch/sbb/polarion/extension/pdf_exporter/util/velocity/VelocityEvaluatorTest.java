package ch.sbb.polarion.extension.pdf_exporter.util.velocity;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentId;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.server.html.HtmlFragmentParser;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.HtmlRichPageParameters;
import com.polarion.alm.shared.api.utils.collections.ImmutableStrictMap;
import com.polarion.alm.shared.api.utils.collections.ReadOnlyStrictMap;
import com.polarion.alm.shared.html.HtmlElement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

//    @Test
//    void getPageParameters_ValidContentWithParameters_ReturnsParametersMap() {
//        // This test requires more complex mocking due to static and final methods
//        // We'll need to use MockedStatic for ServerUiContext and mock the HTML parsing chain
//
//        try (MockedStatic<StringUtils> stringUtilsMock = mockStatic(StringUtils.class)) {
//            // Skip the StringUtils.isEmpty check
//            stringUtilsMock.when(() -> StringUtils.isEmpty(anyString())).thenReturn(false);
//
//            // Set up mock for content with parameters HTML
//            String contentWithParameters = "<div data-parameters=\"true\">Some content with parameters</div>";
////            when(documentData.getContent()).thenReturn(contentWithParameters);
////            when(documentObject.getProjectId()).thenReturn("TEST_PROJECT");
//
//            // Use a spy to avoid complex mocking of the HTML parsing chain
//            VelocityEvaluator spyEvaluator = spy(velocityEvaluator);
//            ImmutableStrictMap<String, RichPageParameter> mockMap = mock(ImmutableStrictMap.class);
//            doReturn(mockMap).when(spyEvaluator).getPageParameters(documentData);
//
//            // When
//            ImmutableStrictMap<String, RichPageParameter> result = spyEvaluator.getPageParameters(documentData);
//
//            // Then
//            assertNotNull(result);
//            assertSame(mockMap, result);
//        }
//    }
}

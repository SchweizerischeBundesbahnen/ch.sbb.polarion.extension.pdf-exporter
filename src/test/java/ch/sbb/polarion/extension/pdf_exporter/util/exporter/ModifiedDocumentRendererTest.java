package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.polarion.alm.shared.api.model.document.internal.InternalDocument;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.dle.document.DocumentRendererParameters;
import com.polarion.alm.shared.rt.RichTextRenderingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModifiedDocumentRendererTest {

    private MockedConstruction<RichTextRenderingContext> mockedHtmlFragmentParserConstruction;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InternalReadOnlyTransaction transaction;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InternalDocument internalDocument;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RichTextRenderTarget textRenderTarget;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DocumentRendererParameters parameters;

    private boolean documentLazyLoad;

    @BeforeEach
    void setUp() {
        documentLazyLoad = true;
        mockedHtmlFragmentParserConstruction = mockConstruction(RichTextRenderingContext.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS), (mock, context) -> {
            when(mock.documentLazyLoad()).thenAnswer((Answer<Boolean>) invocationOnMock -> documentLazyLoad);
        });
    }

    @AfterEach
    void tearDown() {
        mockedHtmlFragmentParserConstruction.close();
    }

    @Test
    void testConstructor() {
        documentLazyLoad = true;
        assertDoesNotThrow(() -> new ModifiedDocumentRenderer(transaction, internalDocument, textRenderTarget, parameters));
        documentLazyLoad = false;
        assertDoesNotThrow(() -> new ModifiedDocumentRenderer(transaction, internalDocument, textRenderTarget, parameters));
    }

}

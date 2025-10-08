package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.polarion.alm.server.rt.parts.ServerFieldRichTextRenderer;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.rt.RichTextRenderingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModifiedServerFieldRichTextRendererTest {

    @Test
    void testConstructor() {
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class, RETURNS_DEEP_STUBS);

        assertDoesNotThrow(() -> new ModifiedServerFieldRichTextRenderer(transaction),
                "Constructor should not throw exception with mocked transaction");
    }

    @Test
    void testRenderDescriptionMethodExists() {
        ModifiedServerFieldRichTextRenderer renderer = spy(new ModifiedServerFieldRichTextRenderer(mock(ReadOnlyTransaction.class, RETURNS_DEEP_STUBS)));
        renderer.setRichTextRenderingContext(mock(RichTextRenderingContext.class, RETURNS_DEEP_STUBS));
        doReturn(Boolean.TRUE).when((ServerFieldRichTextRenderer) renderer).renderDescription(any(HtmlContentBuilder.class), any(WorkItemReference.class), anyBoolean());
        assertDoesNotThrow(() -> renderer.renderDescription(mock(HtmlContentBuilder.class), mock(WorkItemReference.class), true));
    }

}

package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.polarion.alm.server.rt.parts.Renderer;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.dle.DleReferencesCollector;
import com.polarion.alm.shared.rt.RichTextRenderingContext;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @SuppressWarnings("unused")
    void testRenderDescription() {
        ModifiedServerFieldRichTextRenderer renderer = spy(new ModifiedServerFieldRichTextRenderer(mock(ReadOnlyTransaction.class, RETURNS_DEEP_STUBS)));
        RichTextRenderingContext context = mock(RichTextRenderingContext.class, RETURNS_DEEP_STUBS);
        doReturn(RichTextRenderTarget.PREVIEW).when(context).getRenderTarget();
        renderer.setRichTextRenderingContext(context);
        WorkItemReference workItemReference = mock(WorkItemReference.class, RETURNS_DEEP_STUBS);
        try (MockedConstruction<Renderer> rendererConstructionMock = mockConstruction(Renderer.class);
             MockedStatic<FieldUtils> mockFieldUtils = mockStatic(FieldUtils.class)) {
            assertDoesNotThrow(() -> renderer.renderDescription(mock(HtmlContentBuilder.class), workItemReference, true));

            // test @SneakyThrows
            mockFieldUtils.when(() -> FieldUtils.writeField(eq(context), eq("renderTarget"), any(), anyBoolean())).thenThrow(new IllegalAccessException("Test"));
            assertThrows(IllegalAccessException.class, () -> renderer.renderDescription(mock(HtmlContentBuilder.class), workItemReference, true));
        }
    }

    /** A referenced work item renders the comment icons of its description while it is rendered, and is referenced again after. */
    @Test
    void rendersTheCommentIconsOfAReferencedWorkItem() {
        ModifiedServerFieldRichTextRenderer renderer = spy(new ModifiedServerFieldRichTextRenderer(mock(ReadOnlyTransaction.class, RETURNS_DEEP_STUBS)));
        RichTextRenderingContext context = mock(RichTextRenderingContext.class, RETURNS_DEEP_STUBS);
        doReturn(RichTextRenderTarget.PDF_EXPORT).when(context).getRenderTarget();
        DleReferencesCollector referencesCollector = new DleReferencesCollector();
        when(context.dle().getReferencesCollector()).thenReturn(referencesCollector);
        renderer.setRichTextRenderingContext(context);
        WorkItemReference workItem = mock(WorkItemReference.class, RETURNS_DEEP_STUBS);
        when(workItem.getCurrent()).thenReturn(workItem);
        referencesCollector.addExternalWorkItem(workItem);

        boolean[] referencedWhileRendering = {true};
        try (MockedConstruction<Renderer> rendererConstructionMock = mockConstruction(Renderer.class,
                (constructed, constructionContext) -> referencedWhileRendering[0] = referencesCollector.isExternalWorkItem(workItem))) {
            renderer.renderDescription(mock(HtmlContentBuilder.class), workItem, true);
            assertFalse(rendererConstructionMock.constructed().isEmpty(), "The description is rendered");
        }

        assertFalse(referencedWhileRendering[0], "Polarion renders the comment icons of the work item as of one of the document");
        assertTrue(referencesCollector.isExternalWorkItem(workItem), "The work item is referenced again once its description is rendered");
    }

}

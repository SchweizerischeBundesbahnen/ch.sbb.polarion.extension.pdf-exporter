package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.polarion.alm.server.rt.parts.Renderer;
import com.polarion.alm.shared.api.model.document.DocumentReference;
import com.polarion.alm.shared.api.model.document.internal.InternalDocument;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.dle.DleReferencesCollector;
import com.polarion.alm.shared.rt.RichTextRenderingContext;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    /** The comment markers in the description of a referenced work item belong to the document which owns it, so Polarion skips them. */
    @Test
    void keepsAReferencedWorkItemExternalWhileItsDescriptionRenders() {
        ModifiedServerFieldRichTextRenderer renderer = spy(new ModifiedServerFieldRichTextRenderer(mock(ReadOnlyTransaction.class, RETURNS_DEEP_STUBS)));
        RichTextRenderingContext context = mock(RichTextRenderingContext.class, RETURNS_DEEP_STUBS);
        doReturn(RichTextRenderTarget.PDF_EXPORT).when(context).getRenderTarget();
        DleReferencesCollector referencesCollector = new DleReferencesCollector();
        when(context.dle().getReferencesCollector()).thenReturn(referencesCollector);
        renderer.setRichTextRenderingContext(context);
        WorkItemReference workItem = mock(WorkItemReference.class, RETURNS_DEEP_STUBS);
        when(workItem.getCurrent()).thenReturn(workItem);
        referencesCollector.addExternalWorkItem(workItem);

        boolean[] externalWhileRendering = {false};
        try (MockedConstruction<Renderer> rendererConstructionMock = mockConstruction(Renderer.class,
                (constructed, constructionContext) -> externalWhileRendering[0] = referencesCollector.isExternalWorkItem(workItem))) {
            renderer.renderDescription(mock(HtmlContentBuilder.class), workItem, true);
            assertFalse(rendererConstructionMock.constructed().isEmpty(), "The description is rendered");
        }

        assertTrue(externalWhileRendering[0], "Polarion still sees the work item as referenced from another document");
    }

    /** Polarion renders the icons of the comments which refer to a work item for a preview, and not for a PDF. */
    @Test
    @SneakyThrows
    void rendersTheIconsOfTheCommentsWhichReferToAWorkItem() {
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class, RETURNS_DEEP_STUBS);
        ModifiedServerFieldRichTextRenderer renderer = new ModifiedServerFieldRichTextRenderer(transaction);
        RichTextRenderingContext context = mock(RichTextRenderingContext.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        FieldUtils.writeField(context, "renderTarget", RichTextRenderTarget.PDF_EXPORT, true);
        renderer.setRichTextRenderingContext(context);

        RichTextRenderTarget[] targetWhileRendering = {null};
        InternalDocument document = mock(InternalDocument.class);
        when(document.isUnresolvable()).thenAnswer(invocation -> {
            targetWhileRendering[0] = context.getRenderTarget();
            return true;
        });
        DocumentReference documentReference = mock(DocumentReference.class);
        doReturn(document).when(documentReference).getOriginal(transaction);

        assertTrue(renderer.renderReferredCommentMarkers(mock(HtmlContentBuilder.class), documentReference, mock(WorkItemReference.class)).isEmpty());

        assertEquals(RichTextRenderTarget.PREVIEW, targetWhileRendering[0], "The icons are rendered as for a preview");
        assertEquals(RichTextRenderTarget.PDF_EXPORT, context.getRenderTarget(), "The render target is restored after");
    }

}

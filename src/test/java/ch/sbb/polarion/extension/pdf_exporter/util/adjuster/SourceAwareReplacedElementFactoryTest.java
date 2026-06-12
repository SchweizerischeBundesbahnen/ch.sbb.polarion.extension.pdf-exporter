package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.util.adjuster.TableAnalyzer.SourceAwareReplacedElementFactory;
import org.junit.jupiter.api.Test;
import org.xhtmlrenderer.extend.NamespaceHandler;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;
import org.xhtmlrenderer.swing.EmptyReplacedElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SourceAwareReplacedElementFactoryTest {

    private final ReplacedElementFactory delegate = mock(ReplacedElementFactory.class);
    private final SourceAwareReplacedElementFactory factory = new SourceAwareReplacedElementFactory(delegate);

    private final LayoutContext context = mock(LayoutContext.class);
    private final BlockBox box = mock(BlockBox.class);
    private final UserAgentCallback uac = mock(UserAgentCallback.class);
    private final NamespaceHandler namespaceHandler = mock(NamespaceHandler.class);
    private final org.w3c.dom.Element element = mock(org.w3c.dom.Element.class);

    @Test
    void returnsEmptyElementForImageWithNullSourceWithoutDelegating() {
        when(box.getElement()).thenReturn(element);
        when(context.getNamespaceHandler()).thenReturn(namespaceHandler);
        when(namespaceHandler.isImageElement(element)).thenReturn(true);
        when(namespaceHandler.getImageSourceURI(element)).thenReturn(null);

        ReplacedElement result = factory.createReplacedElement(context, box, uac, -1, -1);

        EmptyReplacedElement empty = assertInstanceOf(EmptyReplacedElement.class, result);
        // -1 dimensions must be clamped to 0 to avoid flying-saucer's -1x-1 placeholder attempt
        assertEquals(0, empty.getIntrinsicWidth());
        assertEquals(0, empty.getIntrinsicHeight());
        verify(delegate, org.mockito.Mockito.never()).createReplacedElement(context, box, uac, -1, -1);
    }

    @Test
    void returnsEmptyElementForImageWithBlankSourceClampingNegativeDimensions() {
        when(box.getElement()).thenReturn(element);
        when(context.getNamespaceHandler()).thenReturn(namespaceHandler);
        when(namespaceHandler.isImageElement(element)).thenReturn(true);
        when(namespaceHandler.getImageSourceURI(element)).thenReturn("   ");

        ReplacedElement result = factory.createReplacedElement(context, box, uac, 50, -1);

        EmptyReplacedElement empty = assertInstanceOf(EmptyReplacedElement.class, result);
        assertEquals(50, empty.getIntrinsicWidth());
        assertEquals(0, empty.getIntrinsicHeight());
    }

    @Test
    void delegatesForImageWithUsableSource() {
        ReplacedElement delegated = mock(ReplacedElement.class);
        when(box.getElement()).thenReturn(element);
        when(context.getNamespaceHandler()).thenReturn(namespaceHandler);
        when(namespaceHandler.isImageElement(element)).thenReturn(true);
        when(namespaceHandler.getImageSourceURI(element)).thenReturn("image.png");
        when(delegate.createReplacedElement(context, box, uac, 100, 80)).thenReturn(delegated);

        ReplacedElement result = factory.createReplacedElement(context, box, uac, 100, 80);

        assertSame(delegated, result);
    }

    @Test
    void delegatesForNonImageElement() {
        ReplacedElement delegated = mock(ReplacedElement.class);
        when(box.getElement()).thenReturn(element);
        when(context.getNamespaceHandler()).thenReturn(namespaceHandler);
        when(namespaceHandler.isImageElement(element)).thenReturn(false);
        when(delegate.createReplacedElement(context, box, uac, 10, 20)).thenReturn(delegated);

        ReplacedElement result = factory.createReplacedElement(context, box, uac, 10, 20);

        assertSame(delegated, result);
        // For non-image elements the source must never be inspected
        verify(namespaceHandler, org.mockito.Mockito.never()).getImageSourceURI(element);
    }

    @Test
    void delegatesWhenBoxHasNoElement() {
        ReplacedElement delegated = mock(ReplacedElement.class);
        when(box.getElement()).thenReturn(null);
        when(delegate.createReplacedElement(context, box, uac, 5, 5)).thenReturn(delegated);

        ReplacedElement result = factory.createReplacedElement(context, box, uac, 5, 5);

        assertSame(delegated, result);
        verifyNoInteractions(namespaceHandler);
    }

    @Test
    void resetDelegates() {
        factory.reset();
        verify(delegate).reset();
    }

    @Test
    void removeDelegates() {
        factory.remove(element);
        verify(delegate).remove(element);
    }

    @Test
    void setFormSubmissionListenerDelegates() {
        FormSubmissionListener listener = mock(FormSubmissionListener.class);
        factory.setFormSubmissionListener(listener);
        verify(delegate).setFormSubmissionListener(listener);
    }
}

package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.api.utils.html.impl.HtmlBuilder;
import com.polarion.alm.shared.rt.RichTextRenderingContext;
import com.polarion.alm.shared.rt.document.PartIdGeneratorImpl;
import com.polarion.alm.shared.rt.parts.impl.readonly.PageBreakPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomPageBreakPartTest {

    @Test
    @SuppressWarnings("unused")
    void testConstructor() {
        PageBreakPart originalPart = mock(PageBreakPart.class, RETURNS_DEEP_STUBS);

        try (MockedConstruction<PartIdGeneratorImpl> ignored = mockConstruction(PartIdGeneratorImpl.class)) {
            assertDoesNotThrow(() -> new CustomPageBreakPart(originalPart),
                    "Constructor should not throw exception with mocked PageBreakPart");
        }
    }

    @Test
    @SuppressWarnings({"unused", "java:S2699"})
    void testRenderPdfTargetLandscape() {
        // Arrange
        PageBreakPart originalPart = mock(PageBreakPart.class, RETURNS_DEEP_STUBS);

        HtmlBuilder builder = mock(HtmlBuilder.class, RETURNS_DEEP_STUBS);

        RichTextRenderingContext context = mock(RichTextRenderingContext.class, RETURNS_DEEP_STUBS);
        RichTextRenderTarget target = mock(RichTextRenderTarget.class);
        when(context.getRenderTarget()).thenReturn(target);
        when(target.isPdf()).thenReturn(true);

        try (MockedConstruction<PartIdGeneratorImpl> ignored = mockConstruction(PartIdGeneratorImpl.class)) {
            CustomPageBreakPart customPart = new CustomPageBreakPart(originalPart);

            // Act - using reflection internally, so just verify no exceptions
            assertDoesNotThrow(() -> customPart.render(builder, context, 0));
        }
    }

    @Test
    @SuppressWarnings({"unused", "java:S2699"})
    void testRenderPdfTargetPortrait() {
        // Arrange
        PageBreakPart originalPart = mock(PageBreakPart.class, RETURNS_DEEP_STUBS);

        HtmlBuilder builder = mock(HtmlBuilder.class, RETURNS_DEEP_STUBS);

        RichTextRenderingContext context = mock(RichTextRenderingContext.class, RETURNS_DEEP_STUBS);
        RichTextRenderTarget target = mock(RichTextRenderTarget.class);
        when(context.getRenderTarget()).thenReturn(target);
        when(target.isPdf()).thenReturn(true);

        try (MockedConstruction<PartIdGeneratorImpl> ignored = mockConstruction(PartIdGeneratorImpl.class)) {
            CustomPageBreakPart customPart = new CustomPageBreakPart(originalPart);

            // Act - using reflection internally, so just verify no exceptions
            assertDoesNotThrow(() -> customPart.render(builder, context, 0));
        }
    }

    @Test
    @SuppressWarnings("unused")
    void testRenderNonPdfTargetDelegatesToSuper() {
        // Arrange
        PageBreakPart originalPart = mock(PageBreakPart.class, RETURNS_DEEP_STUBS);

        HtmlBuilder builder = mock(HtmlBuilder.class, RETURNS_DEEP_STUBS);
        RichTextRenderingContext context = mock(RichTextRenderingContext.class, RETURNS_DEEP_STUBS);
        RichTextRenderTarget target = mock(RichTextRenderTarget.class);
        when(context.getRenderTarget()).thenReturn(target);
        when(target.isPdf()).thenReturn(false);

        try (MockedConstruction<PartIdGeneratorImpl> ignored = mockConstruction(PartIdGeneratorImpl.class)) {
            CustomPageBreakPart customPart = spy(new CustomPageBreakPart(originalPart));

            // Act - should delegate to super, just verify no exception
            assertDoesNotThrow(() -> customPart.render(builder, context, 0));
        }
    }

}

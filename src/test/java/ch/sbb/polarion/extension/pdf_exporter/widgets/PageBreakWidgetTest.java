package ch.sbb.polarion.extension.pdf_exporter.widgets;

import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.rp.parameter.BooleanParameter;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import com.polarion.alm.shared.api.utils.collections.ReadOnlyStrictMap;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageBreakWidgetTest {

    private final PageBreakWidget widget = new PageBreakWidget();

    private String render(boolean landscape) {
        return render(landscape, RichTextRenderTarget.RP_VIEW);
    }

    private String render(boolean landscape, RichTextRenderTarget target) {
        RichPageWidgetRenderingContext context = mock(RichPageWidgetRenderingContext.class);
        BooleanParameter landscapeParam = mock(BooleanParameter.class);
        when(landscapeParam.value()).thenReturn(landscape);
        when(context.<BooleanParameter>parameter("landscape")).thenReturn(landscapeParam);
        when(context.target()).thenReturn(target);
        return widget.renderHtml(context);
    }

    @Test
    void emitsRendererIndependentPageBreak() {
        // The marker forces a page break in any renderer, with no post-processing needed
        String html = render(false);
        assertTrue(html.contains("class=\"pdf-exporter-page-break\""), html);
        assertTrue(html.contains("break-before:page"), html);
        assertTrue(html.contains("page-break-before:always"), html);
        // No landscape modifier on a plain page break
        assertFalse(html.contains("pdf-exporter-page-break-landscape"), html);
    }

    @Test
    void landscapeAddsModifierClass() {
        // The landscape modifier is the signal HtmlProcessor uses to switch the lifted section to landscape
        String html = render(true);
        assertTrue(html.contains("class=\"pdf-exporter-page-break pdf-exporter-page-break-landscape\""), html);
        assertTrue(html.contains("break-before:page"), html);
    }

    @Test
    void labelIsShownInScreenView() {
        // Plain break shows "Page Break" in the on-screen report view
        String html = render(false);
        assertTrue(html.contains(">Page Break</span>"), html);
        // Landscape break shows "Page Break - Landscape"
        assertTrue(render(true).contains(">Page Break (Landscape)</span>"), render(true));
        // Plus an @media print rule so a browser print of that screen HTML also drops it
        assertTrue(html.contains("@media print{.pdf-exporter-page-break-label{display:none !important}}"), html);
        assertTrue(html.contains("class=\"pdf-exporter-page-break-label\""), html);
    }

    @Test
    void labelIsOmittedForBuiltInPdfAndPrintTargets() {
        // Polarion's built-in "Export to PDF" / "Print" ignore @media print, so the label must not be emitted at all.
        // The marker (with its page break) must still be present.
        for (RichTextRenderTarget target : new RichTextRenderTarget[]{
                RichTextRenderTarget.PDF_EXPORT, RichTextRenderTarget.COMPARE_PDF_EXPORT,
                RichTextRenderTarget.PRINT, RichTextRenderTarget.COMPARE_PRINT}) {
            String html = render(true, target);
            assertFalse(html.contains("Page Break"), target + " -> " + html);
            assertFalse(html.contains("pdf-exporter-page-break-label"), target + " -> " + html);
            assertTrue(html.contains("class=\"pdf-exporter-page-break pdf-exporter-page-break-landscape\""), target + " -> " + html);
            assertTrue(html.contains("break-before:page"), target + " -> " + html);
        }
    }

    @Test
    void exposesWidgetMetadata() {
        RichPageWidgetContext widgetContext = mock(RichPageWidgetContext.class);
        SharedContext sharedContext = mock(SharedContext.class);

        assertEquals("/polarion/pdf-exporter-admin/ui/images/app-icon.svg", widget.getIcon(widgetContext));
        assertEquals("Page Break", widget.getLabel(sharedContext));
        assertTrue(widget.getDetailsHtml(widgetContext).toLowerCase().contains("breaks the page"));

        List<String> tags = new ArrayList<>();
        widget.getTags(sharedContext).forEach(tags::add);
        assertEquals(List.of("PDF Export"), tags);
    }

    @Test
    void parametersDefinitionDeclaresLandscapeCheckbox() {
        ParameterFactory factory = mock(ParameterFactory.class);
        BooleanParameter.Builder builder = mock(BooleanParameter.Builder.class);
        BooleanParameter param = mock(BooleanParameter.class);
        when(factory.bool(anyString())).thenReturn(builder);
        when(builder.value(anyBoolean())).thenReturn(builder);
        when(builder.build()).thenReturn(param);

        ReadOnlyStrictMap<String, RichPageParameter> parameters = widget.getParametersDefinition(factory);

        assertEquals(1, parameters.size());
        assertSame(param, parameters.get("landscape"));
        verify(factory).bool("Landscape (works only with PDF Exporter)");
        verify(builder).value(false);
    }
}

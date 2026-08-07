package ch.sbb.polarion.extension.pdf_exporter.widgets;

import ch.sbb.polarion.extension.generic.rest.model.Version;
import ch.sbb.polarion.extension.generic.util.VersionUtils;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.collections.ReadOnlyStrictMap;
import com.polarion.alm.shared.api.utils.html.HtmlAttributesBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportToPdfButtonTest {

    private final ExportToPdfButton widget = new ExportToPdfButton();

    @Test
    void exposesWidgetMetadata() {
        RichPageWidgetContext widgetContext = mock(RichPageWidgetContext.class);
        SharedContext sharedContext = mock(SharedContext.class);

        // Icon and tags come from the shared AbstractPdfExporterButtonWidget base class
        assertEquals("/polarion/pdf-exporter-app/ui/images/app-icon.svg", widget.getIcon(widgetContext));
        assertEquals("Export to PDF Button", widget.getLabel(sharedContext));
        assertTrue(widget.getDetailsHtml(widgetContext).toLowerCase().contains("export"));

        List<String> tags = new ArrayList<>();
        widget.getTags(sharedContext).forEach(tags::add);
        assertEquals(List.of("PDF Export"), tags);
    }

    @Test
    void hasNoParameters() {
        ParameterFactory factory = mock(ParameterFactory.class);
        ReadOnlyStrictMap<String, RichPageParameter> parameters = widget.getParametersDefinition(factory);
        assertEquals(0, parameters.size());
    }

    @Test
    void opensTheExportDialogOfTheAppForTheReportItSitsOn() {
        // The dialog is a React module of the pdf-exporter-app webapp, named by a URL that nothing links to it
        // at compile time - so the URL, the export it calls and the document type are asserted here.
        String onClick = ExportToPdfButtonRenderer.onClickAction();

        assertTrue(onClick.contains("/polarion/pdf-exporter-app/ui/app/assets/export-popup.js?v="), onClick);
        assertTrue(onClick.contains("module.openExportPopup({documentType: 'LIVE_REPORT'})"), onClick);
    }

    @Test
    void carriesTheBundleVersionSoAnUpdateIsNotServedFromTheBrowserCache() {
        try (MockedStatic<VersionUtils> versions = mockStatic(VersionUtils.class)) {
            versions.when(VersionUtils::getVersion).thenReturn(Version.builder().bundleVersion("13.5.1").build());

            assertTrue(ExportToPdfButtonRenderer.onClickAction().contains("export-popup.js?v=13.5.1"));
        }
    }

    @Test
    void fallsBackToZeroWhenTheManifestCarriesNoVersion() {
        try (MockedStatic<VersionUtils> versions = mockStatic(VersionUtils.class)) {
            versions.when(VersionUtils::getVersion).thenReturn(Version.builder().build());

            assertTrue(ExportToPdfButtonRenderer.onClickAction().contains("export-popup.js?v=0"));
        }
    }

    @Test
    void wiresTheClickHandlerOntoTheLinkItRenders() {
        // The handler above is only reached if the renderer actually puts it on the anchor. Polarion's
        // OpenInTableButtonWidgetRenderer builds <span><a>, then hands the <a> to configureLinkAttributes -
        // which is the override this widget exists for, and the one line of it no other test reaches.
        RichPageWidgetCommonContext context = mock(RichPageWidgetCommonContext.class, RETURNS_DEEP_STUBS);
        // AbstractWidgetRenderer casts the context's transaction to the internal one
        when(context.transaction()).thenReturn(mock(InternalReadOnlyTransaction.class, RETURNS_DEEP_STUBS));
        // A real literal, not a deep stub: RichTextRenderTarget is a sealed abstract enum, which Mockito cannot
        // mock at all. RP_VIEW is a report page being viewed, which is where this widget sits. `doReturn` and not
        // `when`, because `when(context.target())` would run the deep-stub answer and try to mock the enum first.
        doReturn(RichTextRenderTarget.RP_VIEW).when(context).target();

        HtmlFragmentBuilder builder = mock(HtmlFragmentBuilder.class, RETURNS_DEEP_STUBS);
        HtmlTagBuilder anchor = mock(HtmlTagBuilder.class, RETURNS_DEEP_STUBS);
        HtmlAttributesBuilder anchorAttributes = mock(HtmlAttributesBuilder.class, RETURNS_DEEP_STUBS);
        when(anchor.attributes()).thenReturn(anchorAttributes);
        when(builder.tag().span().append().tag().a()).thenReturn(anchor);
        // Escaping belongs to the page being rendered into; pass the handler through to assert it as written
        when(builder.target().escapeForAttribute(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        new ExportToPdfButtonRenderer(context).render(builder);

        ArgumentCaptor<String> onClick = ArgumentCaptor.forClass(String.class);
        verify(anchorAttributes).onClick(onClick.capture());
        assertTrue(onClick.getValue().contains(ExportToPdfButtonRenderer.POPUP_MODULE_URL), onClick.getValue());
        assertTrue(onClick.getValue().contains("module.openExportPopup({documentType: 'LIVE_REPORT'})"), onClick.getValue());
    }
}

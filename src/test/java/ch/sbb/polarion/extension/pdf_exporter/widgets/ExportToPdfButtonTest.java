package ch.sbb.polarion.extension.pdf_exporter.widgets;

import ch.sbb.polarion.extension.generic.rest.model.Version;
import ch.sbb.polarion.extension.generic.util.VersionUtils;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.utils.collections.ReadOnlyStrictMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

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
}

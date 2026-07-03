package ch.sbb.polarion.extension.pdf_exporter.widgets;

import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.utils.collections.ReadOnlyStrictMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ExportToPdfButtonTest {

    private final ExportToPdfButton widget = new ExportToPdfButton();

    @Test
    void exposesWidgetMetadata() {
        RichPageWidgetContext widgetContext = mock(RichPageWidgetContext.class);
        SharedContext sharedContext = mock(SharedContext.class);

        // Icon and tags come from the shared AbstractPdfExporterButtonWidget base class
        assertEquals("/polarion/pdf-exporter-admin/ui/images/app-icon.svg", widget.getIcon(widgetContext));
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
}

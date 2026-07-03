package ch.sbb.polarion.extension.pdf_exporter.widgets;

import com.polarion.alm.server.api.model.rp.widget.GenericButtonWidget;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.rp.parameter.BooleanParameter;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import com.polarion.alm.shared.api.utils.collections.ImmutableStrictList;
import com.polarion.alm.shared.api.utils.collections.ReadOnlyStrictMap;
import com.polarion.alm.shared.api.utils.collections.StrictMap;
import com.polarion.alm.shared.api.utils.collections.StrictMapImpl;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import org.jetbrains.annotations.NotNull;

import static ch.sbb.polarion.extension.pdf_exporter.util.exporter.Constants.PAGE_BREAK_WIDGET_CLASS;
import static ch.sbb.polarion.extension.pdf_exporter.util.exporter.Constants.PAGE_BREAK_WIDGET_LANDSCAPE_CLASS;

/**
 * Report widget that inserts a page break, optionally switching the following content to landscape orientation.
 * <p>
 * {@link #renderHtml} emits only a marker {@code <div>} carrying a renderer-independent {@code break-before} style. The
 * page break therefore works in any renderer with no post-processing. Orientation, however, cannot be done from here:
 * the widget output lands inside a {@code polarion-rp-column-layout} table cell, and the CSS {@code page} property does
 * not propagate out of a table cell. When the document is exported through this extension, {@code HtmlProcessor} detects
 * the marker (by {@link ch.sbb.polarion.extension.pdf_exporter.util.exporter.Constants#PAGE_BREAK_WIDGET_CLASS}) and lifts the content following it out of the cell into a
 * body-level landscape/portrait section. In other renderers only the page break applies, which is the best achievable.
 */
public class PageBreakWidget extends GenericButtonWidget {

    private static final String PARAM_LANDSCAPE = "landscape";
    private static final String LABEL_CLASS = "pdf-exporter-page-break-label";

    @NotNull
    @Override
    public String getIcon(@NotNull RichPageWidgetContext widgetContext) {
        return "/polarion/pdf-exporter-admin/ui/images/app-icon.svg";
    }

    @NotNull
    @Override
    public Iterable<String> getTags(@NotNull SharedContext context) {
        return new ImmutableStrictList<>(Constants.PDF_EXPORT_TAG);
    }

    @NotNull
    @Override
    public String getLabel(@NotNull SharedContext sharedContext) {
        return "Page Break";
    }

    @NotNull
    @Override
    public String getDetailsHtml(@NotNull RichPageWidgetContext widgetContext) {
        return "Breaks the page; optionally switches the following content to landscape orientation";
    }

    @NotNull
    @Override
    public ReadOnlyStrictMap<String, RichPageParameter> getParametersDefinition(@NotNull ParameterFactory parameterFactory) {
        StrictMap<String, RichPageParameter> parameters = new StrictMapImpl<>();
        parameters.put(PARAM_LANDSCAPE, parameterFactory.bool("Landscape (works only with PDF Exporter)").value(false).build());
        return parameters;
    }

    @NotNull
    @Override
    public String renderHtml(@NotNull RichPageWidgetRenderingContext renderingContext) {
        BooleanParameter landscapeParam = renderingContext.parameter(PARAM_LANDSCAPE);
        boolean landscape = landscapeParam.value();
        String cssClass = landscape
                ? PAGE_BREAK_WIDGET_CLASS + " " + PAGE_BREAK_WIDGET_LANDSCAPE_CLASS
                : PAGE_BREAK_WIDGET_CLASS;

        // The marker always carries an inline break-before so the page break works in any renderer even without the
        // extension's HtmlProcessor, and so HtmlProcessor can always detect it to apply the landscape switch.
        String markerOpen = String.format("<div class=\"%s\" style=\"break-before:page;page-break-before:always;\">", cssClass);

        // The label is an authoring hint that must never reach a printout. Polarion's built-in "Export to PDF"/"Print"
        // render the widget with a pdf/print target using a renderer that ignores @media print, so for those targets we
        // omit the label entirely. For the on-screen report view we keep it (so authors see where the break is), plus an
        // @media print rule so a browser print of that screen HTML also drops it.
        RichTextRenderTarget target = renderingContext.target();
        if (target.isPdf() || target.isPrint()) {
            return markerOpen + "</div>";
        }

        String labelText = landscape ? "Page Break (Landscape)" : "Page Break";
        return markerOpen
                + String.format("<style>@media print{.%s{display:none !important}}</style>", LABEL_CLASS)
                + String.format("<span class=\"%s\" style=\"display:block;text-align:center;color:#888;font-style:italic;"
                        + "border-top:1px dashed #bbb;border-bottom:1px dashed #bbb;padding:2px 0;\">%s</span>", LABEL_CLASS, labelText)
                + "</div>";
    }

}

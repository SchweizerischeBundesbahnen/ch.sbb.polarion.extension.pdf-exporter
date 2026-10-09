package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * One widget of a Live Report, exported alone (#1183).
 * <p>
 * Polarion stores a Live Report as HTML in which every widget is an element of the class {@value #WIDGET_PART_CLASS}
 * with an ID of its own, kept with the page: {@code <div id="polarion_client1" class="polarion-rp-widget-part"
 * data-widget="...">}. The same element carries the same ID on the page in the browser, which is where the export
 * dialog takes it from. Keeping only that element in the stored HTML before it is rendered makes Polarion render the
 * one widget, so the widget itself needs to know nothing about the export.
 * </p>
 */
@UtilityClass
public class LiveReportWidget {

    /** The class Polarion gives the element of every widget of a Live Report. */
    public static final String WIDGET_PART_CLASS = "polarion-rp-widget-part";

    /**
     * The stored HTML of a Live Report reduced to its title and the widget of the given ID, laid out in one column the
     * width of the page as a report's own column is.
     * <p>
     * Fails with a {@link UserFriendlyRuntimeException} where the report holds no widget of that ID: it was removed, or
     * the page the export was started from is not the one stored.
     * </p>
     */
    public static @NotNull String keepOnly(@NotNull String pageHtml, @NotNull String widgetId, @NotNull String title) {
        Document page = Jsoup.parseBodyFragment(pageHtml);
        Element widget = page.getElementById(widgetId);
        if (widget == null || !widget.hasClass(WIDGET_PART_CLASS)) {
            throw new UserFriendlyRuntimeException("The report '%s' has no widget '%s' to export. Reload the report and export it again.".formatted(title, widgetId));
        }

        Document result = Jsoup.parseBodyFragment("");
        result.outputSettings().prettyPrint(false);
        Element column = result.body().appendElement("div").addClass("polarion-rp-column").attr("style", "width: 100%;");
        column.appendElement("h1").text(title);
        column.appendChild(widget.clone());
        return result.body().html();
    }
}

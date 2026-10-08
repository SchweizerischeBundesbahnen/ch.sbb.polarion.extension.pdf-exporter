package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiveReportWidgetTest {

    /** A Live Report as Polarion stores it: a title, and two Bulk PDF Export widgets in two columns. */
    private static final String PAGE = resource("liveReportWithTwoWidgets.html");

    @Test
    void keepsTheTitleAndTheWidgetOfTheGivenId() {
        Document result = Jsoup.parseBodyFragment(LiveReportWidget.keepOnly(PAGE, "polarion_client2", "Bulk processing"));

        assertThat(result.select("." + LiveReportWidget.WIDGET_PART_CLASS)).extracting(Element::id).containsExactly("polarion_client2");
        assertThat(result.select("h1")).extracting(Element::text).containsExactly("Bulk processing");
        assertThat(result.text()).doesNotContain("This Page has no content yet");
    }

    @Test
    void keepsTheParametersOfTheWidgetAsStored() {
        Element stored = Jsoup.parseBodyFragment(PAGE).getElementById("polarion_client2");

        Element kept = Jsoup.parseBodyFragment(LiveReportWidget.keepOnly(PAGE, "polarion_client2", "Bulk processing")).getElementById("polarion_client2");

        assertThat(kept).isNotNull();
        assertThat(kept.attr("data-widget")).isEqualTo("ch.sbb.polarion.extension.pdf.exporter.widgets.bulkPdfExportWidget");
        assertThat(kept.outerHtml()).isEqualTo(Objects.requireNonNull(stored).outerHtml());
    }

    @Test
    void laysTheWidgetOutTheWidthOfThePage() {
        Element column = Jsoup.parseBodyFragment(LiveReportWidget.keepOnly(PAGE, "polarion_client2", "Bulk processing")).selectFirst(".polarion-rp-column");

        assertThat(column).isNotNull();
        assertThat(column.attr("style")).isEqualTo("width: 100%;");
        assertThat(column.select("." + LiveReportWidget.WIDGET_PART_CLASS)).hasSize(1);
    }

    @Test
    void escapesTheTitle() {
        String result = LiveReportWidget.keepOnly(PAGE, "polarion_client1", "Tom & Jerry <b>");

        assertThat(Jsoup.parseBodyFragment(result).select("h1")).extracting(Element::text).containsExactly("Tom & Jerry <b>");
        assertThat(result).doesNotContain("<b>");
    }

    /** No such element, or an element of that ID which is no widget: a paragraph, or a parameter of a widget. */
    @ParameterizedTest
    @ValueSource(strings = {"polarion_client3", "polarion_hardcoded_2", "dataSet"})
    void refusesAnIdWhichNamesNoWidget(String widgetId) {
        assertThatThrownBy(() -> LiveReportWidget.keepOnly(PAGE, widgetId, "Bulk processing"))
                .isInstanceOf(UserFriendlyRuntimeException.class)
                .hasMessageContaining(widgetId)
                .hasMessageContaining("Bulk processing");
    }

    @SneakyThrows
    private static String resource(String name) {
        try (InputStream stream = Objects.requireNonNull(LiveReportWidgetTest.class.getResourceAsStream("/" + name), name)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

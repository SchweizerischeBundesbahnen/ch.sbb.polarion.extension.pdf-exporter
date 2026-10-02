package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class PageRulesTest {

    @Test
    void takesTheMarginsEveryPageStates() {
        PageRules.Heights heights = PageRules.contentHeights("@page { margin: 120px 60px 90px 80px; }");

        // A portrait A4 is 1122.5 px high, a landscape one 793.7 px
        assertThat(heights.ofEveryPage()).containsEntry("portA4", 912).containsEntry("landA4", 583);
        assertThat(heights.ofNamedPage()).containsEntry("portA4", 912).containsEntry("landA4", 583);
    }

    @Test
    void takesTheMarginsAPageOfItsOwnNameStates() {
        PageRules.Heights heights = PageRules.contentHeights("@page { margin: 120px 60px 90px 80px; } @page portA3 { size: A3 portrait; margin: 140px 80px 110px; }");

        // A portrait A3 is 1587.4 px high: its own page has its own margins, the page every page is has those of all
        assertThat(heights.ofNamedPage()).containsEntry("portA3", 1337).containsEntry("portA4", 912);
        assertThat(heights.ofEveryPage()).containsEntry("portA3", 1377);
    }

    @Test
    void takesTheLastMarginsItStates() {
        PageRules.Heights heights = PageRules.contentHeights("@page { margin: 120px; } @page { margin-top: 1in; margin-bottom: 0; }");

        assertThat(heights.ofEveryPage()).containsEntry("portA4", 1026);
    }

    @Test
    void leavesAPageWhoseMarginsItDoesNotStateOut() {
        assertThat(PageRules.contentHeights("@page :first { margin: 0; } body { margin: 0; }").ofEveryPage()).isEmpty();
        assertThat(PageRules.contentHeights("@page { margin: 5em; }").ofNamedPage()).isEmpty();
        assertThat(PageRules.contentHeights("")).isEqualTo(PageRules.Heights.NONE);
    }

    @Test
    @SneakyThrows
    void readsTheDefaultCss() {
        try (InputStream css = PageRulesTest.class.getResourceAsStream("/default/dle-pdf-export.css")) {
            PageLayout layout = PageLayout.of(new String(Objects.requireNonNull(css).readAllBytes(), StandardCharsets.UTF_8));

            assertThat(layout.contentHeight(ConversionParams.builder().paperSize(PaperSize.A4).orientation(Orientation.PORTRAIT).build())).isEqualTo(912);
            assertThat(layout.contentHeight(ConversionParams.builder().paperSize(PaperSize.A4).orientation(Orientation.LANDSCAPE).build())).isEqualTo(583);
            // A5 states margins of its own, 90 and 60 px, which only its named page in an area between page breaks has
            ConversionParams a5 = ConversionParams.builder().paperSize(PaperSize.A5).orientation(Orientation.PORTRAIT).build();
            assertThat(layout.contentHeight(a5)).isEqualTo(583);
            assertThat(layout.on(PageLayout.Pages.NAMED_PAGE).contentHeight(a5)).isEqualTo(643);
            // A3 states larger margins of its own, so of either page its named one is the lower
            ConversionParams a3 = ConversionParams.builder().paperSize(PaperSize.A3).orientation(Orientation.PORTRAIT).build();
            assertThat(layout.on(PageLayout.Pages.EITHER_PAGE).contentHeight(a3)).isEqualTo(layout.on(PageLayout.Pages.NAMED_PAGE).contentHeight(a3))
                    .isLessThan(layout.contentHeight(a3));
            assertThat(layout.on(PageLayout.Pages.EITHER_PAGE).contentHeight(a5)).isEqualTo(layout.contentHeight(a5));
        }
    }

    @Test
    void fallsBackToThePaperWhereTheCssStatesNoMargins() {
        ConversionParams a4 = ConversionParams.builder().paperSize(PaperSize.A4).orientation(Orientation.PORTRAIT).build();

        assertThat(PageLayout.NONE.contentHeight(a4)).isEqualTo(PaperSizeUtils.getMaxHeight(a4));
    }
}

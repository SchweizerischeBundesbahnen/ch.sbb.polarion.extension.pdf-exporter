package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        assertThat(PageLayout.NONE.contentWidth(a4)).isEqualTo(PaperSizeUtils.getMaxWidth(a4));
        assertThat(PageLayout.NONE.widthForATable(a4)).isEqualTo(PaperSizeUtils.getMaxWidth(a4));
    }

    @Test
    void leavesATableTheDefaultsOfWeasyPrintWhereTheCssStatesNoSideMargins() {
        ConversionParams a4 = ConversionParams.builder().paperSize(PaperSize.A4).orientation(Orientation.PORTRAIT).build();

        // A page margin of 75px and a body margin of 8px on each side leave a portrait A4 793.7 - 150 - 16 = 627px
        assertThat(PageLayout.of("p { margin: 0; }").widthForATable(a4)).isEqualTo(627);
        assertThat(PageLayout.of("@page { margin: 5%; }").widthForATable(a4)).as("Margins it cannot read leave the paper").isEqualTo(PaperSizeUtils.getMaxWidth(a4));
    }

    @Test
    void fallsBackToThePaperWhereTheCssLeavesATableNoRoom() {
        ConversionParams a4 = ConversionParams.builder().paperSize(PaperSize.A4).orientation(Orientation.PORTRAIT).build();

        // A portrait A4 is 793.7 px wide: margins of 450px leave it none, and of 390px less than the body takes
        assertThat(PageLayout.of("@page { margin: 0 450px; }").widthForATable(a4)).isEqualTo(PaperSizeUtils.getMaxWidth(a4));
        assertThat(PageLayout.of("@page { margin: 0 390px; }").widthForATable(a4)).isEqualTo(PaperSizeUtils.getMaxWidth(a4));
        assertThat(PageLayout.of("@page { margin: 0 450px; }").contentWidth(a4)).isEqualTo(PaperSizeUtils.getMaxWidth(a4));
    }

    @Test
    void takesTheSideMarginsEveryPageStates() {
        PageRules.Widths widths = PageRules.contentWidths("@page { margin: 120px 60px 90px 80px; }");

        // A portrait A4 is 793.7 px wide, a landscape one 1122.5 px
        assertThat(widths.ofEveryPage()).containsEntry("portA4", 653).containsEntry("landA4", 982);
        assertThat(widths.ofNamedPage()).containsEntry("portA4", 653).containsEntry("landA4", 982);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "@page { margin: 50px; }                         | 693",
            "@page { margin: 10px 50px; }                    | 693",
            "@page { margin: 10px 50px 20px; }               | 693",
            "@page { margin: 10px 60px 20px 40px; }          | 693",
            "@page { margin: 0; margin-left: 1in; margin-right: 4px; } | 693"
    })
    void takesTheSideMarginsAsTheShorthandStatesThem(String css, int width) {
        assertThat(PageRules.contentWidths(css).ofEveryPage()).containsEntry("portA4", width);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "@page { margin: 2cm; }                  | 642",
            "@page { margin: 10mm 15mm 10mm 20mm; }  | 661",
            "@page { margin: 0.5in 1in; }            | 601",
            "@page { margin: 36pt; }                 | 697",
            "@page { margin: 3pc 2pc; }              | 729",
            "@page { margin: 0; }                    | 793"
    })
    void takesTheSideMarginsInAnyAbsoluteUnit(String css, int width) {
        // A portrait A4 is 793.7 px wide
        assertThat(PageRules.contentWidths(css).ofEveryPage()).containsEntry("portA4", width);
    }

    @Test
    void takesTheSideMarginsANamedLandscapePageStates() {
        PageLayout layout = PageLayout.of("@page { margin: 1in; } @page landLETTER { size: letter landscape; margin: 0.5in; }");
        ConversionParams letter = ConversionParams.builder().paperSize(PaperSize.LETTER).orientation(Orientation.LANDSCAPE).build();

        // A landscape letter is 11 in, 1056 px wide
        assertThat(layout.contentWidth(letter)).isEqualTo(864);
        assertThat(layout.on(PageLayout.Pages.NAMED_PAGE).contentWidth(letter)).isEqualTo(960);
        assertThat(layout.on(PageLayout.Pages.EITHER_PAGE).contentWidth(letter)).as("Of either page, the narrower one counts").isEqualTo(864);
    }

    /**
     * The width of the page every page is and of the page of its own name, portrait and landscape. A5, B5 and JIS-B5 state
     * side margins of 60 and 40 px, A3, legal and ledger of 100 and 80 px, which only their named pages have. The rest
     * have those of every page, 80 and 60 px.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "A5     | 419 | 653  | 459 | 693",
            "A4     | 653 | 982  | 653 | 982",
            "A3     | 982 | 1447 | 942 | 1407",
            "B5     | 525 | 804  | 565 | 844",
            "B4     | 804 | 1194 | 804 | 1194",
            "JIS_B5 | 547 | 831  | 587 | 871",
            "JIS_B4 | 831 | 1235 | 831 | 1235",
            "LETTER | 676 | 916  | 676 | 916",
            "LEGAL  | 676 | 1204 | 636 | 1164",
            "LEDGER | 916 | 1492 | 876 | 1452"
    })
    @SneakyThrows
    void readsTheWidthOfEveryPaperSizeAndOrientationFromTheDefaultCss(PaperSize size, int portrait, int landscape, int namedPortrait, int namedLandscape) {
        try (InputStream css = PageRulesTest.class.getResourceAsStream("/default/dle-pdf-export.css")) {
            PageLayout layout = PageLayout.of(new String(Objects.requireNonNull(css).readAllBytes(), StandardCharsets.UTF_8));
            ConversionParams upright = ConversionParams.builder().paperSize(size).orientation(Orientation.PORTRAIT).build();
            ConversionParams turned = ConversionParams.builder().paperSize(size).orientation(Orientation.LANDSCAPE).build();

            assertThat(layout.contentWidth(upright)).isEqualTo(portrait);
            assertThat(layout.contentWidth(turned)).isEqualTo(landscape);
            assertThat(layout.on(PageLayout.Pages.NAMED_PAGE).contentWidth(upright)).isEqualTo(namedPortrait);
            assertThat(layout.on(PageLayout.Pages.NAMED_PAGE).contentWidth(turned)).isEqualTo(namedLandscape);
        }
    }

    @Test
    void takesTheSideMarginsAPageOfItsOwnNameStates() {
        PageRules.Widths widths = PageRules.contentWidths("@page { margin: 120px 60px 90px 80px; } @page portA3 { size: A3 portrait; margin: 140px 80px 110px 100px; }");

        // A portrait A3 is 1122.5 px wide: its own page has its own margins, the page every page is has those of all
        assertThat(widths.ofNamedPage()).containsEntry("portA3", 942).containsEntry("portA4", 653);
        assertThat(widths.ofEveryPage()).containsEntry("portA3", 982);
    }

    @Test
    void takesTheSideMarginsOfWeasyPrintWhereTheCssStatesNone() {
        // WeasyPrint gives a page a margin of 75px, which leaves a portrait A4 643px
        assertThat(PageRules.contentWidths("@page { margin-top: 1in; margin-bottom: 1in; }").ofEveryPage()).containsEntry("portA4", 643);
        assertThat(PageRules.contentWidths("p { margin: 0; }").ofEveryPage()).containsEntry("portA4", 643);
        assertThat(PageRules.contentWidths("@page { margin-left: 0; }").ofEveryPage()).containsEntry("portA4", 718);
    }

    @Test
    void leavesAPageWhoseSideMarginsItCannotReadOut() {
        assertThat(PageRules.contentWidths("@page { margin: 5%; }").ofEveryPage()).isEmpty();
        assertThat(PageRules.contentWidths("@page { margin: 0; margin-right: 2em; }").ofEveryPage()).isEmpty();
    }

    @Test
    @SneakyThrows
    void readsTheWidthsOfTheDefaultCss() {
        try (InputStream css = PageRulesTest.class.getResourceAsStream("/default/dle-pdf-export.css")) {
            PageLayout layout = PageLayout.of(new String(Objects.requireNonNull(css).readAllBytes(), StandardCharsets.UTF_8));

            assertThat(layout.contentWidth(ConversionParams.builder().paperSize(PaperSize.A4).orientation(Orientation.PORTRAIT).build())).isEqualTo(653);
            assertThat(layout.contentWidth(ConversionParams.builder().paperSize(PaperSize.A4).orientation(Orientation.LANDSCAPE).build())).isEqualTo(982);
            // A5 states margins of its own, 60 and 40 px, which only its named page in an area between page breaks has
            ConversionParams a5 = ConversionParams.builder().paperSize(PaperSize.A5).orientation(Orientation.PORTRAIT).build();
            assertThat(layout.contentWidth(a5)).isEqualTo(419);
            assertThat(layout.on(PageLayout.Pages.NAMED_PAGE).contentWidth(a5)).isEqualTo(459);
            assertThat(layout.on(PageLayout.Pages.EITHER_PAGE).contentWidth(a5)).isEqualTo(419);
            // A table has the page less the margins of 8px WeasyPrint gives the body and its padding of 10px on the right
            assertThat(layout.widthForATable(ConversionParams.builder().paperSize(PaperSize.A4).orientation(Orientation.PORTRAIT).build())).isEqualTo(627);
        }
    }
}

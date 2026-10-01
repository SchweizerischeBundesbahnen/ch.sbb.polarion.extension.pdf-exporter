package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LongWordsAdjusterTest {

    @Test
    void givesALongUrlPlacesToBreakAfterItsSeparators() {
        Document document = Jsoup.parse("<table><tr><td><a href=\"https://example.com/a\">https://example.com/very/deep/path?x=1&amp;y=2</a></td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document);

        Element link = document.selectFirst("a");
        assertThat(link.text()).as("The text reads as before").isEqualTo("https://example.com/very/deep/path?x=1&y=2");
        assertThat(link.attr("href")).as("The address is left alone").isEqualTo("https://example.com/a");
        assertThat(link.html()).isEqualTo("https:/<wbr>/<wbr>example.<wbr>com/<wbr>very/<wbr>deep/<wbr>path?<wbr>x=<wbr>1&amp;<wbr>y=<wbr>2");
    }

    @Test
    void breaksAVeryLongWordWithoutSeparatorsEveryTwentyCharacters() {
        Document document = Jsoup.parse("<table><tr><td>" + "ABCDEFGHIJ".repeat(5) + "</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document);

        assertThat(document.selectFirst("td").html()).isEqualTo("ABCDEFGHIJABCDEFGHIJ<wbr>ABCDEFGHIJABCDEFGHIJ<wbr>ABCDEFGHIJ");
    }

    @Test
    void leavesALongWordOfALanguageWhole() {
        // A word of a language breaks where the line ends, with a hyphen where the document has a language, not at an arbitrary character
        Document document = Jsoup.parse("<table><tr><td>Die Sicherheitsanforderungen und der Verantwortlichkeitsbereich</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document);

        assertThat(document.select("wbr")).isEmpty();
    }

    @Test
    void treatsWordsWhichNoLineMayBreakBetweenAsOneWord() {
        // Unicode allows no break before a slash, even after a space
        Document document = Jsoup.parse("<table><tr><td>/-123 /-123 /-123 /-123</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document);

        assertThat(document.selectFirst("td").html()).isEqualTo("/<wbr>-<wbr>123 /<wbr>-<wbr>123 /<wbr>-<wbr>123 /<wbr>-<wbr>123");
    }

    @Test
    void countsAWordAcrossTheElementsItIsWrittenIn() {
        Document document = Jsoup.parse("<table><tr><td><b>ABCDEFGHIJABCDEFGHIJ</b><i>ABCDEFGHIJABCDEFGHIJ</i>ABCDEFGHIJ</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document);

        assertThat(document.selectFirst("td").select("wbr")).as("A break after each twenty characters of the word, wherever it falls").hasSize(2);
        assertThat(document.selectFirst("td").text()).isEqualTo("ABCDEFGHIJ".repeat(5));
    }

    @Test
    void neverBreaksBetweenTheTwoHalvesOfACharacter() {
        String emoji = "🔴";
        Document document = Jsoup.parse("<table><tr><td>" + "A".repeat(19) + emoji.repeat(25) + "</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document);

        String cell = document.selectFirst("td").html();
        assertThat(cell).contains("A".repeat(19) + emoji + "<wbr>").doesNotContain("\uD83D<wbr>");
    }

    @Test
    void leavesShortWordsTextOutsideTablesAndTheCellsOfANestedTableToThemselves() {
        Document document = Jsoup.parse("<p>https://example.com/very/deep/path/outside/of/a/table</p>"
                + "<table><tr><th>Disapproved</th><td>TMSPRG-13164 &lt;b&gt; 2022-01-05<table><tr><td>Short words only</td></tr></table></td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document);

        assertThat(document.select("wbr")).as("No word is long enough to break").isEmpty();
        assertThat(document.selectFirst("p").text()).isEqualTo("https://example.com/very/deep/path/outside/of/a/table");
    }

    @Test
    void endsAWordWhereABlockOrALineBreakStarts() {
        Document document = Jsoup.parse("<table><tr><td>ABCDEFGHIJABCDEFGHIJ<br>ABCDEFGHIJ<div>ABCDEFGHIJABCDEFGHIJ</div></td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document);

        assertThat(document.select("wbr")).as("No single word is long enough to break").isEmpty();
    }
}

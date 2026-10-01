package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

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
    void givesAWindowsPathAndAnAddressPlacesToBreak() {
        Document document = Jsoup.parse("<table><tr><td>C:\\Program Files\\Polarion\\polarion.properties</td><td>support.team@polarion.example.com</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document);

        assertThat(document.select("td").get(0).html()).isEqualTo("C:\\Program Files\\<wbr>Polarion\\<wbr>polarion.<wbr>properties");
        assertThat(document.select("td").get(1).html()).isEqualTo("support.<wbr>team@<wbr>polarion.<wbr>example.<wbr>com");
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
    void readsAVeryLongCellInOnePass() {
        // A regular expression with a repetition inside a repetition could overflow the stack on a text this long
        String text = "Words of a description https://example.com/a/very/deep/path/to/a/resource /-123 /-123 /-123 ".repeat(5_000);
        Document document = Jsoup.parse("<table><tr><td>" + text + "</td></tr></table>");

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> LongWordsAdjuster.addBreakPoints(document));

        assertThat(document.selectFirst("td").text()).as("The text reads as before").isEqualTo(text.strip());
        assertThat(document.select("wbr")).as("Each long word gets its places to break").hasSizeGreaterThan(5_000);
    }

    @Test
    void endsAWordWhereABlockOrALineBreakStarts() {
        Document document = Jsoup.parse("<table><tr><td>ABCDEFGHIJABCDEFGHIJ<br>ABCDEFGHIJ<div>ABCDEFGHIJABCDEFGHIJ</div></td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document);

        assertThat(document.select("wbr")).as("No single word is long enough to break").isEmpty();
    }
}

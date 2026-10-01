package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

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
    void breaksAVeryLongWordWithoutSeparatorsIntoEvenParts() {
        Document document = Jsoup.parse("<table><tr><td>" + "ABCDEFGHIJ".repeat(5) + "</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document);

        assertThat(document.selectFirst("td").html()).isEqualTo("ABCDEFGHIJABCDEF<wbr>GHIJABCDEFGHIJABC<wbr>DEFGHIJABCDEFGHIJ");
    }

    @Test
    void leavesALongWordOfALanguageWhole() {
        // A word of a language breaks where the line ends, with a hyphen where the document has a language, not at an arbitrary character
        Document document = Jsoup.parse("<table><tr><td>Die Sicherheitsanforderungen und der Verantwortlichkeitsbereich</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document);

        assertThat(document.select("wbr")).isEmpty();
    }

    @Test
    void breaksALongIdWithADigitIntoEvenParts() {
        // A digit makes it an ID, not a word of a language, so it breaks even though it is shorter than a very long word
        Document document = Jsoup.parse("<table><tr><td>REQUIREMENT2026ABCDEFGHIJKLMNOP</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document);

        assertThat(document.selectFirst("td").html()).isEqualTo("REQUIREMENT2026<wbr>ABCDEFGHIJKLMNOP");
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

        assertThat(document.selectFirst("td").select("wbr")).as("The word breaks into three even parts, wherever they fall").hasSize(2);
        assertThat(document.selectFirst("td").text()).isEqualTo("ABCDEFGHIJ".repeat(5));
    }

    @Test
    void breaksTheWordsOfATableWithNoRoomIntoShorterParts() {
        Document document = Jsoup.parse("<table><tr><td>Disapproved Sicherheitsanforderungen TMSPRG-13164</td></tr></table>");

        LongWordsAdjuster.addBreakPointsToFit(document.selectFirst("table"), 10);

        assertThat(document.selectFirst("td").html()).as("Even parts of ten characters at most, and a break after a separator")
                .isEqualTo("Disap<wbr>proved Sicherhe<wbr>itsanfor<wbr>derungen TMSPRG-<wbr>13164");
    }

    @Test
    void neverBreaksBetweenTheTwoHalvesOfACharacter() {
        String emoji = "🔴";
        Document document = Jsoup.parse("<table><tr><td>" + "A".repeat(19) + emoji.repeat(25) + "</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document);

        String cell = document.selectFirst("td").html();
        assertThat(cell).as("Three parts of 14 and 15 characters, the second break between two emoji")
                .isEqualTo("A".repeat(14) + "<wbr>" + "A".repeat(5) + emoji.repeat(10) + "<wbr>" + emoji.repeat(15));
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

    @Test
    void hyphenatesAVeryLongWordOfLettersInADocumentWithALanguage() {
        Document document = Jsoup.parse("<table><tr><td>Grundstücksverkehrsgenehmigungszuständigkeitsübertragungsverordnung</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document, "de");

        assertThat(document.selectFirst("table").attr("lang")).as("The table is in the language of the document").isEqualTo("de");
        assertThat(document.select("wbr")).as("A syllable breaks the word, with a hyphen").isEmpty();
        assertThat(document.selectFirst("td").attr("style")).isEqualTo("hyphens:auto;");
    }

    @Test
    void keepsTheBreakPointsOfAnIdInADocumentWithALanguage() {
        Document document = Jsoup.parse("<table><tr><td>REQUIREMENT2026ABCDEFGHIJKLMNOP</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document, "de");

        assertThat(document.selectFirst("td").html()).as("No dictionary hyphenates an ID").isEqualTo("REQUIREMENT2026<wbr>ABCDEFGHIJKLMNOP");
        assertThat(document.selectFirst("td").hasAttr("style")).isFalse();
    }

    @Test
    void leavesTheLanguageAndTheHyphenationATableStatesAsTheyAre() {
        Document document = Jsoup.parse("<table lang=\"fr\"><tr><td style=\"hyphens: manual\">" + "anticonstitutionnellement".repeat(2) + "</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document, "de");

        assertThat(document.selectFirst("table").attr("lang")).isEqualTo("fr");
        assertThat(document.selectFirst("td").attr("style")).isEqualTo("hyphens: manual");
        assertThat(document.select("wbr")).as("A cell which hyphenates by hand keeps the break points of its words").isNotEmpty();
    }

    @Test
    void hyphenatesTheWordsOfATableWithNoRoomInADocumentWithALanguage() {
        Document document = Jsoup.parse("<table><tr><td>Die Rechtsschutzversicherungsgesellschaft TMSPRG-13164-ABCDEFGHIJ</td></tr></table>");
        LongWordsAdjuster.addBreakPoints(document, "de");

        LongWordsAdjuster.addBreakPointsToFit(document.selectFirst("table"), 15);

        Element cell = document.selectFirst("td");
        assertThat(cell.text()).isEqualTo("Die Rechtsschutzversicherungsgesellschaft TMSPRG-13164-ABCDEFGHIJ");
        assertThat(cell.html()).as("The word of letters hyphenates, the ID breaks").startsWith("Die Rechtsschutzversicherungsgesellschaft TMSPRG-<wbr>");
        assertThat(cell.attr("style")).isEqualTo("hyphens:auto;");
    }

    @Test
    void keepsTheBreakPointsOfANameOfCodeInADocumentWithALanguage() {
        // A hyphen in a class name would read as a part of it
        Document document = Jsoup.parse("<table><tr><td>SuperLongUnbreakableWordThatWillNotWrapInsideTheTableCell</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document, "en");

        assertThat(document.select("wbr")).hasSize(2);
        assertThat(document.selectFirst("td").hasAttr("style")).isFalse();
    }

    @Test
    void hyphenatesTheWordsANoBreakSpaceJoins() {
        // Polarion writes a no-break space after a short word, which joins it to the next one
        Document document = Jsoup.parse("<table><tr><td>von der&nbsp;Rechtsschutzversicherungsgesellschaft geprüft</td></tr></table>");
        LongWordsAdjuster.addBreakPoints(document, "de");

        LongWordsAdjuster.addBreakPointsToFit(document.selectFirst("table"), 15);

        assertThat(document.select("wbr")).isEmpty();
        assertThat(document.selectFirst("td").attr("style")).isEqualTo("hyphens:auto;");
    }

    @Test
    void hyphenatesATableInTheLanguageItInherits() {
        Document document = Jsoup.parse("<div lang=\"fr\"><table><tr><td>" + "anticonstitutionnellement".repeat(2) + "</td></tr></table></div>");

        LongWordsAdjuster.addBreakPoints(document, "de");

        assertThat(document.selectFirst("table").hasAttr("lang")).as("The table keeps the language of its section").isFalse();
        assertThat(document.select("wbr")).isEmpty();
        assertThat(document.selectFirst("td").attr("style")).isEqualTo("hyphens:auto;");
    }

    @Test
    void keepsTheBreakPointsOfAWordInALanguageWithNoDictionary() {
        // WeasyPrint has no dictionary to hyphenate Japanese, so the word would not break at all
        Document document = Jsoup.parse("<table><tr><td>" + "ABCDEFGHIJ".repeat(5) + "</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document, "ja");

        assertThat(document.select("wbr")).hasSize(2);
        assertThat(document.selectFirst("td").hasAttr("style")).isFalse();
    }

    @Test
    void keepsTheBreakPointsOfAWordWhereAnElementAroundTurnsHyphenationOff() {
        Document document = Jsoup.parse("<div style=\"hyphens: none\"><table><tr><td>" + "ABCDEFGHIJ".repeat(5) + "</td></tr></table></div>");

        LongWordsAdjuster.addBreakPoints(document, "de");

        assertThat(document.select("wbr")).as("A word which may not hyphenate breaks at its break points").hasSize(2);
        assertThat(document.selectFirst("td").hasAttr("style")).isFalse();
    }

    @Test
    void keepsTheBreakPointsOfAWordWhichARuleOfTheCssKeepsFromHyphenating() {
        Document document = Jsoup.parse("<h1>Grundstücksverkehrsgenehmigungszuständigkeitsübertragungsverordnung</h1>"
                + "<table class=\"plain\"><tr><td>Grundstücksverkehrsgenehmigungszuständigkeitsübertragungsverordnung</td></tr></table>"
                + "<table><tr><td>Grundstücksverkehrsgenehmigungszuständigkeitsübertragungsverordnung</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document, new Hyphenation("de", List.of("h1", "table.plain td")));

        List<Element> cells = document.select("td");
        assertThat(cells.get(0).select("wbr")).as("The rule reaches the cell, so its word keeps its break points").hasSize(3);
        assertThat(cells.get(1).select("wbr")).as("A rule for headings leaves the cells of a table to hyphenate").isEmpty();
        assertThat(cells.get(1).attr("style")).isEqualTo("hyphens:auto;");
    }

    @Test
    void matchesTheRulesOfTheCssAsThePdfLaysTheDocumentOut() {
        // The template wraps the document in a div.content, which a rule of a style package names
        String table = "<table><tr><td>Grundstücksverkehrsgenehmigungszuständigkeitsübertragungsverordnung</td></tr></table>";
        Document inside = Jsoup.parse(table);
        Document wrapper = Jsoup.parse(table);

        LongWordsAdjuster.addBreakPoints(inside, new Hyphenation("de", List.of(".content td")));
        LongWordsAdjuster.addBreakPoints(wrapper, new Hyphenation("de", List.of(".content")));

        assertThat(inside.select("wbr")).as("The rule reaches the cell inside the wrapper").hasSize(3);
        assertThat(wrapper.select("wbr")).as("The rule reaches the cell through the wrapper").hasSize(3);
        assertThat(wrapper.selectFirst("table").hasAttr("data-pdf-exporter-no-hyphenation")).as("The mark outlives a split of the document").isTrue();
        assertThat(wrapper.select("div.content")).as("The wrapper is gone again").isEmpty();
    }

    @Test
    void matchesARuleWhichNamesAnElementByItsPlaceAfterTheHeaderAndTheFooter() {
        // In the PDF the header comes first, so a rule for the first div of the body does not reach the document
        Document document = Jsoup.parse("<table><tr><td>Grundstücksverkehrsgenehmigungszuständigkeitsübertragungsverordnung</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document, new Hyphenation("de", List.of("body > div:first-child"), "<div class='header'></div><div class='footer'></div>"));

        assertThat(document.select("wbr")).isEmpty();
        assertThat(document.selectFirst("td").attr("style")).isEqualTo("hyphens:auto;");
        assertThat(document.body().children()).as("The header and the footer are gone again").extracting(Element::tagName).containsExactly("table");
    }

    @Test
    void leavesATableOfADocumentWithoutALanguageUnmarked() {
        Document document = Jsoup.parse("<table><tr><td>" + "ABCDEFGHIJ".repeat(5) + "</td></tr></table>");

        LongWordsAdjuster.addBreakPoints(document, Hyphenation.NONE);

        assertThat(document.selectFirst("table").hasAttr("lang")).isFalse();
        assertThat(document.select("wbr")).as("Without a dictionary only break points break the word").hasSize(2);
    }
}

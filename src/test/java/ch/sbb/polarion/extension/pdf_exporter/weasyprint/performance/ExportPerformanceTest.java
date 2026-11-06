package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * Documents of the shapes which make an export slow, each timed against a reference time of the exporter and of WeasyPrint.
 * <p>
 * The reference times of each part are in {@code performance/reference-times-<architecture>.properties}: three cells of 10,000
 * characters take WeasyPrint under a second, and took it minutes when the cells could break anywhere (#1101). The small
 * document, the large table, the page breaks and the long cells are the basket, which the run timed first.
 * </p>
 */
class ExportPerformanceTest extends BasePerformanceTest {

    private static final String LANGUAGE_FIELD = "docLanguage";

    /**
     * A small document, which takes the exporter little but what every export costs: a cost added to every export, as a
     * parse of the CSS with its fonts (#1139), shows here as a multiple of its time. It is judged against the fixed piece
     * of JDK work, the other documents against the basket it belongs to.
     */
    @Test
    void exportsASmallDocument() {
        Timing timing = export(SMALL_DOCUMENT);

        assertThat(pageCount(timing.pdf())).isEqualTo(1);
        assertWithinReference(timing);
    }

    /**
     * The basket as a whole, against the fixed piece of JDK work: the other exports are scaled by the basket, so a
     * regression of every large document slows the basket as well and shows only here.
     */
    @Test
    void exportsTheBasketAsTheFixedWorkExpects() {
        assertWithinReference(wholeBasket());
    }

    @Test
    void exportsALargeTable() {
        Timing timing = export(LARGE_TABLE);

        assertThat(pageCount(timing.pdf())).isGreaterThan(10);
        assertWithinReference(timing);
    }

    @Test
    void exportsCellsRunningAcrossPages() {
        Timing timing = export(LONG_CELLS);

        assertThat(pageCount(timing.pdf())).isGreaterThan(5);
        assertWithinReference(timing);
    }

    @Test
    void exportsManyImages() {
        Timing timing = export("manyImages", "Many images", Documents.manyImages(80), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThan(5);
        assertWithinReference(timing);
    }

    @Test
    void exportsManyWorkItems() {
        Timing timing = export("manyWorkItems", "Many work items", Documents.manyWorkItems(300), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThan(20);
        assertWithinReference(timing);
    }

    @Test
    void exportsSectionsWhichPageBreaksTurn() {
        Timing timing = export(PAGE_BREAK_SECTIONS);

        assertThat(pageCount(timing.pdf())).isGreaterThanOrEqualTo(40);
        assertWithinReference(timing);
    }

    @Test
    void exportsCrampedTables() {
        Timing timing = export("crampedTables", "Cramped tables", Documents.crampedTables(30), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThan(5);
        assertWithinReference(timing);
    }

    @Test
    void exportsHyphenatedTablesInADocumentWithALanguage() {
        lenient().when(module.getCustomField(LANGUAGE_FIELD)).thenReturn("de");
        ExportParams params = portraitA4().fitToPage(true).languageCustomField(LANGUAGE_FIELD).build();

        Timing timing = export("hyphenatedTables", "Silbentrennung", Documents.hyphenatedTables(30), params);

        assertThat(pageCount(timing.pdf())).isGreaterThan(5);
        assertWithinReference(timing);
    }

    /**
     * A large document of images and diagrams: what every export costs is a small part of it, and a cost which grows with
     * the document shows, as a search through its embedded images once did (#1141).
     */
    @Test
    void exportsALargeDocumentWithImagesAndDiagrams() {
        Timing timing = export("largeDocument", "A large document", Documents.largeDocument(250), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThan(300);
        assertWithinReference(timing);
    }
}

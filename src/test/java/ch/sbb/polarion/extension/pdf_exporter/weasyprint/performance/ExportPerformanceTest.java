package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * Documents of the shapes which make an export slow, each timed against a budget of the exporter and of WeasyPrint.
 * <p>
 * The budgets, in ms, are twice what each part took on the machine of {@link BasePerformanceTest}: three cells of
 * 10,000 characters take WeasyPrint under a second, and took it minutes when the cells could break anywhere (#1101).
 * </p>
 */
class ExportPerformanceTest extends BasePerformanceTest {

    private static final String LANGUAGE_FIELD = "docLanguage";

    /**
     * A small document, which takes the exporter little but what every export costs: a cost added to every export, as a
     * parse of the CSS with its fonts (#1139), shows here as a multiple of its time.
     */
    @Test
    void exportsASmallDocument() {
        Timing timing = fastestOfThree("reference", "Reference", readHtmlResource("performance/reference"), portraitA4().build());

        assertThat(pageCount(timing.pdf())).isEqualTo(1);
        assertWithinBudget(timing, 900, 2_500);
    }

    @Test
    void exportsALargeTable() {
        Timing timing = export("largeTable", "A large table", Documents.largeTable(400), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThan(10);
        assertWithinBudget(timing, 1_300, 10_500);
    }

    @Test
    void exportsCellsRunningAcrossPages() {
        Timing timing = export("longCells", "Long cells", Documents.longCells(3, 10_000), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThan(5);
        assertWithinBudget(timing, 900, 3_000);
    }

    @Test
    void exportsManyImages() {
        Timing timing = export("manyImages", "Many images", Documents.manyImages(80), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThan(5);
        assertWithinBudget(timing, 1_700, 2_800);
    }

    @Test
    void exportsManyWorkItems() {
        Timing timing = export("manyWorkItems", "Many work items", Documents.manyWorkItems(300), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThan(20);
        assertWithinBudget(timing, 1_400, 6_300);
    }

    @Test
    void exportsSectionsWhichPageBreaksTurn() {
        Timing timing = export("pageBreakSections", "Page breaks", Documents.pageBreakSections(40), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThanOrEqualTo(40);
        assertWithinBudget(timing, 1_300, 10_600);
    }

    @Test
    void exportsCrampedTables() {
        Timing timing = export("crampedTables", "Cramped tables", Documents.crampedTables(30), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThan(5);
        assertWithinBudget(timing, 2_000, 10_000);
    }

    @Test
    void exportsHyphenatedTablesInADocumentWithALanguage() {
        lenient().when(module.getCustomField(LANGUAGE_FIELD)).thenReturn("de");
        ExportParams params = portraitA4().fitToPage(true).languageCustomField(LANGUAGE_FIELD).build();

        Timing timing = export("hyphenatedTables", "Silbentrennung", Documents.hyphenatedTables(30), params);

        assertThat(pageCount(timing.pdf())).isGreaterThan(5);
        assertWithinBudget(timing, 900, 4_300);
    }
}

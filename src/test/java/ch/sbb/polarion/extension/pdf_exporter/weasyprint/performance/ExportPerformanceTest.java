package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * Documents of the shapes which make an export slow, each timed against a budget of the exporter and of WeasyPrint.
 * <p>
 * The budgets, in ms, are some ten times what each part took on the machine of {@link BasePerformanceTest}: three cells
 * of 10,000 characters take WeasyPrint under a second, and took it minutes when the cells could break anywhere (#1101).
 * </p>
 */
class ExportPerformanceTest extends BasePerformanceTest {

    private static final String LANGUAGE_FIELD = "docLanguage";

    @Test
    void exportsALargeTable() {
        Timing timing = export("A large table", Documents.largeTable(400), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThan(10);
        assertWithinBudget("largeTable", timing, 7_000, 45_000);
    }

    @Test
    void exportsCellsRunningAcrossPages() {
        Timing timing = export("Long cells", Documents.longCells(3, 10_000), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThan(5);
        assertWithinBudget("longCells", timing, 5_000, 10_000);
    }

    @Test
    void exportsManyImages() {
        Timing timing = export("Many images", Documents.manyImages(80), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThan(5);
        assertWithinBudget("manyImages", timing, 8_000, 7_000);
    }

    @Test
    void exportsManyWorkItems() {
        Timing timing = export("Many work items", Documents.manyWorkItems(300), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThan(20);
        assertWithinBudget("manyWorkItems", timing, 7_000, 21_000);
    }

    @Test
    void exportsSectionsWhichPageBreaksTurn() {
        Timing timing = export("Page breaks", Documents.pageBreakSections(40), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThanOrEqualTo(40);
        assertWithinBudget("pageBreakSections", timing, 7_000, 48_000);
    }

    @Test
    void exportsCrampedTables() {
        Timing timing = export("Cramped tables", Documents.crampedTables(30), portraitA4().fitToPage(true).build());

        assertThat(pageCount(timing.pdf())).isGreaterThan(5);
        assertWithinBudget("crampedTables", timing, 15_000, 43_000);
    }

    @Test
    void exportsHyphenatedTablesInADocumentWithALanguage() {
        lenient().when(module.getCustomField(LANGUAGE_FIELD)).thenReturn("de");
        ExportParams params = portraitA4().fitToPage(true).languageCustomField(LANGUAGE_FIELD).build();

        Timing timing = export("Silbentrennung", Documents.hyphenatedTables(30), params);

        assertThat(pageCount(timing.pdf())).isGreaterThan(5);
        assertWithinBudget("hyphenatedTables", timing, 5_000, 13_000);
    }
}

package ch.sbb.polarion.extension.pdf_exporter.weasyprint.performance;

import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.CommentsRenderType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.HeaderFooterModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * One document holding every kind of content the exporter handles, exported with every option a style package sets: a
 * cover page, a header and footer of the first page, a watermark, comments, the language of the document and fit to page.
 * Its pages are compared with reference images, so that one export shows whether any of it broke, and the export is
 * timed against its reference time.
 * <p>
 * The document is stitched together from the fixtures of the integration tests: the tables of figures and tables, text,
 * lists and comments, tables with no room for their words, URLs and IDs, a table in German, special symbols, a work item
 * kept on one page, a wide image, a drawing, a landscape page and tables in several scripts.
 * </p>
 */
class FeatureDocumentTest extends BasePerformanceTest {

    private static final String LANGUAGE_FIELD = "docLanguage";

    private static final int PAGES = 16;

    @Test
    void exportsEveryFeatureAsBefore() {
        when(headerFooterSettings.load(any(), eq(SettingId.fromName("running")))).thenReturn(HeaderFooterModel.builder()
                .useCustomValues(true)
                .headerLeft("Every feature").headerCenter("{{ DOCUMENT_TITLE }}").headerRight("Revision {{ REVISION }}")
                .footerLeft("Performance test").footerCenter("").footerRight("Page {{ PAGE_NUMBER }} of {{ PAGES_TOTAL_COUNT }}")
                .build());
        when(headerFooterSettings.load(any(), eq(SettingId.fromName("title")))).thenReturn(HeaderFooterModel.builder()
                .useCustomValues(true)
                .headerLeft("").headerCenter("The first page").headerRight("")
                .footerLeft("").footerCenter("Confidential").footerRight("")
                .build());
        lenient().when(module.getCustomField(LANGUAGE_FIELD)).thenReturn("en");
        ExportParams params = portraitA4()
                .coverPage("cover")
                .headerFooter("running")
                .firstPageHeaderFooter("title")
                .watermark(true)
                .renderComments(CommentsRenderType.ALL)
                .languageCustomField(LANGUAGE_FIELD)
                .fitToPage(true)
                .build();

        Timing timing = export("featureDocument", "Every feature of an export", readHtmlResource("performance/featureDocument"), params);

        // Counted first, as a page which no reference image has stops the comparison
        assertThat(pageCount(timing.pdf())).as("The pages the document runs to").isEqualTo(PAGES);
        assertFalse(compareContentUsingReferenceImages("featureDocument", timing.pdf()), "The pages differ from the reference images");
        assertWithinReference(timing);
    }
}

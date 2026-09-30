package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.css.CssModel;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import com.polarion.alm.tracker.model.IModule;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * An icon sits in the middle of the line it is in, next to the text it stands for.
 * <p>
 * The icons of enum values (a status, a severity) come with an inline "vertical-align: bottom" from Polarion, the icon
 * of a link to a document with the class "polarion-Icons". A line taller than the icon, as a custom CSS makes it with a
 * larger line height, used to leave them at its bottom, below their text.
 * </p>
 */
class IconAlignmentTest extends BasePdfConverterTest {

    /** The words each icon of the document stands before: a link to a document, a severity, a status, a status in a table. */
    private static final List<String> WORDS_AFTER_ICONS = List.of("Product", "Should", "Reviewed", "Draft");

    /** How far the middle of an icon may be from the middle of its text: the icon is drawn by whole pixels. */
    private static final float TOLERANCE_PT = 1.5f;

    @Test
    void alignsIconsWithTheirTextInALineOfTheDefaultHeight() {
        assertIconsInTheMiddleOfTheirLines(getCurrentMethodName(), "");
    }

    @Test
    void alignsIconsWithTheirTextInALineTwiceAsHigh() {
        assertIconsInTheMiddleOfTheirLines(getCurrentMethodName(), "body, td, p, div { line-height: 2 !important; }");
    }

    private void assertIconsInTheMiddleOfTheirLines(@NotNull String testName, @NotNull String customCss) {
        when(cssSettings.load(any(), any())).thenReturn(CssModel.builder()
                .disableDefaultCss(false)
                .css(readCssResource(CSS_BASIC, FONT_REGULAR) + customCss)
                .build());
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .build();
        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Icons in a line")
                .content(readHtmlResource("iconsInLine"))
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        byte[] pdf = converter.convertToPdf(params, null);

        List<DrawnImages.Box> icons = DrawnImages.boxesIn(pdf);
        assertThat(icons).hasSize(WORDS_AFTER_ICONS.size());
        List<TextPosition> words = firstLettersOf(pdf);
        for (int icon = 0; icon < icons.size(); icon++) {
            TextPosition word = words.get(icon);
            float textMiddle = word.getYDirAdj() - word.getHeightDir() / 2;
            assertThat(icons.get(icon).middle()).as("The icon before \"%s\" is in the middle of its line", WORDS_AFTER_ICONS.get(icon))
                    .isCloseTo(textMiddle, within(TOLERANCE_PT));
        }
        assertFalse(compareContentUsingReferenceImages(testName, pdf), "The pages differ from the reference images");
    }

    /** The first letter of each word an icon stands before, in the order of the document. */
    @SneakyThrows
    private @NotNull List<TextPosition> firstLettersOf(byte @NotNull [] pdf) {
        List<TextPosition> letters = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    for (String word : WORDS_AFTER_ICONS) {
                        int at = text.indexOf(word);
                        if (at >= 0 && letters.size() < WORDS_AFTER_ICONS.size() && WORDS_AFTER_ICONS.get(letters.size()).equals(word)) {
                            letters.add(positions.get(at));
                        }
                    }
                }
            };
            stripper.getText(document);
        }
        assertThat(letters).as("Each word an icon stands before is printed").hasSize(WORDS_AFTER_ICONS.size());
        return letters;
    }
}

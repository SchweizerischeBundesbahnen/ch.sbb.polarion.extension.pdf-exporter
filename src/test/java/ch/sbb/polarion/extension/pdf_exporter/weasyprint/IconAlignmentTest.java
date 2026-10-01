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
import java.util.HashMap;
import java.util.Map;
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

    /**
     * A square icon of 16 pixels, as Polarion draws them. A PNG, as Polarion's icons are bitmaps: the conversion service turns every SVG into a PNG first, which
     * takes it about half a second an image.
     */
    private static final String ICON = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAAAF0lEQVR4nGM4w8BAEiJN9aiGUQ1DSgMAQWfMAdovJBMAAAAASUVORK5CYII=";

    /** Each kind of lines starts a page of its own, so that none of them is split between two pages. */
    private static final String PAGE_BREAK = "<div style=\"break-before: page;\"></div>";

    /** Work items enough of each kind for a difference of their heights to add up. */
    private static final int LINES = 12;

    /** How much higher a work item with an icon may be than one without. */
    private static final float LINE_TOLERANCE_PT = 0.25f;

    /** The room between the icons of two lines of a list: a pixel above each and below each. */
    private static final float MIN_ROOM_BETWEEN_ICONS_PT = 1.5f;

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

    /**
     * Centered, an icon of 16 pixels reaches below the text of a line of the default height, and the line grows. In a
     * long document the lines with icons add up, and the pages break at other places than they did with the icons at
     * the bottom of their lines.
     */
    @Test
    void keepsALineWithAnIconAsHighAsALineWithout() {
        String linkIcon = "<span style=\"white-space:nowrap;\"><img src=\"" + ICON + "\" class=\"polarion-Icons\"/></span>";
        String enumIcon = "<img style=\"vertical-align:bottom; border:0px; margin-right:2px;\" src=\"" + ICON + "\"/>";
        StringBuilder content = new StringBuilder();
        for (int line = 1; line <= LINES; line++) {
            content.append(workItem("L" + line + "L", "<a class=\"polarion-Hyperlink\" href=\"#\">" + linkIcon + "Planning</a>"));
        }
        content.append(PAGE_BREAK);
        for (int line = 1; line <= LINES; line++) {
            content.append(workItem("S" + line + "S", "<span class=\"polarion-JSEnumOption\" title=\"Should Have\">" + enumIcon + "Should Have</span>"));
        }
        content.append(PAGE_BREAK);
        for (int line = 1; line <= LINES; line++) {
            content.append(workItem("P" + line + "P", "<span>Should Have</span>"));
        }

        byte[] pdf = export(content.toString(), readFontCss());

        float plain = lineHeight(pdf, "P");
        assertThat(lineHeight(pdf, "L")).as("A line with the icon of a link is as high as a line of text").isCloseTo(plain, within(LINE_TOLERANCE_PT));
        assertThat(lineHeight(pdf, "S")).as("A line with the icon of an enum value is as high as a line of text").isCloseTo(plain, within(LINE_TOLERANCE_PT));
    }

    /**
     * A list keeps its lines as high as their text, lower than an icon, so the icons of a list of links to documents
     * stood one on another with no room between them.
     */
    @Test
    void leavesRoomBetweenTheIconsOfAList() {
        String item = "<li><span class=\"polarion-rte-link\"><a class=\"polarion-Hyperlink\" href=\"#\"><span style=\"white-space:nowrap;\">"
                + "<img src=\"" + ICON + "\" class=\"polarion-Icons\"/></span>Specification</a></span></li>";

        byte[] pdf = export("<ul>" + item.repeat(3) + "</ul>", readFontCss());

        List<DrawnImages.Box> icons = DrawnImages.boxesIn(pdf);
        assertThat(icons).hasSize(3);
        for (int icon = 1; icon < icons.size(); icon++) {
            DrawnImages.Box above = icons.get(icon - 1);
            assertThat(icons.get(icon).top() - (above.top() + above.height())).as("Room between icon %d and the one above it", icon + 1)
                    .isGreaterThanOrEqualTo(MIN_ROOM_BETWEEN_ICONS_PT);
        }
    }

    private void assertIconsInTheMiddleOfTheirLines(@NotNull String testName, @NotNull String customCss) {
        byte[] pdf = export(readHtmlResource("iconsInLine"), readFontCss() + customCss);

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

    /** Exports the content with the default CSS and the given CSS after it. */
    private byte @NotNull [] export(@NotNull String content, @NotNull String css) {
        when(cssSettings.load(any(), any())).thenReturn(CssModel.builder()
                .disableDefaultCss(false)
                .css(css)
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
                .content(content)
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);

        return converter.convertToPdf(params, null);
    }

    /** A work item as Polarion renders it: its title on one line, its fields in brackets on the next. */
    private static @NotNull String workItem(@NotNull String mark, @NotNull String field) {
        return """
                <div class="polarion-dle-workitem-basic-0 polarion-dle-workitem-basic-internal"><span class="polarion-dle-workitem-title">\
                <span class="polarion-dle-workitem-fields-start">%s -&thinsp;</span>A work item<br/></span>\
                <span class="polarion-dle-workitem-fields-end"><span class="polarion-dle-workitem-fields-end-inner">&thinsp;<b>[</b>%s<b>]</b></span></span></div>
                """.formatted(mark, field);
    }

    /** The height each work item marked with the given letter takes, from the first of them to the last. */
    @SneakyThrows
    private float lineHeight(byte @NotNull [] pdf, @NotNull String letter) {
        Map<String, Float> tops = new HashMap<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    for (int line : new int[]{1, LINES}) {
                        int at = text.indexOf(letter + line + letter);
                        if (at >= 0) {
                            tops.put(letter + line, positions.get(at).getYDirAdj());
                        }
                    }
                }
            };
            stripper.getText(document);
        }
        assertThat(tops).as("The first and the last line marked %s are printed", letter).hasSize(2);
        return (tops.get(letter + LINES) - tops.get(letter + 1)) / (LINES - 1);
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

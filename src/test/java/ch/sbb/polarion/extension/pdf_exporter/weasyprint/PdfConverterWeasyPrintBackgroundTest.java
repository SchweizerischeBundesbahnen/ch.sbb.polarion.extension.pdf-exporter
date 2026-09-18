package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.coverpage.CoverPageModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.css.CssModel;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import com.polarion.alm.tracker.model.IModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A page background set with {@code @page}, and on which pages of the PDF it shows.
 * <p>
 * The cover page is converted on its own, with the CSS of the cover page only, and replaces the first page of the
 * document. So a background in the CSS of the cover page shows on the cover page alone, and a background in the CSS
 * of the export on the pages of the document alone.
 * </p>
 */
class PdfConverterWeasyPrintBackgroundTest extends BasePdfConverterTest {

    private static final String BACKGROUND_RULE = "@page { background: url('%s') no-repeat center; background-size: cover; }";
    private static final String SERVER_BACKGROUND_PATH = "/polarion/ria/images/test-background.png";
    private static final String UNREACHABLE_BACKGROUND_URL = "https://images.example.com/background.png";
    /**
     * A rule naming an address the extension cannot account for: the parser reads no url term here, because a
     * url written without quotes ends at the bracket inside it. The address is taken out of the text instead,
     * and the selector matches nothing in the document, so the pages must come out as they do without it.
     */
    private static final String RULE_WITH_AN_ADDRESS_NOTHING_ACCOUNTS_FOR =
            " .no-element-carries-this { background: url(https://images.example.com/x.png?a=(b)); }";
    /** A rule the parser cannot read at all, which makes it refuse the stylesheet it stands in. */
    private static final String RULE_WHICH_CANNOT_BE_PARSED =
            " .no-element-carries-this { background: url('https://images.example.com/x.png); }";
    private static final String BACKGROUND_DATA_URL = solidPngDataUrl(new Color(0xCC, 0xE5, 0xFF));

    @Test
    void testBackgroundInCoverPageCss() {
        ExportParams params = exportParams(true);
        useCoverPageCss(background(BACKGROUND_DATA_URL));

        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    void testBackgroundInCssWithCoverPage() {
        ExportParams params = exportParams(true);
        useCss(false, background(BACKGROUND_DATA_URL));

        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    void testBackgroundFromServerPathInCss() {
        ExportParams params = exportParams(false);
        // the extension reads a path on the Polarion server itself and embeds what it read
        when(fileResourceProvider.getResourceAsBase64String(anyString()))
                .thenAnswer(invocation -> SERVER_BACKGROUND_PATH.equals(invocation.getArgument(0)) ? BACKGROUND_DATA_URL : null);
        useCss(false, background(SERVER_BACKGROUND_PATH));

        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
        verify(fileResourceProvider, atLeastOnce()).getResourceAsBase64String(SERVER_BACKGROUND_PATH);
    }

    @Test
    void testBackgroundWhichCannotBeLoaded() {
        ExportParams params = exportParams(false);
        // the provider loads nothing for the address: the export goes on without it
        useCss(false, background(UNREACHABLE_BACKGROUND_URL));

        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
        verify(fileResourceProvider, atLeastOnce()).getResourceAsBase64String(UNREACHABLE_BACKGROUND_URL);
    }

    @Test
    void testBackgroundInCustomCssOnly() {
        ExportParams params = exportParams(false);
        useCss(true, background(BACKGROUND_DATA_URL));

        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    void testStylesApplyAlthoughAnAddressCouldNotBeChecked() {
        ExportParams params = exportParams(false, "A url() the parser reads as no url of its own",
                "The stylesheet also says background: url(https://images.example.com/x.png?a=(b)), which the"
                        + " parser reads as no url term, because a url written without quotes ends at the bracket"
                        + " inside it. That address is replaced where it stands. The page background below and"
                        + " this text are what the rest of the stylesheet says, and they have to be here.");
        useCss(false, background(BACKGROUND_DATA_URL) + RULE_WITH_AN_ADDRESS_NOTHING_ACCOUNTS_FOR);

        // such a stylesheet used to be dropped whole, which took every style on this page with it
        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    void testStylesApplyAlthoughTheStylesheetCannotBeParsed() {
        ExportParams params = exportParams(false, "A stylesheet the parser refuses",
                "The stylesheet also holds a rule with an unbalanced quote, which makes the parser refuse all"
                        + " of the text. Its addresses are taken out one by one instead. The page background"
                        + " below and this text are what the rest of the stylesheet says, and they have to be here.");
        useCss(false, background(BACKGROUND_DATA_URL) + RULE_WHICH_CANNOT_BE_PARSED);

        // a stylesheet the parser refuses is still one the renderer reads, and the page says so
        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    @Test
    void testCoverPageStylesApplyAlthoughAnAddressCouldNotBeChecked() {
        // the cover page is converted on its own, with the CSS of the cover page only, and it goes through the
        // same pass: what happens to a style package when one of its addresses cannot be checked happens here
        ExportParams params = exportParams(true, "A cover page whose stylesheet names such a url()",
                "The cover page in front of this one carries the same rule the document CSS carries in the test"
                        + " above. Its background is what its own stylesheet says, and it has to be there.");
        useCoverPageCss("<div>This cover page keeps the background its own stylesheet gives it,"
                        + " although that stylesheet names a url() nothing in it accounts for</div>",
                background(BACKGROUND_DATA_URL) + RULE_WITH_AN_ADDRESS_NOTHING_ACCOUNTS_FOR);

        boolean hasDiff = compareContentUsingReferenceImages(getCurrentMethodName(), converter.convertToPdf(params, null));
        assertFalse(hasDiff);
    }

    private ExportParams exportParams(boolean withCoverPage) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .coverPage(withCoverPage ? "test" : null)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Document with Background")
                .content("""
                        <h1>Page 1</h1>
                        <p>First page of the document.</p>
                        """ + PAGE_BREAK + """
                        <h1>Page 2</h1>
                        <p>Second page of the document.</p>
                        """)
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);
        return params;
    }

    /**
     * A document of one page which says what the test it belongs to is about. The references of these tests
     * are read by a person looking for what a stylesheet does to a page, and a page reading "Page 1" tells
     * that person nothing: the heading names the case and the paragraph names what has to be visible.
     */
    private ExportParams exportParams(boolean withCoverPage, String heading, String what) {
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .coverPage(withCoverPage ? "test" : null)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title(heading)
                .content("<h1>" + heading + "</h1>\n<p>" + what + "</p>\n")
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);
        return params;
    }

    private void useCss(boolean customCssOnly, String rule) {
        when(cssSettings.load(any(), any())).thenReturn(CssModel.builder()
                .disableDefaultCss(customCssOnly)
                .css(readCssResource(CSS_BASIC, FONT_REGULAR) + rule)
                .build());
    }

    private void useCoverPageCss(String rule) {
        useCoverPageCss("<div>Cover Page Title</div>", rule);
    }

    private void useCoverPageCss(String html, String rule) {
        lenient().when(coverPageSettings.load(any(), any())).thenReturn(CoverPageModel.builder()
                .useCustomValues(true)
                .templateHtml(html)
                .templateCss(readCssResource(CSS_BASIC, FONT_REGULAR) + rule)
                .build());
    }

    private static String background(String url) {
        return BACKGROUND_RULE.formatted(url);
    }

    @SneakyThrows
    private static String solidPngDataUrl(Color color) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
    }
}

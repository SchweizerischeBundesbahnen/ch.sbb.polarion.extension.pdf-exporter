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
        // nothing is loaded for the address, as for one the resource policy refuses: the export goes on without it
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

    private void useCss(boolean customCssOnly, String rule) {
        when(cssSettings.load(any(), any())).thenReturn(CssModel.builder()
                .disableDefaultCss(customCssOnly)
                .css(readCssResource(CSS_BASIC, FONT_REGULAR) + rule)
                .build());
    }

    private void useCoverPageCss(String rule) {
        lenient().when(coverPageSettings.load(any(), any())).thenReturn(CoverPageModel.builder()
                .useCustomValues(true)
                .templateHtml("<div>Cover Page Title</div>")
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

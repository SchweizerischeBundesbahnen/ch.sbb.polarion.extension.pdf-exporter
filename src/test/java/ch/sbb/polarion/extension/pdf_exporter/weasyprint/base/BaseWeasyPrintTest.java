package ch.sbb.polarion.extension.pdf_exporter.weasyprint.base;

import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.core.util.StringUtils;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class, PdfExporterExtensionConfigurationExtension.class})
@SkipTestWhenParamNotSet
public abstract class BaseWeasyPrintTest {

    public static final String IMPL_NAME_PARAM = "wpExporterImpl";
    public static final String WEASYPRINT_SERVICE_URL_PROPERTY = "weasyprint.service.url";
    public static final String PAGE_SUFFIX = "_page_";
    public static final String WEASYPRINT_TEST_RESOURCES_FOLDER = "/weasyprint/html/";
    public static final String WEASYPRINT_TEST_PNG_RESOURCES_FOLDER = "/weasyprint/png/";
    public static final String WEASYPRINT_TEST_CSS_RESOURCES_FOLDER = "/weasyprint/css/";
    public static final String WEASYPRINT_TEST_FONT_RESOURCES_FOLDER = "/weasyprint/font/";
    public static final String FONT_BASE64_REPLACE_PARAM = "{FONT_BASE64}";
    public static final String FONT_REGULAR = "OpenSans-Regular";

    /** Embeds Liberation Sans, the font of an export, and puts it on every element, so text is laid out the same wherever WeasyPrint runs. */
    private static final String CSS_LIBERATION_SANS = "liberationSans";

    /** The faces of Liberation Sans the stylesheet embeds, each where the stylesheet names it in braces. */
    private static final List<String> LIBERATION_SANS_FACES = List.of("LiberationSans-Regular", "LiberationSans-Bold", "LiberationSans-Italic", "LiberationSans-BoldItalic");

    protected static final String REPORTS_FOLDER_PATH = "target/surefire-reports/";
    protected static final String EXT_HTML = ".html";
    protected static final String EXT_PNG = ".png";
    protected static final String EXT_PDF = ".pdf";
    protected static final String EXT_CSS = ".css";
    protected static final String EXT_WOFF = ".woff";
    protected static final String EXT_TTF = ".ttf";

    private static final Logger logger = LoggerFactory.getLogger(BaseWeasyPrintTest.class);

    /** The name of this node Polarion reads, which spares the lookup it makes without it. */
    private static final String NODE_HOSTNAME_PROPERTY = "com.siemens.polarion.cluster.nodeHostname";

    static {
        // Without a base.url the export takes the name of this node for the base of its HTML. Polarion looks it up by a
        // reverse DNS query, which takes seconds on a machine whose name the resolver does not know. The tests run no
        // cluster, and the name means nothing to the container of WeasyPrint, so they give one.
        if (System.getProperty(NODE_HOSTNAME_PROPERTY) == null) {
            System.setProperty(NODE_HOSTNAME_PROPERTY, "localhost");
        }
    }

    @SneakyThrows
    @SuppressWarnings("ConstantConditions")
    public static String readHtmlResource(String resourceName) {
        return StringUtils.readToString(BaseWeasyPrintTest.class.getResourceAsStream(WEASYPRINT_TEST_RESOURCES_FOLDER + resourceName + EXT_HTML));
    }

    @SneakyThrows
    public static InputStream readPngResource(String resourceName) {
        return BaseWeasyPrintTest.class.getResourceAsStream(WEASYPRINT_TEST_PNG_RESOURCES_FOLDER + resourceName + EXT_PNG);
    }

    @SneakyThrows
    @SuppressWarnings("ConstantConditions")
    public static String readCssResource(String resourceName, String fontResourceName) {
        return StringUtils.readToString(BaseWeasyPrintTest.class.getResourceAsStream(WEASYPRINT_TEST_CSS_RESOURCES_FOLDER + resourceName + EXT_CSS))
                .replace(FONT_BASE64_REPLACE_PARAM, Base64.getEncoder().encodeToString(readFontResource(fontResourceName)));
    }

    /** The stylesheet which puts the font of an export, embedded, on every element. */
    @SneakyThrows
    @SuppressWarnings("ConstantConditions")
    public static String readFontCss() {
        String css = StringUtils.readToString(BaseWeasyPrintTest.class.getResourceAsStream(WEASYPRINT_TEST_CSS_RESOURCES_FOLDER + CSS_LIBERATION_SANS + EXT_CSS));
        for (String face : LIBERATION_SANS_FACES) {
            try (InputStream font = BaseWeasyPrintTest.class.getResourceAsStream(WEASYPRINT_TEST_FONT_RESOURCES_FOLDER + face + EXT_TTF)) {
                css = css.replace("{" + face + "}", Base64.getEncoder().encodeToString(font.readAllBytes()));
            }
        }
        return css;
    }

    @SneakyThrows
    public static byte[] readFontResource(String resourceName) {
        try (InputStream resourceAsStream = BaseWeasyPrintTest.class.getResourceAsStream(WEASYPRINT_TEST_FONT_RESOURCES_FOLDER + resourceName + EXT_WOFF)) {
            if (resourceAsStream != null) {
                return resourceAsStream.readAllBytes();
            }
        }
        throw new IllegalArgumentException("Cannot load font " + resourceName);
    }

    protected byte[] exportToPdf(String html, @NotNull WeasyPrintOptions weasyPrintOptions) {
        WeasyPrintServiceConnector weasyPrintServiceConnector = getWeasyPrintServiceConnector();
        return weasyPrintServiceConnector.convertToPdf(html, weasyPrintOptions);
    }

    protected byte[] exportToPdf(String html, @NotNull WeasyPrintOptions weasyPrintOptions, @NotNull DocumentData<? extends IUniqueObject> documentData) {
        WeasyPrintServiceConnector weasyPrintServiceConnector = getWeasyPrintServiceConnector();
        return weasyPrintServiceConnector.convertToPdf(html, weasyPrintOptions, documentData);
    }

    /**
     * Returns the WeasyPrintServiceConnector using either an externally configured WeasyPrint service
     * (via {@value #WEASYPRINT_SERVICE_URL_PROPERTY} system property, e.g. {@code http://localhost:9080})
     * or the shared Testcontainers instance as a fallback.
     */
    public static @NotNull WeasyPrintServiceConnector getWeasyPrintServiceConnector() {
        return new WeasyPrintServiceConnector(getWeasyPrintServiceUrl());
    }

    /** The address of the WeasyPrint service the exports of the tests go to, as {@link #getWeasyPrintServiceConnector()} chooses it. */
    public static @NotNull String getWeasyPrintServiceUrl() {
        String externalUrl = System.getProperty(WEASYPRINT_SERVICE_URL_PROPERTY);
        if (externalUrl != null) {
            externalUrl = stripTrailingSlashes(externalUrl.trim());
            if (!externalUrl.isBlank()) {
                return externalUrl;
            }
        }

        GenericContainer<?> weasyPrintService = SharedWeasyPrintContainer.getInstance();
        assertTrue(weasyPrintService.isRunning(), "WeasyPrint container should be running");

        return "http://" + weasyPrintService.getHost() + ":" + weasyPrintService.getFirstMappedPort();
    }

    public static @NotNull String stripTrailingSlashes(@NotNull String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    protected List<BufferedImage> exportAndGetAsImages(String fileName) {
        return exportAndGetAsImages(fileName, readHtmlResource(fileName));
    }

    @SneakyThrows
    @NotNull
    protected List<BufferedImage> exportAndGetAsImages(String fileName, String html) {
        byte[] pdfBytes = exportToPdf(html, WeasyPrintOptions.builder().build());
        if (pdfBytes != null) {
            return getAllPagesAsImagesAndLogAsReports(fileName, pdfBytes);
        } else {
            logger.warn("No pdf file generated for name {}", fileName);
            return new ArrayList<>();
        }
    }

    @SneakyThrows
    protected List<BufferedImage> getAllPagesAsImagesAndLogAsReports(@NotNull String fileName, byte[] pdfBytes) {
        List<BufferedImage> result = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage image = MediaUtils.pdfPageToImage(doc, i);
                writeReportImage(String.format("%s%s%d", fileName, PAGE_SUFFIX, i), image); //write each page image to reports folder
                result.add(image);
            }
            return result;
        }
    }

    @SneakyThrows
    protected void writeReportImage(String resourceName, BufferedImage image) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(REPORTS_FOLDER_PATH + resourceName + EXT_PNG)) {
            fileOutputStream.write(MediaUtils.toPng(image));
        }
    }

    /**
     * Compares each page of the pdf with the reference image of the same name, and writes what differs into
     * the reports folder.
     * <p>
     * NOTE: if something changes in the future and the images are no longer identical,
     * simply copy &amp; replace the reference resource images with the new ones from the reports folder.
     * </p>
     */
    @SneakyThrows
    protected boolean compareContentUsingReferenceImages(String testName, byte[] pdf) {
        writeReportPdf(testName, "generated", pdf);
        List<BufferedImage> resultImages = getAllPagesAsImagesAndLogAsReports(testName, pdf);
        boolean hasDiff = false;
        for (int i = 0; i < resultImages.size(); i++) {
            BufferedImage expectedImage = ImageIO.read(readPngResource(testName + PAGE_SUFFIX + i));
            BufferedImage resultImage = resultImages.get(i);
            List<Point> diffPoints = MediaUtils.diffImages(expectedImage, resultImage);
            if (!diffPoints.isEmpty()) {
                MediaUtils.fillImagePoints(resultImage, diffPoints, Color.BLUE.getRGB());
                writeReportImage(String.format("%s%s%d_diff", testName, PAGE_SUFFIX, i), resultImage);
                hasDiff = true;
            }
        }
        return hasDiff;
    }

    @SneakyThrows
    protected void writeReportPdf(String testName, String fileSuffix, byte[] bytes) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(getReportFilePath(testName, fileSuffix))) {
            fileOutputStream.write(bytes);
        }
    }

    protected String getReportFilePath(String testName, String fileSuffix) {
        return REPORTS_FOLDER_PATH + testName + "_" + fileSuffix + EXT_PDF;
    }

    /**
     * Returns the calling method name
     */
    protected String getCurrentMethodName() {
        return Thread.currentThread().getStackTrace()[2].getMethodName();
    }
}

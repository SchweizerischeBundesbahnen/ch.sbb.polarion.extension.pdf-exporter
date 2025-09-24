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
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class, PdfExporterExtensionConfigurationExtension.class})
@SkipTestWhenParamNotSet
public abstract class BaseWeasyPrintTest {

    public static final String DOCKER_IMAGE_NAME = "ghcr.io/schweizerischebundesbahnen/weasyprint-service:latest";

    private static final ConcurrentHashMap<Class<?>, GenericContainer<?>> sharedContainers = new ConcurrentHashMap<>();

    public static final String IMPL_NAME_PARAM = "wpExporterImpl";
    public static final String PAGE_SUFFIX = "_page_";
    public static final String WEASYPRINT_TEST_RESOURCES_FOLDER = "/weasyprint/html/";
    public static final String WEASYPRINT_TEST_PNG_RESOURCES_FOLDER = "/weasyprint/png/";
    public static final String WEASYPRINT_TEST_CSS_RESOURCES_FOLDER = "/weasyprint/css/";
    public static final String WEASYPRINT_TEST_FONT_RESOURCES_FOLDER = "/weasyprint/font/";
    public static final String FONT_BASE64_REPLACE_PARAM = "{FONT_BASE64}";
    public static final String CSS_BASIC = "basic";
    public static final String FONT_REGULAR = "OpenSans-Regular";

    protected static final String REPORTS_FOLDER_PATH = "target/surefire-reports/";
    protected static final String EXT_HTML = ".html";
    protected static final String EXT_PNG = ".png";
    protected static final String EXT_PDF = ".pdf";
    protected static final String EXT_CSS = ".css";
    protected static final String EXT_WOFF = ".woff";

    private static final Logger logger = LoggerFactory.getLogger(BaseWeasyPrintTest.class);

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
        GenericContainer<?> weasyPrintService = getOrCreateSharedContainer();

        String weasyPrintServiceBaseUrl = "http://" + weasyPrintService.getHost() + ":" + weasyPrintService.getFirstMappedPort();
        WeasyPrintServiceConnector weasyPrintServiceConnector = new WeasyPrintServiceConnector(weasyPrintServiceBaseUrl);
        return weasyPrintServiceConnector.convertToPdf(html, weasyPrintOptions);
    }

    protected byte[] exportToPdf(String html, @NotNull WeasyPrintOptions weasyPrintOptions, @NotNull DocumentData<? extends IUniqueObject> documentData) {
        GenericContainer<?> weasyPrintService = getOrCreateSharedContainer();

        String weasyPrintServiceBaseUrl = "http://" + weasyPrintService.getHost() + ":" + weasyPrintService.getFirstMappedPort();
        WeasyPrintServiceConnector weasyPrintServiceConnector = new WeasyPrintServiceConnector(weasyPrintServiceBaseUrl);
        return weasyPrintServiceConnector.convertToPdf(html, weasyPrintOptions, documentData);
    }

    protected List<BufferedImage> exportAndGetAsImages(String fileName) {
        return exportAndGetAsImages(fileName, readHtmlResource(fileName));
    }

    @SneakyThrows
    @NotNull
    protected List<BufferedImage> exportAndGetAsImages(String fileName, String html) {
        byte[] pdfBytes = exportToPdf(html, new WeasyPrintOptions());
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

    /**
     * Gets or creates a shared Docker container for WeasyPrint service.
     * Container is reused across all tests in the same test class to improve performance.
     */
    private GenericContainer<?> getOrCreateSharedContainer() {
        Class<?> testClass = this.getClass();
        return sharedContainers.computeIfAbsent(testClass, k -> {
            GenericContainer<?> container = new GenericContainer<>(DOCKER_IMAGE_NAME)
                    .withImagePullPolicy(PullPolicy.alwaysPull())
                    .withExposedPorts(9080)
                    .waitingFor(Wait.forHttp("/version").forPort(9080));
            container.start();
            assertTrue(container.isRunning());

            // Register shutdown hook to stop container when JVM exits
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (container.isRunning()) {
                    container.stop();
                }
                sharedContainers.remove(testClass);
            }));

            return container;
        });
    }
}

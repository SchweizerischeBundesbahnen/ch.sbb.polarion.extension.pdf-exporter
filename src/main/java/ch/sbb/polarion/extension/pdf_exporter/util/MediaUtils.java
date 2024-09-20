package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@UtilityClass
public class MediaUtils {
    public static final String IMG_SRC_REGEX = "<img[^<>]*src=(\"|')(?<url>[^(\"|')]*)(\"|')";
    public static final String URL_REGEX = "url\\(\\s*([\"'])?(?<url>.*?)\\1?\\s*\\)";
    public static final String RESOURCE_EXTENSION_REGEX = "^.*\\.(?<extension>[a-zA-Z\\d]{3,4})(?:[?&#]|$)";
    public static final String DATA_URL_PREFIX = "data:";
    private static final Logger logger = Logger.getLogger(MediaUtils.class);
    private static final int RIGHT_WHITE_AREA_PX = 30;
    private static final int PDF_TO_PNG_DPI = 72;
    private static final String IMG_FORMAT_PNG = "png";
    private static final String ALLOWED_FOLDER_FOR_BINARY_FILES = "/default/";
    private static final Tika tika = new Tika();

    private static final Map<String, String> CUSTOM_MIME_TYPES_MAP = Map.of(
            "cur", "image/x-icon",
            "woff", "application/font-woff",
            "ttf", "application/font-ttf"
    );

    @SneakyThrows
    public BufferedImage pdfPageToImage(PDDocument document, int page) {
        return new PDFRenderer(document).renderImageWithDPI(page, PDF_TO_PNG_DPI);
    }

    @SuppressWarnings("squid:S109") // ignore 8, 16 & 255 constants creation proposal
    public boolean checkAllRightPixelsAreWhite(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = width - RIGHT_WHITE_AREA_PX; x < width; x++) {
                int pixel = img.getRGB(x, y);
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                if (red != 255 || green != 255 || blue != 255) {
                    return false;
                }
            }
        }
        return true;
    }

    @SneakyThrows
    public byte[] toPng(BufferedImage image) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, IMG_FORMAT_PNG, os);
        return os.toByteArray();
    }

    public String getImageFormat(@NotNull String imagePath) {
        if (imagePath.endsWith(".gif")) {
            return "image/gif";
        } else if (imagePath.endsWith(".png")) {
            return "image/png";
        } else {
            return "image/jpeg";
        }
    }

    public boolean compareImages(BufferedImage image1, BufferedImage image2) {
        if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) {
            return false;
        }

        for (int y = 0; y < image1.getHeight(); y++) {
            for (int x = 0; x < image1.getWidth(); x++) {
                if (image1.getRGB(x, y) != image2.getRGB(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }

    @SneakyThrows
    public byte[] overwriteFirstPageWithTitle(byte[] destinationPdf, byte[] titlePdf) {
        ByteArrayOutputStream modifiedTitleOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream modifiedContentOutputStream = new ByteArrayOutputStream();
        try (PDDocument titleDoc = Loader.loadPDF(titlePdf);
             PDDocument contentDoc = Loader.loadPDF(destinationPdf)) {
            while (titleDoc.getNumberOfPages() > 1) { //remove all pages except the first one from title pdf
                titleDoc.removePage(1);
            }
            titleDoc.save(modifiedTitleOutputStream);
            contentDoc.removePage(0);
            contentDoc.save(modifiedContentOutputStream);
        }

        ByteArrayOutputStream resultOutputStream = new ByteArrayOutputStream();
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.addSource(new RandomAccessReadBuffer(modifiedTitleOutputStream.toByteArray()));
        merger.addSource(new RandomAccessReadBuffer(modifiedContentOutputStream.toByteArray()));
        merger.setDestinationStream(resultOutputStream);
        merger.mergeDocuments(null);

        return resultOutputStream.toByteArray();
    }

    @SuppressWarnings("java:S1168")
    public byte[] getBinaryFileFromJar(@NotNull String filePath) {
        if (filePath.contains("..") || !filePath.startsWith(ALLOWED_FOLDER_FOR_BINARY_FILES)) {
            throw new IllegalArgumentException("Attempt to read from restricted path: " + filePath);
        }
        try (InputStream is = ScopeUtils.class.getClassLoader().getResourceAsStream(filePath)) {
            return is != null ? is.readAllBytes() : null;
        } catch (IOException e) {
            logger.error("Error reading template image content from: " + filePath, e);
            return null;
        }
    }

    @SuppressWarnings("java:S1168")
    public byte[] getBinaryFileFromSvn(@NotNull String path) {
        ILocation location = Location.getLocationWithRepository("default", path);
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            IRepositoryReadOnlyConnection readOnlyConnection = new PdfExporterPolarionService().getReadOnlyConnection(location);
            if (!readOnlyConnection.exists(location)) {
                logger.warn("Location does not exist: " + location.getLocationPath());
                return null;
            }

            try (InputStream inputStream = readOnlyConnection.getContent(location)) {
                return inputStream.readAllBytes();
            } catch (Exception e) {
                logger.error("Error reading content from: " + location.getLocationPath(), e);
                return null;
            }
        });
    }

    /**
     * Check whether particular string is a <a href="https://www.rfc-editor.org/rfc/rfc2397">'data' URL</a>-encoded entry.
     */
    public boolean isDataUrl(@Nullable String resourceUrl) {
        return resourceUrl != null && resourceUrl.startsWith(DATA_URL_PREFIX);
    }

    public String inlineBase64Resources(String content, FileResourceProvider fileResourceProvider) {
        RegexMatcher.IReplacementCalculator dataReplacement = engine -> {
            String url = engine.group("url");
            String base64String = MediaUtils.isDataUrl(url) ? url : fileResourceProvider.getResourceAsBase64String(url);
            return base64String == null ? null : engine.group().replace(url, base64String);
        };

        // replace tags like <img src="...
        String intermediateResult = RegexMatcher.get(IMG_SRC_REGEX).replace(content, dataReplacement);
        // replace CSS parameters like background: src('/polarion/...
        return RegexMatcher.get(URL_REGEX).useJavaUtil().replace(intermediateResult, dataReplacement);
    }

    public String removeSvgUnsupportedFeatureHint(String html) {
        return html.replaceAll("(?s)<switch>[^<]*?<g requiredFeatures=\"[^\"]+?\"/>.*?</switch>", "");
    }

    /**
     * Attempt to guess media type using resource name or its content.
     * <a href="https://www.iana.org/assignments/media-types/media-types.xhtml">More about media types.</a>
     *
     * @param resource      resource name or link address
     * @param resourceBytes content
     * @return media type or null if it's not recognized by given parameters
     */
    @SneakyThrows
    @Nullable
    @SuppressWarnings("squid:S1166") // no need to log or rethrow exception by design
    public String guessMimeType(@NotNull String resource, byte[] resourceBytes) {

        // there are several ways to recognize mime type, so we're going to try them all until positive result
        List<BiFunction<String, byte[], String>> mimeSources = Arrays.asList(
                MediaUtils::getMimeTypeUsingCustomRegex,
                MediaUtils::getMimeTypeUsingTikaByResourceName,
                MediaUtils::getMimeTypeUsingTikaByContent,
                MediaUtils::getMimeTypeUsingFilesProbe,
                MediaUtils::getMimeTypeUsingURLConnection
        );

        for (BiFunction<String, byte[], String> source : mimeSources) {
            try {
                String mimeType = source.apply(resource, resourceBytes);
                if (!StringUtils.isEmpty(mimeType)) {
                    return mimeType;
                }
            } catch (Exception e) {
                // ignore exceptions by design, no need to log their details, just proceed to the next attempt
            }
        }
        logger.error("Cannot get mime type for the resource: " + resource);
        return null;
    }

    private String getMimeTypeUsingCustomRegex(@NotNull String resource, byte[] resourceBytes) {
        return CUSTOM_MIME_TYPES_MAP.get(RegexMatcher.get(RESOURCE_EXTENSION_REGEX).findFirst(resource, engine -> engine.group("extension")).map(String::toLowerCase).orElse(""));
    }

    @SneakyThrows
    private String getMimeTypeUsingFilesProbe(@NotNull String resource, byte[] resourceBytes) {
        return Files.probeContentType(Paths.get(resource));
    }

    private String getMimeTypeUsingTikaByResourceName(@NotNull String resource, byte[] resourceBytes) {
        String detected = tika.detect(resource);
        return MimeTypes.OCTET_STREAM.equals(detected) ? null : detected; // ignore 'application/octet-stream' fallback
    }

    private String getMimeTypeUsingTikaByContent(@NotNull String resource, byte[] resourceBytes) {
        String detected = tika.detect(resourceBytes);
        return MimeTypes.OCTET_STREAM.equals(detected) ? null : detected; // ignore 'application/octet-stream' fallback
    }

    @SneakyThrows
    private String getMimeTypeUsingURLConnection(@NotNull String resource, byte[] resourceBytes) {
        try (InputStream is = new BufferedInputStream(new ByteArrayInputStream(resourceBytes))) {
            return URLConnection.guessContentTypeFromStream(is);
        }
    }
}

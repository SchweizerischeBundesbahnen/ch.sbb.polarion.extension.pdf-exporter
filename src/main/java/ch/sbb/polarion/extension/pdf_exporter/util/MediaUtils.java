package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.generic.util.BundleJarsPrioritizingRunnable;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.BiFunction;

import static ch.sbb.polarion.extension.pdf_exporter.util.TikaMimeTypeResolver.PARAM_RESULT;
import static ch.sbb.polarion.extension.pdf_exporter.util.TikaMimeTypeResolver.PARAM_VALUE;

@UtilityClass
public class MediaUtils {
    public static final String IMG_SRC_REGEX = "<img[^<>]*src=(\"|')(?<url>[^(\"|')]*)(\"|')";
    public static final String URL_REGEX = "(?i)url\\(\\s*([\"'])?(?<url>.*?)\\1?\\s*\\)";
    /**
     * What CSS allows between {@code @import} and its target: nothing, whitespace or a comment.
     */
    private static final String CSS_AT_RULE_SEPARATOR = "(?:\\s|/\\*[\\s\\S]*?\\*/)*";
    /**
     * {@code @import "..."} without the {@code url(...)} wrapper, which {@link #URL_REGEX} does not match.
     * The trailing {@code [^;]*} swallows a media condition, so the whole at-rule goes when it is dropped.
     */
    public static final String CSS_IMPORT_REGEX = "(?i)@import" + CSS_AT_RULE_SEPARATOR + "([\"'])(?<url>[^\"']+)\\1[^;]*;?";
    /**
     * {@code @import url(...)}. {@link #URL_REGEX} matches its url as well, but only this one can drop
     * the whole at-rule, media condition included.
     */
    public static final String CSS_IMPORT_URL_REGEX = "(?i)@import" + CSS_AT_RULE_SEPARATOR + "url\\((?<url>[^)]*)\\)[^;]*;?";
    public static final String DATA_URL_PREFIX = "data:";
    private static final String NETWORK_PATH_PREFIX = "//";
    private static final Pattern CSS_ESCAPE_PATTERN = Pattern.compile("\\\\(?:([0-9a-fA-F]{1,6})[ \\t\\r\\n\\f]?|(.))", Pattern.DOTALL);
    private static final String COMMENT_START = "/*";
    private static final String COMMENT_END = "*/";
    public static final String THUMBNAIL_PARAMETER = "thumbnail";
    /**
     * A 1x1 transparent PNG which replaces a resource the {@link ResourceUrlPolicy} rejected.
     */
    public static final String BLOCKED_RESOURCE_PLACEHOLDER =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
    private static final Logger logger = Logger.getLogger(MediaUtils.class);
    private static final int RIGHT_WHITE_AREA_PX = 30;
    private static final int PDF_TO_PNG_DPI = 300;
    private static final String IMG_FORMAT_PNG = "png";
    private static final String ALLOWED_FOLDER_FOR_BINARY_FILES = "/default/";

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

    public boolean sameImages(BufferedImage referenceImage, BufferedImage imageToCompare) {
        return diffImages(referenceImage, imageToCompare).isEmpty();
    }

    @SuppressWarnings("java:S3776") // ignore cognitive complexity complaint
    public List<Point> diffImages(BufferedImage referenceImage, BufferedImage imageToCompare) {
        List<Point> diffPoints = new ArrayList<>();
        int width = imageToCompare.getWidth();
        int height = imageToCompare.getHeight();
        if (referenceImage.getWidth() != imageToCompare.getWidth() || referenceImage.getHeight() != imageToCompare.getHeight()) {
            // when image size is different we return 1px border
            for (int x = 0; x < width; x++) {
                // Top edge
                diffPoints.add(new Point(x, 0));
                // Bottom edge
                diffPoints.add(new Point(x, height - 1));
            }
            for (int y = 1; y < height - 1; y++) {
                // Left edge
                diffPoints.add(new Point(0, y));
                // Right edge
                diffPoints.add(new Point(width - 1, y));
            }
        } else {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (referenceImage.getRGB(x, y) != imageToCompare.getRGB(x, y)) {
                        diffPoints.add(new Point(x, y));
                    }
                }
            }
        }

        return diffPoints;
    }

    public void fillImagePoints(BufferedImage image, List<Point> pointsToFill, int color) {
        for (Point point : pointsToFill) {
            image.setRGB(point.x, point.y, color);
        }
    }

    /**
     * Overwrites the first page of destination PDF with a cover page.
     * <p>
     * This method:
     * <ul>
     *     <li>Removes all pages except the first one from the cover page PDF</li>
     *     <li>Removes the first page from the destination PDF</li>
     *     <li>Merges the cover page with the remaining pages of destination PDF</li>
     *     <li>Applies PDF/A post-processing to fix compliance issues after merging</li>
     * </ul>
     *
     * @param destinationPdf the destination PDF bytes
     * @param firstPage      the cover page PDF bytes
     * @param pdfVariant     the PDF variant used for conversion (used for post-processing)
     * @return the merged PDF with the cover page as first page
     */
    @SneakyThrows
    public byte[] overwriteFirstPageWithTitle(byte[] destinationPdf, byte[] firstPage, @NotNull PdfVariant pdfVariant) {
        ByteArrayOutputStream modifiedTitleOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream modifiedContentOutputStream = new ByteArrayOutputStream();
        try (PDDocument titleDoc = Loader.loadPDF(firstPage);
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

        byte[] mergedPdf = resultOutputStream.toByteArray();

        // Apply PDF/A post-processing to fix compliance issues after merging
        return applyPdfAPostProcessing(mergedPdf, pdfVariant);
    }

    /**
     * Applies PDF/A post-processing to fix compliance issues introduced by PDF merging.
     * <p>
     * This method applies the appropriate processor based on the PDF variant:
     * <ul>
     *     <li>PDF/A-1b: {@link PdfA1bProcessor} to remove transparency masks from images
     *         and save without xref streams (using NO_COMPRESSION)</li>
     *     <li>PDF/A-4 (4b, 4u): {@link PdfA4Processor} to fix version, OutputIntent, and metadata</li>
     * </ul>
     *
     * @param mergedPdf  the merged PDF bytes
     * @param pdfVariant the PDF variant used for conversion
     * @return the processed PDF bytes with compliance fixes applied
     */
    @SneakyThrows
    private byte[] applyPdfAPostProcessing(byte[] mergedPdf, @NotNull PdfVariant pdfVariant) {
        return switch (pdfVariant) {
            case PDF_A_1A -> PdfA1Processor.processPdfA1(mergedPdf, "A");
            case PDF_A_1B -> PdfA1Processor.processPdfA1(mergedPdf, "B");
            case PDF_A_4E -> PdfA4Processor.processPdfA4(mergedPdf, "E");
            case PDF_A_4F -> PdfA4Processor.processPdfA4(mergedPdf, "F");
            case PDF_A_4U -> PdfA4Processor.processPdfA4(mergedPdf, null);
            case PDF_UA_2 -> PdfUa2Processor.processPdfUa2(mergedPdf);
            default -> mergedPdf;
        };
    }

    @SneakyThrows
    public static long getNumberOfPages(byte[] pdfContent) {
        try (PDDocument contentDoc = Loader.loadPDF(pdfContent)) {
            return contentDoc.getNumberOfPages();
        }
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
    /**
     * Resolves the CSS escapes of a value, {@code http\\3a //host} and the like. A stylesheet reaches
     * WeasyPrint as text, and WeasyPrint resolves them, so the check has to see the same value.
     */
    public String decodeCssEscapes(@NotNull String value) {
        if (value.indexOf('\\') < 0) {
            return value;
        }
        Matcher matcher = CSS_ESCAPE_PATTERN.matcher(value);
        StringBuilder decoded = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = hex != null ? codePointOf(hexValueOf(hex)) : matcher.group(2);
            matcher.appendReplacement(decoded, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(decoded);
        return decoded.toString();
    }

    /**
     * Reads the hex digits of a CSS escape. The pattern caps them at six, so the value fits into an int,
     * and every character it captured is a hex digit, so there is nothing here that could fail.
     */
    private int hexValueOf(@NotNull String hex) {
        int value = 0;
        for (int i = 0; i < hex.length(); i++) {
            value = (value << 4) + Character.digit(hex.charAt(i), 16);
        }
        return value;
    }

    /**
     * CSS turns an escape of zero, of a surrogate and of a value above the last code point into the
     * replacement character, and so does this.
     */
    private String codePointOf(int value) {
        boolean valid = value > 0 && value <= Character.MAX_CODE_POINT && !(value >= Character.MIN_SURROGATE && value <= Character.MAX_SURROGATE);
        return valid ? Character.toString(value) : "\uFFFD";
    }

    public boolean isDataUrl(@Nullable String resourceUrl) {
        return resourceUrl != null && resourceUrl.startsWith(DATA_URL_PREFIX);
    }

    public String inlineBase64Resources(String content, FileResourceProvider fileResourceProvider) {
        // replace tags like <img src="...
        String result = RegexMatcher.get(IMG_SRC_REGEX).replace(content, replacement(fileResourceProvider, false));
        // An at-rule is never inlined, so any absolute target has to go: a forbidden one because the
        // policy rejected it, an allowed one because WeasyPrint would then load it from its own network
        // position, with none of the checks this class applies. A relative target stays.
        RegexMatcher.IReplacementCalculator importReplacement = engine ->
                isAbsoluteHttpUrl(unwrapCssUrl(engine.group("url"))) ? "" : null;
        result = RegexMatcher.get(CSS_IMPORT_URL_REGEX).useJavaUtil().replace(result, importReplacement);
        result = RegexMatcher.get(CSS_IMPORT_REGEX).useJavaUtil().replace(result, importReplacement);
        // replace CSS parameters like background: src('/polarion/...
        return RegexMatcher.get(URL_REGEX).useJavaUtil().replace(result, replacement(fileResourceProvider, true));
    }

    private RegexMatcher.IReplacementCalculator replacement(FileResourceProvider fileResourceProvider, boolean css) {
        return engine -> {
            String rawUrl = engine.group("url");
            if (rawUrl == null || rawUrl.isEmpty()) {
                return null;
            }
            return inlineOrBlock(fileResourceProvider, engine.group(), rawUrl, css ? unwrapCssUrl(rawUrl) : rawUrl);
        };
    }

    private String inlineOrBlock(@NotNull FileResourceProvider fileResourceProvider, @NotNull String match, @NotNull String rawUrl, @NotNull String url) {
        if (MediaUtils.isDataUrl(url)) {
            return null;
        }
        String base64String = inline(fileResourceProvider, url);
        if (base64String != null) {
            return match.replace(rawUrl, base64String);
        }
        // An absolute url which was not inlined, whatever the reason, must not stay in the HTML:
        // WeasyPrint would load it from its own network position. A relative url is left untouched,
        // WeasyPrint cannot resolve it.
        return isAbsoluteHttpUrl(url) ? match.replace(rawUrl, BLOCKED_RESOURCE_PLACEHOLDER) : null;
    }

    @Nullable
    private String inline(@NotNull FileResourceProvider fileResourceProvider, @NotNull String url) {
        if (fileResourceProvider.isForbidden(url)) {
            return null;
        }
        // For renderable images (e.g. .png, .svg) strip 'thumbnail' to fetch full-size content.
        // For everything else (spreadsheets, documents, unknown formats) keep 'thumbnail' so Polarion returns an icon preview.
        String resourceUrl = isRenderableImageUrl(url) ? removeQueryParameter(url, THUMBNAIL_PARAMETER) : url;
        return fileResourceProvider.getResourceAsBase64String(resourceUrl);
    }

    /**
     * Takes the target out of a CSS url value. A comment is allowed on either side of it, and a quote
     * stays in the value whenever a comment kept the pattern from capturing it separately. Only the
     * leading and the trailing comment go, a url may well carry the very same characters in its path.
     */
    @NotNull
    public String unwrapCssUrl(@NotNull String url) {
        String unwrapped = decodeCssEscapes(url).trim();
        while (unwrapped.startsWith(COMMENT_START) && unwrapped.contains(COMMENT_END)) {
            unwrapped = unwrapped.substring(unwrapped.indexOf(COMMENT_END) + COMMENT_END.length()).trim();
        }
        while (unwrapped.endsWith(COMMENT_END) && unwrapped.lastIndexOf(COMMENT_START) > 0) {
            unwrapped = unwrapped.substring(0, unwrapped.lastIndexOf(COMMENT_START)).trim();
        }
        if (unwrapped.length() > 1 && (unwrapped.startsWith("\"") && unwrapped.endsWith("\"")
                || unwrapped.startsWith("'") && unwrapped.endsWith("'"))) {
            unwrapped = unwrapped.substring(1, unwrapped.length() - 1).trim();
        }
        return unwrapped;
    }

    /**
     * Normalizes a resource url the same way for the policy check and for the request itself.
     */
    public String normalizeUrl(@NotNull String url) {
        return url.replace(" ", "%20").replace("%5F", "_");
    }

    /**
     * A network path reference like {@code //host/path} counts as well: the conversion service reads the
     * document with a base url and gives such a reference the scheme of that base.
     */
    public boolean isAbsoluteHttpUrl(@Nullable String url) {
        if (url == null) {
            return false;
        }
        String lowerCased = url.trim().toLowerCase(Locale.ROOT);
        return lowerCased.startsWith("http://") || lowerCased.startsWith("https://") || isNetworkPathReference(lowerCased);
    }

    public boolean isNetworkPathReference(@NotNull String url) {
        String trimmed = url.trim();
        return trimmed.startsWith(NETWORK_PATH_PREFIX) && trimmed.length() > NETWORK_PATH_PREFIX.length();
    }

    /**
     * Checks whether the URL points to a resource the exporter can embed as a full-size image
     * (raster image, SVG or convertible diagram), based on its file extension.
     */
    @VisibleForTesting
    static boolean isRenderableImageUrl(@Nullable String url) {
        return PdfExporterExtensionConfiguration.getInstance().getRenderableImageExtensions().contains(getResourceExtension(url));
    }

    /**
     * Extracts the lowercase file extension from a URL, ignoring any query string or fragment.
     * Returns an empty string when the URL has no extension.
     */
    @VisibleForTesting
    static String getResourceExtension(@Nullable String url) {
        if (url == null) {
            return "";
        }
        // strip the query string and fragment, then let Commons IO extract the extension
        String path = url.split("[?#]", 2)[0];
        return FilenameUtils.getExtension(path).toLowerCase(Locale.ROOT);
    }

    /**
     * Removes query-parameter from URL (e.g. 'thumbnail' to ensure full-size resource is fetched).
     */
    public static String removeQueryParameter(String url, String param) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        String decoded = StringEscapeUtils.unescapeHtml4(url);

        int hashIdx = decoded.indexOf('#');
        String fragment = hashIdx >= 0 ? decoded.substring(hashIdx) : "";
        String withoutFragment = hashIdx >= 0 ? decoded.substring(0, hashIdx) : decoded;

        int questionMark = withoutFragment.indexOf('?');
        if (questionMark < 0) {
            return withoutFragment + fragment;
        }

        String base = withoutFragment.substring(0, questionMark);
        String query = withoutFragment.substring(questionMark + 1);

        StringBuilder newQuery = new StringBuilder();

        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;

            if (!key.equals(param)) {
                if (!newQuery.isEmpty()) {
                    newQuery.append('&');
                }
                newQuery.append(pair);
            }
        }

        return (newQuery.isEmpty() ? base : base + "?" + newQuery) + fragment;
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
                MediaUtils::getMimeTypeUsingCustomMap,
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

    private String getMimeTypeUsingCustomMap(@NotNull String resource, byte[] resourceBytes) {
        return CUSTOM_MIME_TYPES_MAP.get(getResourceExtension(resource));
    }

    @SneakyThrows
    private String getMimeTypeUsingFilesProbe(@NotNull String resource, byte[] resourceBytes) {
        return Files.probeContentType(Paths.get(resource));
    }

    public String getMimeTypeUsingTikaByResourceName(@NotNull String resource, byte[] resourceBytes) {
        return detectMimeTypeWithTika(Map.of(PARAM_VALUE, resource));
    }

    public String getMimeTypeUsingTikaByContent(@NotNull String resource, byte[] resourceBytes) {
        return detectMimeTypeWithTika(Map.of(PARAM_VALUE, resourceBytes));
    }

    // BundleJarsPrioritizingRunnable.executeCached returns a map without PARAM_RESULT when the runnable itself couldn't
    // be executed (classloader/reflection/serialization failure — see BundleJarsPrioritizingRunnable.ERROR_KEY). Guard
    // against that so a diagnostic fallback returns null instead of triggering a NullPointerException.
    @Nullable
    private String detectMimeTypeWithTika(@NotNull Map<String, Object> params) {
        Object result = BundleJarsPrioritizingRunnable.executeCached(TikaMimeTypeResolver.class, params).get(PARAM_RESULT);
        return result instanceof Optional<?> optional ? (String) optional.orElse(null) : null;
    }

    @SneakyThrows
    private String getMimeTypeUsingURLConnection(@NotNull String resource, byte[] resourceBytes) {
        try (InputStream is = new BufferedInputStream(new ByteArrayInputStream(resourceBytes))) {
            return URLConnection.guessContentTypeFromStream(is);
        }
    }
}

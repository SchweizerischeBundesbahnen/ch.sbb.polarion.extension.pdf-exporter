package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.generic.util.BundleJarsPrioritizingRunnable;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.helger.css.CSSSourceLocation;
import com.helger.css.ICSSSourceLocationAware;
import com.helger.css.decl.CSSMediaRule;
import com.helger.css.decl.CSSSupportsRule;
import com.helger.css.decl.CSSStyleRule;
import com.helger.css.decl.CSSUnknownRule;
import com.helger.css.decl.ICSSExpressionMember;
import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSImportRule;
import com.helger.css.decl.CSSExpressionMemberTermURI;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.decl.ICSSTopLevelRule;
import com.helger.css.decl.visit.CSSVisitor;
import com.helger.css.decl.CSSExpressionMemberTermSimple;
import com.helger.css.decl.visit.DefaultCSSUrlVisitor;
import com.helger.css.decl.visit.DefaultCSSVisitor;
import com.helger.css.handler.DoNothingCSSParseExceptionCallback;
import com.helger.css.reader.CSSReader;
import com.helger.css.reader.CSSReaderSettings;
import com.helger.css.reader.errorhandler.DoNothingCSSParseErrorHandler;
import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PdfVariant;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.core.util.StringUtils;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.Range;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DataNode;
import org.jsoup.Jsoup;
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
import java.util.Objects;
import java.util.stream.Stream;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import static ch.sbb.polarion.extension.pdf_exporter.util.TikaMimeTypeResolver.PARAM_RESULT;
import static ch.sbb.polarion.extension.pdf_exporter.util.TikaMimeTypeResolver.PARAM_VALUE;

@UtilityClass
public class MediaUtils {
    public static final String IMG_SRC_REGEX = "<img[^<>]*src=(\"|')(?<url>[^(\"|')]*)(\"|')";
    public static final String URL_REGEX = "(?i)url\\(\\s*([\"'])?(?<url>.*?)\\1?\\s*\\)";
    public static final String DATA_URL_PREFIX = "data:";
    private static final String NETWORK_PATH_PREFIX = "//";
    // a url which names a scheme names where it is read from, and this class can vet two of them
    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-z][a-z\\d+.-]*:", Pattern.CASE_INSENSITIVE);

    private static final Pattern CSS_ESCAPE_PATTERN = Pattern.compile("\\\\(?:([0-9a-fA-F]{1,6})[ \\t\\r\\n\\f]?|(.))", Pattern.DOTALL);
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

    /**
     * Check whether particular string is a <a href="https://www.rfc-editor.org/rfc/rfc2397">'data' URL</a>-encoded entry.
     */
    public boolean isDataUrl(@Nullable String resourceUrl) {
        return resourceUrl != null && resourceUrl.startsWith(DATA_URL_PREFIX);
    }

    public String inlineBase64Resources(String content, FileResourceProvider fileResourceProvider) {
        return processResourceRegions(content, fileResourceProvider);
    }

    /**
     * @return what the url of a resource has to be replaced with, null when it may stay as it is
     */
    @Nullable
    private String replacementFor(@NotNull FileResourceProvider fileResourceProvider, @NotNull String rawUrl) {
        // the document may write a url with whitespace around it, every step below has to see the same one
        String url = rawUrl.trim();
        // the escapes are resolved for the decisions and for those only: what is fetched is the url as
        // it stands, and a backslash a path carries is a backslash. isForbidden resolves them as well,
        // so the gate and the decision below cannot read the same url differently
        String decoded = decodeCssEscapes(url);
        if (isDataUrl(decoded)) {
            return null;
        }
        if (!fileResourceProvider.isForbidden(url)) {
            // a renderable image is wanted at its full size, so the thumbnail parameter goes. Everything
            // else keeps it, and Polarion answers with the icon preview it has for a document
            String requested = isRenderableImageUrl(url) ? removeQueryParameter(url, THUMBNAIL_PARAMETER) : url;
            String base64String = fileResourceProvider.getResourceAsBase64String(Objects.requireNonNullElse(requested, url));
            if (base64String != null) {
                return base64String;
            }
        }
        // A url which was not inlined, whatever the reason, must not stay in the document where the
        // conversion service would read it from its own network or its own file system. A relative url
        // is left untouched, no service reads one.
        return isReadElsewhere(decoded) ? BLOCKED_RESOURCE_PLACEHOLDER : null;
    }

    /**
     * Rewrites the resources a stylesheet points at: every url is inlined, replaced by the placeholder or
     * left alone, and an {@code @import} of an absolute address is removed, since an at-rule is never
     * inlined and WeasyPrint would load it itself.
     * <p>
     * The stylesheet is parsed rather than matched. A comment, an escape or a media condition may sit
     * almost anywhere in CSS, and a pattern that survives all of them does not exist.
     * </p>
     */
    public String inlineCssResources(@NotNull String css, @NotNull FileResourceProvider fileResourceProvider) {
        if (!mayReferenceAResource(css)) {
            // no url, no import and no address: there is nothing to rewrite and nothing to check, and a
            // style attribute of a large document is not worth a parser run for that
            return css;
        }
        CascadingStyleSheet stylesheet = parse(css);
        if (stylesheet == null) {
            logger.warn("Dropped a stylesheet which cannot be parsed, the resources it points at cannot be checked");
            return "";
        }

        // The parser tells where each url and each import stands, and only those parts are rewritten.
        // Everything else keeps the formatting the document came with.
        int[] lineStarts = lineStartsOf(css);
        CssRewrite rewrite = new CssRewrite();
        readImports(css, stylesheet, lineStarts, rewrite);
        readUrls(css, stylesheet, lineStarts, rewrite, fileResourceProvider);

        if (!rewrite.complete() || namesAnAddressNothingAccountedFor(css, stylesheet, lineStarts, rewrite.accounted())) {
            logger.warn("Dropped a stylesheet: it names an absolute address which could not be read as a resource");
            return "";
        }
        return applyEdits(css, rewrite.edits());
    }

    @Nullable
    private CascadingStyleSheet parse(@NotNull String css) {
        CSSReaderSettings settings = new CSSReaderSettings()
                // a browser keeps what it understands and skips the rest, and so does the conversion service
                .setBrowserCompliantMode(true)
                // a tab counts as one column, so a column of the parser is an offset of the text again:
                // the default of eight moves every position on a line which carries one
                .setTabSize(1)
                .setCustomErrorHandler(new DoNothingCSSParseErrorHandler())
                .setCustomExceptionHandler(new DoNothingCSSParseExceptionCallback());
        return CSSReader.readFromStringReader(css, settings);
    }

    private void readImports(@NotNull String css, @NotNull CascadingStyleSheet stylesheet, int[] lineStarts, @NotNull CssRewrite rewrite) {
        for (CSSImportRule importRule : stylesheet.getAllImportRules()) {
            CssRange range = rangeOf(lineStarts, css, importRule.getSourceLocation());
            boolean absolute = isReadElsewhere(decodeCssEscapes(importRule.getLocationString()));
            if (range == null) {
                rewrite.missed(!absolute);
            } else {
                rewrite.accounted().add(range);
                if (absolute) {
                    // an at-rule is never inlined, so the target of an absolute one has to go
                    rewrite.edits().add(new CssEdit(range, ""));
                }
            }
        }
    }

    private void readUrls(@NotNull String css, @NotNull CascadingStyleSheet stylesheet, int[] lineStarts,
                          @NotNull CssRewrite rewrite, @NotNull FileResourceProvider fileResourceProvider) {
        CSSVisitor.visitCSSUrl(stylesheet, new DefaultCSSUrlVisitor() {
            @Override
            public void onUrlDeclaration(@Nullable ICSSTopLevelRule topLevelRule, @NotNull CSSDeclaration declaration, @NotNull CSSExpressionMemberTermURI uri) {
                CssRange range = rangeOf(lineStarts, css, uri.getSourceLocation());
                // the parser resolves the escapes of a url term itself, so this value is the one the
                // renderer would read, and the one the policy is asked about
                String replacement = replacementFor(fileResourceProvider, uri.getURIString());
                if (range == null) {
                    rewrite.missed(replacement == null);
                    return;
                }
                rewrite.accounted().add(range);
                if (replacement != null) {
                    // no quotes around it: the value is a data url, and a quote would end a style attribute
                    rewrite.edits().add(new CssEdit(range, "url(" + replacement + ")"));
                }
            }
        });
    }

    /**
     * What a run over a stylesheet collects: where the parser accounted for something, what has to be
     * written back, and whether anything it had to rewrite could not be placed.
     */
    @VisibleForTesting
    static final class CssRewrite {
        private final List<CssRange> accounted = new ArrayList<>();
        private final List<CssEdit> edits = new ArrayList<>();
        private boolean complete = true;

        List<CssRange> accounted() {
            return accounted;
        }

        List<CssEdit> edits() {
            return edits;
        }

        boolean complete() {
            return complete;
        }

        /**
         * @param harmless whether what could not be placed needed no rewriting in the first place
         */
        void missed(boolean harmless) {
            complete &= harmless;
        }
    }

    private record CssRange(int start, int end) {
    }

    private record CssEdit(@NotNull CssRange range, @NotNull String replacement) {
    }

    /**
     * Applies the edits back to front, so that an offset stays valid while the ones before it are used.
     */
    private String applyEdits(@NotNull String css, @NotNull List<CssEdit> edits) {
        StringBuilder result = new StringBuilder(css);
        edits.stream()
                .sorted((left, right) -> Integer.compare(right.range().start(), left.range().start()))
                .forEach(edit -> result.replace(edit.range().start(), edit.range().end(), edit.replacement()));
        return result.toString();
    }

    /**
     * Where each line of a stylesheet begins, counted the way the parser counts it: a line feed ends a
     * line, a carriage return ends one as well, the two together end one line and not two. A form feed
     * ends none, which is what the parser reports for it.
     */
    private int[] lineStartsOf(@NotNull String css) {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        int i = 0;
        while (i < css.length()) {
            char current = css.charAt(i);
            if (current == '\r' && i + 1 < css.length() && css.charAt(i + 1) == '\n') {
                i += 2;
                starts.add(i);
            } else if (current == '\r' || current == '\n') {
                i++;
                starts.add(i);
            } else {
                i++;
            }
        }
        return starts.stream().mapToInt(Integer::intValue).toArray();
    }

    private int offsetOf(int[] lineStarts, @NotNull String css, int line, int column) {
        if (line < 1 || line > lineStarts.length) {
            return -1;
        }
        int offset = lineStarts[line - 1] + column;
        return offset > css.length() ? -1 : offset;
    }

    private boolean mayReferenceAResource(@NotNull String css) {
        String probe = decodeCssEscapes(css).toLowerCase(Locale.ROOT);
        // an address of its own still has to reach the backstop, which is what judges whether anything
        // accounted for it: the payload of a data url is exempted there, by the range of the url() term
        return probe.contains("url(") || probe.contains("@import") || namesAnAbsoluteAddress(probe);
    }

    /**
     * Tells whether a text names an address at all. Nothing is taken out of it here: the caller decides
     * what its probe holds, and inside the backstop a url the parser read is already out of the way.
     */
    private boolean namesAnAbsoluteAddress(@NotNull String css) {
        String probe = decodeCssEscapes(css).toLowerCase(Locale.ROOT);
        // file: is the one scheme besides http and https which a conversion service here resolves
        return probe.contains("http:") || probe.contains("https:") || probe.contains("file:")
                || probe.contains(NETWORK_PATH_PREFIX);
    }

    /**
     * Tells whether a stylesheet names an absolute address which nothing accounted for.
     * <p>
     * What is accounted for is taken out of the text first: a url and an import the parser read, a
     * namespace rule, the selector of every rule, the strings the parser read as values, the comments
     * and the data urls. None of those makes the renderer fetch an unchecked address. Whatever names an
     * address after that, an escaped at-keyword, a value the parser dropped, a function it does not
     * know, was read by nothing, and the renderer may well read it.
     * </p>
     */
    private boolean namesAnAddressNothingAccountedFor(@NotNull String css, @NotNull CascadingStyleSheet stylesheet,
                                                      int[] lineStarts, @NotNull List<CssRange> accounted) {
        List<CssRange> ranges = new ArrayList<>(accounted);
        stylesheet.getAllNamespaceRules().stream()
                .map(rule -> rangeOf(lineStarts, css, rule.getSourceLocation()))
                .filter(Objects::nonNull)
                .forEach(ranges::add);
        stylesheet.getAllRules().stream()
                // an unknown rule is one the parser could not read, and that is what this looks for
                .filter(rule -> !(rule instanceof CSSUnknownRule))
                .filter(ICSSSourceLocationAware.class::isInstance)
                .map(rule -> rangeOf(lineStarts, css, ((ICSSSourceLocationAware) rule).getSourceLocation()))
                .filter(Objects::nonNull)
                .map(range -> selectorOf(css, range))
                .forEach(ranges::add);
        ranges.addAll(textRangesOf(css, stylesheet, lineStarts));

        // each range keeps its length, so one range never moves another and an overlap changes nothing
        char[] probe = css.toCharArray();
        ranges.forEach(range -> Arrays.fill(probe, range.start(), range.end(), ' '));
        return namesAnAbsoluteAddress(stripCssComments(new String(probe)));
    }

    /**
     * @return where the parser read something which fetches nothing: a string, and the selector or the
     * prelude of a rule at any depth, which the top-level pass does not reach inside a grouping at-rule
     */
    private List<CssRange> textRangesOf(@NotNull String css, @NotNull CascadingStyleSheet stylesheet, int[] lineStarts) {
        List<CssRange> ranges = new ArrayList<>();
        CSSVisitor.visitCSS(stylesheet, new DefaultCSSVisitor() {
            @Override
            public void onBeginStyleRule(@NotNull CSSStyleRule styleRule) {
                addPreludeOf(styleRule);
            }

            @Override
            public void onBeginMediaRule(@NotNull CSSMediaRule mediaRule) {
                addPreludeOf(mediaRule);
            }

            @Override
            public void onBeginSupportsRule(@NotNull CSSSupportsRule supportsRule) {
                addPreludeOf(supportsRule);
            }

            private void addPreludeOf(@NotNull ICSSSourceLocationAware rule) {
                CssRange range = rangeOf(lineStarts, css, rule.getSourceLocation());
                if (range != null) {
                    ranges.add(selectorOf(css, range));
                }
            }

            @Override
            public void onDeclaration(@NotNull CSSDeclaration declaration) {
                declaration.getExpression().getAllMembers().stream()
                        .filter(CSSExpressionMemberTermSimple.class::isInstance)
                        .map(CSSExpressionMemberTermSimple.class::cast)
                        .filter(member -> member.getValue().startsWith("\"") || member.getValue().startsWith("'"))
                        .map(member -> rangeOf(lineStarts, css, member.getSourceLocation()))
                        .filter(Objects::nonNull)
                        .forEach(ranges::add);
            }
        });
        return ranges;
    }

    /**
     * @return the part of a rule up to its body, the selector or the prelude, which fetches nothing
     */
    private CssRange selectorOf(@NotNull String css, @NotNull CssRange ruleRange) {
        int body = css.indexOf('{', ruleRange.start());
        return new CssRange(ruleRange.start(), body < 0 || body > ruleRange.end() ? ruleRange.end() : body);
    }

    @Nullable
    private CssRange rangeOf(int[] lineStarts, @NotNull String css, @Nullable CSSSourceLocation location) {
        if (location == null) {
            return null;
        }
        int start = offsetOf(lineStarts, css, location.getFirstTokenBeginLineNumber(), location.getFirstTokenBeginColumnNumber() - 1);
        int end = offsetOf(lineStarts, css, location.getLastTokenEndLineNumber(), location.getLastTokenEndColumnNumber());
        return start < 0 || end < start || end > css.length() ? null : new CssRange(start, end);
    }

    /**
     * Removes the comments of a stylesheet, for reading it only. A quoted string keeps what it holds,
     * css starts no comment in there, and an unterminated comment runs to the end, as css says it does.
     */
    private String stripCssComments(@NotNull String css) {
        StringBuilder result = new StringBuilder(css.length());
        int i = 0;
        while (i < css.length()) {
            char current = css.charAt(i);
            if (current == '"' || current == '\'') {
                i = appendString(css, i, result);
            } else if (isCommentStart(css, i)) {
                i = endOfComment(css, i);
            } else {
                result.append(current);
                i++;
            }
        }
        return result.toString();
    }

    private boolean isCommentStart(@NotNull String css, int index) {
        return css.charAt(index) == '/' && index + 1 < css.length() && css.charAt(index + 1) == '*';
    }

    private int endOfComment(@NotNull String css, int index) {
        int end = css.indexOf("*/", index + 2);
        return end < 0 ? css.length() : end + 2;
    }

    private int appendString(@NotNull String css, int index, @NotNull StringBuilder result) {
        char quote = css.charAt(index);
        result.append(quote);
        int i = index + 1;
        while (i < css.length()) {
            char current = css.charAt(i++);
            result.append(current);
            if (current == '\\' && i < css.length()) {
                result.append(css.charAt(i++));
            } else if (current == quote) {
                break;
            }
        }
        return i;
    }


    /**
     * Where a conversion service reads an address out of the markup, measured against the services this
     * extension family drives: pandoc-service loads {@code img[src]} and {@code iframe[src]},
     * weasyprint-service loads {@code img[src]}, {@code object[data]} and {@code embed[src]}. The image
     * of an inline svg is not loaded by either of them today and is rewritten all the same, a data url
     * being a valid href there.
     */
    private static final List<String[]> RESOURCE_ATTRIBUTES = List.of(
            new String[]{"img[src]", "src"},
            new String[]{"object[data]", "data"},
            new String[]{"embed[src]", "src"},
            new String[]{"iframe[src]", "src"},
            new String[]{"image[href]", "href"},
            new String[]{"image[xlink:href]", "xlink:href"});

    /**
     * Rewrites every resource an HTML document points at: the source of an image, the css of a style
     * element and the css of a style attribute. JSoup says where each of them stands, so every form HTML
     * allows is covered and nothing outside a tag is mistaken for one. JSoup does not write the document
     * back, only the parts which changed are replaced in the original text.
     */
    private String processResourceRegions(@NotNull String html, @NotNull FileResourceProvider fileResourceProvider) {
        Document document = Jsoup.parse(html, "", Parser.htmlParser().setTrackPosition(true));
        List<Region> regions = new ArrayList<>();
        for (String[] attribute : RESOURCE_ATTRIBUTES) {
            for (Element element : document.select(attribute[0])) {
                // the value of an attribute is read as the parser read it, entities resolved: that is the
                // address the renderer sees, and the markup says nothing the renderer would not
                addAttributeRegion(regions, element, attribute[1],
                        url -> Optional.ofNullable(replacementFor(fileResourceProvider, url)).orElse(url));
            }
        }
        for (Element styleElement : document.select("style")) {
            // a style element holds raw text, so its source and its value are the same characters
            styleElement.dataNodes().forEach(data ->
                    addRegion(regions, data.sourceRange(), html, css -> inlineCssResources(css, fileResourceProvider), false));
        }
        for (Element element : document.select("[style]")) {
            addAttributeRegion(regions, element, "style",
                    css -> rewriteDeclarations(css, declarations -> inlineCssResources(declarations, fileResourceProvider)));
        }

        StringBuilder result = new StringBuilder(html);
        regions.stream()
                .sorted((left, right) -> Integer.compare(right.start(), left.start()))
                .forEach(region -> {
                    String rewritten = region.rewrite().apply(region.input());
                    if (!rewritten.equals(region.input())) {
                        // an attribute is written back as markup, so what goes in there is escaped again
                        result.replace(region.start(), region.end(), region.escape() ? Entities.escape(rewritten) : rewritten);
                    }
                });
        return result.toString();
    }

    private void addAttributeRegion(@NotNull List<Region> regions, @NotNull Element element, @NotNull String attribute,
                                    @NotNull UnaryOperator<String> rewrite) {
        Range range = element.attributes().sourceRange(attribute).valueRange();
        if (range.isTracked() && range.startPos() >= 0 && range.endPos() >= range.startPos()) {
            regions.add(new Region(range.startPos(), range.endPos(), element.attr(attribute), rewrite, true));
        }
    }

    /**
     * @param input   what the region says, as the parser read it
     * @param escape  whether writing it back means writing markup
     */
    private record Region(int start, int end, @NotNull String input, @NotNull UnaryOperator<String> rewrite, boolean escape) {
    }

    private void addRegion(@NotNull List<Region> regions, @NotNull Range range, @NotNull String html,
                           @NotNull UnaryOperator<String> rewrite, boolean escape) {
        if (range.isTracked() && range.startPos() >= 0 && range.endPos() >= range.startPos()) {
            regions.add(new Region(range.startPos(), range.endPos(), html.substring(range.startPos(), range.endPos()), rewrite, escape));
        }
    }

    /**
     * The value of a style attribute is a declaration list, not a stylesheet, so it is wrapped into a rule
     * for the parser and unwrapped afterwards.
     */
    private String rewriteDeclarations(@NotNull String declarations, @NotNull UnaryOperator<String> rewrite) {
        String rewritten = rewrite.apply("a{" + declarations + "}");
        int open = rewritten.indexOf('{');
        int close = rewritten.lastIndexOf('}');
        // an empty or unexpected result means the declarations were dropped, they do not come back
        return open < 0 || close < open ? "" : rewritten.substring(open + 1, close);
    }

    /**
     * Normalizes a resource url the same way for the policy check and for the request itself.
     */
    public String normalizeUrl(@NotNull String url) {
        return url.replace(" ", "%20").replace("%5F", "_");
    }

    /**
     * Tells whether a converter would read this url from somewhere of its own, which here means an
     * address in any scheme, and a network path reference, which takes the scheme of the base url.
     * Measured against weasyprint-service: it reads a {@code file:} url through an {@code @import},
     * while a path from the root goes to the base url the document carries, which is the Polarion
     * server. A relative url is read from nowhere, and a data url carries its own content.
     */
    public boolean isReadElsewhere(@Nullable String url) {
        if (url == null) {
            return false;
        }
        String trimmed = url.trim();
        if (isDataUrl(trimmed)) {
            // a data url carries the resource itself, there is nowhere for it to be read from
            return false;
        }
        return SCHEME_PATTERN.matcher(trimmed).find() || isNetworkPathReference(trimmed);
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

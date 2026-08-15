package ch.sbb.polarion.extension.pdf_exporter.util.html;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.pdf_exporter.util.FileResourceProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
import com.polarion.core.util.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Optional;
import java.util.stream.Stream;

public class ExternalCssInternalizer implements LinkInternalizer {

    private static final String DATA_PRECEDENCE = "data-precedence";
    private static final Pattern CLOSING_STYLE_PATTERN = Pattern.compile("</style", Pattern.CASE_INSENSITIVE);
    private static final Document.OutputSettings ATTRIBUTE_OUTPUT_SETTINGS =
            new Document.OutputSettings().escapeMode(Entities.EscapeMode.xhtml);
    private static final String HREF = "href";
    private final FileResourceProvider fileResourceProvider;

    public ExternalCssInternalizer(FileResourceProvider fileResourceProvider) {
        this.fileResourceProvider = fileResourceProvider;
    }

    @Override
    public Optional<String> inline(Map<String, String> attributes) {
        String url = attributes.get(HREF);
        if (!namesAStylesheet(attributes.get("rel"))
                || StringUtils.isEmptyTrimmed(url)) {
            return Optional.empty();
        }
        StringBuilder inlinedContent = new StringBuilder("<style");
        if (attributes.containsKey(DATA_PRECEDENCE)) {
            // the value comes from the document, and it is written back as markup: an unescaped quote
            // in it would end the attribute and everything after it would be read as markup of its own
            inlinedContent.append(" ")
                    .append(DATA_PRECEDENCE)
                    .append("=\"")
                    .append(Entities.escape(attributes.get(DATA_PRECEDENCE), ATTRIBUTE_OUTPUT_SETTINGS))
                    .append("\"");
        }
        inlinedContent.append(">");

        String cssContent = new String(fileResourceProvider.getResourceAsBytes(url));
        cssContent = processRelativeUrls(url, cssContent);
        cssContent = MediaUtils.inlineCssResources(cssContent, fileResourceProvider);
        inlinedContent.append(keepInsideStyleElement(cssContent));
        inlinedContent.append("</style>");

        return Optional.of(inlinedContent.toString());
    }

    private String processRelativeUrls(String resourceUrl, String cssContent) {
        int lastSlashPosition = resourceUrl.lastIndexOf('/');
        if (lastSlashPosition == -1) {
            return cssContent;
        }
        String resourcePath = resourceUrl.substring(0, lastSlashPosition + 1);
        return RegexMatcher.get(MediaUtils.URL_REGEX).useJavaUtil().replace(cssContent, engine -> {
            String url = engine.group("url");
            // the pattern reads a url in any case, so the prefixes are compared in one
            String lowerCased = url.toLowerCase(Locale.ROOT);
            return Stream.of("/", "http:", "https:", MediaUtils.DATA_URL_PREFIX).anyMatch(lowerCased::startsWith) ? null :
                    "url(%s%s)".formatted(resourcePath, url);
        });
    }
    /**
     * Reads the rel of a link the way a renderer reads it: a list of tokens, separated by whitespace,
     * each of them case insensitive. An exact comparison misses {@code rel="Stylesheet"}, which a
     * renderer loads all the same. An alternative style sheet is left alone, since nothing selects one
     * in an export and inlining it would apply a stylesheet the renderer would have skipped.
     */
    private boolean namesAStylesheet(@Nullable String rel) {
        if (rel == null) {
            return false;
        }
        List<String> tokens = Arrays.asList(rel.trim().toLowerCase(Locale.ROOT).split("\\s+"));
        return tokens.contains("stylesheet") && !tokens.contains("alternate");
    }
    /**
     * A style element holds raw text, which ends at the first {@code </style}: a stylesheet carrying
     * those characters would close the element and have the rest of itself read as markup. The slash
     * is escaped, which css reads as the slash itself wherever such a sequence can legally stand.
     */
    private String keepInsideStyleElement(@NotNull String cssContent) {
        return CLOSING_STYLE_PATTERN.matcher(cssContent).replaceAll("<\\\\/style");
    }
}

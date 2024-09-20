package ch.sbb.polarion.extension.pdf_exporter.util.html;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.pdf_exporter.util.FileResourceProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
import com.polarion.core.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ExternalCssInternalizer implements LinkInternalizer {

    private static final String DATA_PRECEDENCE = "data-precedence";
    private static final String HREF = "href";
    private final FileResourceProvider fileResourceProvider;

    public ExternalCssInternalizer(FileResourceProvider fileResourceProvider) {
        this.fileResourceProvider = fileResourceProvider;
    }

    @Override
    public Optional<String> inline(Map<String, String> attributes) {
        String url = attributes.get(HREF);
        if (!"stylesheet".equals(attributes.get("rel"))
                || StringUtils.isEmptyTrimmed(url)) {
            return Optional.empty();
        }
        StringBuilder inlinedContent = new StringBuilder("<style");
        if (attributes.containsKey(DATA_PRECEDENCE)) {
            inlinedContent.append(" ")
                    .append(DATA_PRECEDENCE)
                    .append("=\"")
                    .append(attributes.get(DATA_PRECEDENCE))
                    .append("\"");
        }
        inlinedContent.append(">");

        String cssContent = new String(fileResourceProvider.getResourceAsBytes(url));
        cssContent = processRelativeUrls(url, cssContent);
        cssContent = MediaUtils.inlineBase64Resources(cssContent, fileResourceProvider);
        inlinedContent.append(cssContent);
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
            return Stream.of("/", "http:", "https:", MediaUtils.DATA_URL_PREFIX).anyMatch(url::startsWith) ? null :
                    "url(%s%s)".formatted(resourcePath, url);
        });
    }
}

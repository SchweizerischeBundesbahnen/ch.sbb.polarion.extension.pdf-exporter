package ch.sbb.polarion.extension.pdf_exporter.util.html;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.pdf_exporter.util.FileResourceProvider;
import com.polarion.core.util.StringUtils;

import java.util.Map;
import java.util.Optional;

public class ExternalCssInternalizer implements LinkInternalizer {

    private static final String URL_REGEX = "url\\(\\s*([\"'])?(?<url>.*?)\\1?\\s*\\)";
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
        return RegexMatcher.get(URL_REGEX).useJavaUtil().replace(cssContent, engine -> {
            String url = engine.group("url");
            return url.startsWith("/") || url.toLowerCase().startsWith("http:") || url.toLowerCase().startsWith("https:") ? null :
                    "url(%s%s)".formatted(resourcePath, url);
        });
    }
}

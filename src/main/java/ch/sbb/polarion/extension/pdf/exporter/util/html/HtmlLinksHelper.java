package ch.sbb.polarion.extension.pdf.exporter.util.html;

import ch.sbb.polarion.extension.pdf.exporter.util.FileResourceProvider;
import com.polarion.core.config.impl.SystemValueReader;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.polarion.core.util.StringUtils.isEmpty;

public class HtmlLinksHelper {
    private static final String INTERNALIZE_CSS_LINKS_SYSTEM_PROPERTY = "ch.sbb.polarion.extension.pdf-exporter.internalize-css-links";
    private static final Pattern LINK_PATTERN = Pattern.compile("<link\\s*[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("([\\w-]+)\\s*=\\s*(['\"])(.*?)\\2");

    private final Set<LinkInternalizer> linkInliners;

    public HtmlLinksHelper(FileResourceProvider fileResourceProvider) {
        this (Set.of(
                new CssLinkInternalizer(fileResourceProvider)
        ));
    }

    public HtmlLinksHelper(Set<LinkInternalizer> linkInliners) {
        this.linkInliners = linkInliners;
    }

    public String internalizeLinks(String htmlContent) {
        String enablingProperty = SystemValueReader.getInstance().readString(INTERNALIZE_CSS_LINKS_SYSTEM_PROPERTY, "false");
        if (isEmpty(enablingProperty) || !Boolean.parseBoolean(enablingProperty)) {
            return htmlContent;
        }

        Matcher matcher = LINK_PATTERN.matcher(htmlContent);
        StringBuilder newHtmlContent = new StringBuilder();

        while (matcher.find()) {
            String linkTag = matcher.group(0);

            Map<String, String> attributesMap = parseLinkTagAttributes(linkTag);
            String replacement = inlineLinkTag(linkTag, attributesMap);

            matcher.appendReplacement(newHtmlContent, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(newHtmlContent);

        return newHtmlContent.toString();
    }

    public static Map<String, String> parseLinkTagAttributes(String linkTag) {
        Map<String, String> attributes = new LinkedHashMap<>();

        Matcher matcher = ATTRIBUTE_PATTERN.matcher(linkTag);

        while (matcher.find()) {
            String attributeName = matcher.group(1);
            String attributeValue = matcher.group(3);
            attributes.put(attributeName, attributeValue);
        }
        return attributes;
    }

    private String inlineLinkTag(String linkTag, Map<String, String> attributesMap) {
        return linkInliners.stream()
                .map(i -> i.inline(attributesMap))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElse(linkTag);
    }
}
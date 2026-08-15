package ch.sbb.polarion.extension.pdf_exporter.util.html;

import ch.sbb.polarion.extension.pdf_exporter.util.FileResourceProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Range;
import org.jsoup.parser.Parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class HtmlLinksHelper {

    private final Set<LinkInternalizer> linkInliners;

    public HtmlLinksHelper(FileResourceProvider fileResourceProvider) {
        this (Set.of(
                new ExternalCssInternalizer(fileResourceProvider)
        ));
    }

    public HtmlLinksHelper(Set<LinkInternalizer> linkInliners) {
        this.linkInliners = linkInliners;
    }

    /**
     * @return the attributes of a link tag, as the html parser reads them: the names lowercased, the
     * values with their entities resolved, and a value which carries no quotes read like any other
     */
    public static Map<String, String> parseLinkTagAttributes(String linkTag) {
        Element link = Jsoup.parse(linkTag).selectFirst("link");
        return link == null ? Map.of() : attributesOf(link);
    }

    private static Map<String, String> attributesOf(Element link) {
        Map<String, String> attributes = new LinkedHashMap<>();
        for (Attribute attribute : link.attributes()) {
            attributes.put(attribute.getKey().toLowerCase(Locale.ROOT), attribute.getValue());
        }
        return attributes;
    }

    /**
     * Replaces every link tag an inliner answers for. The tags are located by the html parser at the
     * positions it reports, so a value without quotes, a value carrying a {@code >} and an attribute
     * written in any case are read the way the renderer reads them, and the rest of the document is
     * left exactly as it came.
     */
    public String internalizeLinks(String htmlContent) {
        Document document = Jsoup.parse(htmlContent, "", Parser.htmlParser().setTrackPosition(true));
        List<int[]> replaced = new ArrayList<>();
        List<String> replacements = new ArrayList<>();
        for (Element link : document.select("link")) {
            Range range = link.sourceRange();
            if (!range.isTracked() || range.startPos() < 0 || range.endPos() < range.startPos()) {
                continue;
            }
            Optional<String> inlined = inlineLinkTag(attributesOf(link));
            if (inlined.isPresent()) {
                replaced.add(new int[]{range.startPos(), range.endPos()});
                replacements.add(inlined.get());
            }
        }

        StringBuilder result = new StringBuilder(htmlContent);
        for (int i = replaced.size() - 1; i >= 0; i--) {
            result.replace(replaced.get(i)[0], replaced.get(i)[1], replacements.get(i));
        }
        return result.toString();
    }

    private Optional<String> inlineLinkTag(Map<String, String> attributesMap) {
        return linkInliners.stream()
                .map(i -> i.inline(attributesMap))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }
}

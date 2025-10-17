package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.regex.IRegexEngine;
import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.HtmlUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.adjuster.PageWidthAdjuster;
import ch.sbb.polarion.extension.pdf_exporter.util.exporter.CustomPageBreakPart;
import ch.sbb.polarion.extension.pdf_exporter.util.html.HtmlLinksHelper;
import com.polarion.alm.shared.util.StringUtils;
import com.polarion.core.util.xml.CSSStyle;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static ch.sbb.polarion.extension.pdf_exporter.util.exporter.Constants.*;

public class HtmlProcessor {

    public static final String TABLE_ROW_END_TAG = "</tr>";
    public static final String TABLE_COLUMN_END_TAG = "</td>";
    private static final String DIV_START_TAG = "<div>";
    private static final String DIV_END_TAG = "</div>";
    private static final String SPAN_END_TAG = "</span>";
    private static final String COMMENT_START = "[span";
    private static final String COMMENT_END = "[/span]";
    private static final String NUMBER = "number";
    private static final String DOLLAR_SIGN = "$";
    private static final String DOLLAR_ENTITY = "&dollar;";

    private static final String UNSUPPORTED_DOCUMENT_TYPE = "Unsupported document type: %s";

    private final FileResourceProvider fileResourceProvider;
    private final LocalizationSettings localizationSettings;
    private final HtmlLinksHelper httpLinksHelper;
    private final PdfExporterPolarionService pdfExporterPolarionService;

    public HtmlProcessor(FileResourceProvider fileResourceProvider, LocalizationSettings localizationSettings, HtmlLinksHelper httpLinksHelper, PdfExporterPolarionService pdfExporterPolarionService) {
        this.fileResourceProvider = fileResourceProvider;
        this.localizationSettings = localizationSettings;
        this.httpLinksHelper = httpLinksHelper;
        this.pdfExporterPolarionService = pdfExporterPolarionService;
    }

    public String processHtmlForPDF(@NotNull String html, @NotNull ExportParams exportParams, @NotNull List<String> selectedRoleEnumValues) {

        // I. FIRST SECTION - manipulate HTML as a String. These changes are either not possible or not made easier with JSoup
        // ----------------

        // Replace all dollar-characters in HTML document before applying any regular expressions, as it has special meaning there
        html = encodeDollarSigns(html);

        // Remove all <pd4ml:page> tags which only have meaning for PD4ML library which we are not using. For all these tags we have either our own implementation or pure CSS solution
        html = removePd4mlTags(html);

        // Change path of enum images from internal Polarion to publicly available
        html = html.replace("/ria/images/enums/", "/icons/default/enums/");

        // II. SECOND SECTION - manipulate HTML as a JSoup document. These changes are vice versa fulfilled easier with JSoup.
        // ----------------

        Document document = Jsoup.parse(html);
        document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.base)
                .prettyPrint(false);

        if (exportParams.isCutEmptyChapters()) {
            document = cutEmptyChapters(document);
        }

        html = document.body().html();

        // TODO: rework below, migrating to JSoup processing when reasonable

        html = adjustImageAlignmentForPDF(html);
        html = adjustHeadingsForPDF(html);

        html = adjustCellWidth(html, exportParams);
        if (exportParams.getChapters() != null) {
            html = cutNotNeededChapters(html, exportParams.getChapters());
        }

        // TODO: This should be String processing either right before or right after JSoup, check this
        html = switch (exportParams.getDocumentType()) {
            case LIVE_DOC, WIKI_PAGE -> {
                String processingHtml = new LiveDocTOCGenerator().addTableOfContent(html);
                yield addTableOfFigures(processingHtml);
            }
            case LIVE_REPORT, TEST_RUN -> {
                String processingHtml = new LiveReportTOCGenerator().addTableOfContent(html);
                processingHtml = adjustReportedBy(processingHtml);
                processingHtml = cutExportToPdfButton(processingHtml);
                processingHtml = adjustColumnWidthInReports(processingHtml);
                yield removeFloatLeftFromReports(processingHtml);
            }
            case BASELINE_COLLECTION -> throw new IllegalArgumentException(UNSUPPORTED_DOCUMENT_TYPE.formatted(exportParams.getDocumentType()));
        };

        html = replaceResourcesAsBase64Encoded(html);
        html = properTableHeads(html);
        html = cleanExtraTableContent(html);

        html = switch (exportParams.getDocumentType()) {
            case LIVE_DOC, WIKI_PAGE -> {
                String processingHtml = new PageBreakAvoidRemover().removePageBreakAvoids(html);
                yield new NumberedListsSanitizer().fixNumberedLists(processingHtml);
            }
            case LIVE_REPORT, TEST_RUN -> html;
            case BASELINE_COLLECTION -> throw new IllegalArgumentException(UNSUPPORTED_DOCUMENT_TYPE.formatted(exportParams.getDocumentType()));
        };

        // ----
        // This sequence is important! We need first filter out Linked WorkItems and only then cut empty attributes,
        // cause after filtering Linked WorkItems can become empty. Also cutting local URLs should happen afterwards
        // as filtering workitems relies among other on anchors.
        if (!selectedRoleEnumValues.isEmpty()) {
            html = filterTabularLinkedWorkitems(html, selectedRoleEnumValues);
            html = filterNonTabularLinkedWorkitems(html, selectedRoleEnumValues);
        }
        if (exportParams.isCutEmptyWIAttributes()) {
            html = cutEmptyWIAttributes(html);
        }

        html = rewritePolarionUrls(html);

        if (exportParams.isCutLocalUrls()) {
            html = cutLocalUrls(html);
        }
        // ----

        html = switch (exportParams.getDocumentType()) {
            case LIVE_DOC, WIKI_PAGE -> localizeEnums(html, exportParams);
            case LIVE_REPORT, TEST_RUN -> html;
            case BASELINE_COLLECTION -> throw new IllegalArgumentException(UNSUPPORTED_DOCUMENT_TYPE.formatted(exportParams.getDocumentType()));
        };

        if (exportParams.getRenderComments() != null) {
            html = processComments(html);
        }
        if (hasCustomPageBreaks(html)) {
            //processPageBrakes contains its own adjustContentToFitPage() calls
            html = processPageBrakes(html, exportParams);
        } else if (exportParams.isFitToPage()) {
            html = adjustContentToFitPage(html, exportParams);
        }

        html = fixTableHeadRowspan(html);

        // Do not change this entry order, '&nbsp;' can be used in the logic above, so we must cut them off as the last step
        html = cutExtraNbsp(html);
        return html;
    }

    @NotNull
    private String removePd4mlTags(@NotNull String html) {
        return RegexMatcher.get("(<pd4ml:page.*>)(.)").replace(html, regexEngine -> regexEngine.group(2));
    }

    /**
     * Escapes dollar signs in HTML to prevent them from being interpreted as regex special characters.
     * Should be called after Jsoup parsing operations that may convert &dollar; back to $.
     *
     * @param html HTML content to escape
     * @return HTML with dollar signs replaced by &dollar; entity
     */
    @NotNull
    private String encodeDollarSigns(@NotNull String html) {
        return html.replace(DOLLAR_SIGN, DOLLAR_ENTITY);
    }

    @NotNull
    @VisibleForTesting
    Document cutEmptyChapters(@NotNull Document document) {
        // 'Empty chapter' is a heading tag which doesn't have any visible content "under it",
        // i.e. there are only not visible or whitespace elements between itself and next heading of same/higher level or end of parent/document.

        // Process from lowest to highest priority (h6 to h1), otherwise logic can be broken
        for (int headingLevel = H_TAG_MIN_PRIORITY; headingLevel >= 1; headingLevel--) {
            List<Element> headingsToRemove = JSoupUtils.selectEmptyHeadings(document, headingLevel);
            for (Element heading : headingsToRemove) {

                // In addition to removing heading itself, remove all following empty siblings until next heading, but not comments as they can have special meaning
                Node nextSibling = heading.nextSibling();
                while (nextSibling != null) {
                    if (JSoupUtils.isHeading(nextSibling)) {
                        break;
                    } else {
                        Node siblingToRemove = nextSibling instanceof Comment ? null : nextSibling;
                        nextSibling = nextSibling.nextSibling();
                        if (siblingToRemove != null) {
                            siblingToRemove.remove();
                        }
                    }
                }

                heading.remove();
            }
        }

        return document;
    }

    @NotNull
    @VisibleForTesting
    @SuppressWarnings({"java:S5843", "java:S5852"})
    String cutLocalUrls(@NotNull String html) {
        // This regexp consists of 2 parts combined by OR-condition. In first part it looks for <a>-tags
        // containing "/polarion/#" in its href attribute and match a content inside of this <a> into named group "content".
        // In second part of this regexp it looks for <a>-tags which href attribute starts with "http" and containing <img>-tag inside of it
        // and matches a content inside of this <a> into named group "imgContent". The sense of this regexp is to find
        // all links (as text-link or images) linking to local Polarion resources and to cut these links off, though leaving
        // their content in text.
        return RegexMatcher.get("<a[^>]+?href=[^>]*?/polarion/#[^>]*?>(?<content>[\\s\\S]+?)</a>|<a[^>]+?href=\"http[^>]+?>(?<imgContent><img[^>]+?src=\"data:[^>]+?>)</a>")
                .replace(html, regexEngine -> {
                    String content = regexEngine.group("content");
                    return content != null ? content : regexEngine.group("imgContent");
                });
    }

    /**
     * Rewrites Polarion Work Item hyperlinks so that they become intra-document anchor links.
     **/
    @NotNull
    @VisibleForTesting
    @SuppressWarnings({"java:S3776", "java:S135"}) //complexity is acceptable here
    String rewritePolarionUrls(@NotNull String html) {
        Document doc = Jsoup.parse(html);

        Set<String> workItemAnchors = new HashSet<>();
        for (Element anchor : doc.select("a[id^=work-item-anchor-]")) {
            String id = anchor.id();
            if (!id.isEmpty()) {
                workItemAnchors.add(id);
            }
        }

        for (Element link : doc.select("a[href]")) {
            String href = link.attr("href");
            int polarionIdx = href.indexOf("/polarion/#/project/");
            if (polarionIdx < 0) {
                continue;
            }
            String afterProject = href.substring(polarionIdx + "/polarion/#/project/".length());
            int slashIdx = afterProject.indexOf('/');
            if (slashIdx < 0) {
                continue;
            }
            String projectId = afterProject.substring(0, slashIdx);
            int workItemIdx = afterProject.indexOf("workitem?id=");
            if (workItemIdx < 0) {
                continue;
            }
            String workItemPart = afterProject.substring(workItemIdx + "workitem?id=".length());
            int ampIdx = workItemPart.indexOf('&');
            if (ampIdx >= 0) {
                workItemPart = workItemPart.substring(0, ampIdx);
            }
            int hashIdx = workItemPart.indexOf('#');
            if (hashIdx >= 0) {
                workItemPart = workItemPart.substring(0, hashIdx);
            }
            if (workItemPart.isEmpty()) {
                continue;
            }
            String expectedAnchorId = "work-item-anchor-" + projectId + "/" + workItemPart;
            if (workItemAnchors.contains(expectedAnchorId)) {
                link.attr("href", "#" + expectedAnchorId);
            }
        }

        html = encodeDollarSigns(doc.body().html()); // Jsoup may convert &dollar; back to $ in some cases, so we need to replace it again
        return html;
    }

    /**
     * {@link CustomPageBreakPart} inserts specific 'marks' into positions where we must place page breaks.
     * The solution below replaces marks with proper html tags and does additional processing.
     */
    @NotNull
    @VisibleForTesting
    @SuppressWarnings("java:S3776")
    String processPageBrakes(@NotNull String html, ExportParams exportParams) {
        //remove repeated page breaks, leave just the first one
        html = RegexMatcher.get(String.format("(%s|%s){2,}", PAGE_BREAK_PORTRAIT_ABOVE, PAGE_BREAK_LANDSCAPE_ABOVE)).replace(html, regexEngine -> {
            String sequence = regexEngine.group();
            return sequence.startsWith(PAGE_BREAK_PORTRAIT_ABOVE) ? PAGE_BREAK_PORTRAIT_ABOVE : PAGE_BREAK_LANDSCAPE_ABOVE;
        });

        StringBuilder resultBuf = new StringBuilder();
        LinkedList<String> areas = new LinkedList<>(Arrays.asList(html.split(PAGE_BREAK_MARK))); //use linked list for processing list in backward order
        boolean landscape = exportParams.getOrientation() == Orientation.LANDSCAPE; //in the last block we use global orientation setting

        //the idea here is to wrap areas with different orientation into divs with correspondent class
        while (!areas.isEmpty()) {
            String area = areas.pollLast();
            String orientationClass = (landscape ? "land" : "port") + exportParams.getPaperSize();
            String mark = null;
            if (area.startsWith(LANDSCAPE_ABOVE_MARK)) {
                mark = LANDSCAPE_ABOVE_MARK;
            } else if (area.startsWith(PORTRAIT_ABOVE_MARK)) {
                mark = PORTRAIT_ABOVE_MARK;
            }
            boolean firstArea = mark == null;
            area = firstArea ? area : area.substring(mark.length());

            if (!firstArea) { //see below why we don't wrap first area
                resultBuf.insert(0, DIV_END_TAG);
            }

            //here we can make additional areas processing
            if (exportParams.isFitToPage()) {
                area = adjustContentToFitPage(area, exportParams);
            }

            resultBuf.insert(0, area);
            if (firstArea) {
                //instead of wrapping  the first area into div we place on the body specific orientation (IMPORTANT: here we must use page identifiers but luckily in our case they are the same as the class names)
                //this will prevent weasyprint from creating leading empty page
                resultBuf.insert(0, String.format("<style>body {page: %s;}</style>", orientationClass));
            } else {
                resultBuf.insert(0, String.format("<div class=\"sbb_page_break %s\">", orientationClass));
            }
            landscape = Objects.equals(LANDSCAPE_ABOVE_MARK, mark); //calculate orientation for the next/previous area
        }
        return resultBuf.toString();
    }

    @NotNull
    @VisibleForTesting
    @SuppressWarnings("java:S5852")
        //regex checked
    String cutNotNeededChapters(@NotNull String html, List<String> selectedChapters) {
        // LinkedHashMap is chosen intentionally to keep an order of insertion
        Map<String, Boolean> chaptersMapping = new LinkedHashMap<>();

        // This regexp searches for most high level chapters (<h1>-elements) extracting their numbers into
        // named group "number" and extracting whole <h1>-element into named group "chapter"
        RegexMatcher.get("(?<chapter><h1[^>]*?>.*?<span[^>]*?><span[^>]*?>(?<number>.+?)</span>.*?</span>.+?</h1>)").processEntry(html, regexEngine -> {
            String number = regexEngine.group(NUMBER);
            String chapter = regexEngine.group("chapter");
            chaptersMapping.put(chapter, selectedChapters.contains(number));
        });

        StringBuilder buf = new StringBuilder(html);
        Integer cutStart = null;
        Integer cutEnd = null;
        for (Map.Entry<String, Boolean> entry : chaptersMapping.entrySet()) {
            Boolean entryValue = entry.getValue();
            if (Boolean.FALSE.equals(entryValue) && cutStart == null) {
                cutStart = buf.indexOf(entry.getKey());
            } else if (Boolean.TRUE.equals(entryValue) && cutStart != null) {
                cutEnd = buf.indexOf(entry.getKey());
            }
            if (cutStart != null && cutEnd != null) {
                buf.replace(cutStart, cutEnd, getTopPageBrake(buf.substring(cutStart, cutEnd)));
                cutStart = null;
                cutEnd = null;
            }
        }
        if (cutStart != null) {
            cutEnd = buf.lastIndexOf(DIV_END_TAG);
            buf.replace(cutStart, cutEnd, getTopPageBrake(buf.substring(cutStart, cutEnd)));
        }

        return buf.toString();
    }

    /**
     * When we remove some area from html we have to copy the most top page break (if it exists) in order to preserve expected orientation.
     */
    @NotNull
    private String getTopPageBrake(@NotNull String area) {
        int brakePosition = area.indexOf(PAGE_BREAK_MARK);
        if (brakePosition == -1) {
            return "";
        } else if (area.indexOf(PAGE_BREAK_LANDSCAPE_ABOVE) == brakePosition) {
            return PAGE_BREAK_LANDSCAPE_ABOVE;
        } else {
            return PAGE_BREAK_PORTRAIT_ABOVE;
        }
    }

    @NotNull
    private String filterTabularLinkedWorkitems(@NotNull String html, @NotNull List<String> selectedRoleEnumValues) {
        StringBuilder result = new StringBuilder();
        int cellStart = getLinkedWorkItemsCellStart(html, 0);
        int cellEnd = getLinkedWorkItemsCellEnd(html, cellStart);
        if (cellStart > 0 && cellEnd > 0) {
            result.append(html, 0, cellStart);
        } else {
            return html;
        }

        while (cellStart > 0 && cellEnd > 0) {
            result.append(filterByRoles(html.substring(cellStart, cellEnd), selectedRoleEnumValues));

            cellStart = getLinkedWorkItemsCellStart(html, cellEnd);
            if (cellEnd < (html.length() - 1)) {
                result.append(html, cellEnd + 1, cellStart < 0 ? html.length() : cellStart);
            }
            cellEnd = getLinkedWorkItemsCellEnd(html, cellStart);
        }

        return result.toString();
    }

    private int getLinkedWorkItemsCellStart(@NotNull String html, int prevCellEnd) {
        return html.indexOf("<td id=\"polarion_editor_field=linkedWorkItems\"", prevCellEnd);
    }

    private int getLinkedWorkItemsCellEnd(@NotNull String html, int cellStart) {
        return cellStart > 0 ? html.indexOf(TABLE_COLUMN_END_TAG, cellStart) + TABLE_COLUMN_END_TAG.length() : -1;
    }

    @NotNull
    private String filterNonTabularLinkedWorkitems(@NotNull String html, @NotNull List<String> selectedRoleEnumValues) {
        StringBuilder result = new StringBuilder();
        int spanStart = getLinkedWorkItemsSpanStart(html, 0);
        int spanEnd = getLinkedWorkItemsSpanEnd(html, spanStart);
        if (spanStart > 0 && spanEnd > 0) {
            result.append(html, 0, spanStart);
        } else {
            return html;
        }

        while (spanStart > 0 && spanEnd > 0) {
            String filtered = filterByRoles(html.substring(spanStart, spanEnd), selectedRoleEnumValues);
            result.append(filtered);

            spanStart = getLinkedWorkItemsSpanStart(html, spanEnd);
            if (spanEnd < (html.length() - 1)) {
                result.append(html, spanEnd, spanStart < 0 ? html.length() : spanStart);
            }
            spanEnd = getLinkedWorkItemsSpanEnd(html, spanStart);
        }

        return result.toString();
    }

    private int getLinkedWorkItemsSpanStart(@NotNull String html, int prevCellEnd) {
        return html.indexOf("<span id=\"polarion_editor_field=linkedWorkItems\"", prevCellEnd);
    }

    private int getLinkedWorkItemsSpanEnd(@NotNull String html, int cellStart) {
        return cellStart > 0 ? html.indexOf("&nbsp;", cellStart) : -1;
    }

    @NotNull
    @SuppressWarnings("squid:S5843")
    private String filterByRoles(@NotNull String linkedWorkItems, @NotNull List<String> selectedRoleEnumValues) {
        String polarionVersion = pdfExporterPolarionService.getPolarionVersion();
        // This regexp searches for spans (named group "row") containing linked WorkItem with its role (named group "role").
        // If linked WorkItem role is not among ones selected by user we cut it from resulted HTML
        String regex = getRegexp(polarionVersion);

        StringBuilder filteredContent = new StringBuilder();

        RegexMatcher.get(regex)
                .processEntry(linkedWorkItems, regexEngine -> {
                    String role = regexEngine.group("role");
                    String row = regexEngine.group("row");
                    if (selectedRoleEnumValues.contains(role)) {
                        if (!filteredContent.isEmpty()) {
                            filteredContent.append(",<br>");
                        }
                        filteredContent.append(row);
                    }
                });
        // filteredContent - is literally content of td or span element, we need to prepend <td>/<span> with its attributes and append </td>/</span> to it
        return linkedWorkItems.substring(0, linkedWorkItems.indexOf(">") + 1)
                + filteredContent
                + linkedWorkItems.substring(linkedWorkItems.lastIndexOf("</"));
    }

    private @NotNull String getRegexp(@Nullable String polarionVersion) {
        String filterByRolesRegexBeforePolarion2404 = "(?<row><span>\\s*<span class=\"polarion-JSEnumOption\"[^>]*?>(?<role>[^<]*?)</span>:\\s*<a[^>]*?>\\s*<span[^>]*?>\\s*<img[^>]*?>\\s*<span[^>]*?>[^<]*?</span>\\s*-\\s*<span[^>]*?>[^<]*?</span>\\s*</span>\\s*</a>\\s*</span>)";
        String filterByRolesRegexAfterPolarion2404 = "(?<row><div\\s*[^>]*?>\\s*<span\\s*[^>]*?>(?<role>[^<]*?)<\\/span>\\s*<\\/div>\\s*:\\s*.*?<\\/span><\\/a><\\/span>)";

        if (polarionVersion == null) {
            return filterByRolesRegexAfterPolarion2404;
        }

        return polarionVersion.compareTo("2404") < 0 ? filterByRolesRegexBeforePolarion2404 : filterByRolesRegexAfterPolarion2404;
    }

    @NotNull
    @VisibleForTesting
    String localizeEnums(@NotNull String html, @NotNull ExportParams exportParams) {
        String localizationSettingsName = exportParams.getLocalization() != null ? exportParams.getLocalization() : NamedSettings.DEFAULT_NAME;
        final Map<String, String> localizationMap = localizationSettings.load(exportParams.getProjectId(), SettingId.fromName(localizationSettingsName)).getLocalizationMap(exportParams.getLanguage());

        //Polarion document usually keeps enumerated text values inside of spans marked with class 'polarion-JSEnumOption'.
        //Following expression retrieves such spans.
        return RegexMatcher.get("(?s)<span class=\"polarion-JSEnumOption\".+?>(?<enum>[\\w\\s]+)</span>").replace(html, regexEngine -> {
            String enumContainingSpan = regexEngine.group();
            String enumName = regexEngine.group("enum");
            String replacementString = localizationMap.get(enumName);
            return StringUtils.isEmptyTrimmed(replacementString) ? null :
                    enumContainingSpan.replace(enumName + SPAN_END_TAG, replacementString + SPAN_END_TAG);
        });
    }

    @NotNull
    String cutEmptyWIAttributes(@NotNull String html) {
        // This is a sign of empty (no value) WorkItem attribute in case of tabular view - an empty <td>-element
        // with class "polarion-dle-workitem-fields-end-table-value"
        String emptyTableAttributeMarker = "class=\"polarion-dle-workitem-fields-end-table-value\" style=\"width: 80%;\" onmousedown=\"return false;\" contentEditable=\"false\"></td>";
        String res = html;

        String searchMarkerLower = emptyTableAttributeMarker.toLowerCase();

        int markerIndex;
        do {
            markerIndex = res.toLowerCase().indexOf(searchMarkerLower);
            if (markerIndex == -1) {
                break;
            }

            int trStart = res.lastIndexOf("<tr>", markerIndex);
            int trEnd = res.indexOf(TABLE_ROW_END_TAG, markerIndex) + TABLE_ROW_END_TAG.length();

            if (trStart >= 0 && trEnd > trStart) {
                res = res.substring(0, trStart) + res.substring(trEnd);
            }
        } while (true);

        // This is a sign of empty (no value) WorkItem attribute in case of non-tabular view - <span>-element
        // with title "This field is empty"
        String emptySpanAttributeMarker = "<span style=\"color: #7F7F7F;\" title=\"This field is empty\">";
        while (res.contains(emptySpanAttributeMarker)) {
            String[] parts = res.split(emptySpanAttributeMarker, 2);
            int parentSpanStart = parts[0].lastIndexOf("<span");
            int trEnd = res.indexOf("</span></span>", parentSpanStart) + "</span></span>".length();

            String firstPart = res.substring(0, parentSpanStart);
            if (firstPart.endsWith(",&nbsp;")) {
                firstPart = firstPart.substring(0, firstPart.lastIndexOf(",&nbsp;"));
            }

            res = firstPart + res.substring(trEnd);
        }
        return res;
    }

    @NotNull
    @VisibleForTesting
    @SuppressWarnings({"java:S5869", "java:S6019"})
    String properTableHeads(@NotNull String html) {
        // Searches for all subsequent table rows (<tr>-tags) inside <tbody> which contain <th>-tags
        // followed by a row which doesn't contain <th> (or closing </tbody> tag).
        // There are 2 groups in this regexp, first one is unnamed, containing <tbody> and <tr>-tags containing <th>-tags,
        // second one is named ("header") and contains those <tr>-tags which include <th>-tags. The regexp is ending
        // by positive lookahead "(?=<tr)" which doesn't take part in replacement.
        // The sense in this regexp is to find <tr>-tags containing <th>-tags and move it from <tbody> into <thead>,
        // for table headers to repeat on each page.
        return RegexMatcher.get("(<tbody>[^<]*(?<header><tr>[^<]*<th[\\s|\\S]*?))(?=(<tr|</tbody))").useJavaUtil().replace(html, regexEngine -> {
            String header = regexEngine.group("header");
            return "<thead>" + header + "</thead><tbody>";
        });
    }

    @NotNull
    @VisibleForTesting
    @SuppressWarnings("java:S5852")
        //regex checked
    String adjustCellWidth(@NotNull String html, @NotNull ExportParams exportParams) {
        if (exportParams.isFitToPage()) {
            // This regexp searches for <td> or <th> elements of regular tables which width in styles specified in pixels ("px").
            // <td> or <th> element till "width:" in styles matched into first unnamed group and width value - into second unnamed group.
            // Then we replace matched content by first group content plus "auto" instead of value in pixels.
            html = RegexMatcher.get("(<t[dh][^>]+?width:\\s*)(\\d+px)")
                    .replace(html, regexEngine -> regexEngine.group(1) + "auto");
        }

        // Next step we look for tables which represent WorkItem attributes and force them to take 100% of available width
        html = RegexMatcher.get("(class=\"polarion-dle-workitem-fields-end-table\")")
                .replace(html, regexEngine -> regexEngine.group() + " style=\"width: 100%;\"");

        // Then for column with attribute name we specify to take 20% of table width
        html = RegexMatcher.get("(class=\"polarion-dle-workitem-fields-end-table-label\")")
                .replace(html, regexEngine -> regexEngine.group() + " style=\"width: 20%;\"");

        // ...and for column with attribute value we specify to take 80% of table width
        return RegexMatcher.get("(class=\"polarion-dle-workitem-fields-end-table-value\")")
                .replace(html, regexEngine -> regexEngine.group() + " style=\"width: 80%;\"");
    }

    public @NotNull Document adjustContentToFitPage(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        return new PageWidthAdjuster(document, conversionParams)
                .adjustImageSizeInTables()
                .adjustImageSize()
                .adjustTableSize()
                .getDocument();
    }

    public @NotNull String adjustContentToFitPage(@NotNull String html, @NotNull ConversionParams conversionParams) {
        return new PageWidthAdjuster(html, conversionParams)
                .adjustImageSizeInTables()
                .adjustImageSize()
                .adjustTableSize()
                .toHTML();
    }

    @NotNull
    private String cleanExtraTableContent(@NotNull String html) {
        //Was noticed that some externally imported/pasted elements (at this moment tables) contain strange extra block like
        //<div style="clear:both;"> with the duplicated content inside.
        //Potential fix below is simple: just hide these blocks.
        return html.replace("style=\"clear:both;\"", "style=\"clear:both;display:none;\"");
    }

    @NotNull
    @VisibleForTesting
    String processComments(@NotNull String html) {
        html = RegexMatcher.get("\\[span class=comment level-(?<level>\\d+)\\]").replace(html, regexEngine -> {
            String nestingLevel = regexEngine.group("level");
            return String.format("<span class='comment level-%s'>", nestingLevel);
        });
        html = html.replace("[span class=meta]", "<span class='meta'>");
        html = html.replace("[span class=details]", "<span class='details'>");
        html = html.replace("[span class=date]", "<span class='date'>");
        html = html.replace("[span class=status-resolved]", "<span class='status-resolved'>");
        html = html.replace("[span class=author]", "<span class='author'>");
        html = html.replace("[span class=text]", "<span class='text'>");
        html = html.replace(COMMENT_END, SPAN_END_TAG);
        return html;
    }

    @NotNull
    @VisibleForTesting
    public String cutExtraNbsp(@NotNull String html) {
        //Polarion editfield inserts a lot of extra nbsp. Also, user can copy&paste html from different sources
        //which may contain a lot of nbsp too. This may occasionally result in exceeding page width lines.
        //Seems that there is no better solution than basically remove them completely.
        return html.replaceAll("&nbsp;|\u00A0", " ");
    }

    @NotNull
    String addTableOfFigures(@NotNull final String html) {
        Map<String, String> tofByLabel = new HashMap<>();
        return RegexMatcher.get("<div data-sequence=\"(?<label>[^\"]+)\" id=\"polarion_wiki macro name=tof[^>]*></div>").replace(html, regexEngine -> {
            String label = regexEngine.group("label");
            return tofByLabel.computeIfAbsent(label, notYetGeneratedLabel -> generateTableOfFigures(html, notYetGeneratedLabel));
        });
    }

    @NotNull
    private String generateTableOfFigures(@NotNull String html, @NotNull String label) {
        StringBuilder buf = new StringBuilder(DIV_START_TAG);

        // This regexp searches for paragraphs with class 'polarion-rte-caption-paragraph'
        // with text contained in 'label' parameter in it followed by span-element with class 'polarion-rte-caption' and number inside it (number of figure),
        // which in its turn followed by a-element with name 'dlecaption_<N>' (where <N> - is figure number), which in its turn is followed by figure caption
        RegexMatcher.get(String.format("<p[^>]+?class=\"polarion-rte-caption-paragraph\"[^>]*>\\s*?.*?%s[^<]*<span data-sequence=\"%s\" " +
                "class=\"polarion-rte-caption\">(?<number>\\d+)<a name=\"dlecaption_(?<id>\\d+)\"></a></span>(?<caption>[^<]+)", label, label)).processEntry(html, regexEngine -> {
            String number = regexEngine.group(NUMBER);
            String id = regexEngine.group("id");
            String caption = regexEngine.group("caption");
            if (caption.contains(SPAN_END_TAG)) {
                caption = caption.substring(0, caption.indexOf(SPAN_END_TAG));
            }
            while (caption.contains(COMMENT_START)) {
                StringBuilder captionBuf = new StringBuilder(caption);
                int start = caption.indexOf(COMMENT_START);
                int ending = HtmlUtils.getEnding(caption, start, COMMENT_START, COMMENT_END);
                captionBuf.replace(start, ending, "");
                caption = captionBuf.toString();
            }
            buf.append(String.format("<a href=\"#dlecaption_%s\">%s %s. %s</a><br>", id, label, number, caption.trim()));
        });
        buf.append(DIV_END_TAG);
        return buf.toString();
    }

    @NotNull
    @VisibleForTesting
    String adjustReportedBy(@NotNull String html) {
        // This regexp searches for div containing 'Reported by' text and adjusts its styles
        return RegexMatcher.get("<div style=\"(?<style>[^\"]*)\">Reported by").replace(html, regexEngine -> {
            String initialStyle = regexEngine.group("style");
            String styleAdjustment = "top: 0; font-size: 8px;";
            return String.format("<div style=\"%s;%s\">Reported by", initialStyle, styleAdjustment);
        });
    }

    @NotNull
    @VisibleForTesting
    @SuppressWarnings({"java:S5852", "java:S5869"})
        //regex checked
    String cutExportToPdfButton(@NotNull String html) {
        // This regexp searches for 'Export to PDF' button enclosed into table-element with class 'polarion-TestsExecutionButton-buttons-content',
        // which in its turn enclosed into div-element with class 'polarion-TestsExecutionButton-buttons-pdf'
        return RegexMatcher.get("<div[^>]*class=\"polarion-TestsExecutionButton-buttons-pdf\">" +
                        "[\\w|\\W]*<table class=\"polarion-TestsExecutionButton-buttons-content\">[\\w|\\W]*<div[^>]*>Export to PDF</div>[\\w|\\W]*?</div>")
                .removeAll(html);
    }

    @NotNull
    @VisibleForTesting
    String adjustColumnWidthInReports(@NotNull String html) {
        // Replace fixed width value by relative one
        return html.replace("<table class=\"polarion-rp-column-layout\" style=\"width: 1000px;\">",
                "<table class=\"polarion-rp-column-layout\" style=\"width: 100%;\">");
    }

    @NotNull
    @VisibleForTesting
    String removeFloatLeftFromReports(@NotNull String html) {
        // Remove "float: left;" style definition from tables
        return RegexMatcher.get("(?<table><table[^>]*)style=\"float: left;\"")
                .replace(html, regexEngine -> regexEngine.group("table"));
    }

    @NotNull
    private String adjustHeadingsForPDF(@NotNull String html) {
        html = RegexMatcher.get("<(h[1-6])").replace(html, regexEngine -> {
            String tag = regexEngine.group(1);
            return tag.equals("h1") ? "<div class=\"title\"" : ("<" + liftHeadingTag(tag));
        });

        return RegexMatcher.get("</(h[1-6])>").replace(html, regexEngine -> {
            String tag = regexEngine.group(1);
            return tag.equals("h1") ? DIV_END_TAG : ("</" + liftHeadingTag(tag) + ">");
        });
    }

    @NotNull
    private static String liftHeadingTag(@NotNull String tag) {
        return switch (tag) {
            case "h2" -> "h1";
            case "h3" -> "h2";
            case "h4" -> "h3";
            case "h5" -> "h4";
            case "h6" -> "h5";
            default -> "h6";
        };
    }

    // Images with styles and "display: block" are searched here. For such image we do following: wrap it into div with text-align style
    // and value "right" if image margin is "auto 0px auto auto" or "center" otherwise.
    @NotNull
    @SuppressWarnings({"java:S5852", "java:S5857"}) //need by design
    private String adjustImageAlignmentForPDF(@NotNull String html) {
        String startImgPattern = "<img [^>]*style=\"([^>]*)\".*?>";
        IRegexEngine regexEngine = RegexMatcher.get(startImgPattern).createEngine(html);
        StringBuilder sb = new StringBuilder();

        while (true) {
            String group;
            CSSStyle css;
            CSSStyle.Rule displayRule;
            do {
                do {
                    if (!regexEngine.find()) {
                        regexEngine.appendTail(sb);
                        return sb.toString();
                    }

                    group = regexEngine.group();
                    String style = regexEngine.group(1);
                    css = CSSStyle.parse(style);
                    displayRule = css.getRule("display");
                } while (displayRule == null);
            } while (!"block".equals(displayRule.getValue()));

            final String align;
            CSSStyle.Rule marginRule = css.getRule("margin");
            if (marginRule != null && "auto 0px auto auto".equals(marginRule.getValue())) {
                align = "right";
            } else {
                align = "center";
            }

            group = "<div style=\"text-align: " + align + "\">" + group + DIV_END_TAG;
            regexEngine.appendReplacement(sb, group);
        }
    }

    @SneakyThrows
    @SuppressWarnings({"java:S5852", "java:S5857"}) //need by design
    public String replaceResourcesAsBase64Encoded(String html) {
        return MediaUtils.inlineBase64Resources(html, fileResourceProvider);
    }

    public String internalizeLinks(String html) {
        return httpLinksHelper.internalizeLinks(html);
    }

    private boolean hasCustomPageBreaks(String html) {
        return html.contains(PAGE_BREAK_MARK);
    }

    /**
     * Fixes malformed tables where thead contains single one row which cells has rowspan attribute greater than 1.
     * Such cells semantically extend beyond the thead boundary, which causes incorrect table rendering.
     * This method extends thead by moving rows from tbody into thead to match the rowspan values.
     *
     * @param sourceHtml HTML to process
     * @return the same HTML with fixed table structure
     */
    @NotNull
    @VisibleForTesting
    public String fixTableHeadRowspan(@NotNull String sourceHtml) {
        Document document = Jsoup.parse(sourceHtml);
        document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.base)
                .prettyPrint(false);

        Elements tables = document.select("table");

        for (Element table : tables) {
            Element thead = table.selectFirst("thead");
            if (thead != null) {
                Element headRow = getHeadRow(thead);
                if (headRow != null) {
                    int maxRowspan = getMaxRowspan(headRow);
                    // If all cells have rowspan=1 or no rowspan, nothing to fix
                    if (maxRowspan <= 1) {
                        continue;
                    }

                    Elements tbodyRows = getBodyRows(table);

                    // Move (maxRowspan - 1) rows from tbody to thead
                    int rowsToMove = Math.min(maxRowspan - 1, tbodyRows.size());
                    for (int i = 0; i < rowsToMove; i++) {
                        Element rowToMove = tbodyRows.get(i);
                        rowToMove.remove();
                        thead.appendChild(rowToMove);
                    }
                }
            }
        }

        String resultedHtml = document.body().html();
        // after processing with jsoup we need to replace $-symbol with "&dollar;" because of regular expressions, as it has special meaning there
        resultedHtml = encodeDollarSigns(resultedHtml);
        return resultedHtml;
    }

    private Element getHeadRow(@NotNull Element thead) {
        Elements theadRows = thead.select("> tr");
        if (theadRows.size() != 1) {
            return null;
        }
        return theadRows.first();
    }

    private int getMaxRowspan(@NotNull Element headRow) {
        Elements cells = headRow.select("> th, > td");
        int maxRowspan = 1;

        // Find the maximum rowspan value
        for (Element cell : cells) {
            if (cell.hasAttr("rowspan")) {
                try {
                    int rowspan = Integer.parseInt(cell.attr("rowspan"));
                    if (rowspan > maxRowspan) {
                        maxRowspan = rowspan;
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid rowspan values
                }
            }
        }
        return maxRowspan;
    }

    private Elements getBodyRows(@NotNull Element table) {
        Element tbody = table.selectFirst("tbody");
        if (tbody == null) {
            // No tbody, nothing to move
            return new Elements();
        }

        return tbody.select("> tr");
    }

}

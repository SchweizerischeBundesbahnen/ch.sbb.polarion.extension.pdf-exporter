package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.regex.IRegexEngine;
import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.HtmlUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.exporter.CustomPageBreakPart;
import ch.sbb.polarion.extension.pdf_exporter.util.html.HtmlLinksHelper;
import com.polarion.alm.shared.util.StringUtils;
import com.polarion.core.util.xml.CSSStyle;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static ch.sbb.polarion.extension.pdf_exporter.util.exporter.Constants.*;

public class HtmlProcessor {

    private static final int A5_PORTRAIT_WIDTH = 420;
    private static final int A5_PORTRAIT_HEIGHT = 620;
    private static final int A5_LANDSCAPE_WIDTH = 660;
    private static final int A5_LANDSCAPE_HEIGHT = 380;
    private static final int B5_PORTRAIT_WIDTH = 500;
    private static final int B5_PORTRAIT_HEIGHT = 760;
    private static final int B5_LANDSCAPE_WIDTH = 810;
    private static final int B5_LANDSCAPE_HEIGHT = 460;
    private static final int JIS_B5_PORTRAIT_WIDTH = 520;
    private static final int JIS_B5_PORTRAIT_HEIGHT = 770;
    private static final int JIS_B5_LANDSCAPE_WIDTH = 830;
    private static final int JIS_B5_LANDSCAPE_HEIGHT = 480;
    private static final float NEXT_SIZE_ASPECT_RATIO = 1.41f;
    private static final float NEXT_SIZE_ASPECT_RATIO_TWICE = NEXT_SIZE_ASPECT_RATIO * NEXT_SIZE_ASPECT_RATIO;

    private static final Map<PaperSize, Integer> MAX_PORTRAIT_WIDTHS = Map.of(
            PaperSize.A5, A5_PORTRAIT_WIDTH,
            PaperSize.A4, (int) (A5_PORTRAIT_WIDTH * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.A3, (int) (A5_PORTRAIT_WIDTH * NEXT_SIZE_ASPECT_RATIO_TWICE),
            PaperSize.B5, B5_PORTRAIT_WIDTH,
            PaperSize.B4, (int) (B5_PORTRAIT_WIDTH * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.JIS_B5, JIS_B5_PORTRAIT_WIDTH,
            PaperSize.JIS_B4, (int) (JIS_B5_PORTRAIT_WIDTH * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.LETTER, 610,
            PaperSize.LEGAL, 610,
            PaperSize.LEDGER, 790
    );
    private static final Map<PaperSize, Integer> MAX_LANDSCAPE_WIDTHS = Map.of(
            PaperSize.A5, A5_LANDSCAPE_WIDTH,
            PaperSize.A4, (int) (A5_LANDSCAPE_WIDTH * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.A3, (int) (A5_LANDSCAPE_WIDTH * NEXT_SIZE_ASPECT_RATIO_TWICE),
            PaperSize.B5, B5_LANDSCAPE_WIDTH,
            PaperSize.B4, (int) (B5_LANDSCAPE_WIDTH * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.JIS_B5, JIS_B5_LANDSCAPE_WIDTH,
            PaperSize.JIS_B4, (int) (JIS_B5_LANDSCAPE_WIDTH * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.LETTER, 900,
            PaperSize.LEGAL, 1150,
            PaperSize.LEDGER, 1400
    );
    private static final Map<PaperSize, Integer> MAX_PORTRAIT_HEIGHTS = Map.of(
            PaperSize.A5, A5_PORTRAIT_HEIGHT,
            PaperSize.A4, (int) (A5_PORTRAIT_HEIGHT * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.A3, (int) (A5_PORTRAIT_HEIGHT * NEXT_SIZE_ASPECT_RATIO_TWICE),
            PaperSize.B5, B5_PORTRAIT_HEIGHT,
            PaperSize.B4, (int) (B5_PORTRAIT_HEIGHT * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.JIS_B5, JIS_B5_PORTRAIT_HEIGHT,
            PaperSize.JIS_B4, (int) (JIS_B5_PORTRAIT_HEIGHT * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.LETTER, 820,
            PaperSize.LEGAL, 1050,
            PaperSize.LEDGER, 1270
    );
    private static final Map<PaperSize, Integer> MAX_LANDSCAPE_HEIGHTS = Map.of(
            PaperSize.A5, A5_LANDSCAPE_HEIGHT,
            PaperSize.A4, (int) (A5_LANDSCAPE_HEIGHT * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.A3, (int) (A5_LANDSCAPE_HEIGHT * NEXT_SIZE_ASPECT_RATIO_TWICE),
            PaperSize.B5, B5_LANDSCAPE_HEIGHT,
            PaperSize.B4, (int) (B5_LANDSCAPE_HEIGHT * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.JIS_B5, JIS_B5_LANDSCAPE_HEIGHT,
            PaperSize.JIS_B4, (int) (JIS_B5_LANDSCAPE_HEIGHT * NEXT_SIZE_ASPECT_RATIO),
            PaperSize.LETTER, 550,
            PaperSize.LEGAL, 550,
            PaperSize.LEDGER, 730
    );

    private static final Map<PaperSize, Integer> MAX_PORTRAIT_WIDTHS_IN_TABLES = Map.of(
            PaperSize.A5, MAX_PORTRAIT_WIDTHS.get(PaperSize.A5) / 3,
            PaperSize.A4, MAX_PORTRAIT_WIDTHS.get(PaperSize.A4) / 3,
            PaperSize.A3, MAX_PORTRAIT_WIDTHS.get(PaperSize.A3) / 3,
            PaperSize.B5, MAX_PORTRAIT_WIDTHS.get(PaperSize.B5) / 3,
            PaperSize.B4, MAX_PORTRAIT_WIDTHS.get(PaperSize.B4) / 3,
            PaperSize.JIS_B5, MAX_PORTRAIT_WIDTHS.get(PaperSize.JIS_B5) / 3,
            PaperSize.JIS_B4, MAX_PORTRAIT_WIDTHS.get(PaperSize.JIS_B4) / 3,
            PaperSize.LETTER, MAX_PORTRAIT_WIDTHS.get(PaperSize.LETTER) / 3,
            PaperSize.LEGAL, MAX_PORTRAIT_WIDTHS.get(PaperSize.LEGAL) / 3,
            PaperSize.LEDGER, MAX_PORTRAIT_WIDTHS.get(PaperSize.LEDGER) / 3
    );
    private static final Map<PaperSize, Integer> MAX_LANDSCAPE_WIDTHS_IN_TABLES = Map.of(
            PaperSize.A5, MAX_LANDSCAPE_WIDTHS.get(PaperSize.A5) / 3,
            PaperSize.A4, MAX_LANDSCAPE_WIDTHS.get(PaperSize.A4) / 3,
            PaperSize.A3, MAX_LANDSCAPE_WIDTHS.get(PaperSize.A3) / 3,
            PaperSize.B5, MAX_LANDSCAPE_WIDTHS.get(PaperSize.B5) / 3,
            PaperSize.B4, MAX_LANDSCAPE_WIDTHS.get(PaperSize.B4) / 3,
            PaperSize.JIS_B5, MAX_LANDSCAPE_WIDTHS.get(PaperSize.JIS_B5) / 3,
            PaperSize.JIS_B4, MAX_LANDSCAPE_WIDTHS.get(PaperSize.JIS_B4) / 3,
            PaperSize.LETTER, MAX_LANDSCAPE_WIDTHS.get(PaperSize.LETTER) / 3,
            PaperSize.LEGAL, MAX_LANDSCAPE_WIDTHS.get(PaperSize.LEGAL) / 3,
            PaperSize.LEDGER, MAX_LANDSCAPE_WIDTHS.get(PaperSize.LEDGER) / 3
    );

    private static final int FULL_WIDTH_PERCENT = 100;
    private static final float EX_TO_PX_RATIO = 6.5F;
    private static final String MEASURE_PX = "px";
    private static final String MEASURE_EX = "ex";
    private static final String MEASURE_PERCENT = "%";
    private static final String TABLE_OPEN_TAG = "<table";
    private static final String TABLE_END_TAG = "</table>";
    private static final String DIV_START_TAG = "<div>";
    private static final String DIV_END_TAG = "</div>";
    private static final String SPAN_END_TAG = "</span>";
    private static final String COMMENT_START = "[span";
    private static final String COMMENT_END = "[/span]";
    private static final String WIDTH = "width";
    private static final String MEASURE = "measure";
    private static final String MEASURE_WIDTH = "measureWidth";
    private static final String HEIGHT = "height";
    private static final String MEASURE_HEIGHT = "measureHeight";
    private static final String NUMBER = "number";

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
        // Replace all dollar-characters in HTML document before applying any regular expressions, as it has special meaning there
        html = html.replace("$", "&dollar;");

        html = removePd4mlTags(html);
        html = html.replace("/ria/images/enums/", "/icons/default/enums/");
        html = html.replace("<p><br></p>", "<br/>");
        html = html.replace("vertical-align:middle;", "page-break-inside:avoid;");
        html = adjustImageAlignmentForPDF(html);
        html = html.replaceAll("margin: *auto;", "align:center;");
        html = adjustHeadingsForPDF(html);
        if (exportParams.isCutEmptyChapters()) {
            html = cutEmptyChapters(html);
        }

        html = adjustCellWidth(html, exportParams);
        html = html.replace(">\n ", "> ");
        html = html.replace("\n</", "</");
        if (exportParams.getChapters() != null) {
            html = cutNotNeededChapters(html, exportParams.getChapters());
        }

        html = switch (exportParams.getDocumentType()) {
            case LIVE_DOC, WIKI_PAGE -> {
                String processingHtml = addTableOfContent(html);
                yield addTableOfFigures(processingHtml);
            }
            case LIVE_REPORT, TEST_RUN -> {
                String processingHtml = adjustReportedBy(html);
                processingHtml = cutExportToPdfButton(processingHtml);
                processingHtml = adjustColumnWidthInReports(processingHtml);
                yield removeFloatLeftFromReports(processingHtml);
            }
        };
        html = replaceImagesAsBase64Encoded(html);
        html = removeSvgUnsupportedFeatureHint(html); //note that there is one more replacement attempt before replacing images with base64 representation
        html = properTableHeads(html);
        html = cleanExtraTableContent(html);
        html = switch (exportParams.getDocumentType()) {
            case LIVE_DOC, WIKI_PAGE -> {
                String processingHtml = new PageBreakAvoidRemover().removePageBreakAvoids(html);
                yield new NumberedListsSanitizer().fixNumberedLists(processingHtml);
            }
            case LIVE_REPORT, TEST_RUN -> html;
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
        if (exportParams.isCutLocalUrls()) {
            html = cutLocalUrls(html);
        }
        // ----

        html = switch (exportParams.getDocumentType()) {
            case LIVE_DOC, WIKI_PAGE -> localizeEnums(html, exportParams);
            case LIVE_REPORT, TEST_RUN -> html;
        };

        if (exportParams.isEnableCommentsRendering()) {
            html = processComments(html);
        }
        if (hasCustomPageBreaks(html)) {
            //processPageBrakes contains its own adjustContentToFitPage() calls
            html = processPageBrakes(html, exportParams);
        } else if (exportParams.isFitToPage()) {
            html = adjustContentToFitPage(html, exportParams.getOrientation(), exportParams.getPaperSize());
        }

        // Do not change this entry order, '&nbsp;' can be used in the logic above, so we must cut them off as the last step
        html = cutExtraNbsp(html);
        return html;
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
                area = adjustContentToFitPage(area, landscape ? Orientation.LANDSCAPE : Orientation.PORTRAIT, exportParams.getPaperSize());
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
    String cutEmptyChapters(@NotNull String html) {
        //We have to traverse all existing heading levels from the lowest priority to the highest and find corresponding 'empty chapters'.
        //'Empty chapter' is the area which starts from heading tag and followed by any number of empty 'p', 'br' or page brake related tags.
        //Area must be followed either by the next opening heading tag with the same or higher importance or any closing tag except 'p'.
        for (int i = H_TAG_MIN_PRIORITY; i >= 1; i--) {
            html = RegexMatcher.get(String.format("(?s)(?><h%1$d.*?</h%1$d>)(\\s|<p[^>]*?>|<br/>|</p>|%2$s)*?(?=<h[1-%1$d]|</[^p])",
                    i, String.join("|", PAGE_BREAK_MARK, PORTRAIT_ABOVE_MARK, LANDSCAPE_ABOVE_MARK))).useJavaUtil().replace(html, regexEngine -> {
                String areaToDelete = regexEngine.group();
                return getTopPageBrake(areaToDelete);
            });
        }
        return html;
    }

    @NotNull
    @VisibleForTesting
    @SuppressWarnings("java:S5852") //regex checked
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
        return cellStart > 0 ? html.indexOf("</td>", cellStart) + "</td>".length() : -1;
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
        while (res.contains(emptyTableAttributeMarker)) {
            String[] parts = res.split(emptyTableAttributeMarker, 2);
            int trStart = parts[0].lastIndexOf("<tr>");
            int trEnd = res.indexOf("</tr>", trStart) + "</tr>".length();

            res = res.substring(0, trStart) + res.substring(trEnd);
        }

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
    private String removePd4mlTags(@NotNull String html) {
        return RegexMatcher.get("(<pd4ml:page.*>)(.)").replace(html, regexEngine -> regexEngine.group(2));
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
            return  "<thead>" + header + "</thead><tbody>";
        });
    }

    @NotNull
    @VisibleForTesting
    @SuppressWarnings("java:S5852") //regex checked
    String adjustCellWidth(@NotNull String html, @NotNull ExportParams exportParams) {
        if (exportParams.isFitToPage()) {
            // This regexp searches for <td> or <th> elements of regular tables which width in styles specified in pixels ("px").
            // <td> or <th> element till "width:" in styles matched into first unnamed group and width value - into second unnamed group.
            // Then we replace matched content by first group content plus "auto" instead of value in pixels.
            html = RegexMatcher.get("(<t[dh].+?width:.*?)(\\d+px)")
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

    @NotNull
    @VisibleForTesting
    String adjustContentToFitPage(@NotNull String html, @NotNull Orientation orientation, @NotNull PaperSize paperSize) {
        html = adjustImageSizeInTables(html, orientation, paperSize);
        html = adjustImageSize(html, orientation, paperSize);
        return adjustTableSize(html, orientation, paperSize);
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
        html = html.replace("[span class=date]", "<span class='date'>");
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
    String addTableOfContent(@NotNull String html) {
        final int MAX_DEFAULT_NODE_NESTING = 6;

        int startIndex = html.indexOf("<pd4ml:toc");
        RegexMatcher tocInitMatcher = RegexMatcher.get("tocInit=\"(?<startLevel>\\d+)\"");
        RegexMatcher tocMaxMatcher = RegexMatcher.get("tocMax=\"(?<maxLevel>\\d+)\"");
        while (startIndex >= 0) {
            int endIndex = html.indexOf(">", startIndex);
            String tocMacro = html.substring(startIndex, endIndex);

            int startLevel = tocInitMatcher.findFirst(tocMacro, regexEngine -> regexEngine.group("startLevel"))
                    .map(Integer::parseInt).orElse(1);

            int maxLevel = tocMaxMatcher.findFirst(tocMacro, regexEngine -> regexEngine.group("maxLevel"))
                    .map(Integer::parseInt).orElse(MAX_DEFAULT_NODE_NESTING);

            String toc = generateTableOfContent(html, startLevel, maxLevel);

            html = html.substring(0, startIndex) + toc + html.substring(endIndex + 1);


            startIndex = html.indexOf("<pd4ml:toc", endIndex);
        }

        return html;
    }

    @NotNull
    @SuppressWarnings("java:S5852") //regex checked
    private String generateTableOfContent(@NotNull String html, int startLevel, int maxLevel) {
        TocLeaf root = new TocLeaf(null, 0, null, null, null);
        AtomicReference<TocLeaf> current = new AtomicReference<>(root);

        // This regexp searches for headers of any level (elements <h1>, <h2> etc.). Level of chapter is extracted into
        // named group "level", id of <a> element inside of it (to reference from TOC) - into named group "id",
        // number of this chapter - into named group "number" and text of this header - into named group "text"
        // Also we search for wiki headers, they have slightly different structure + don't have numbers
        RegexMatcher.get("<h(?<level>[1-6])[^>]*?>[^<]*(<a id=\"(?<id>[^\"]+?)\"[^>]*?></a>[^<]*<span[^>]*>\\s*<span[^>]*>(?<number>.+?)</span>[^<]*</span>\\s*(?<text>.+?)\\s*" +
                "|<span id=\"(?<wikiHeaderId>[^\"]+?)\"[^>]*?>(?<wikiHeaderText>.+?)</span>)</h[1-6]>").processEntry(html, regexEngine -> {
            // Then we take all these named groups of certain chapter and generate appropriate element of table of content
            int level = Integer.parseInt(regexEngine.group("level"));
            String id = regexEngine.group("id");
            String number = regexEngine.group(NUMBER);
            String text = regexEngine.group("text");
            String wikiHeaderId = regexEngine.group("wikiHeaderId");
            String wikiHeaderText = regexEngine.group("wikiHeaderText");

            TocLeaf parent;
            TocLeaf newLeaf;
            if (current.get().getLevel() < level) {
                parent = current.get();
            } else {
                parent = current.get().getParent();
                while (parent.getLevel() >= level) {
                    parent = parent.getParent();
                }
            }

            newLeaf = new TocLeaf(parent, level, id != null ? id : wikiHeaderId, number, text != null ? text : wikiHeaderText);
            parent.getChildren().add(newLeaf);

            current.set(newLeaf);
        });

        return root.asString(startLevel, maxLevel);
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
    @SuppressWarnings({"java:S5852", "java:S5869"}) //regex checked
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
        switch (tag) {
            case "h2":
                return "h1";
            case "h3":
                return "h2";
            case "h4":
                return "h3";
            case "h5":
                return "h4";
            default:
                return tag.equals("h6") ? "h5" : "h6";
        }
    }

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

    @NotNull
    @VisibleForTesting
    @SuppressWarnings("java:S5852") //regex checked
    public String adjustImageSize(@NotNull String html, @NotNull Orientation orientation, @NotNull PaperSize paperSize) {
        // We are looking here for images which widths and heights are explicitly specified.
        // Named group "prepend" - is everything which stands before width/height and named group "append" - after.
        // Then we check if width (named group "width") exceeds limit we override it by value "100%"
        return RegexMatcher.get("(<img(?<prepend>[^>]+?)width:\\s*?(?<width>[\\d.]*?)(?<measureWidth>px|ex);\\s*?height:\\s*?(?<height>[\\d.]*?)(?<measureHeight>px|ex)(?<append>[^>]*?)>)")
                .replace(html, regexEngine -> {
                    float maxWidth = orientation == Orientation.PORTRAIT ? MAX_PORTRAIT_WIDTHS.get(paperSize) : MAX_LANDSCAPE_WIDTHS.get(paperSize);
                    float maxHeight = orientation == Orientation.PORTRAIT ? MAX_PORTRAIT_HEIGHTS.get(paperSize) : MAX_LANDSCAPE_HEIGHTS.get(paperSize);

                    float width = parseDimension(regexEngine, WIDTH, MEASURE_WIDTH);
                    float height = parseDimension(regexEngine, HEIGHT, MEASURE_HEIGHT);

                    String prepend = regexEngine.group("prepend");
                    String append = regexEngine.group("append");

                    return generateAdjustedImageTag(prepend, append, width, height, maxWidth, maxHeight);
                });
    }

    private float parseDimension(IRegexEngine regexEngine, String dimension, String measure) {
        float value = Float.parseFloat(regexEngine.group(dimension));
        if (MEASURE_EX.equals(regexEngine.group(measure))) {
            value *= EX_TO_PX_RATIO;
        }
        return value;
    }

    private String generateAdjustedImageTag(String prepend, String append, float width, float height, float maxWidth, float maxHeight) {
        float widthExceedingRatio = width / maxWidth;
        float heightExceedingRatio = height / maxHeight;

        if (widthExceedingRatio <= 1 && heightExceedingRatio <= 1) {
            return null;
        }

        final float adjustedWidth;
        final float adjustedHeight;

        if (widthExceedingRatio > heightExceedingRatio) {
            adjustedWidth = width / widthExceedingRatio;
            adjustedHeight = height / widthExceedingRatio;
        } else {
            adjustedWidth = width / heightExceedingRatio;
            adjustedHeight = height / heightExceedingRatio;
        }

        return "<img" + prepend + "width: " + ((int) adjustedWidth) + "px; height: " + ((int) adjustedHeight) + "px" + append + ">";
    }

    @NotNull
    @VisibleForTesting
    public String adjustTableSize(@NotNull String html, @NotNull Orientation orientation, @NotNull PaperSize paperSize) {
        // We are looking here for tables which widths are explicitly specified.
        // When width exceeds limit we override it by value "100%"
        return RegexMatcher.get("<table[^>]+?width:\\s*?(?<width>[\\d.]+?)(?<measure>px|%)").replace(html, regexEngine -> {
            String width = regexEngine.group(WIDTH);
            String measure = regexEngine.group(MEASURE);
            float widthParsed = Float.parseFloat(width);
            float maxWidth = orientation == Orientation.PORTRAIT ? MAX_PORTRAIT_WIDTHS.get(paperSize) : MAX_LANDSCAPE_WIDTHS.get(paperSize);
            if (MEASURE_PX.equals(measure) && widthParsed > maxWidth || MEASURE_PERCENT.equals(measure) && widthParsed > FULL_WIDTH_PERCENT) {
                return regexEngine.group().replace(width + measure, "100%");
            } else {
                return null;
            }
        });
    }

    @NotNull
    @VisibleForTesting
    @SuppressWarnings({"java:S3776", "java:S5852", "java:S5857", "java:S135"}) //regex checked
    public String adjustImageSizeInTables(@NotNull String html, @NotNull Orientation orientation, @NotNull PaperSize paperSize) {
        StringBuilder buf = new StringBuilder();
        int pos = 0;

        //The main idea below is:
        // 1) find the most top-level tables
        // 2) replace all suspicious img tags inside tables with reduced width

        while (true) {
            int tableStart = html.indexOf(TABLE_OPEN_TAG, pos);
            if (tableStart == -1) {
                buf.append(html.substring(pos));
                break;
            }
            int tableEnd = findTableEnd(html, tableStart);
            if (tableEnd == -1) {
                buf.append(html.substring(pos));
                break;
            } else {
                tableEnd = tableEnd + TABLE_END_TAG.length();
            }
            if (pos != tableStart) {
                buf.append(html, pos, tableStart);
            }
            String tableHtml = html.substring(tableStart, tableEnd);

            String modifiedTableContent = RegexMatcher.get("(<img[^>]+?width:\\s*?(?<widthValue>(?<width>[\\d.]*?)(?<measure>px|ex)|auto);[^>]+?>)").replace(tableHtml, regexEngine -> {
                String widthValue = regexEngine.group("widthValue");
                float width;
                if (widthValue.equals("auto")) {
                    width = Float.MAX_VALUE;
                } else {
                    width = Float.parseFloat(regexEngine.group(WIDTH));
                    if (MEASURE_EX.equals(regexEngine.group(MEASURE))) {
                        width = width * EX_TO_PX_RATIO;
                    }
                }
                float maxWidth = orientation == Orientation.PORTRAIT ? MAX_PORTRAIT_WIDTHS_IN_TABLES.get(paperSize) : MAX_LANDSCAPE_WIDTHS_IN_TABLES.get(paperSize);
                return width <= maxWidth ? null : regexEngine.group()
                        .replaceAll("max-width:\\s*?([\\d.]*?(px|ex)|auto);", "") //it seems that max-width doesn't work in WP
                        .replaceAll("width:\\s*?([\\d.]*?(px|ex)|auto);", "")     //remove width too, we will add it later
                        .replaceAll("height:\\s*?[\\d.]*?(px|ex);", "")           //remove height completely in order to keep image ratio
                        .replace("style=\"", "style=\"width: " + ((int) maxWidth) + "px;");
            });

            buf.append(modifiedTableContent);
            pos = tableEnd;
        }
        return buf.toString();
    }

    @SuppressWarnings("java:S135")
    private int findTableEnd(String html, int tableStart) {
        int pos = tableStart;
        int tableEnd = -1;
        int depth = 0;
        while (pos < html.length()) {
            int nextTableStart = html.indexOf(TABLE_OPEN_TAG, pos);
            int nextTableEnd = html.indexOf(TABLE_END_TAG, pos);
            if (nextTableStart != -1 && nextTableStart < nextTableEnd) {
                depth++;
                pos = nextTableStart + TABLE_OPEN_TAG.length();
            } else if (nextTableEnd != -1) {
                depth--;
                pos = nextTableEnd + TABLE_END_TAG.length();
                if (depth == 0) {
                    tableEnd = nextTableEnd;
                    break;
                }
            } else {
                break;
            }
        }
        return tableEnd;
    }

    @SneakyThrows
    @SuppressWarnings({"java:S5852", "java:S5857"}) //need by design
    public String replaceImagesAsBase64Encoded(String html) {
        StringBuilder result = new StringBuilder();

        //Retrieves data of 'src' attribute from 'img' tags
        IRegexEngine imageRegexEngine = RegexMatcher.get("<img[^<>]*src=(\"|')(?<url>[^(\"|')]*)(\"|')").createEngine(html);
        while (imageRegexEngine.find()) {
            String url = imageRegexEngine.group("url");
            if (url.startsWith("data:")) {
                continue;
            }
            byte[] imgBytes = fileResourceProvider.getResourceAsBytes(url.replace("%5F", "_")); // Replace encoded underscore symbol in 'src' attribute of images
            if (imgBytes != null && imgBytes.length != 0) { // Don't make any manipulations if image wasn't resolved
                try (InputStream is = new BufferedInputStream(new ByteArrayInputStream(imgBytes))) {
                    String mimeType = URLConnection.guessContentTypeFromStream(is);

                    // looks like sometimes mime type for svg isn't recognized
                    if (url.contains(".svg") && (mimeType == null || mimeType.equals(MIME_TYPE_SVG))) {
                        imgBytes = processPossibleSvgImage(imgBytes);
                    }

                    String encodedImage = String.format("data:%s;base64, %s", mimeType, Base64.getEncoder().encodeToString(imgBytes));

                    imageRegexEngine.appendReplacement(result, imageRegexEngine.group().replace(url, encodedImage));
                }
            }
        }
        imageRegexEngine.appendTail(result);

        return result.toString();
    }

    public String internalizeLinks(String html) {
        return httpLinksHelper.internalizeLinks(html);
    }

    @VisibleForTesting
    @SuppressWarnings("squid:S1166") // no need to log or rethrow exception by design
    public byte[] processPossibleSvgImage(byte[] possibleSvgImageBytes) {
        try {
            String svgContent = new String(possibleSvgImageBytes, StandardCharsets.UTF_8);
            return removeSvgUnsupportedFeatureHint(svgContent).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            // not a valid string, just nvm
        }
        return possibleSvgImageBytes;
    }

    @VisibleForTesting
    public String removeSvgUnsupportedFeatureHint(String html) {
        return html.replaceAll("(?s)<switch>[^<]*?<g requiredFeatures=\"[^\"]+?\"/>.*?</switch>", "");
    }

    private boolean hasCustomPageBreaks(String html) {
        return html.contains(PAGE_BREAK_MARK);
    }

}

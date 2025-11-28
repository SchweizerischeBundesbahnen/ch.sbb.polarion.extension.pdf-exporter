package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.HtmlUtils;
import ch.sbb.polarion.extension.pdf_exporter.constants.CssProp;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTag;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTagAttr;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.adjuster.PageWidthAdjuster;
import ch.sbb.polarion.extension.pdf_exporter.util.exporter.CustomPageBreakPart;
import ch.sbb.polarion.extension.pdf_exporter.util.html.HtmlLinksHelper;
import com.polarion.alm.shared.util.StringUtils;
import com.steadystate.css.parser.CSSOMParser;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.w3c.dom.css.CSSStyleDeclaration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType.*;
import static ch.sbb.polarion.extension.pdf_exporter.util.exporter.Constants.*;

public class HtmlProcessor {

    private static final String DIV_END_TAG = "</div>";
    private static final String SPAN_END_TAG = "</span>";
    private static final String COMMENT_START = "[span";
    private static final String COMMENT_END = "[/span]";
    private static final String DOLLAR_SIGN = "$";
    private static final String DOLLAR_ENTITY = "&dollar;";
    private static final String ROWSPAN_ATTR = "rowspan";
    private static final String RIGHT_ALIGNMENT_MARGIN = "auto 0px auto auto";
    private static final String EMPTY_FIELD_TITLE = "This field is empty";
    private static final String URL_PROJECT_ID_PREFIX = "/polarion/#/project/";
    private static final String URL_WORK_ITEM_ID_PREFIX = "workitem?id=";
    private static final String POLARION_URL_MARKER = "/polarion/#";
    private static final String TABLE_OF_FIGURES_ANCHOR_ID_PREFIX = "dlecaption_";

    private static final String UNSUPPORTED_DOCUMENT_TYPE = "Unsupported document type: %s";

    private final FileResourceProvider fileResourceProvider;
    private final LocalizationSettings localizationSettings;
    private final HtmlLinksHelper httpLinksHelper;

    private final @NotNull CSSOMParser parser = new CSSOMParser();

    public HtmlProcessor(FileResourceProvider fileResourceProvider, LocalizationSettings localizationSettings, HtmlLinksHelper httpLinksHelper) {
        this.fileResourceProvider = fileResourceProvider;
        this.localizationSettings = localizationSettings;
        this.httpLinksHelper = httpLinksHelper;
    }

    public String processHtmlForPDF(@NotNull String html, @NotNull ExportParams exportParams, @NotNull List<String> selectedRoleEnumValues) {
        if (exportParams.getDocumentType() == BASELINE_COLLECTION) {
            // Unsupported document type
            throw new IllegalArgumentException(UNSUPPORTED_DOCUMENT_TYPE.formatted(exportParams.getDocumentType()));
        }

        // I. FIRST SECTION - manipulate HTML as a String. These changes are either not possible or not made easier with JSoup
        // ----------------

        // Replace all dollar-characters in HTML document before applying any regular expressions, as it has special meaning there
        html = encodeDollarSigns(html);

        // Remove all <pd4ml:page> tags which only have meaning for PD4ML library which we are not using.
        html = removePd4mlPageTags(html);

        // Change path of enum images from internal Polarion to publicly available
        html = html.replace("/ria/images/enums/", "/icons/default/enums/");

        // Was noticed that some externally imported/pasted elements (at this moment tables) contain strange extra block like
        // <div style="clear:both;"> with the duplicated content inside.
        // Potential fix below is simple: just hide these blocks.
        html = html.replace("style=\"clear:both;\"", "style=\"clear:both;display:none;\"");

        // fix HTML adding closing tag for <pd4ml:toc> - JSoup requires it
        // Only add closing tag if it's a self-closing tag (e.g., <pd4ml:toc/> or <pd4ml:toc attr="val"/>)
        // Don't add if it already has a closing tag (e.g., <pd4ml:toc></pd4ml:toc>)
        html = html.replaceAll("(<pd4ml:toc[^>/]*)/?>(?!</pd4ml:toc>)", "$1></pd4ml:toc>");

        if (exportParams.getRenderComments() != null) {
            html = processComments(html);
        }

        // II. SECOND SECTION - manipulate HTML as a JSoup document. These changes are vice versa fulfilled easier with JSoup.
        // ----------------

        Document document = JSoupUtils.parseHtml(html);

        // From Polarion perspective h1 - is a document title, h2 are h1 heading etc. We are making such headings' uplifting here
        adjustDocumentHeadings(document);

        if (exportParams.isCutEmptyChapters()) {
            // Cut empty chapters if explicitly requested by user
            cutEmptyChapters(document);
        }
        if (exportParams.getChapters() != null) {
            // Leave only chapters explicitly selected by user
            cutNotNeededChapters(document, exportParams.getChapters());
        }

        if (exportParams.getDocumentType() == LIVE_DOC || exportParams.getDocumentType() == WIKI_PAGE) {
            // Moves WorkItem content out of table wrapping it
            removePageBreakAvoids(document);

            // Fixes nested HTML lists structure
            fixNestedLists(document);

            // Localize enumeration values
            localizeEnums(document, exportParams);

            addTableOfFigures(document);
        }

        if (exportParams.getDocumentType() == LIVE_REPORT || exportParams.getDocumentType() == TEST_RUN) {
            // Searches for div containing 'Reported by' text and adjusts its styles
            adjustReportedBy(document);

            // Cuts "Export To PDF" button from report's content
            cutExportToPdfButton(document);

            // Remove "float: left;" style definition from tables
            removeFloatLeftFromReports(document);

            // Replaces fixed width value of report tables by relative one
            adjustColumnWidthInReports(document);
        }

        // Polarion doesn't place table rows with th-tags into thead, placing them in table's tbody, which is wrong as table header won't
        // repeat on each next page if table is split across multiple pages. We are fixing this moving such rows into thead.
        fixTableHeads(document);

        // If on next step we placed into thead rows which contain rowspan > 1 and this "covers" rows which are still in tbody, we are fixing
        // this here, moving such rows also in thead
        fixTableHeadRowspan(document);

        // Images with styles and "display: block" are searched here. For such images we do following: wrap them into div with text-align style
        // and value "right" if image margin is "auto 0px auto auto" or "center" otherwise.
        adjustImageAlignment(document);

        // Adjusts WorkItem attributes tables to stretch to full page width for better usage of page space and better readability.
        // Also changes absolute widths of normal table cells from absolute values to "auto" if "Fit tables and images to page" is on
        adjustCellWidth(document, exportParams);

        // ----
        // This sequence is important! We need first filter out Linked WorkItems and only then cut empty attributes,
        // cause after filtering Linked WorkItems can become empty. Also cutting local URLs should happen afterwards
        // as filtering workitems relies among other on anchors.
        if (!selectedRoleEnumValues.isEmpty()) {
            filterTabularLinkedWorkItems(document, selectedRoleEnumValues);
            filterNonTabularLinkedWorkItems(document, selectedRoleEnumValues);
        }
        if (exportParams.isCutEmptyWIAttributes()) {
            cutEmptyWIAttributes(document);
        }
        // Rewrites Polarion Work Item hyperlinks so that they become intra-document anchor links.
        rewritePolarionUrls(document);
        if (exportParams.isCutLocalUrls()) {
            cutLocalUrls(document);
        }
        // ----

        getTocGenerator(exportParams.getDocumentType()).addTableOfContent(document);

        if (exportParams.isFitToPage() && !hasCustomPageBreaks(html)) {
            // ---- BOOKMARK 1
            // In case of custom page breaks adjustContentToFitPage() will be called separately for each HTML block between
            // page breaks separately (see BOOKMARK 2 below), as paper orientation can be changed by page break
            adjustContentToFitPage(document, exportParams);
            // ----
        }

        html = document.body().html();

        // Jsoup may convert &dollar; back to $ in some cases, so we need to replace it again
        html = encodeDollarSigns(html);

        html = replaceResourcesAsBase64Encoded(html);

        if (hasCustomPageBreaks(html)) {
            // ---- BOOKMARK 2
            // processPageBrakes() contains its own adjustContentToFitPage() calls, see BOOKMARK 1 for same logic without custom page breaks
            html = processPageBrakes(html, exportParams);
            // ----
        }

        // Do not change this entry order, '&nbsp;' can be used in the logic above, so we must cut them off as the last step
        html = cutExtraNbsp(html);
        return html;
    }

    @NotNull
    private String removePd4mlPageTags(@NotNull String html) {
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

    @VisibleForTesting
    void cutEmptyChapters(@NotNull Document document) {
        // 'Empty chapter' is a heading tag which doesn't have any visible content "under it",
        // i.e. there are only not visible or whitespace elements between itself and next heading of same/higher level or end of parent/document.

        // Process from lowest to highest priority (h6 to h1), otherwise logic can be broken
        for (int headingLevel = H_TAG_MIN_PRIORITY; headingLevel >= 1; headingLevel--) {
            removeEmptyHeadings(document, headingLevel);
        }
    }

    private void removeEmptyHeadings(@NotNull Document document, int headingLevel) {
        List<Element> headingsToRemove = JSoupUtils.selectEmptyHeadings(document, headingLevel);
        for (Element heading : headingsToRemove) {

            // In addition to removing heading itself, remove all following empty siblings until next heading, but not comments as they can have special meaning
            // We don't check additionally if sibling is empty, because if a heading was selected for removal there are only empty siblings under it
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

    @VisibleForTesting
    void cutNotNeededChapters(@NotNull Document document, @NotNull List<String> selectedChapters) {
        List<ChapterInfo> chapters = getChaptersInfo(document, selectedChapters);

        // Process chapters to remove unwanted ones
        for (ChapterInfo currentChapter : chapters) {
            if (!currentChapter.shouldKeep()) {
                // Remember parent element for possible future usage
                Element parent = currentChapter.heading().parent();

                // Collect first 2 page break comments in the block to remove
                List<Comment> topPageBreakComments = collectPageBreakComments(currentChapter);

                Node nextChapterNode = removeChapter(currentChapter);

                // Re-insert page break comments at the position where the block was removed
                if (nextChapterNode != null) {
                    // Insert before the next H1
                    for (Comment pageBreak : topPageBreakComments) {
                        nextChapterNode.before(pageBreak);
                    }
                } else if (parent != null) {
                    // Otherwise insert at the end of parent
                    for (Comment pageBreak : topPageBreakComments) {
                        parent.appendChild(pageBreak);
                    }
                }
            }
        }
    }

    @NotNull
    private List<ChapterInfo> getChaptersInfo(@NotNull Document document, @NotNull List<String> selectedChapters) {
        List<ChapterInfo> chapters = new ArrayList<>();

        for (Element h1 : document.select(HtmlTag.H1)) {
            boolean shouldKeep = false;

            // Extract chapter number from the h1 structure: <h1><span><span>NUMBER</span></span>...</h1>
            Elements innerSpans = h1.select("span > span");
            if (!innerSpans.isEmpty()) {
                String chapterNumber = Objects.requireNonNull(innerSpans.first()).text();
                shouldKeep = selectedChapters.contains(chapterNumber);
            }
            chapters.add(new ChapterInfo(h1, shouldKeep));
        }
        return chapters;
    }

    /**
     * Collects top PAGE_BREAK related comments starting from current to next chapter (H1 elements)
     */
    @NotNull
    private List<Comment> collectPageBreakComments(@NotNull ChapterInfo currentChapter) {
        List<Comment> topPageBreakComments = new ArrayList<>();
        Node current = currentChapter.heading().nextSibling();

        // Traverse siblings until we reach the next h1 or end of siblings
        while (current != null && !JSoupUtils.isH1(current) && !JSoupUtils.containsH1(current)) {
            // Collect top PAGE_BREAK comments at current level
            collectPageBreakComments(current, topPageBreakComments);
            current = current.nextSibling();
        }

        return topPageBreakComments;
    }

    /**
     * Recursively collects top PAGE_BREAK related comments from an element and its descendants,
     * but only first 2 of them: <!--PAGE_BREAK--> and then either <!--PORTRAIT_ABOVE--> or <!--LANDSCAPE_ABOVE-->,
     * the rest won't be relevant as they belong to removed content.
     */
    private void collectPageBreakComments(@NotNull Node node, @NotNull List<Comment> topPageBreakComments) {
        if (topPageBreakComments.size() < 2) {
            if (node instanceof Comment comment && isPageBreakComment(comment)) {
                topPageBreakComments.add(new Comment(comment.getData()));
            } else if (node instanceof Element element) {
                for (Node child : element.childNodes()) {
                    collectPageBreakComments(child, topPageBreakComments);
                }
            }
        }
    }

    private boolean isPageBreakComment(@NotNull Comment comment) {
        String commentData = comment.getData();
        return commentData.equals(PAGE_BREAK) || commentData.equals(LANDSCAPE_ABOVE) || commentData.equals(PORTRAIT_ABOVE);
    }

    @Nullable
    private Node removeChapter(@NotNull ChapterInfo currentChapter) {
        Node current = currentChapter.heading();
        Node nextChapterNode = null; // Can return null if no next chapter found

        // Remove chapter itself and all siblings between it and next h1-tag
        while (current != null) {
            Node next = current.nextSibling();
            current.remove();

            // Check if next sibling is H1
            if (next != null && (JSoupUtils.isH1(next) || JSoupUtils.containsH1(next))) {
                nextChapterNode = next;
                break;
            }

            current = next;
        }

        return nextChapterNode;
    }

    @VisibleForTesting
    void fixTableHeads(@NotNull Document document) {
        Elements tables = document.select(HtmlTag.TABLE);
        for (Element table : tables) {
            List<Element> headerRows = JSoupUtils.getRowsWithHeaders(table);
            if (headerRows.isEmpty()) {
                continue;
            }

            Element header = table.selectFirst(HtmlTag.THEAD);
            if (header == null) {
                header = new Element(HtmlTag.THEAD);
                table.prependChild(header);
            }

            for (Element headerRow : headerRows) {
                // Parent of each header row can't be null as we got them as child nodes of a table
                if (!Objects.requireNonNull(headerRow.parent()).tagName().equals(HtmlTag.THEAD)) {
                    // Header row is located not in thead - moving it there
                    headerRow.remove();
                    header.appendChild(headerRow);
                }
            }
        }
    }

    /**
     * Fixes malformed tables where thead contains single one row which cells has rowspan attribute greater than 1.
     * Such cells semantically extend beyond the thead boundary, which causes incorrect table rendering.
     * This method extends thead by moving rows from tbody into thead to match the rowspan values.
     */
    @VisibleForTesting
    public void fixTableHeadRowspan(@NotNull Document document) {
        Elements tables = document.select(HtmlTag.TABLE);

        for (Element table : tables) {
            Element thead = table.selectFirst(HtmlTag.THEAD);
            if (thead != null) {
                Element headRow = getHeadRow(thead);
                if (headRow != null) {
                    int maxRowspan = getMaxRowspan(headRow);
                    // If all cells have rowspan=1 or no rowspan, nothing to fix
                    if (maxRowspan <= 1) {
                        continue;
                    }

                    List<Element> tbodyRows = JSoupUtils.getBodyRows(table);

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
            if (cell.hasAttr(ROWSPAN_ATTR)) {
                try {
                    int rowspan = Integer.parseInt(cell.attr(ROWSPAN_ATTR));
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

    private void adjustDocumentHeadings(@NotNull Document document) {
        Elements headings = document.select("h1, h2, h3, h4, h5, h6");

        for (Element heading : headings) {
            if (JSoupUtils.isH1(heading)) {
                heading.tagName(HtmlTag.DIV);
                heading.addClass("title");
            } else {
                int level = heading.tagName().charAt(1) - '0';
                int newLevel = Math.max(1, Math.min(6, level - 1));
                heading.tagName("h" + newLevel);
            }
        }
    }

    @VisibleForTesting
    void adjustImageAlignment(@NotNull Document document) {
        Elements images = document.select(HtmlTag.IMG);
        for (Element image : images) {
            if (image.hasAttr(HtmlTagAttr.STYLE)) {
                String style = image.attr(HtmlTagAttr.STYLE);
                CSSStyleDeclaration cssStyle = parseCss(style);

                String displayValue = getCssValue(cssStyle, CssProp.DISPLAY);
                if (!CssProp.DISPLAY_BLOCK_VALUE.equals(displayValue)) {
                    continue;
                }

                Element wrapper = new Element(HtmlTag.DIV);

                String marginValue = Optional.ofNullable(cssStyle.getPropertyValue(CssProp.MARGIN)).orElse("").trim();
                if (RIGHT_ALIGNMENT_MARGIN.equals(marginValue)) {
                    wrapper.attr(HtmlTagAttr.STYLE, String.format("%s: %s;", CssProp.TEXT_ALIGN, CssProp.TEXT_ALIGN_RIGHT_VALUE));
                } else {
                    wrapper.attr(HtmlTagAttr.STYLE, String.format("%s: %s;", CssProp.TEXT_ALIGN, CssProp.TEXT_ALIGN_CENTER_VALUE));
                }

                Element previousSibling = image.previousElementSibling();
                if (previousSibling != null) {
                    previousSibling.after(wrapper);
                } else {
                    Element parent = image.parent();
                    Objects.requireNonNullElse(parent, document.body()).prependChild(wrapper);
                }
                image.remove();
                wrapper.appendChild(image);
            }
        }
    }

    @VisibleForTesting
    void adjustCellWidth(@NotNull Document document, @NotNull ExportParams exportParams) {
        if (exportParams.isFitToPage()) {
            autoCellWidth(document);
        }

        Elements wiAttrTables = document.select("table.polarion-dle-workitem-fields-end-table");
        for (Element table : wiAttrTables) {
            table.attr(HtmlTagAttr.STYLE, "width: 100%");

            Elements attrNameCells = table.select("td.polarion-dle-workitem-fields-end-table-label");
            for (Element attrNameCell : attrNameCells) {
                attrNameCell.attr(HtmlTagAttr.STYLE, "width: 20%");
            }

            Elements attrNameValues = table.select("td.polarion-dle-workitem-fields-end-table-value");
            for (Element attrNameValue : attrNameValues) {
                attrNameValue.attr(HtmlTagAttr.STYLE, "width: 80%");
            }
        }
    }

    private void autoCellWidth(@NotNull Document document) {
        // Searches for <td> or <th> elements of regular tables whose width in styles specified not in percentage.
        // If they contain absolute values we replace them with auto, otherwise tables containing them can easily go outside boundaries of a page.
        Elements cells = document.select(String.format("%s, %s", HtmlTag.TH, HtmlTag.TD));
        for (Element cell : cells) {
            if (cell.hasAttr(HtmlTagAttr.STYLE)) {
                String style = cell.attr(HtmlTagAttr.STYLE);
                CSSStyleDeclaration cssStyle = parseCss(style);

                String widthValue = getCssValue(cssStyle, CssProp.WIDTH);
                if (!widthValue.isEmpty() && !widthValue.contains("%")) {
                    cssStyle.setProperty(CssProp.WIDTH, CssProp.WIDTH_AUTO_VALUE, null);
                    cell.attr(HtmlTagAttr.STYLE, cssStyle.getCssText());
                }
            }
        }
    }

    @VisibleForTesting
    void cutLocalUrls(@NotNull Document document) {
        // Looks for <a>-tags containing "/polarion/#" in its href attribute or for <a>-tags which href attribute starts with "http" and containing <img>-tag inside of it.
        // Then it moves content of such links outside it and removing links themselves.
        for (Element link : document.select("a[href]")) {
            String href = link.attr(HtmlTagAttr.HREF);
            boolean cutUrl = href.contains(POLARION_URL_MARKER) || JSoupUtils.isImg(link.firstElementChild());
            if (cutUrl) {
                for (Node contentNodes : link.childNodes()) {
                    link.before(contentNodes.clone());
                }
                link.remove();
            }
        }
    }

    @VisibleForTesting
    void rewritePolarionUrls(@NotNull Document document) {
        Set<String> workItemAnchors = new HashSet<>();
        for (Element anchor : document.select("a[id^=work-item-anchor-]")) {
            workItemAnchors.add(anchor.id());
        }

        for (Element link : document.select("a[href]")) {
            String href = link.attr(HtmlTagAttr.HREF);

            String afterProject = substringAfter(href, URL_PROJECT_ID_PREFIX);
            String projectId = substringBefore(afterProject, "/", false);
            String workItemId = substringAfter(afterProject, URL_WORK_ITEM_ID_PREFIX);

            if (afterProject == null || projectId == null || workItemId == null) {
                continue;
            }

            workItemId = substringBefore(workItemId, "&", true);
            workItemId = workItemId != null ? substringBefore(workItemId, "#", true) : null;
            if (!StringUtils.isEmpty(workItemId)) {
                String expectedAnchorId = "work-item-anchor-" + projectId + "/" + workItemId;
                if (workItemAnchors.contains(expectedAnchorId)) {
                    link.attr(HtmlTagAttr.HREF, "#" + expectedAnchorId);
                }
            }
        }
    }

    @Nullable
    private String substringBefore(@Nullable String str, @NotNull String marker, boolean initialStringIfNotFound) {
        if (str != null && str.contains(marker)) {
            return str.substring(0, str.indexOf(marker));
        } else {
            return initialStringIfNotFound ? str : null;
        }
    }

    @Nullable
    private String substringAfter(@Nullable String str, @NotNull String marker) {
        return str != null && str.contains(marker) ? str.substring(str.indexOf(marker) + marker.length()) : null;
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

    private void filterTabularLinkedWorkItems(@NotNull Document document, @NotNull List<String> selectedRoleEnumValues) {
        Elements linkedWorkItemsCells = document.select("td[id='polarion_editor_field=linkedWorkItems']");
        for (Element linkedWorkItemsCell : linkedWorkItemsCells) {
            filterByRoles(linkedWorkItemsCell, selectedRoleEnumValues);
        }
    }

    @VisibleForTesting
    void filterNonTabularLinkedWorkItems(@NotNull Document document, @NotNull List<String> selectedRoleEnumValues) {
        Elements linkedWorkItemsContainers = document.select("span[id='polarion_editor_field=linkedWorkItems']");
        for (Element linkedWorkItemsContainer : linkedWorkItemsContainers) {
            filterByRoles(linkedWorkItemsContainer, selectedRoleEnumValues);
        }
    }

    private void filterByRoles(@NotNull Element linkedWorkItemsContainer, @NotNull List<String> selectedRoleEnumValues) {
        Element nextChild = linkedWorkItemsContainer.firstElementChild();

        List<LinkedWorkitemNodes> linkedWorkitemNodesList = new LinkedList<>();
        while (nextChild != null) {
            LinkedWorkitemNodes linkedWorkitemNodes = extractLinkedWorkItemNodes(nextChild);
            if (linkedWorkitemNodes != null) {
                nextChild = linkedWorkitemNodes.getNextSibling();
                if (!selectedRoleEnumValues.contains(linkedWorkitemNodes.role)) {
                    linkedWorkitemNodes.removeAll();
                } else {
                    linkedWorkitemNodesList.add(linkedWorkitemNodes);
                }
            } else {
                nextChild = null;
            }
        }

        for (int i = 0; i < linkedWorkitemNodesList.size(); i++) {
            if (i < linkedWorkitemNodesList.size() - 1) {
                linkedWorkitemNodesList.get(i).appendComma(); // Separate each group by comma
            } else {
                linkedWorkitemNodesList.get(i).removeBr(); // Remove br-tag after last group
            }
        }
    }

    private LinkedWorkitemNodes extractLinkedWorkItemNodes(@NotNull Element nextChild) {
        Element roleElement = null;
        String role = null;
        if (nextChild.tagName().equals(HtmlTag.DIV)) {
            Element internalElement = nextChild.children().size() == 1 ? nextChild.firstElementChild() : null;
            if (internalElement != null && internalElement.tagName().equals(HtmlTag.SPAN) && !internalElement.text().isBlank()) {
                roleElement = nextChild;
                role = internalElement.text();
            }
        }
        if (roleElement == null) {
            return null; // Not expected elements structure, stop processing
        }

        TextNode colonNode = extractColonNode(roleElement.nextSibling());
        if (colonNode == null) {
            return null; // Not expected elements structure, stop processing
        }

        Element linkedWorkItemElement = extractLinkedWorkItemElement(colonNode.nextSibling());
        if (linkedWorkItemElement == null) {
            return null; // Not expected elements structure, stop processing
        }

        // There will be no br-tag after last linked WorkItem, so not obligatory
        Element brElement = extractBrElement(linkedWorkItemElement.nextElementSibling());

        return new LinkedWorkitemNodes(role, roleElement, colonNode, linkedWorkItemElement, brElement);
    }

    private TextNode extractColonNode(@Nullable Node node) {
        if (node instanceof TextNode textNode && textNode.text().contains(":")) {
            return textNode;
        } else {
            return null;
        }
    }

    private Element extractLinkedWorkItemElement(@Nullable Node node) {
        if (node instanceof Element element) {
            return element.select("> a.polarion-Hyperlink").isEmpty() ? null : element;
        } else {
            return null;
        }
    }

    private Element extractBrElement(@Nullable Element element) {
        return element != null && element.tagName().equals(HtmlTag.BR) ? element : null;
    }

    @VisibleForTesting
    void cutEmptyWIAttributes(@NotNull Document document) {
        cutEmptyWIAttributesInTables(document);
        cutEmptyWIAttributesInText(document);
    }

    private void cutEmptyWIAttributesInTables(@NotNull Document document) {
        // Iterates through <td class="polarion-dle-workitem-fields-end-table-value"> elements and if they are empty (no value) removes enclosing them tr-elements
        Elements attributeValueCells = document.select("td.polarion-dle-workitem-fields-end-table-value");
        for (Element attributeValueCell : attributeValueCells) {
            if (attributeValueCell.text().isEmpty()) {
                Element parent = attributeValueCell.parent();
                if (parent != null && parent.nodeName().equals(HtmlTag.TR)) {
                    parent.remove();
                }
            }
        }
    }

    private void cutEmptyWIAttributesInText(@NotNull Document document) {
        // Iterates through sequential spans and if second one in this sequence has title="This field is empty", removes such sequence.
        // Finally removes comma separator which and if precedes this sequence
        Elements sequentialSpans = document.select("span > span");
        for (Element span : sequentialSpans) {
            if (EMPTY_FIELD_TITLE.equals(span.attr("title"))) {
                Element parent = span.parent();
                if (parent != null) {
                    Node previousSibling = parent.previousSibling();
                    if (previousSibling instanceof TextNode previousSiblingTextNode && ", ".equals(previousSiblingTextNode.text())) {
                        previousSiblingTextNode.remove();
                    }
                    parent.remove();
                }
            }
        }
    }

    void removePageBreakAvoids(@NotNull Document document) {
        // Polarion wraps content of a work item as it is into table's cell with table's styling "page-break-inside: avoid"
        // if it's configured to avoid page breaks:
        //
        // <table style="page-break-inside:avoid;">
        //   <tr>
        //     <td>
        //       <CONTENT>
        //     </td>
        //   </tr>
        // </table>
        //
        // This styling "page-break-inside: avoid" doesn't influence rendering by pd4ml converter,
        // but breaks rendering of tables with help of WeasyPrint. Moreover, this configuration was initially introduced
        // for pd4ml converter because table headers are not repeated at page start when table takes more than 1 page.
        // Last drawback is not applied to WeasyPrint and thus such workaround can be safely removed.
        //
        // Taking into account that work item content can also contain tables this task should be done with cautious.
        // Removing "page-break-inside:avoid;" from table's styling doesn't help, tables are still broken. So, solution
        // is to remove that table wrapping at all. As a result above example should become just:
        //
        // <CONTENT>
        //

        Elements tables = document.select("table");
        for (Element table : tables) {
            String pageBreakInsideValue = getCssValue(table, CssProp.PAGE_BREAK_INSIDE);
            if (!pageBreakInsideValue.equals(CssProp.PAGE_BREAK_INSIDE_AVOID_VALUE)) {
                continue;
            }

            Element tbody = JSoupUtils.getSingleChildByTag(table, HtmlTag.TBODY);

            Element tr = JSoupUtils.getSingleChildByTag(tbody != null ? tbody : table, HtmlTag.TR);
            if (tr != null) {
                Element td = JSoupUtils.getSingleChildByTag(tr, HtmlTag.TD);
                if (td != null) {
                    // Move td's children to replace the table
                    for (Node contentNodes : td.childNodes()) {
                        table.before(contentNodes.clone());
                    }
                    table.remove();
                }
            }
        }
    }

    public void fixNestedLists(Document doc) {
        // Polarion generates not valid HTML for multi-level lists:
        //
        // <ol>
        //   <li>first item</li>
        //   <ol>
        //     <li>sub-item</li
        //   </ol>
        // </ol>
        //
        // By HTML specification ol/ul elements can contain only li-elements as their direct children.
        // So, valid HTML will be:
        //
        // <ol>
        //   <li>first item
        //     <ol>
        //       <li>sub-item</li
        //     </ol>
        //   </li>
        // </ol>
        //
        // This method fixes the problem described above.

        boolean modified;
        String listsSelector = String.format("%s, %s", HtmlTag.OL, HtmlTag.UL);
        do {
            modified = false;
            Elements lists = doc.select(listsSelector);

            for (Element list : lists) {
                modified = fixNestedLists(list);
                if (modified) {
                    break; // Restart to avoid concurrent modification
                }
            }
        } while (modified); // Repeat to cover all nesting levels, until the point when nothing was modified / fixed
    }

    private boolean fixNestedLists(@NotNull Element list) {
        for (Element child : list.children()) {
            if (child.tagName().equals(HtmlTag.OL) || child.tagName().equals(HtmlTag.UL)) {
                Element previousSibling = child.previousElementSibling();
                if (previousSibling != null && previousSibling.tagName().equals(HtmlTag.LI)) {
                    child.remove();
                    previousSibling.appendChild(child);
                    return true;
                }
            }
        }
        return false;
    }

    @VisibleForTesting
    void localizeEnums(@NotNull Document document, @NotNull ExportParams exportParams) {
        String localizationSettingsName = exportParams.getLocalization() != null ? exportParams.getLocalization() : NamedSettings.DEFAULT_NAME;
        Map<String, String> localizationMap = localizationSettings.load(exportParams.getProjectId(), SettingId.fromName(localizationSettingsName)).getLocalizationMap(exportParams.getLanguage());

        Elements enums = document.select("span.polarion-JSEnumOption");
        for (Element enumElement : enums) {
            String replacementString = localizationMap.get(enumElement.text());
            if (!StringUtils.isEmptyTrimmed(replacementString)) {
                enumElement.text(replacementString);
            }
        }
    }

    public void adjustContentToFitPage(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        new PageWidthAdjuster(document, conversionParams)
                .adjustImageSizeInTables()
                .adjustImageSize()
                .adjustTableSize();
    }

    public @NotNull String adjustContentToFitPage(@NotNull String html, @NotNull ConversionParams conversionParams) {
        return new PageWidthAdjuster(html, conversionParams)
                .adjustImageSizeInTables()
                .adjustImageSize()
                .adjustTableSize()
                .toHTML();
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

        html = html.replace("[span class=sticky-note]", "<span class='sticky-note'>");
        html = html.replace("[span class=sticky-note-time]", "<span class='sticky-note-time'>");
        html = html.replace("[span class=sticky-note-username]", "<span class='sticky-note-username'>");
        html = html.replace("[span class=sticky-note-text]", "<span class='sticky-note-text'>");
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

    @VisibleForTesting
    void addTableOfFigures(@NotNull Document document) {
        for (Element tofPlaceholder : document.select("div[id*=macro name=tof][data-sequence]")) {
            String label = tofPlaceholder.dataset().get("sequence");
            Element tof = generateTableOfFigures(document, label);
            tofPlaceholder.before(tof);
            tofPlaceholder.remove();
        }
    }

    private Element generateTableOfFigures(@NotNull Document document, @NotNull String label) {
        Element tof = new Element(HtmlTag.DIV);
        int generatedAnchorIndex = 0;

        // Find all caption spans with the specified data-sequence, regardless of whether they have anchors
        for (Element captionSpan : document.select(String.format("p.polarion-rte-caption-paragraph span.polarion-rte-caption[data-sequence=%s]",
                escapeCssSelectorValue(label)))) {

            // Check if anchor already exists inside the span
            Element existingAnchor = captionSpan.selectFirst(String.format("a[name^=%s]", TABLE_OF_FIGURES_ANCHOR_ID_PREFIX));
            String anchorId;

            if (existingAnchor != null) {
                // Use existing anchor id
                anchorId = existingAnchor.attr("name").substring(TABLE_OF_FIGURES_ANCHOR_ID_PREFIX.length());
            } else {
                // Generate new anchor and insert it into the span
                anchorId = "generated_" + generatedAnchorIndex++;
                Element newAnchor = new Element(HtmlTag.A);
                newAnchor.attr("name", TABLE_OF_FIGURES_ANCHOR_ID_PREFIX + anchorId);
                captionSpan.appendChild(newAnchor);
            }

            Node numberNode = captionSpan.childNodes().stream().filter(TextNode.class::isInstance).findFirst().orElse(null);
            String number = numberNode instanceof TextNode numberTextNode ? numberTextNode.text() : null;
            Node captionNode = captionSpan.nextSibling();
            String caption = captionNode instanceof TextNode captionTextNode ? captionTextNode.text() : null;

            if (StringUtils.isEmpty(anchorId) || number == null || caption == null) {
                continue;
            }

            while (caption.contains(COMMENT_START)) {
                StringBuilder captionBuf = new StringBuilder(caption);
                int start = caption.indexOf(COMMENT_START);
                int ending = HtmlUtils.getEnding(caption, start, COMMENT_START, COMMENT_END);
                captionBuf.replace(start, ending, "");
                caption = captionBuf.toString();
            }

            Element tofItem = new Element(HtmlTag.A);
            tofItem.attr(HtmlTagAttr.HREF, String.format("#%s%s", TABLE_OF_FIGURES_ANCHOR_ID_PREFIX, anchorId));
            tofItem.text(String.format("%s %s. %s", label, number, caption.trim()));

            tof.appendChild(tofItem);
            tof.appendChild(new Element(HtmlTag.BR));
        }

        return tof;
    }

    private String escapeCssSelectorValue(String value) {
        if (value == null) {
            return "";
        }

        return "'"
                + value
                .replace("\\", "\\\\") // Escape backslash (must be first!)
                .replace("\"", "\\\"") // Escape double quotes
                .replace("'", "\\'")   // Escape single quotes
                + "'";
    }

    @VisibleForTesting
    void adjustReportedBy(@NotNull Document document) {
        Element reportedByDiv = document.select("div:contains(Reported by):not(:has(div))").first();
        if (reportedByDiv != null) {
            CSSStyleDeclaration cssStyle = getCssStyle(reportedByDiv);
            cssStyle.setProperty(CssProp.TOP, "0", null);
            cssStyle.setProperty(CssProp.FONT_SIZE, "8px", null);
            reportedByDiv.attr(HtmlTagAttr.STYLE, cssStyle.getCssText());
        }
    }

    @VisibleForTesting
    void cutExportToPdfButton(@NotNull Document document) {
        // Searches for 'Export to PDF' button enclosed into table-element with class 'polarion-TestsExecutionButton-buttons-content',
        // which in its turn enclosed into div-element with class 'polarion-TestsExecutionButton-buttons-pdf'
        Element exportToPdfButtonStructure = document.select("div.polarion-TestsExecutionButton-buttons-pdf:has(table.polarion-TestsExecutionButton-buttons-content:has(div.polarion-TestsExecutionButton-labelTextNew:contains(Export to PDF)))").first();
        if (exportToPdfButtonStructure != null) {
            exportToPdfButtonStructure.remove();
        }
    }

    @VisibleForTesting
    void adjustColumnWidthInReports(@NotNull Document document) {
        Elements reportTables = document.select("table.polarion-rp-column-layout");
        for (Element reportTable : reportTables) {
            CSSStyleDeclaration cssStyle = getCssStyle(reportTable);
            String width = getCssValue(cssStyle, CssProp.WIDTH);
            if ("1000px".equals(width)) {
                cssStyle.setProperty(CssProp.WIDTH, "100%", null);
                reportTable.attr(HtmlTagAttr.STYLE, cssStyle.getCssText());
            }
        }
    }

    @VisibleForTesting
    void removeFloatLeftFromReports(@NotNull Document document) {
        Elements tables = document.select("table");
        for (Element table : tables) {
            CSSStyleDeclaration cssStyle = getCssStyle(table);
            String cssFloat = getCssValue(cssStyle, CssProp.FLOAT);
            if (CssProp.FLOAT_LEFT_VALUE.equals(cssFloat)) {
                cssStyle.removeProperty(CssProp.FLOAT);
                if (cssStyle.getLength() == 0) {
                    table.removeAttr(HtmlTagAttr.STYLE);
                } else {
                    table.attr(HtmlTagAttr.STYLE, cssStyle.getCssText());
                }
            }
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

    private String getCssValue(@NotNull Element element, @NotNull String cssProperty) {
        CSSStyleDeclaration cssStyle = getCssStyle(element);
        return getCssValue(cssStyle, cssProperty);
    }

    private String getCssValue(@NotNull CSSStyleDeclaration cssStyle, @NotNull String cssProperty) {
        return Optional.ofNullable(cssStyle.getPropertyValue(cssProperty)).orElse("").trim();
    }

    private CSSStyleDeclaration getCssStyle(@NotNull Element element) {
        String style = "";
        if (element.hasAttr(HtmlTagAttr.STYLE)) {
            style = element.attr(HtmlTagAttr.STYLE);
        }
        return parseCss(style);
    }

    private CSSStyleDeclaration parseCss(@NotNull String style) {
        return CssUtils.parseCss(parser, style);
    }

    private DocumentTOCGenerator getTocGenerator(DocumentType documentType) {
        return switch (documentType) {
            case LIVE_DOC, WIKI_PAGE -> new LiveDocTOCGenerator();
            case LIVE_REPORT, TEST_RUN -> new LiveReportTOCGenerator();
            default -> throw new IllegalArgumentException(UNSUPPORTED_DOCUMENT_TYPE.formatted(documentType));
        };
    }

    /**
     * Internal record to hold chapter information during processing.
     */
    private record ChapterInfo(@NotNull Element heading, boolean shouldKeep) {
    }

    private record LinkedWorkitemNodes(@NotNull String role, @NotNull Element roleElement, @NotNull TextNode colonNode,
                                       @NotNull Element linkedWorkItemElement, @Nullable Element brElement) {
        Element getNextSibling() {
            return brElement != null ? brElement.nextElementSibling() : linkedWorkItemElement.nextElementSibling();
        }

        void removeAll() {
            roleElement.remove();
            colonNode.remove();
            linkedWorkItemElement.remove();
            if (brElement != null) {
                brElement.remove();
            }
        }

        void removeBr() {
            if (brElement != null) {
                brElement.remove();
            }
        }

        void appendComma() {
            linkedWorkItemElement.after(",");
        }

    }
}

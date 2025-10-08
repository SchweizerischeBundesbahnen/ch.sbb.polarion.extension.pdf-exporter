package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Element;
import org.w3c.dom.Document;
import org.xhtmlrenderer.newtable.TableSectionBox;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.LineBox;
import org.xhtmlrenderer.simple.Graphics2DRenderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class TableAnalyzer {
    private static final String TABLE = "table";
    private static final String TBODY = "tbody";
    private static final String TR = "tr";
    private static final String TD = "td";
    private static final String TH = "th";

    // Doesn't really matter, our concern here are widths
    private static final int PAGE_HEIGHT = 1000;

    public Map<Integer, Integer> getColumnWidths(@NotNull Element tableElement, int pageWidth) {
        Map<Integer, Integer> columnWidths = new HashMap<>();

        Document doc = toSelfDocument(tableElement);
        Box rootBox = render(doc, pageWidth);
        findTableAndAnalyze(rootBox, columnWidths);

        return adjustWidths(columnWidths, pageWidth);
    }

    private Document toSelfDocument(@NotNull Element tableElement) {
        org.jsoup.nodes.Document tempDoc = org.jsoup.nodes.Document.createShell("");
        tempDoc.body().appendChild(tableElement.clone());
        return new W3CDom().fromJsoup(tempDoc);
    }

    private Box render(@NotNull Document doc, int pageWidth) {
        Graphics2DRenderer renderer = new Graphics2DRenderer(doc, "");

        BufferedImage image = new BufferedImage(pageWidth, PAGE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = image.createGraphics();
        try {
            Dimension dim = new Dimension(pageWidth, PAGE_HEIGHT);
            renderer.layout(g2d, dim);
        } finally {
            g2d.dispose();
        }

        return renderer.getPanel().getRootBox();
    }

    private void findTableAndAnalyze(Box box, Map<Integer, Integer> columnWidths) {
        if (box == null) {
            return;
        }

        // Check if this is a table box
        if (box.getElement() != null && TABLE.equalsIgnoreCase(box.getElement().getNodeName())) {
            gatherColumnWidths(box, columnWidths);
            return; // Found and analyzed, no need to go deeper
        }

        // Recursively search children
        if (box instanceof LineBox lineBox) {
            for (Box inlinedBox : lineBox.getNonFlowContent()) {
                findTableAndAnalyze(inlinedBox, columnWidths);
            }
        } else {
            for (int i = 0; i < box.getChildCount(); i++) {
                findTableAndAnalyze(box.getChild(i), columnWidths);
            }
        }
    }

    private void gatherColumnWidths(@NotNull Box tableBox, @NotNull Map<Integer, Integer> columnWidths) {
        List<Box> tbody = findChildrenByTag(tableBox, TBODY);
        List<Box> rows = findChildrenByTag(!tbody.isEmpty() ? tbody.get(0) : tableBox, TR);

        // Analyze all rows to properly handle colspan
        for (Box row : rows) {
            List<Box> cells = findChildrenByTag(row, TD, TH);
            int columnIndex = 0;

            for (Box cell : cells) {
                int colspan = getColspan(cell);
                int cellWidth = cell.getContentWidth();

                if (colspan == 1) {
                    addColumnWidth(columnWidths, columnIndex, cellWidth);
                    columnIndex++;
                } else {
                    // Multi-column cell - distribute width proportionally
                    int widthPerColumn = cellWidth / colspan;
                    for (int i = 0; i < colspan; i++) {
                        addColumnWidth(columnWidths, columnIndex + i, widthPerColumn);
                    }
                    columnIndex += colspan;
                }
            }
        }
    }

    private void addColumnWidth(@NotNull Map<Integer, Integer> columnWidths, int index, int width) {
        if (!columnWidths.containsKey(index)) {
            columnWidths.put(index, width);
        } else {
            // Update with maximum width seen
            columnWidths.put(index, Math.max(columnWidths.get(index), width));
        }
    }

    private int getColspan(@NotNull Box cell) {
        if (cell.getElement() != null && cell.getElement().hasAttribute("colspan")) {
            try {
                return Integer.parseInt(cell.getElement().getAttribute("colspan"));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    private List<Box> findChildrenByTag(Box parent, String... tagNames) {
        List<Box> result = new ArrayList<>();
        Set<String> tags = new HashSet<>(Arrays.asList(tagNames));

        for (int i = 0; i < parent.getChildCount(); i++) {
            Box child = parent.getChild(i);
            // When no tbody exists and rows are included directly to table element, rows are enclosed into artificial TableSectionBox which we should just skip and go deeper into hierarchy
            if (child instanceof TableSectionBox tableSectionBox) {
                result.addAll(findChildrenByTag(tableSectionBox, tagNames));
            } else if (child.getElement() != null && tags.contains(child.getElement().getNodeName().toLowerCase())) {
                result.add(child);
            }
        }
        return result;
    }

    private Map<Integer, Integer> adjustWidths(Map<Integer, Integer> renderedWidths, int pageWidth) {
        int renderedWidth = renderedWidths.values().stream().reduce(0, Integer::sum);
        Map<Integer, Integer> adjustedWidths = new HashMap<>();
        if (renderedWidth > 0) {
            float adjustingRatio = (float) pageWidth / renderedWidth;
            for (Map.Entry<Integer, Integer> entry : renderedWidths.entrySet()) {
                adjustedWidths.put(entry.getKey(), (int) (entry.getValue() * adjustingRatio));
            }
        }
        return adjustedWidths;
    }
}

package ch.sbb.polarion.extension.pdf.exporter.util;

import com.polarion.core.util.StringUtils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class TocLeaf {
    private final TocLeaf parent;
    private final int level;
    private final String id;
    private final String number;
    private final String text;
    private final List<TocLeaf> children = new ArrayList<>();

    TocLeaf(TocLeaf parent, int level, String id, String number, String text) {
        this.parent = parent;
        this.level = level;
        this.id = id;
        this.number = number;
        this.text = text;
    }

    public String asString(int startLevel, int maxLevel) {
        if (level == 0) {
            if (children.isEmpty()) {
                return "";
            } else {
                return String.format("<ul class='toc'>%s</ul>", getChildrenString(startLevel, maxLevel));
            }
        } else {
            if (children.isEmpty()) {
                return getLeafHtml(startLevel, maxLevel, "");
            } else {
                return getLeafHtml(startLevel, maxLevel, getChildrenString(startLevel, maxLevel));
            }
        }
    }

    private String getLeafHtml(int startLevel, int maxLevel, String children) {
        if (level >= startLevel && level <= maxLevel) {
            return String.format(""
                            + "<li>"
                            + "  <a href='#%s'>" // Link ID
                            + "    <span class='number' %s>%s</span>" // List item number, it is null in case of wiki headers - then we hide it
                            + "    <span class='text'>%s</span>" // List item text
                            + "  </a>"
                            + "  <a href='#%s' class='page-number'>" // Link ID one more time
                            + "  </a>"
                            + "</li>"
                            + "%s" // Sub-list
                            + "",
                    id, number == null ? "style='display:none;'" : "", number, text, id, (!StringUtils.isEmpty(children) ? String.format("<ul>%s</ul>", children) : ""));

        } else {
            return children; // Render only children nodes output (also filtered) if this node is out of required levels range
        }
    }

    private String getChildrenString(int startLevel, int maxLevel) {
        return children.stream().map(tocLeaf -> tocLeaf.asString(startLevel, maxLevel)).collect(Collectors.joining());
    }
}

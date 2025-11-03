package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTag;
import lombok.Getter;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

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

    public Element asTableOfContent(int startLevel, int maxLevel) {
        if (level == 0) {
            Element ul = new Element(HtmlTag.UL);
            ul.addClass("toc");
            ul.appendChildren(getChildren(startLevel, maxLevel));
            return ul;
        } else {
            throw new IllegalStateException("This method should be called only for root element with level 0");
        }
    }

    private List<Element> getChildren(int startLevel, int maxLevel) {
        return children.stream().map(tocLeaf -> {
            if (tocLeaf.children.isEmpty()) {
                return tocLeaf.asTableOfContentItems(startLevel, maxLevel, List.of());
            } else {
                return tocLeaf.asTableOfContentItems(startLevel, maxLevel, tocLeaf.getChildren(startLevel, maxLevel));
            }
        }).flatMap(List::stream).toList();
    }

    private List<Element> asTableOfContentItems(int startLevel, int maxLevel, List<Element> children) {
        if (level >= startLevel && level <= maxLevel) {

            List<Element> items = new ArrayList<>();

            Element tocItem = new Element(HtmlTag.LI);

            Element textLink = new Element(HtmlTag.A);
            textLink.attr("href", String.format("#%s", id));
            if (number != null) {
                Element itemNumberSpan = new Element(HtmlTag.SPAN);
                itemNumberSpan.addClass("number");
                itemNumberSpan.text(number);
                textLink.appendChild(itemNumberSpan);
            }
            Element itemTextSpan = new Element(HtmlTag.SPAN);
            itemTextSpan.addClass("text");
            itemTextSpan.text(text);
            textLink.appendChild(itemTextSpan);
            tocItem.appendChild(textLink);

            Element pageNumberLink = new Element(HtmlTag.A);
            pageNumberLink.attr("href", String.format("#%s", id));
            pageNumberLink.addClass("page-number");
            tocItem.appendChild(pageNumberLink);

            items.add(tocItem);

            if (!children.isEmpty()) {
                Element nestedList = new Element(HtmlTag.UL);
                nestedList.appendChildren(children);
                items.add(nestedList);
            }

            return items;
        } else {
            return children; // Render only children nodes output (also filtered) if this node is out of required levels range
        }
    }

}

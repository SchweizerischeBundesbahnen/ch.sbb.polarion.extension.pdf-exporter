package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * What the CSS of an export says of the page which fitting the content to it needs, and the inline styles of the content
 * do not carry: the rules which make a table header taller, and the height each page leaves its content.
 *
 * @param tableHeaderCss the rules of the CSS which make a table header taller, as CSS the measure of a table reads
 * @param heights        the height each page leaves its content, of the page every page is and of each named page
 * @param namedPages     whether the content is printed on the named pages the export gives the areas between page breaks
 * @param roomUnderAnImage the room a paragraph takes under an image, which an image filling a page gives up
 */
public record PageLayout(@NotNull String tableHeaderCss, @NotNull PageRules.Heights heights, boolean namedPages, int roomUnderAnImage) {

    /** Nothing of the CSS: the measure reads inline styles alone, and a page is as high as its paper size makes it. */
    public static final PageLayout NONE = new PageLayout("", PageRules.Heights.NONE, false, ParagraphRules.DEFAULT_ROOM_PX);

    public static @NotNull PageLayout of(@NotNull String css) {
        return new PageLayout(TableHeaderRules.measuredBy(css), PageRules.contentHeights(css), false, ParagraphRules.roomUnderAnImage(css));
    }

    /** The same layout for an area between page breaks, which the export prints on a page of its own name. */
    public @NotNull PageLayout onNamedPages() {
        return new PageLayout(tableHeaderCss, heights, true, roomUnderAnImage);
    }

    /** The height the page leaves its content, as the CSS gives it, or as its paper size makes it where the CSS gives none. */
    public int contentHeight(@NotNull ConversionParams page) {
        Integer height = heightsOfThePage().get(nameOf(page));
        return height != null ? height : PaperSizeUtils.getMaxHeight(page);
    }

    /**
     * The height an image may fill on the page: the height the CSS gives the page less the room a paragraph takes under
     * what fills it, the one the image stands in or the empty one Polarion ends a document with. Where the CSS gives no
     * height, the one of the paper size is lower than any page by more than that.
     */
    public int heightForAnImage(@NotNull ConversionParams page) {
        return givesContentHeightOf(page) ? contentHeight(page) - roomUnderAnImage : PaperSizeUtils.getMaxHeight(page);
    }

    /** Whether the CSS gives the height of the page, which it fills to the pixel, rather than the paper size a lower one. */
    public boolean givesContentHeightOf(@NotNull ConversionParams page) {
        return heightsOfThePage().containsKey(nameOf(page));
    }

    private @NotNull Map<String, Integer> heightsOfThePage() {
        return namedPages ? heights.ofNamedPage() : heights.ofEveryPage();
    }

    private static @NotNull String nameOf(@NotNull ConversionParams page) {
        PaperSize size = page.getPaperSize() != null ? page.getPaperSize() : PaperSize.A4;
        return (page.getOrientation() == Orientation.LANDSCAPE ? "land" : "port") + size.name();
    }
}

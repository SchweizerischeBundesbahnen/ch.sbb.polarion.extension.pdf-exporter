package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import com.helger.css.decl.CascadingStyleSheet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * What the CSS of an export says of the page which fitting the content to it needs, and the inline styles of the content
 * do not carry: the rules which make a table header taller, and the height each page leaves its content.
 *
 * @param tableHeaderCss the rules of the CSS which make a table header taller, as CSS the measure of a table reads
 * @param heights        the height each page leaves its content, of the page every page is and of each named page
 * @param pages          the pages the content is printed on
 * @param roomUnderAnImage the room a paragraph takes under an image, which an image filling a page gives up
 */
public record PageLayout(@NotNull String tableHeaderCss, @NotNull PageRules.Heights heights, @NotNull Pages pages, int roomUnderAnImage) {

    /** The pages content is printed on, which state margins of their own. */
    public enum Pages {
        /** The page every page is, which a document without page breaks is printed on. */
        EVERY_PAGE,
        /** The page of its own name, which the export gives an area between page breaks. */
        NAMED_PAGE,
        /** Either of them, as a Live Report whose page break widgets print sections on named pages: the lower one counts. */
        EITHER_PAGE
    }

    /** Nothing of the CSS: the measure reads inline styles alone, and a page is as high as its paper size makes it. */
    public static final PageLayout NONE = new PageLayout("", PageRules.Heights.NONE, Pages.EVERY_PAGE, ParagraphRules.DEFAULT_ROOM_PX);

    /** The layout the CSS of an export gives, read from one stylesheet: the parse of the CSS is most of what this takes. */
    public static @NotNull PageLayout of(@NotNull String css) {
        CascadingStyleSheet stylesheet = ExportStylesheet.read(css);
        return new PageLayout(TableHeaderRules.measuredBy(stylesheet), PageRules.contentHeights(stylesheet), Pages.EVERY_PAGE, ParagraphRules.roomUnderAnImage(stylesheet));
    }

    /** The same layout on the given pages. */
    public @NotNull PageLayout on(@NotNull Pages printedOn) {
        return new PageLayout(tableHeaderCss, heights, printedOn, roomUnderAnImage);
    }

    /** The height the page leaves its content, as the CSS gives it, or as its paper size makes it where the CSS gives none. */
    public int contentHeight(@NotNull ConversionParams page) {
        Integer height = heightOf(page);
        return height != null ? height : PaperSizeUtils.getMaxHeight(page);
    }

    /** Whether the CSS gives the height of the page, which it fills to the pixel, rather than the paper size a lower one. */
    public boolean givesContentHeightOf(@NotNull ConversionParams page) {
        return heightOf(page) != null;
    }

    /**
     * The height an image may fill on the page: the height the CSS gives the page less the room a paragraph takes under
     * what fills it, the one the image stands in or the empty one Polarion ends a document with. Where the CSS gives no
     * height, the one of the paper size is lower than any page by more than that.
     */
    public int heightForAnImage(@NotNull ConversionParams page) {
        return givesContentHeightOf(page) ? contentHeight(page) - roomUnderAnImage : PaperSizeUtils.getMaxHeight(page);
    }

    private @Nullable Integer heightOf(@NotNull ConversionParams page) {
        String name = nameOf(page);
        Integer everyPage = heights.ofEveryPage().get(name);
        Integer namedPage = heights.ofNamedPage().get(name);
        return switch (pages) {
            case EVERY_PAGE -> everyPage;
            case NAMED_PAGE -> namedPage;
            case EITHER_PAGE -> everyPage == null || namedPage == null ? null : Math.min(everyPage, namedPage);
        };
    }

    private static @NotNull String nameOf(@NotNull ConversionParams page) {
        PaperSize size = page.getPaperSize() != null ? page.getPaperSize() : PaperSize.A4;
        return (page.getOrientation() == Orientation.LANDSCAPE ? "land" : "port") + size.name();
    }
}

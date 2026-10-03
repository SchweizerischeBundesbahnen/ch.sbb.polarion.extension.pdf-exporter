package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import lombok.Getter;
import ch.sbb.polarion.extension.pdf_exporter.util.PageLayout;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;

public class PageWidthAdjuster {

    @Getter
    private final @NotNull Document document;

    private final @NotNull ImageSizeInTablesAdjuster imageSizeInTablesAdjuster;
    private final @NotNull ImageSizeAdjuster imageSizeAdjuster;
    private final @NotNull TableSizeAdjuster tableSizeAdjuster;

    public PageWidthAdjuster(@NotNull String html) {
        this(html, ConversionParams.builder().build());
    }

    public PageWidthAdjuster(@NotNull String html, @NotNull ConversionParams conversionParams) {
        this(Jsoup.parse(html), conversionParams);
    }

    public PageWidthAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams) {
        this(document, conversionParams, PageLayout.NONE);
    }

    /** @param pageLayout what the CSS of the export says of the page which fitting the content to it needs */
    public PageWidthAdjuster(@NotNull Document document, @NotNull ConversionParams conversionParams, @NotNull PageLayout pageLayout) {
        this.document = document;
        this.document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.base)
                .prettyPrint(false);

        imageSizeInTablesAdjuster = new ImageSizeInTablesAdjuster(document, conversionParams, pageLayout);
        imageSizeAdjuster = new ImageSizeAdjuster(document, conversionParams, pageLayout);
        tableSizeAdjuster = new TableSizeAdjuster(document, conversionParams);
    }

    public @NotNull String toHTML() {
        String html = document.body().html();
        // after processing with jsoup we need to replace $-symbol with "&dollar;" because of regular expressions, as it has special meaning there
        html = html.replace("$", "&dollar;");
        return html;
    }

    public @NotNull PageWidthAdjuster adjustImageSize() {
        imageSizeAdjuster.execute();
        return this;
    }

    public @NotNull PageWidthAdjuster adjustImageSizeInTables() {
        imageSizeInTablesAdjuster.execute();
        return this;
    }

    public @NotNull PageWidthAdjuster adjustTableSize() {
        tableSizeAdjuster.execute();
        return this;
    }
}

package ch.sbb.polarion.extension.pdf_exporter.converter;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.pdf_exporter.constants.HtmlTag;
import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlLogger;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfExporterFileResourceProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.html.HtmlLinksHelper;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HtmlToPdfConverter {
    private final PdfTemplateProcessor pdfTemplateProcessor;
    private final HtmlProcessor htmlProcessor;
    @Getter
    private final WeasyPrintServiceConnector weasyPrintServiceConnector;

    public HtmlToPdfConverter() {
        this.pdfTemplateProcessor = new PdfTemplateProcessor();
        PdfExporterFileResourceProvider fileResourceProvider = new PdfExporterFileResourceProvider();
        this.htmlProcessor = new HtmlProcessor(fileResourceProvider, new LocalizationSettings(), new HtmlLinksHelper(fileResourceProvider), null);
        this.weasyPrintServiceConnector = new WeasyPrintServiceConnector();
    }

    @VisibleForTesting
    public HtmlToPdfConverter(PdfTemplateProcessor pdfTemplateProcessor, HtmlProcessor htmlProcessor, WeasyPrintServiceConnector weasyPrintServiceConnector) {
        this.pdfTemplateProcessor = pdfTemplateProcessor;
        this.htmlProcessor = htmlProcessor;
        this.weasyPrintServiceConnector = weasyPrintServiceConnector;
    }

    public byte[] convert(@NotNull String origHtml, @NotNull ConversionParams conversionParams) {
        validateHtml(origHtml);
        String html = preprocessHtml(origHtml, conversionParams);
        if (PdfExporterExtensionConfiguration.getInstance().isDebug()) {
            new HtmlLogger().log(origHtml, html, "");
        }

        WeasyPrintOptions weasyPrintOptions = WeasyPrintOptions.builder()
                .followHTMLPresentationalHints(conversionParams.isFollowHTMLPresentationalHints())
                .pdfVariant(conversionParams.getPdfVariant())
                .build();
        return weasyPrintServiceConnector.convertToPdf(html, weasyPrintOptions);
    }

    private void validateHtml(String origHtml) {
        if (!checkTagExists(origHtml, "html") || !checkTagExists(origHtml, "body")) {
            throw new IllegalArgumentException("Input html is malformed, expected html and body tags");
        }
    }

    public boolean checkTagExists(String html, String tagName) {
        return RegexMatcher.get("<" + tagName + "(\\s[^>]*)?\\s*/?>").anyMatch(html);
    }

    @NotNull
    @VisibleForTesting
    String preprocessHtml(@NotNull String origHtml, @NotNull ConversionParams conversionParams) {
        Document document = Jsoup.parse(origHtml);
        Element head = document.head();
        @Nullable Element styleTag = head.selectFirst(HtmlTag.STYLE);

        head.append(pdfTemplateProcessor.buildBaseUrlHeader());

        String additionalCss = pdfTemplateProcessor.buildSizeCss(conversionParams.getOrientation(), conversionParams.getPaperSize());
        if (styleTag != null) {
            styleTag.appendText(additionalCss);
        } else {
            head.appendElement(HtmlTag.STYLE).text(additionalCss);
        }

        if (conversionParams.isFitToPage()) {
            document = htmlProcessor.adjustContentToFitPage(document, conversionParams);
        }

        String processedHtml = htmlProcessor.replaceResourcesAsBase64Encoded(document.html());
        processedHtml = htmlProcessor.internalizeLinks(processedHtml);

        return processedHtml;
    }

}

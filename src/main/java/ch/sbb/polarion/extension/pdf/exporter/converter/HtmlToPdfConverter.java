package ch.sbb.polarion.extension.pdf.exporter.converter;

import ch.sbb.polarion.extension.pdf.exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf.exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf.exporter.util.HtmlLogger;
import ch.sbb.polarion.extension.pdf.exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfExporterFileResourceProvider;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.html.HtmlLinksHelper;
import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.service.WeasyPrintServiceConnector;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

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

    public byte[] convert(String origHtml, Orientation orientation, PaperSize paperSize) {
        validateHtml(origHtml);
        String html = preprocessHtml(origHtml, orientation, paperSize);
        if (PdfExporterExtensionConfiguration.getInstance().isDebug()) {
            new HtmlLogger().log(origHtml, html, "");
        }
        return weasyPrintServiceConnector.convertToPdf(html, WeasyPrintOptions.builder().followHTMLPresentationalHints(true).build());
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
    String preprocessHtml(String origHtml, Orientation orientation, PaperSize paperSize) {
        String origHead = extractTagContent(origHtml, "head");
        String origCss = extractTagContent(origHead, "style");

        String head = origHead + pdfTemplateProcessor.buildBaseUrlHeader();
        String css = origCss + pdfTemplateProcessor.buildSizeCss(orientation, paperSize);
        if (origCss.isBlank()) {
            head = head + String.format("<style>%s</style>", css);
        } else {
            head = replaceTagContent(head, "style", css);
        }
        String html;
        if (origHead.isBlank()) {
            html = addHeadTag(origHtml, head);
        } else {
            html = replaceTagContent(origHtml, "head", head);
        }
        html = htmlProcessor.replaceImagesAsBase64Encoded(html);
        html = htmlProcessor.internalizeLinks(html);

        return html;
    }

    private String extractTagContent(String html, String tag) {
        return RegexMatcher.get("<" + tag + ">(.*?)</" + tag + ">", RegexMatcher.DOTALL)
                .findFirst(html, regexEngine -> regexEngine.group(1)).orElse("");
    }

    private String replaceTagContent(String container, String tag, String newContent) {
        return RegexMatcher.get("<" + tag + ">(.*?)</" + tag + ">", RegexMatcher.DOTALL)
                .replaceAll(container, "<" + tag + ">" + newContent + "</" + tag + ">");
    }

    private String addHeadTag(String html, String headContent) {
        String pattern = "(<html[^<>]*>)";
        return html.replaceAll(pattern, "$1<head>" + headContent + "</head>");
    }
}

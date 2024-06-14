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
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConverter;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConnectorFactory;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlToPdfConverter {
    private final PdfTemplateProcessor pdfTemplateProcessor;
    private final HtmlProcessor htmlProcessor;
    @Getter
    private final WeasyPrintConverter weasyPrintConverter;

    public HtmlToPdfConverter() {
        this.pdfTemplateProcessor = new PdfTemplateProcessor();
        PdfExporterFileResourceProvider fileResourceProvider = new PdfExporterFileResourceProvider();
        this.htmlProcessor = new HtmlProcessor(fileResourceProvider, new LocalizationSettings(), new HtmlLinksHelper(fileResourceProvider));
        this.weasyPrintConverter = WeasyPrintConnectorFactory.getWeasyPrintExecutor();
    }

    @VisibleForTesting
    public HtmlToPdfConverter(PdfTemplateProcessor pdfTemplateProcessor, HtmlProcessor htmlProcessor, WeasyPrintConverter weasyPrintConverter) {
        this.pdfTemplateProcessor = pdfTemplateProcessor;
        this.htmlProcessor = htmlProcessor;
        this.weasyPrintConverter = weasyPrintConverter;
    }

    public byte[] convert(String origHtml, Orientation orientation, PaperSize paperSize) {
        validateHtml(origHtml);
        String html = preprocessHtml(origHtml, orientation, paperSize);
        if (PdfExporterExtensionConfiguration.getInstance().isDebug()) {
            new HtmlLogger().log(origHtml, html, "");
        }
        return weasyPrintConverter.convertToPdf(html, WeasyPrintOptions.builder().followHTMLPresentationalHints(true).build());
    }

    private void validateHtml(String origHtml) {
        if (!checkTagExists(origHtml, "html") || !checkTagExists(origHtml, "body")) {
            throw new IllegalArgumentException("Input html is malformed, expected html and body tags");
        }
    }
    public boolean checkTagExists(String html, String tagName) {
        String regex = "<" + tagName + "(\\s[^>]*)?\\s*/?>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        return matcher.find();
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
        String regex = "<" + tag + ">(.*?)</" + tag + ">";

        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }

    private String replaceTagContent(String container, String tag, String newContent) {
        String regex = "<" + tag + ">(.*?)</" + tag + ">";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(container);
        return matcher.replaceAll("<" + tag + ">" + newContent + "</" + tag + ">");
    }

    private String addHeadTag(String html, String headContent) {
        String pattern = "(<html[^>]*>)";
        return html.replaceAll(pattern, "$1<head>" + headContent + "</head>");
    }
}

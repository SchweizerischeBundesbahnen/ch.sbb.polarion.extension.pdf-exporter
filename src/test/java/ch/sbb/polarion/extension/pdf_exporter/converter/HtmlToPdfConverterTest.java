package ch.sbb.polarion.extension.pdf_exporter.converter;

import ch.sbb.polarion.extension.pdf_exporter.TestStringUtils;
import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.WeasyPrintOptions;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.service.WeasyPrintServiceConnector;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, PdfExporterExtensionConfigurationExtension.class})
class HtmlToPdfConverterTest {
    @Mock
    private PdfTemplateProcessor pdfTemplateProcessor;

    @Mock
    private HtmlProcessor htmlProcessor;

    @Mock
    private WeasyPrintServiceConnector weasyPrintServiceConnector;

    @InjectMocks
    private HtmlToPdfConverter htmlToPdfConverter;

    @Test
    void shouldInjectHeadAndStyle() {
        String html = """
                <html>
                    <body>
                        <span>example text</span>
                    </body>
                </html>""";

        when(pdfTemplateProcessor.buildBaseUrlHeader()).thenReturn("<base href='http://test' />");
        when(pdfTemplateProcessor.buildSizeCss(Orientation.PORTRAIT, PaperSize.A4)).thenReturn("@page {size: test;}");
        when(htmlProcessor.replaceResourcesAsBase64Encoded(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(htmlProcessor.internalizeLinks(anyString())).thenAnswer(a -> a.getArgument(0));
        String resultHtml = htmlToPdfConverter.preprocessHtml(html, ConversionParams.builder().build());

        assertThat(TestStringUtils.removeNonsensicalSymbols(resultHtml)).isEqualTo(TestStringUtils.removeNonsensicalSymbols("""
                <html><head><base href="http://test"><style>@page {size: test;}</style></head>
                    <body>
                        <span>example text</span>
                    </body>
                </html>"""));

        verify(htmlProcessor).replaceResourcesAsBase64Encoded(resultHtml);
    }

    @Test
    void shouldExtendExistingHeadAndStyle() {
        String html = """
                <html>
                    <head>
                        <style>style content</style>
                    </head>
                    <body>
                        <span>example text</span>
                    </body>
                </html>""";

        when(pdfTemplateProcessor.buildBaseUrlHeader()).thenReturn("<base href='http://test' />");
        when(pdfTemplateProcessor.buildSizeCss(Orientation.LANDSCAPE, PaperSize.A3)).thenReturn(" @page {size: test;}");
        when(htmlProcessor.replaceResourcesAsBase64Encoded(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(htmlProcessor.internalizeLinks(anyString())).thenAnswer(a -> a.getArgument(0));
        ConversionParams conversionParams = ConversionParams.builder()
                .orientation(Orientation.LANDSCAPE)
                .paperSize(PaperSize.A3)
                .build();
        String resultHtml = htmlToPdfConverter.preprocessHtml(html, conversionParams);

        assertThat(TestStringUtils.removeNonsensicalSymbols(resultHtml)).isEqualTo(TestStringUtils.removeNonsensicalSymbols("""
                <html>
                    <head>
                        <style>style content @page {size: test;}</style>
                        <base href="http://test">
                    </head>
                    <body>
                        <span>example text</span>
                    </body>
                </html>"""));

        verify(htmlProcessor).replaceResourcesAsBase64Encoded(resultHtml);
    }

    @Test
    void shouldExtendHeadAndAddStyle() {
        String html = """
                <html>
                    <head>
                        <meta charset="UTF-8">
                    </head>
                    <body>
                        <span>example text</span>
                    </body>
                </html>""";

        when(pdfTemplateProcessor.buildBaseUrlHeader()).thenReturn("<base href='http://test' />");
        when(pdfTemplateProcessor.buildSizeCss(Orientation.LANDSCAPE, PaperSize.A3)).thenReturn(" @page {size: test;}");
        when(htmlProcessor.replaceResourcesAsBase64Encoded(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(htmlProcessor.internalizeLinks(anyString())).thenAnswer(a -> a.getArgument(0));
        ConversionParams conversionParams = ConversionParams.builder()
                .orientation(Orientation.LANDSCAPE)
                .paperSize(PaperSize.A3)
                .build();
        String resultHtml = htmlToPdfConverter.preprocessHtml(html, conversionParams);

        assertThat(TestStringUtils.removeNonsensicalSymbols(resultHtml)).isEqualTo(TestStringUtils.removeNonsensicalSymbols("""
                <html>
                    <head>
                        <metacharset="UTF-8">
                        <base href="http://test">
                        <style> @page {size: test;}</style>
                    </head>
                    <body>
                        <span>example text</span>
                    </body>
                </html>"""));

        verify(htmlProcessor).replaceResourcesAsBase64Encoded(resultHtml);
    }

    @Test
    void shouldThrowIllegalArgumentForMalformedHtml() {
        String html = "<span>example text</span>";

        ConversionParams conversionParams = ConversionParams.builder()
                .orientation(Orientation.LANDSCAPE)
                .paperSize(PaperSize.A3)
                .build();
        assertThatThrownBy(() -> htmlToPdfConverter.convert(html, conversionParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("html is malformed");
    }

    @Test
    void shouldAcceptAttributesAndEmptyTags() {
        String html = """
                <html myAttribute="test">
                    <body/>
                </html>""";

        when(pdfTemplateProcessor.buildBaseUrlHeader()).thenReturn("<base href='http://test' />");
        when(pdfTemplateProcessor.buildSizeCss(Orientation.PORTRAIT, PaperSize.A4)).thenReturn("@page {size: test;}");
        when(htmlProcessor.replaceResourcesAsBase64Encoded(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(htmlProcessor.internalizeLinks(anyString())).thenAnswer(a -> a.getArgument(0));
        String resultHtml = htmlToPdfConverter.preprocessHtml(html, ConversionParams.builder().build());

        assertThat(TestStringUtils.removeNonsensicalSymbols(resultHtml)).isEqualTo(TestStringUtils.removeNonsensicalSymbols("""
                <html myattribute="test"><head><base href="http://test"><style>@page {size: test;}</style></head>
                    <body></body>
                </html>"""));

        verify(htmlProcessor).replaceResourcesAsBase64Encoded(resultHtml);
    }


    @Test
    void shouldInvokeWeasyPrint() {
        String origHtml = "<html><body>test html</body></html>";
        String resultHtml = Jsoup.parse("<html><head><base href=\"http://test\"><style>@page {size: test;}</style></head><body>test html</body></html>").html();

        when(pdfTemplateProcessor.buildBaseUrlHeader()).thenReturn("<base href='http://test' />");
        when(pdfTemplateProcessor.buildSizeCss(Orientation.LANDSCAPE, PaperSize.A3)).thenReturn("@page {size: test;}");
        when(htmlProcessor.replaceResourcesAsBase64Encoded(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(htmlProcessor.internalizeLinks(anyString())).thenAnswer(a -> a.getArgument(0));
        when(weasyPrintServiceConnector.convertToPdf(eq(resultHtml), any(WeasyPrintOptions.class))).thenReturn("test content".getBytes());

        ConversionParams conversionParams = ConversionParams.builder()
                .orientation(Orientation.LANDSCAPE)
                .paperSize(PaperSize.A3)
                .followHTMLPresentationalHints(true)
                .build();
        byte[] result = htmlToPdfConverter.convert(origHtml, conversionParams);
        assertThat(result).isEqualTo("test content".getBytes());
    }
}

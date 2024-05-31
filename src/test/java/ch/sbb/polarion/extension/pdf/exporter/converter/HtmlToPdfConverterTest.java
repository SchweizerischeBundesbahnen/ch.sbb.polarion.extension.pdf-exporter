package ch.sbb.polarion.extension.pdf.exporter.converter;

import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf.exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf.exporter.util.PdfTemplateProcessor;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConverter;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HtmlToPdfConverterTest {
    @Mock
    private PdfTemplateProcessor pdfTemplateProcessor;

    @Mock
    private HtmlProcessor htmlProcessor;

    @Mock
    private WeasyPrintConverter weasyPrintConverter;

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
        when(htmlProcessor.replaceImagesAsBase64Encoded(anyString())).thenAnswer(invocation ->
                invocation.getArgument(0));
        String resultHtml = htmlToPdfConverter.preprocessHtml(html, Orientation.PORTRAIT, PaperSize.A4);

        assertThat(resultHtml).isEqualTo("""
                <html><head><base href='http://test' /><style>@page {size: test;}</style></head>
                    <body>
                        <span>example text</span>
                    </body>
                </html>""");

        verify(htmlProcessor).replaceImagesAsBase64Encoded(resultHtml);
    }

    @Test
    void shouldExtendExistingHeadAndStyle() {
        String html = """
                <html>
                    <head>
                        test head content before
                        <style>style content</style>
                    </head>
                    <body>
                        <span>example text</span>
                    </body>
                </html>""";

        when(pdfTemplateProcessor.buildBaseUrlHeader()).thenReturn("<base href='http://test' />");
        when(pdfTemplateProcessor.buildSizeCss(Orientation.LANDSCAPE, PaperSize.A3)).thenReturn(" @page {size: test;}");
        when(htmlProcessor.replaceImagesAsBase64Encoded(anyString())).thenAnswer(invocation ->
                invocation.getArgument(0));
        String resultHtml = htmlToPdfConverter.preprocessHtml(html, Orientation.LANDSCAPE, PaperSize.A3);

        assertThat(resultHtml).isEqualTo("""
                <html>
                    <head>
                        test head content before
                        <style>style content @page {size: test;}</style>
                    <base href='http://test' /></head>
                    <body>
                        <span>example text</span>
                    </body>
                </html>""");

        verify(htmlProcessor).replaceImagesAsBase64Encoded(resultHtml);
    }

    @Test
    void shouldExtendHeadAndAddStyle() {
        String html = """
                <html>
                    <head>
                        test head content before
                    </head>
                    <body>
                        <span>example text</span>
                    </body>
                </html>""";

        when(pdfTemplateProcessor.buildBaseUrlHeader()).thenReturn("<base href='http://test' />");
        when(pdfTemplateProcessor.buildSizeCss(Orientation.LANDSCAPE, PaperSize.A3)).thenReturn(" @page {size: test;}");
        when(htmlProcessor.replaceImagesAsBase64Encoded(anyString())).thenAnswer(invocation ->
                invocation.getArgument(0));
        String resultHtml = htmlToPdfConverter.preprocessHtml(html, Orientation.LANDSCAPE, PaperSize.A3);

        assertThat(resultHtml).isEqualTo("""
                <html>
                    <head>
                        test head content before
                    <base href='http://test' /><style> @page {size: test;}</style></head>
                    <body>
                        <span>example text</span>
                    </body>
                </html>""");

        verify(htmlProcessor).replaceImagesAsBase64Encoded(resultHtml);
    }

    @Test
    void shouldThrowIllegalArgumentForMalformedHtml() {
        String html = "<span>example text</span>";

        assertThatThrownBy(() -> htmlToPdfConverter.convert(html, Orientation.LANDSCAPE, PaperSize.A3))
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
        when(htmlProcessor.replaceImagesAsBase64Encoded(anyString())).thenAnswer(invocation ->
                invocation.getArgument(0));
        String resultHtml = htmlToPdfConverter.preprocessHtml(html, Orientation.PORTRAIT, PaperSize.A4);

        assertThat(resultHtml).isEqualTo("""
                <html myAttribute="test"><head><base href='http://test' /><style>@page {size: test;}</style></head>
                    <body/>
                </html>""");

        verify(htmlProcessor).replaceImagesAsBase64Encoded(resultHtml);
    }


    @Test
    void shouldInvokeWeasyPrint() {
        String origHtml = "<html><body>test html</body></html>";
        String resultHtml = "<html><head><base href='http://test' /><style>@page {size: test;}</style></head><body>test html</body></html>";

        when(pdfTemplateProcessor.buildBaseUrlHeader()).thenReturn("<base href='http://test' />");
        when(pdfTemplateProcessor.buildSizeCss(Orientation.LANDSCAPE, PaperSize.A3)).thenReturn("@page {size: test;}");
        when(htmlProcessor.replaceImagesAsBase64Encoded(anyString())).thenAnswer(invocation ->
                invocation.getArgument(0));
        when(weasyPrintConverter.convertToPdf(resultHtml, new WeasyPrintOptions(true))).thenReturn("test content".getBytes());

        byte[] result = htmlToPdfConverter.convert(origHtml, Orientation.LANDSCAPE, PaperSize.A3);
        assertThat(result).isEqualTo("test content".getBytes());
    }
}
package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.TestStringUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import com.polarion.core.boot.PolarionProperties;
import com.polarion.core.config.Configuration;
import com.polarion.core.config.IClusterConfiguration;
import com.polarion.core.config.IConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfTemplateProcessorTest {
    private final PdfTemplateProcessor pdfTemplateProcessor = new PdfTemplateProcessor();

    @ParameterizedTest
    @MethodSource("paramsForProcessHtmlTemplate")
    void shouldProcessHtmlTemplate(boolean watermark, Orientation orientation, String baseUrl, String expectedResult) {
        // Arrange
        ExportParams exportParams = ExportParams.builder()
                .watermark(watermark)
                .orientation(orientation)
                .build();

        MockedStatic<Configuration> configuration = null;
        try {
            if (baseUrl != null) {
                System.setProperty(PolarionProperties.BASE_URL, baseUrl);
            } else {
                System.clearProperty(PolarionProperties.BASE_URL);
                configuration = mockStaticHostNameConfiguration();
            }

            // Act
            String resultHtml = pdfTemplateProcessor.processUsing(exportParams, "testDocumentName", "test css content", "test html content");

            // Assert
            assertThat(TestStringUtils.removeNonsensicalSymbols(resultHtml)).isEqualTo(TestStringUtils.removeNonsensicalSymbols(expectedResult));
        } finally {
            if (configuration != null) {
                configuration.close();
            }
        }
    }

    @ParameterizedTest
    @CsvSource({
            "test, <base href='http://test' />",
            "http://test, <base href='http://test' />",
            "https://test, <base href='https://test' />"
    })
    void shouldBuildBaseUrlFromProperties(String baseUrl, String expectedResult) {
        System.setProperty(PolarionProperties.BASE_URL, baseUrl);
        try {
            String result = pdfTemplateProcessor.buildBaseUrlHeader();
            assertThat(result).isEqualTo(expectedResult);
        } finally {
            System.setProperty(PolarionProperties.BASE_URL, "");
        }
    }

    private static MockedStatic<Configuration> mockStaticHostNameConfiguration() {
        MockedStatic<Configuration> configuration = mockStatic(Configuration.class);
        IConfiguration configInstance = mock(IConfiguration.class);
        IClusterConfiguration clusterConfiguration = mock(IClusterConfiguration.class);
        configuration.when(Configuration::getInstance).thenReturn(configInstance);
        when(configInstance.cluster()).thenReturn(clusterConfiguration);
        when(clusterConfiguration.nodeHostname()).thenReturn("testClusterNodeHostName");
        return configuration;
    }

    private static Stream<Arguments> paramsForProcessHtmlTemplate() {
        return Stream.of(
                Arguments.of(false, Orientation.PORTRAIT, null, """
                        <?xml version='1.0' encoding='UTF-8'?>
                        <!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>
                        <html lang='en' xml:lang='en' xmlns='http://www.w3.org/1999/xhtml'>
                        <head>
                            <title>testDocumentName</title>
                            <meta content='text/html; charset=UTF-8' http-equiv='Content-Type'/>
                            <base href='http://testClusterNodeHostName' />
                            <link crossorigin='anonymous' href='/polarion/ria/font-awesome-4.0.3/css/font-awesome.css' referrerpolicy='no-referrer' rel='stylesheet'/>
                            <style>
                                test css content
                                img {
                                    max-width: 100%;
                                }
                            </style>
                        </head>
                        <body class="">
                        test html content
                        </body>
                        </html>""".indent(0).trim()),

                Arguments.of(true, Orientation.LANDSCAPE, "custom base url", """
                        <?xml version='1.0' encoding='UTF-8'?>
                        <!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>
                        <html lang='en' xml:lang='en' xmlns='http://www.w3.org/1999/xhtml'>
                        <head>
                            <title>testDocumentName</title>
                            <meta content='text/html; charset=UTF-8' http-equiv='Content-Type'/>
                            <base href='http://custom base url' />
                            <link crossorigin='anonymous' href='/polarion/ria/font-awesome-4.0.3/css/font-awesome.css' referrerpolicy='no-referrer' rel='stylesheet'/>
                            <style>
                                test css content
                                @page {size: A4 landscape;}
                                img {
                                    max-width: 100%;
                                }
                            </style>
                        </head>
                        <body class="watermark">
                        test html content
                        </body>
                        </html>""".indent(0).trim())
        );
    }
}

package ch.sbb.polarion.extension.pdf.exporter.util;

import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.pdf.exporter.TestStringUtils;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.localization.Language;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.localization.LocalizationModel;
import ch.sbb.polarion.extension.pdf.exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf.exporter.util.html.HtmlLinksHelper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("ConstantConditions")
class HtmlProcessorTest {

    @Mock
    private FileResourceProvider fileResourceProvider;
    @Mock
    private LocalizationSettings localizationSettings;
    @Mock
    private HtmlLinksHelper htmlLinksHelper;

    private HtmlProcessor processor;

    @BeforeEach
    void init() {
        processor = new HtmlProcessor(fileResourceProvider, localizationSettings, htmlLinksHelper);
        Map<String, String> deTranslations = Map.of(
                "draft", "Entwurf",
                "not reviewed", "Nicht überprüft"
        );
        Map<String, String> frTranslations = Map.of(
                "draft", "Projet",
                "not reviewed", "Non revu"
        );
        Map<String, String> itTranslations = Map.of(
                "draft", "Bozza",
                "not reviewed", "Non rivisto"
        );
        LocalizationModel localizationModel = new LocalizationModel(deTranslations, frTranslations, itTranslations);

        lenient().when(localizationSettings.load(anyString(), any(SettingId.class))).thenReturn(localizationModel);
    }

    @Test
    void cutLocalUrlsTest() {
        String img = "<img title=\"diagram_123.png\" src=\"data:image/png;BASE64Content\"/>";
        String imgInsideA = String.format("<a href=\"http://localhost/polarion/module-attachment/elibrary/some-path\">%s</a>", img);
        assertEquals(img, processor.cutLocalUrls(imgInsideA));

        String span = "<span id=\"PLANID_Version_1_0\" title=\"Version 1.0 (Version_1_0) (2017-03-31)\" class=\"polarion-Plan\">Version 1.0<span> (2017-03-31)</span></span>";
        String spanInsideA = String.format("<a href=\"http://localhost/polarion/#/project/elibrary/another-path\">%s</a>", span);
        assertEquals(span, processor.cutLocalUrls(spanInsideA));
    }

    @Test
    @SneakyThrows
    void cutLocalUrlsWithRolesFilteringTest() {
        when(localizationSettings.load(any(), any(SettingId.class))).thenReturn(new LocalizationModel(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));

        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/cutLocalUrlsWithRolesFilteringBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/cutLocalUrlsWithRolesFilteringAfterProcessing.html")) {

            ExportParams exportParams = getExportParams();
            exportParams.setCutLocalUrls(true);

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            List<String> selectedRoles = Arrays.asList("has parent", "is parent of", "depends on", "blocks", "verifies", "is verified by");
            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.processHtmlForPDF(invalidHtml, exportParams, selectedRoles).replaceAll(" ", "");
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void processPageBrakesTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/pageBreaksBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/pageBreaksAfterProcessing.html")) {

            ExportParams context = new ExportParams();
            context.setOrientation(Orientation.LANDSCAPE);
            context.setPaperSize(PaperSize.A4);

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.processPageBrakes(invalidHtml, context).replaceAll(" ", "");
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void cutEmptyChaptersTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/emptyChaptersBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/emptyChaptersAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.cutEmptyChapters(invalidHtml).replaceAll(" ", "");
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void cutEmptyWIAttributesTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/emptyWIAttributesBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/emptyWIAttributesAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.cutEmptyWIAttributes(invalidHtml).replaceAll(" ", "");
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void properTableHeadsTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/invalidTableHeads.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/validTableHeads.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.properTableHeads(invalidHtml).replaceAll(" ", "");
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void adjustCellWidthTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/cellWidthBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/cellWidthAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.adjustCellWidth(invalidHtml, new ExportParams()).replaceAll(" ", "");
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void cutNotNeededChaptersTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/notNeededChaptersBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/notNeededChaptersAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.cutNotNeededChapters(invalidHtml, Collections.singletonList("2")).replaceAll(" ", "");
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void localizeEnumsTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/localizeEnumsBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/localizeEnumsAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.localizeEnums(invalidHtml, getExportParams()).replaceAll(" ", "");
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void nothingToReplaceTest() {
        String html = "<div></div>";
        String result = processor.replaceImagesAsBase64Encoded(html);
        assertEquals("<div></div>", result);
    }

    @Test
    @SneakyThrows
    void replaceImagesAsBase64EncodedTest() {
        String html = "<div><img id=\"image\" src=\"http://localhost/some-path/img.png\"/></div>";
        byte[] imgBytes;
        try (InputStream is = this.getClass().getResourceAsStream("/test_img.png")) {
            imgBytes = is != null ? is.readAllBytes() : new byte[0];
        }
        when(fileResourceProvider.getResourceAsBytes(any())).thenReturn(imgBytes);
        String result = processor.replaceImagesAsBase64Encoded(html);
        assertEquals("<div><img id=\"image\" src=\"data:image/png;base64, " + Base64.getEncoder().encodeToString(imgBytes) + "\"/></div>", result);
    }

    @Test
    @SneakyThrows
    void replaceImagesUrlUnderscoreReplacementTest() {
        // feed the url with the encoded underscore in the src attribute
        String html = "<div><img id=\"image\" src=\"http://localhost/some-path/img%5Fname.png\"/></div>";
        byte[] imgBytes;
        try (InputStream is = this.getClass().getResourceAsStream("/test_img.png")) {
            imgBytes = is != null ? is.readAllBytes() : new byte[0];
        }
        // return proper data only for URL containing decoded underscore symbol
        lenient().doReturn(imgBytes).when(fileResourceProvider).getResourceAsBytes(eq("http://localhost/some-path/img_name.png"));
        // return empty content for non-escaped URL
        lenient().doReturn(new byte[0]).when(fileResourceProvider).getResourceAsBytes("http://localhost/some-path/img%5Fname.png");
        String result = processor.replaceImagesAsBase64Encoded(html);
        assertEquals("<div><img id=\"image\" src=\"data:image/png;base64, " + Base64.getEncoder().encodeToString(imgBytes) + "\"/></div>", result);
    }

    @Test
    @SneakyThrows
    void replaceSvgImagesAsBase64EncodedTest() {
        String html = "<div><img id=\"image1\" src=\"http://localhost/some-path/img1.svg\"/> <img id='image2' src='http://localhost/some-path/img2.svg'/> <img id='image1' src='http://localhost/some-path/img1.svg'/></div>";
        byte[] imgBytes;
        try (InputStream is = new ByteArrayInputStream("<svg><switch><g requiredFeatures=\"http://www.w3.org/TR/SVG11/feature#Extensibility\"/></switch></svg>".getBytes(StandardCharsets.UTF_8))) {
            imgBytes = is != null ? is.readAllBytes() : new byte[0];
        }
        when(fileResourceProvider.getResourceAsBytes(any())).thenReturn(imgBytes);
        String result = processor.replaceImagesAsBase64Encoded(html);
        String expected = "<div><img id=\"image1\" src=\"data:null;base64, " + Base64.getEncoder().encodeToString("<svg></svg>".getBytes(StandardCharsets.UTF_8)) + "\"/> " +
                "<img id='image2' src='data:null;base64, " + Base64.getEncoder().encodeToString("<svg></svg>".getBytes(StandardCharsets.UTF_8)) + "'/> " +
                "<img id='image1' src='data:null;base64, " + Base64.getEncoder().encodeToString("<svg></svg>".getBytes(StandardCharsets.UTF_8)) + "'/></div>";
        assertEquals(expected, result);
    }

    @Test
    void processPossibleSvgImageTest() {
        byte[] basicString = "basic".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(basicString, processor.processPossibleSvgImage(basicString));
    }

    @Test
    void removeSvgUnsupportedFeatureHintTest() {
        // svg sample from polarion diagram v23.10
        assertEquals("<svg></svg>", processor.removeSvgUnsupportedFeatureHint("<svg><switch>" +
                "<g requiredFeatures=\"http://www.w3.org/TR/SVG11/feature#Extensibility\"/>" +
                "<a transform=\"translate(0,-5)\" xlink:href=\"https://www.diagrams.net/doc/faq/svg-export-text-problems\" target=\"_blank\">" +
                "<text text-anchor=\"middle\" font-size=\"10px\" x=\"50%\" y=\"100%\">Text is not SVG - cannot display</text>" +
                "<title>https://www.diagrams.net/doc/faq/svg-export-text-problems</title></a></switch>" +
                "</svg>"));

        // alternative hint from https://github.com/jgraph/drawio/issues/774 (also here is extra space before g)
        assertEquals("<svg></svg>", processor.removeSvgUnsupportedFeatureHint("<svg><switch> " +
                "<g requiredFeatures=\"http://www.w3.org/TR/SVG11/feature#Extensibility\"/>" +
                "<a transform=\"translate(0,-5)\" xlink:href=\"https://desk.draw.io/support/solutions/articles/16000042487\" target=\"_blank\">" +
                "<text text-anchor=\"middle\" font-size=\"10px\" x=\"50%\" y=\"100%\">Viewer does not support full SVG 1.1</text>" +
                "</a></switch>" +
                "</svg>"));

        // potential issue with another feature, main idea here is to cut down all requiredFeatures checks no matter which feature it is
        assertEquals("<svg></svg>", processor.removeSvgUnsupportedFeatureHint("<svg><switch>" +
                "<g requiredFeatures=\"http://www.w3.org/TR/SVG11/feature#Gradient\"/>" +
                "<a transform=\"translate(0,-5)\" xlink:href=\"https://some.url\" target=\"_blank\">" +
                "<text text-anchor=\"middle\" font-size=\"10px\" x=\"50%\" y=\"100%\">Some warning</text>" +
                "</a></switch>" +
                "</svg>"));
    }

    @Test
    @SneakyThrows
    void processHtmlForPDFTestCutEmptyWorkItemAttributesDisabled() {
        try (InputStream isHtml = this.getClass().getResourceAsStream("/emptyWIAttributesBeforeProcessing.html")) {

            String html = new String(isHtml.readAllBytes(), StandardCharsets.UTF_8);

            HtmlProcessor spyHtmlProcessor = spy(processor);
            ExportParams exportParams = getExportParams();
            // to avoid changing input html and check with regular equals
            when(spyHtmlProcessor.adjustCellWidth(html, exportParams)).thenReturn(html);
            exportParams.setCutEmptyChapters(false);

            // Spaces, new lines & nbsp symbols are removed to exclude difference in space characters
            String result = spyHtmlProcessor.processHtmlForPDF(html, exportParams, List.of()).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(html.replaceAll("&nbsp;|\u00A0", " ").replaceAll(" ", "")), TestStringUtils.removeLineEndings(result));
        }
    }

    @Test
    void adjustHeadingForPDFTestH1ReplacedWithDiv() {
        String html = "<h1>First level heading</h1>";
        String result = processor.processHtmlForPDF(html, getExportParams(), List.of());
        assertEquals("<div class=\"title\">First level heading</div>", result);
    }

    @Test
    void adjustHeadingForPDFTestLiftHeadingTag() {
        String html = "<h2>First level heading</h2>";
        String result = processor.processHtmlForPDF(html, getExportParams(), List.of());
        assertEquals("<h1>First level heading</h1>", result);
    }

    @Test
    void replaceDollars() {
        String html = "<div>100$</div>";
        String result = processor.processHtmlForPDF(html, getExportParams(), List.of());
        assertEquals("<div>100&dollar;</div>", result);
    }

    @Test
    @SneakyThrows
    void adjustContentToFitPageTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/fitToPageBeforeProcessing.html");
             InputStream isValidPortraitHtml = this.getClass().getResourceAsStream("/fitToPortraitPageAfterProcessing.html");
             InputStream isValidLandscapeHtml = this.getClass().getResourceAsStream("/fitToLandscapePageAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.adjustContentToFitPage(invalidHtml, Orientation.PORTRAIT, PaperSize.A4).replaceAll(" ", "");
            String validHtml = new String(isValidPortraitHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));

            fixedHtml = processor.adjustContentToFitPage(invalidHtml, Orientation.LANDSCAPE, PaperSize.A4).replaceAll(" ", "");
            validHtml = new String(isValidLandscapeHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void adjustImagesInTablesTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/fitImagesWidthBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/fitImagesWidthAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.adjustContentToFitPage(invalidHtml, Orientation.PORTRAIT, PaperSize.A4).replaceAll(" ", "");
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void cutExtraNbspTest() {
        try (InputStream initialHtml = this.getClass().getResourceAsStream("/extraNbspBeforeProcessing.html");
             InputStream resultHtml = this.getClass().getResourceAsStream("/extraNbspAfterProcessing.html")) {

            String initialHtmlString = new String(initialHtml.readAllBytes(), StandardCharsets.UTF_8);

            String fixedHtml = processor.cutExtraNbsp(initialHtmlString);
            String expectedHtml = new String(resultHtml.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestStringUtils.removeLineEndings(expectedHtml), TestStringUtils.removeLineEndings(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void tableOfContentTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/tableOfContentBeforeProcessing.html");
             InputStream isInvalidFormattedHtml = this.getClass().getResourceAsStream("/tableOfContentBeforeProcessingFormatted.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/tableOfContentAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);
            String invalidFormattedHtml = new String(isInvalidFormattedHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.addTableOfContent(invalidHtml).replaceAll(" ", "");
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));

            String fixedFormattedHtml = processor.addTableOfContent(invalidFormattedHtml).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedFormattedHtml));
        }
    }

    @Test
    @SneakyThrows
    void tableOfTablesTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/tableOfTablesBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/tableOfTablesAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.addTableOfFigures(invalidHtml).replaceAll(" ", "");
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void tableOfFiguresTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/tableOfFiguresBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/tableOfFiguresAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.addTableOfFigures(invalidHtml).replaceAll(" ", "");
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8).replaceAll(" ", "");
            assertEquals(TestStringUtils.removeLineEndings(validHtml), TestStringUtils.removeLineEndings(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void adjustReportedByTest() {
        String initialHtml = """
                <p id="polarion_1">
                  <span class="polarion-rp-inline-widget" data-widget="com.polarion.scriptInline" id="polarion_1_iw_1">
                    <span id="polarion-rp-widget-content">
                      <div style="color: grey; text-align: right; position: absolute; top: 22px; right: 10px">Reported by
                        <span class="polarion-no-style-cleanup">System Administrator</span>
                        <br/>
                        January 12, 2024 at 4:19:27 PM UTC
                      </div>
                    </span>
                  </span>
                </p>
            """;
        String processedHtml = processor.adjustReportedBy(initialHtml);

        String expectedHtml = """
                <p id="polarion_1">
                  <span class="polarion-rp-inline-widget" data-widget="com.polarion.scriptInline" id="polarion_1_iw_1">
                    <span id="polarion-rp-widget-content">
                      <div style="color: grey; text-align: right; position: absolute; top: 22px; right: 10px; top: 0; font-size: 8px;">Reported by
                        <span class="polarion-no-style-cleanup">System Administrator</span>
                        <br/>
                        January 12, 2024 at 4:19:27 PM UTC
                      </div>
                    </span>
                  </span>
                </p>
            """.replaceAll(" ", "");
        assertEquals(TestStringUtils.removeLineEndings(expectedHtml), TestStringUtils.removeLineEndings(processedHtml.replaceAll(" ", "")));
    }

    @Test
    @SneakyThrows
    void cutExportToPdfButtonTest() {
        String initialHtml = """
                <p id="polarion_client33">
                  <span class="polarion-rp-inline-widget" data-widget="ch.sbb.polarion.extension.pdf.exporter.widgets.exportToPdfButton" id="polarion_client33_iw_1">
                    <span id="polarion-rp-widget-content">
                      <span class="polarion-TestsExecutionButton-link">
                        <a onclick="PdfExporter.openPopup({context: &#39;report&#39;})">
                          <div style="color:#5E5E5E; text-shadow:#FCFCFC 1px 1px; border-color:#C2C2C2; background-color:#F0F0F0; " class="polarion-TestsExecutionButton-buttons-pdf">
                            <div style="height: 10px;"></div>
                            <table class="polarion-TestsExecutionButton-buttons-content">
                              <tr>
                                <td class="polarion-TestsExecutionButton-buttons-content-labelCell polarion-TestsExecutionButton-buttons-content-labelCell-noSumText">
                                  <div class="polarion-TestsExecutionButton-labelTextNew">Export to PDF</div>
                                </td>
                              </tr>
                              <tr>
                                <td style="color:#5E5E5E; text-shadow:#FCFCFC 1px 1px; " class="polarion-TestsExecutionButton-sumText"></td>
                              </tr>
                            </table>
                          </div>
                        </a>
                      </span>
                    </span>
                  </span>
                </p>
            """;
        String processedHtml = processor.cutExportToPdfButton(initialHtml);

        String expectedHtml = """
                <p id="polarion_client33">
                  <span class="polarion-rp-inline-widget" data-widget="ch.sbb.polarion.extension.pdf.exporter.widgets.exportToPdfButton" id="polarion_client33_iw_1">
                    <span id="polarion-rp-widget-content">
                      <span class="polarion-TestsExecutionButton-link">
                        <a onclick="PdfExporter.openPopup({context: &#39;report&#39;})">
                        </a>
                      </span>
                    </span>
                  </span>
                </p>
            """.replaceAll(" ", "");
        assertEquals(TestStringUtils.removeLineEndings(expectedHtml), TestStringUtils.removeLineEndings(processedHtml.replaceAll(" ", "")));
    }

    @Test
    @SneakyThrows
    void adjustColumnWidthInReportsTest() {
        String initialHtml = """
                <table class="polarion-rp-column-layout" style="width: 1000px;">
                  <tbody>
                    <tr>
                      <td class="polarion-rp-column-layout-cell" style="width: 100%;">
                        <h1 id="polarion_hardcoded_0">PDF Tests</h1>
                        <p id="polarion_hardcoded_1">This Page has no content yet</p>
                      </td>
                    </tr>
                  </tbody>
                </table>
            """;
        String processedHtml = processor.adjustColumnWidthInReports(initialHtml);

        String expectedHtml = """
                <table class="polarion-rp-column-layout" style="width: 100%;">
                  <tbody>
                    <tr>
                      <td class="polarion-rp-column-layout-cell" style="width: 100%;">
                        <h1 id="polarion_hardcoded_0">PDF Tests</h1>
                        <p id="polarion_hardcoded_1">This Page has no content yet</p>
                      </td>
                    </tr>
                  </tbody>
                </table>
            """.replaceAll(" ", "");
        assertEquals(TestStringUtils.removeLineEndings(expectedHtml), TestStringUtils.removeLineEndings(processedHtml.replaceAll(" ", "")));
    }

    @Test
    @SneakyThrows
    void removeFloatLeftFromReportsTest() {
        String initialHtml = """
                <table id="polarion_client20" style="float: left;">
                  <tbody>
                    <tr>
                      <td>Status</td>
                      <td><span title="Accepted">Accepted</span></td>
                    </tr>
                    <tr>
                      <td>Severity</td>
                      <td><span title="Nice to Have">Nice to Have</span></td>
                    </tr>
                  </tbody>
                </table>
                <div style="clear: both;"></div>
            """;
        String processedHtml = processor.removeFloatLeftFromReports(initialHtml);

        String expectedHtml = """
                <table id="polarion_client20" >
                  <tbody>
                    <tr>
                      <td>Status</td>
                      <td><span title="Accepted">Accepted</span></td>
                    </tr>
                    <tr>
                      <td>Severity</td>
                      <td><span title="Nice to Have">Nice to Have</span></td>
                    </tr>
                  </tbody>
                </table>
                <div style="clear: both;"></div>
            """.replaceAll(" ", "");
        assertEquals(TestStringUtils.removeLineEndings(expectedHtml), TestStringUtils.removeLineEndings(processedHtml.replaceAll(" ", "")));
    }

    private ExportParams getExportParams() {
        return ExportParams.builder()
                .projectId("test_project")
                .documentType(DocumentType.DOCUMENT)
                .language(Language.DE.name())
                .build();
    }
}

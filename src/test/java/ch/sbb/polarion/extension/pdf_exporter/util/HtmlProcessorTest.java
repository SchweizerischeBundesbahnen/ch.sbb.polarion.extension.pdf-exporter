package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.pdf_exporter.TestStringUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.localization.Language;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.localization.LocalizationModel;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.html.HtmlLinksHelper;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("ConstantConditions")
class HtmlProcessorTest {

    @Mock
    private PdfExporterFileResourceProvider fileResourceProvider;
    @Mock
    private LocalizationSettings localizationSettings;
    @Mock
    private HtmlLinksHelper htmlLinksHelper;
    @Mock
    private PdfExporterPolarionService pdfExporterPolarionService;

    private HtmlProcessor processor;

    @BeforeEach
    void init() {
        processor = new HtmlProcessor(fileResourceProvider, localizationSettings, htmlLinksHelper, pdfExporterPolarionService);
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
        when(pdfExporterPolarionService.getPolarionVersion()).thenReturn("2310");

        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/cutLocalUrlsWithRolesFilteringBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/cutLocalUrlsWithRolesFilteringAfterProcessing.html")) {

            ExportParams exportParams = getExportParams();
            exportParams.setCutLocalUrls(true);

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            List<String> selectedRoles = Arrays.asList("has parent", "is parent of", "depends on", "blocks", "verifies", "is verified by");
            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.processHtmlForPDF(invalidHtml, exportParams, selectedRoles);
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void rewritePolarionUrlsTest() {
        String anchor = "<a id=\"work-item-anchor-testProject/12345\"></a>";
        String link = "<a href=\"http://localhost/polarion/#/project/testProject/workitem?id=12345\">Work Item 12345</a>";
        String htmlWithAnchor = anchor + link;
        String expected = anchor + "<a href=\"#work-item-anchor-testProject/12345\">Work Item 12345</a>";
        assertEquals(expected, processor.rewritePolarionUrls(htmlWithAnchor));

        String htmlWithoutAnchor = link;
        assertEquals(link, processor.rewritePolarionUrls(htmlWithoutAnchor));
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
            String fixedHtml = processor.processPageBrakes(invalidHtml, context);
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void cutEmptyChaptersTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/emptyChaptersBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/emptyChaptersAfterProcessing.html")) {

            Document document = Jsoup.parse(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));
            document.outputSettings()
                    .syntax(Document.OutputSettings.Syntax.xml)
                    .escapeMode(Entities.EscapeMode.base)
                    .prettyPrint(false);

            processor.cutEmptyChapters(document);
            String fixedHtml = document.body().html();
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void cutEmptyWIAttributesTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/emptyWIAttributesBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/emptyWIAttributesAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.cutEmptyWIAttributes(invalidHtml);
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void properTableHeadsTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/invalidTableHeads.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/validTableHeads.html")) {

            Document document = Jsoup.parse(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));
            document.outputSettings()
                    .syntax(Document.OutputSettings.Syntax.xml)
                    .escapeMode(Entities.EscapeMode.base)
                    .prettyPrint(false);

            processor.fixTableHeads(document);
            String fixedHtml = document.body().html();
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void adjustCellWidthTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/cellWidthBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/cellWidthAfterProcessing.html")) {

            Document document = Jsoup.parse(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));
            document.outputSettings()
                    .syntax(Document.OutputSettings.Syntax.xml)
                    .escapeMode(Entities.EscapeMode.base)
                    .prettyPrint(false);

            processor.adjustCellWidth(document, ExportParams.builder().fitToPage(false).build());
            String fixedHtml = document.body().html();
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void adjustCellWidthFitToPageTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/cellWidthBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/cellWidthFitToPageAfterProcessing.html")) {

            Document document = Jsoup.parse(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));
            document.outputSettings()
                    .syntax(Document.OutputSettings.Syntax.xml)
                    .escapeMode(Entities.EscapeMode.base)
                    .prettyPrint(false);

            processor.adjustCellWidth(document, new ExportParams());
            String fixedHtml = document.body().html();
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void cutNotNeededChaptersTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/notNeededChaptersBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/notNeededChaptersAfterProcessing.html")) {

            Document document = Jsoup.parse(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));
            document.outputSettings()
                    .syntax(Document.OutputSettings.Syntax.xml)
                    .escapeMode(Entities.EscapeMode.base)
                    .prettyPrint(false);

            processor.cutNotNeededChapters(document, List.of("3", "4", "7"));
            String fixedHtml = document.body().html();
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void adjustImageAlignmentTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/imageAlignmentBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/imageAlignmentAfterProcessing.html")) {

            Document document = Jsoup.parse(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));
            document.outputSettings()
                    .syntax(Document.OutputSettings.Syntax.xml)
                    .escapeMode(Entities.EscapeMode.base)
                    .prettyPrint(false);

            processor.adjustImageAlignment(document);
            String fixedHtml = document.body().html();
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void localizeEnumsTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/localizeEnumsBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/localizeEnumsAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.localizeEnums(invalidHtml, getExportParams());
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void selectLinkedWorkItemTypesTableTest() {
        when(pdfExporterPolarionService.getPolarionVersion()).thenReturn("2404");

        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/linkedWorkItemsTableBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/linkedWorkItemsTableAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            ExportParams exportParams = getExportParams();
            exportParams.setLinkedWorkitemRoles(List.of("has parent"));

            List<String> selectedRoleEnumValues = Arrays.asList("has parent", "is parent of");

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.processHtmlForPDF(invalidHtml, exportParams, selectedRoleEnumValues);
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void selectLinkedWorkItemTypesTest() {
        when(pdfExporterPolarionService.getPolarionVersion()).thenReturn(null);

        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/linkedWorkItemsBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/linkedWorkItemsAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            ExportParams exportParams = getExportParams();
            exportParams.setLinkedWorkitemRoles(List.of("has parent"));

            List<String> selectedRoleEnumValues = Arrays.asList("has parent", "is parent of");

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.processHtmlForPDF(invalidHtml, exportParams, selectedRoleEnumValues);
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void nothingToReplaceTest() {
        String html = "<div></div>";
        String result = processor.replaceResourcesAsBase64Encoded(html);
        assertEquals("<div></div>", result);
    }

    @Test
    @SneakyThrows
    void replaceImagesAsBase64EncodedTest() {
        String html = "<div><img id=\"image\" src=\"http://localhost/some-path/img.png\"/></div>";
        when(fileResourceProvider.getResourceAsBase64String(any())).thenReturn("base64Data");
        String result = processor.replaceResourcesAsBase64Encoded(html);
        assertEquals("<div><img id=\"image\" src=\"base64Data\"/></div>", result);
    }

    @Test
    @SneakyThrows
    void replaceSvgImagesAsBase64EncodedTest() {
        String html = "<div><img id=\"image1\" src=\"http://localhost/some-path/img1.svg\"/> <img id='image2' src='http://localhost/some-path/img2.svg'/> <img id='image1' src='http://localhost/some-path/img1.svg'/></div>";
        byte[] imgBytes;
        try (InputStream is = new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\"><switch><g requiredFeatures=\"http://www.w3.org/TR/SVG11/feature#Extensibility\"/></switch></svg>".getBytes(StandardCharsets.UTF_8))) {
            imgBytes = is.readAllBytes();
        }
        when(fileResourceProvider.getResourceAsBytes(any())).thenReturn(imgBytes);
        when(fileResourceProvider.getResourceAsBase64String(any())).thenCallRealMethod();
        String result = processor.replaceResourcesAsBase64Encoded(html);
        String base64SvgImage = Base64.getEncoder().encodeToString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg xmlns=\"http://www.w3.org/2000/svg\"><switch><g requiredFeatures=\"http://www.w3.org/TR/SVG11/feature#Extensibility\"/></switch></svg>".getBytes(StandardCharsets.UTF_8));
        String expected = "<div><img id=\"image1\" src=\"data:image/svg+xml;base64," + base64SvgImage + "\"/> " +
                "<img id='image2' src='data:image/svg+xml;base64," + base64SvgImage + "'/> " +
                "<img id='image1' src='data:image/svg+xml;base64," + base64SvgImage + "'/></div>";
        assertEquals(expected, result);
    }

    @Test
    @SneakyThrows
    void processHtmlForPDFTestCutEmptyWorkItemAttributesDisabled() {
        try (InputStream isHtml = this.getClass().getResourceAsStream("/emptyWIAttributesBeforeProcessing.html")) {

            String html = new String(isHtml.readAllBytes(), StandardCharsets.UTF_8);

            HtmlProcessor spyHtmlProcessor = spy(processor);
            ExportParams exportParams = getExportParams();
            // to avoid changing input html and check with regular equals
            doNothing().when(spyHtmlProcessor).adjustCellWidth(any(), any());
            exportParams.setCutEmptyChapters(false);

            // Spaces, new lines & nbsp symbols are removed to exclude difference in space characters
            String result = spyHtmlProcessor.processHtmlForPDF(html, exportParams, List.of());
            assertEquals(TestStringUtils.removeNonsensicalSymbols(html.replaceAll("&nbsp;|\u00A0", " ").replaceAll(" ", "")), TestStringUtils.removeNonsensicalSymbols(result));
        }
    }

    private static Stream<Arguments> provideHtmlTestCases() {
        return Stream.of(
                Arguments.of("<h1>First level heading</h1>", """
                        <div class="title">
                         First level heading
                        </div>"""),
                Arguments.of("<h2>First level heading</h2>", "<h1>First level heading</h1>"),
                Arguments.of("<div>100$</div>", """
                        <div>
                         100&dollar;
                        </div>""")
        );
    }

    @ParameterizedTest
    @MethodSource("provideHtmlTestCases")
    void processHtmlForPDFTest(String inputHtml, String expectedResult) {
        String result = processor.processHtmlForPDF(inputHtml, getExportParams(), List.of());

        assertEquals(expectedResult, result);
    }

    @Test
    void processHtmlForPDFExtendedTest() {
        ExportParams exportParams = getExportParams();
        exportParams.setCutEmptyChapters(true);
        exportParams.setChapters(List.of("1"));
        String result = processor.processHtmlForPDF("""
                <article>
                 <h2><span><span>1</span></span>Heading 1</h2>
                 <p>Text</p>

                 <h2><span><span>2</span></span>Heading 2</h2>
                 <p>Text</p>

                 <h2><span><span>3</span></span>Heading 3</h2>
                </article>""", exportParams, List.of());

        String expectedResult = """
                <article>
                 <h1><span><span>1</span></span>Heading 1</h1>
                 <p>Text</p>
                </article>""";

        assertEquals(expectedResult, result);
    }

    @Test
    @SneakyThrows
    void adjustContentToFitPageTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/fitToPageBeforeProcessing.html");
             InputStream isValidPortraitHtml = this.getClass().getResourceAsStream("/fitToPortraitPageAfterProcessing.html");
             InputStream isValidLandscapeHtml = this.getClass().getResourceAsStream("/fitToLandscapePageAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            ConversionParams conversionParamsPortrait = ConversionParams.builder()
                    .orientation(Orientation.PORTRAIT)
                    .build();

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.adjustContentToFitPage(invalidHtml, conversionParamsPortrait);
            String validHtml = new String(isValidPortraitHtml.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));

            ConversionParams conversionParamsLandscape = ConversionParams.builder()
                    .orientation(Orientation.LANDSCAPE)
                    .build();
            fixedHtml = processor.adjustContentToFitPage(invalidHtml, conversionParamsLandscape);
            validHtml = new String(isValidLandscapeHtml.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void adjustImagesInTablesTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/fitImagesWidthBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/fitImagesWidthAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.adjustContentToFitPage(invalidHtml, ConversionParams.builder().build());
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
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
            assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void tableOfTablesTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/tableOfTablesBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/tableOfTablesAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.addTableOfFigures(invalidHtml);
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void tableOfFiguresTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/tableOfFiguresBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/tableOfFiguresAfterProcessing.html")) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.addTableOfFigures(invalidHtml);
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
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
                """;
        assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedHtml), TestStringUtils.removeNonsensicalSymbols(processedHtml.replaceAll(" ", "")));
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
                """;
        assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedHtml), TestStringUtils.removeNonsensicalSymbols(processedHtml.replaceAll(" ", "")));
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
                """;
        assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedHtml), TestStringUtils.removeNonsensicalSymbols(processedHtml.replaceAll(" ", "")));
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
                """;
        assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedHtml), TestStringUtils.removeNonsensicalSymbols(processedHtml.replaceAll(" ", "")));
    }

    @Test
    void fixTableHeadRowspanSimpleTest() {
        String html = """
                <table>
                    <thead>
                        <tr>
                            <th rowspan="3">Column 1</th>
                            <th rowspan="2">Column 2</th>
                            <th>Column 3</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Data 1</td>
                            <td>Data 2</td>
                        </tr>
                        <tr>
                            <td>Data 3</td>
                            <td>Data 4</td>
                        </tr>
                        <tr>
                            <td>Data 5</td>
                            <td>Data 6</td>
                        </tr>
                    </tbody>
                </table>
                """;

        Document document = Jsoup.parse(html);
        processor.fixTableHeadRowspan(document);

        // After fixing: thead should have 3 rows (1 original + 2 moved from tbody)
        assertEquals(3, document.select("thead tr").size());
        // tbody should have 1 row left
        assertEquals(1, document.select("tbody tr").size());
    }

    @Test
    void fixTableHeadRowspanNoRowspanTest() {
        String html = """
                <table>
                    <thead>
                        <tr>
                            <th>Column 1</th>
                            <th>Column 2</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Data 1</td>
                            <td>Data 2</td>
                        </tr>
                    </tbody>
                </table>
                """;

        Document document = Jsoup.parse(html);
        processor.fixTableHeadRowspan(document);

        // Should remain unchanged
        assertEquals(1, document.select("thead tr").size());
        assertEquals(1, document.select("tbody tr").size());
    }

    @Test
    void fixTableHeadRowspanNoTheadTest() {
        String html = """
                <table>
                    <tbody>
                        <tr>
                            <td rowspan="2">Data 1</td>
                            <td>Data 2</td>
                        </tr>
                        <tr>
                            <td>Data 3</td>
                        </tr>
                    </tbody>
                </table>
                """;

        Document document = Jsoup.parse(html);
        processor.fixTableHeadRowspan(document);

        // Should remain unchanged (no thead)
        assertEquals(0, document.select("thead").size());
        assertEquals(2, document.select("tbody tr").size());
    }

    @Test
    void fixTableHeadRowspanWithColspanTest() {
        String html = """
                <table>
                    <thead>
                        <tr>
                            <th rowspan="2" colspan="2">Header 1</th>
                            <th>Header 2</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Data 1</td>
                            <td>Data 2</td>
                        </tr>
                        <tr>
                            <td>Data 3</td>
                            <td>Data 4</td>
                        </tr>
                    </tbody>
                </table>
                """;

        Document document = Jsoup.parse(html);
        processor.fixTableHeadRowspan(document);

        // After fixing: thead should have 2 rows (1 original + 1 moved from tbody)
        assertEquals(2, document.select("thead tr").size());
        // tbody should have 1 row left
        assertEquals(1, document.select("tbody tr").size());
    }

    @Test
    void fixTableHeadRowspanMultipleTablesTest() {
        String html = """
                <table>
                    <thead>
                        <tr>
                            <th rowspan="2">Header 1</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr><td>Data 1</td></tr>
                        <tr><td>Data 2</td></tr>
                    </tbody>
                </table>
                <table>
                    <thead>
                        <tr>
                            <th rowspan="3">Header 2</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr><td>Data 3</td></tr>
                        <tr><td>Data 4</td></tr>
                        <tr><td>Data 5</td></tr>
                    </tbody>
                </table>
                """;

        Document document = Jsoup.parse(html);
        processor.fixTableHeadRowspan(document);

        // Both tables should be processed
        assertEquals(2, document.select("table").size());
        // First table thead should have 2 rows, tbody should have 1 row
        assertEquals(2, document.select("table").get(0).select("thead tr").size());
        assertEquals(1, document.select("table").get(0).select("tbody tr").size());
        // Second table thead should have 3 rows, tbody should have 1 row
        assertEquals(3, document.select("table").get(1).select("thead tr").size());
        assertEquals(1, document.select("table").get(1).select("tbody tr").size());
    }

    @Test
    void fixTableHeadRowspanNoTbodyTest() {
        String html = """
                <table>
                    <thead>
                        <tr>
                            <th rowspan="2">Header 1</th>
                            <th>Header 2</th>
                        </tr>
                    </thead>
                </table>
                """;

        Document document = Jsoup.parse(html);
        processor.fixTableHeadRowspan(document);

        // Should remain unchanged (no tbody to move rows from)
        assertEquals(1, document.select("thead tr").size());
    }

    @Test
    void fixTableHeadRowspanInsufficientRowsTest() {
        String html = """
                <table>
                    <thead>
                        <tr>
                            <th rowspan="5">Header 1</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr><td>Data 1</td></tr>
                        <tr><td>Data 2</td></tr>
                    </tbody>
                </table>
                """;

        Document document = Jsoup.parse(html);
        processor.fixTableHeadRowspan(document);

        // Should move only available rows (2 rows) even though rowspan is 5
        assertEquals(3, document.select("thead tr").size()); // 1 original + 2 moved
        assertEquals(0, document.select("tbody tr").size()); // all rows moved
    }

    private ExportParams getExportParams() {
        return ExportParams.builder()
                .projectId("test_project")
                .documentType(DocumentType.LIVE_DOC)
                .language(Language.DE.name())
                .build();
    }
}

package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.test_extensions.BundleJarsPrioritizingRunnableMockExtension;
import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import ch.sbb.polarion.extension.pdf_exporter.TestStringUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.localization.Language;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.localization.LocalizationModel;
import ch.sbb.polarion.extension.pdf_exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.html.HtmlLinksHelper;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, BundleJarsPrioritizingRunnableMockExtension.class, PdfExporterExtensionConfigurationExtension.class})
@SuppressWarnings("ConstantConditions")
class HtmlProcessorTest {

    @Mock
    private PdfExporterFileResourceProvider fileResourceProvider;
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
        String img = "<img title=\"diagram_123.png\" src=\"data:image/png;BASE64Content\" />";
        String imgInsideA = String.format("<a href=\"http://localhost/polarion/module-attachment/elibrary/some-path\">%s</a>", img);
        Document document = JSoupUtils.parseHtml(imgInsideA);
        processor.cutLocalUrls(document);
        assertEquals(img, document.body().html());

        String span = "<span id=\"PLANID_Version_1_0\" title=\"Version 1.0 (Version_1_0) (2017-03-31)\" class=\"polarion-Plan\">Version 1.0<span> (2017-03-31)</span></span>";
        String spanInsideA = String.format("<a href=\"http://localhost/polarion/#/project/elibrary/another-path\">%s</a>", span);
        document = JSoupUtils.parseHtml(spanInsideA);
        processor.cutLocalUrls(document);
        assertEquals(span, document.body().html());
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

        Document document = JSoupUtils.parseHtml(htmlWithAnchor);
        processor.rewritePolarionUrls(document);
        assertEquals(expected, document.body().html());

        String wikiLink = "<a href=\"http://localhost/polarion/#/project/testProject/wiki/Specification/LinkedWorkItems?selection=12345\">Work Item 12345</a>";
        String htmlWithWikiAnchor = anchor + wikiLink;
        document = JSoupUtils.parseHtml(htmlWithWikiAnchor);
        processor.rewritePolarionUrls(document);
        assertEquals(expected, document.body().html());

        document = JSoupUtils.parseHtml(link);
        processor.rewritePolarionUrls(document);
        assertEquals(link, document.body().html());

        link = "<a href=\"http://localhost/polarion/#/project/testProject\">Work Item 12345</a>";
        document = JSoupUtils.parseHtml(link);
        assertEquals(link, document.body().html());

        link = "<a href=\"http://localhost/polarion/testProject\">Work Item 12345</a>";
        document = JSoupUtils.parseHtml(link);
        assertEquals(link, document.body().html());
    }

    private static Stream<Arguments> providePageBreakTestCases() {

        ExportParams landscapeExportParams = new ExportParams();
        landscapeExportParams.setFitToPage(true);
        landscapeExportParams.setChapters(List.of());
        landscapeExportParams.setOrientation(Orientation.LANDSCAPE);

        ExportParams specificChaptersExportParams = new ExportParams();
        specificChaptersExportParams.setChapters(List.of("1", "2", "3"));

        ExportParams cutEmptyExportParams = new ExportParams();
        cutEmptyExportParams.setCutEmptyChapters(true);

        return Stream.of(
                Arguments.of("/pageBreaksAfterProcessing.html", new ExportParams()),
                Arguments.of("/pageBreaksLandscapeAfterProcessing.html", landscapeExportParams),
                Arguments.of("/pageBreaksSpecificChaptersAfterProcessing.html", specificChaptersExportParams),
                Arguments.of("/pageBreaksCutEmptyAfterProcessing.html", cutEmptyExportParams)
        );
    }

    @ParameterizedTest
    @MethodSource("providePageBreakTestCases")
    @SneakyThrows
    void processPageBreaksTest(String expectedHtmlFilePath, ExportParams exportParams) {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/pageBreaksBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream(expectedHtmlFilePath)) {

            String invalidHtml = new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            String fixedHtml = processor.processPageBrakes(invalidHtml, exportParams);
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void cutEmptyChaptersTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/emptyChaptersBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/emptyChaptersAfterProcessing.html")) {

            Document document = JSoupUtils.parseHtml(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));

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

            Document document = JSoupUtils.parseHtml(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));

            processor.cutEmptyWIAttributes(document);
            String fixedHtml = document.body().html();
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    void pageBreakCommentsTest() {
        assertTrue(processor.isPageBreakComment(new Comment("PAGE_BREAK")));
        assertTrue(processor.isPageBreakComment(new Comment("LANDSCAPE_ABOVE")));
        assertTrue(processor.isPageBreakComment(new Comment("PORTRAIT_ABOVE")));
        assertTrue(processor.isPageBreakComment(new Comment("ROTATE_BELOW")));
        assertTrue(processor.isPageBreakComment(new Comment("RESET_BELOW")));
        assertTrue(processor.isPageBreakComment(new Comment("BREAK_BELOW")));
        assertFalse(processor.isPageBreakComment(new Comment("pre PAGE_BREAK")));
    }

    @Test
    @SneakyThrows
    void properTableHeadsTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/invalidTableHeads.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/validTableHeads.html")) {

            Document document = JSoupUtils.parseHtml(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));

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

            Document document = JSoupUtils.parseHtml(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));

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

            Document document = JSoupUtils.parseHtml(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));

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

            Document document = JSoupUtils.parseHtml(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));

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

            Document document = JSoupUtils.parseHtml(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));

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

            Document document = JSoupUtils.parseHtml(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));

            processor.localizeEnums(document, getExportParams());
            String fixedHtml = document.body().html();
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void selectLinkedWorkItemTypesTableTest() {
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
    void malformedLinkedWorkItemTypesTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/malformedLinkedWorkItemsBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/malformedLinkedWorkItemsAfterProcessing.html")) {

            Document document = JSoupUtils.parseHtml(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));

            List<String> selectedRoleEnumValues = Arrays.asList("has parent", "is parent of");

            processor.filterNonTabularLinkedWorkItems(document, selectedRoleEnumValues);
            String fixedHtml = document.body().html();
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void removePageBreakAvoidsTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/withPageBreakAvoids.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/withoutPageBreakAvoids.html")) {

            Document document = JSoupUtils.parseHtml(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));

            processor.removePageBreakAvoids(document);
            String fixedHtml = document.body().html();
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    void removePageBreakAvoidsDoesNotFailOnSpecialCssCharacters() {
        // CSSReaderDeclarationList with default ThrowingCSSParseErrorHandler throws IllegalStateException
        // on certain CSS values containing '%' character
        String html = "<table style=\"page-break-inside:avoid; width:50%\"><tr><td>Content</td></tr></table>"
                + "<table style=\"page-break-inside:avoid; background: url('data:image/svg+xml,%3Csvg%3E')\"><tr><td>Content</td></tr></table>"
                + "<table style=\"width: 100%\"><tr><td>Normal table</td></tr></table>";
        Document document = JSoupUtils.parseHtml(html);

        assertDoesNotThrow(() -> processor.removePageBreakAvoids(document));
    }

    @Test
    @SneakyThrows
    void fixNumberedListsTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/invalidNumberedLists.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/validNumberedLists.html")) {

            Document document = JSoupUtils.parseHtml(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));

            processor.fixNestedLists(document);
            String fixedHtml = document.body().html();
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
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
                        <div class="title">First level heading</div>"""),
                Arguments.of("<h2>First level heading</h2>", "<h1>First level heading</h1>"),
                Arguments.of("<div class=\"heading-7\">Heading level 7</div>", "<h6 class=\"heading-7\">Heading level 7</h6>"),
                Arguments.of("<div class=\"heading-15\">Heading level 15</div>", "<h6 class=\"heading-15\">Heading level 15</h6>"),
                Arguments.of("<div class=\"heading-30\">Heading level 30</div>", "<h6 class=\"heading-30\">Heading level 30</h6>"),
                Arguments.of("<div>100$</div>", """
                        <div>100&dollar;</div>""")
        );
    }

    /**
     * Tests that pd4ml:toc closing tag regex works correctly.
     * The regex should add closing tag only when needed (self-closing or unclosed tags),
     * but NOT add duplicate closing tag when tag is already properly closed.
     */
    @ParameterizedTest
    @MethodSource("providePd4mlTocTestCases")
    void pd4mlTocClosingTagRegexTest(String input, String expected) {
        String result = HtmlProcessor.sanitizeHtmlForToc(input);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> providePd4mlTocTestCases() {
        return Stream.of(
                // Self-closing tags should get proper closing tag
                Arguments.of("<pd4ml:toc/>", "<pd4ml:toc></pd4ml:toc>"),
                Arguments.of("<pd4ml:toc numlen=\"4\"/>", "<pd4ml:toc numlen=\"4\"></pd4ml:toc>"),
                // Unclosed tags should get closing tag
                Arguments.of("<pd4ml:toc>", "<pd4ml:toc></pd4ml:toc>"),
                Arguments.of("<pd4ml:toc numlen=\"4\">", "<pd4ml:toc numlen=\"4\"></pd4ml:toc>"),
                // Already closed tags should NOT get duplicate closing tag
                Arguments.of("<pd4ml:toc></pd4ml:toc>", "<pd4ml:toc></pd4ml:toc>"),
                Arguments.of("<pd4ml:toc numlen=\"4\"></pd4ml:toc>", "<pd4ml:toc numlen=\"4\"></pd4ml:toc>")
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

        assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedResult), TestStringUtils.removeNonsensicalSymbols(result));
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

            Document document = JSoupUtils.parseHtml(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));

            processor.addTableOfFigures(document);
            String fixedHtml = document.body().html();
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void tableOfFiguresTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/tableOfFiguresBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/tableOfFiguresAfterProcessing.html")) {

            Document document = JSoupUtils.parseHtml(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));

            processor.addTableOfFigures(document);
            String fixedHtml = document.body().html();
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    @SneakyThrows
    void replaceTableOfTablesAndTableOfFiguresTest() {
        try (InputStream isInvalidHtml = this.getClass().getResourceAsStream("/tableOfTablesAndTableOfFiguresBeforeProcessing.html");
             InputStream isValidHtml = this.getClass().getResourceAsStream("/tableOfTablesAndTableOfFiguresAfterProcessing.html")) {

            Document document = JSoupUtils.parseHtml(new String(isInvalidHtml.readAllBytes(), StandardCharsets.UTF_8));

            processor.addTableOfFigures(document);
            String fixedHtml = document.body().html();
            String validHtml = new String(isValidHtml.readAllBytes(), StandardCharsets.UTF_8);

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(validHtml), TestStringUtils.removeNonsensicalSymbols(fixedHtml));
        }
    }

    @Test
    void renumberCaptionsRestoresDomOrderTest() {
        // Reproduces the Polarion numbering bug: a work item owns a table (rendered second, so numbered "2")
        // followed by a macro that re-renders the same rich text (rendered first during the wiki phase, so
        // numbered "1"). In DOM order the work item's own caption comes first but carries the higher number.
        String html = """
                <p class="polarion-rte-caption-paragraph">Table <span data-sequence="Table" class="polarion-rte-caption">2<a name="dlecaption_5"></a></span> Own table</p>
                <p class="polarion-rte-caption-paragraph">Table <span data-sequence="Table" class="polarion-rte-caption">1<a name="dlecaption_6"></a></span> Macro copy</p>
                """;
        Document document = JSoupUtils.parseHtml(html);

        processor.renumberCaptions(document);

        Elements captions = document.select("span.polarion-rte-caption[data-sequence]");
        assertEquals("1", captions.get(0).childNodes().stream().filter(org.jsoup.nodes.TextNode.class::isInstance).findFirst().map(n -> ((org.jsoup.nodes.TextNode) n).text()).orElse(null));
        assertEquals("2", captions.get(1).childNodes().stream().filter(org.jsoup.nodes.TextNode.class::isInstance).findFirst().map(n -> ((org.jsoup.nodes.TextNode) n).text()).orElse(null));
    }

    @Test
    void renumberCaptionsKeepsSequencesIndependentTest() {
        // Each sequence (Table, Figure, ...) is numbered independently, following DOM order within the sequence.
        String html = """
                <p class="polarion-rte-caption-paragraph">Figure <span data-sequence="Figure" class="polarion-rte-caption">3</span> Fig A</p>
                <p class="polarion-rte-caption-paragraph">Table <span data-sequence="Table" class="polarion-rte-caption">2</span> Tab A</p>
                <p class="polarion-rte-caption-paragraph">Figure <span data-sequence="Figure" class="polarion-rte-caption">9</span> Fig B</p>
                <p class="polarion-rte-caption-paragraph">Table <span data-sequence="Table" class="polarion-rte-caption">7</span> Tab B</p>
                """;
        Document document = JSoupUtils.parseHtml(html);

        processor.renumberCaptions(document);

        Elements captions = document.select("span.polarion-rte-caption[data-sequence]");
        assertEquals("1", captions.get(0).text()); // Figure 1
        assertEquals("1", captions.get(1).text()); // Table 1
        assertEquals("2", captions.get(2).text()); // Figure 2
        assertEquals("2", captions.get(3).text()); // Table 2
    }

    @Test
    void renumberCaptionsLeavesNonNumericSpansUntouchedTest() {
        // Captions without a resolved number (e.g. an editor placeholder) must not be rewritten.
        String html = """
                <p class="polarion-rte-caption-paragraph">Table <span data-sequence="Table" class="polarion-rte-caption">#</span> Placeholder</p>
                <p class="polarion-rte-caption-paragraph">Table <span data-sequence="Table" class="polarion-rte-caption">5</span> Real</p>
                """;
        Document document = JSoupUtils.parseHtml(html);

        processor.renumberCaptions(document);

        Elements captions = document.select("span.polarion-rte-caption[data-sequence]");
        assertEquals("#", captions.get(0).text());
        assertEquals("1", captions.get(1).text());
    }

    @Test
    @SneakyThrows
    void adjustReportedByTest() {
        String initialHtml = """
                    <div>
                      <div>
                        <div style="color: grey; text-align: right; position: absolute; top: 22px; right: 10px">Reported by
                            <span class="polarion-no-style-cleanup">System Administrator</span>
                            <br/>
                            January 12, 2024 at 4:19:27 PM UTC
                        </div>
                      </div>
                    </div>
                """;

        Document document = JSoupUtils.parseHtml(initialHtml);

        processor.adjustReportedBy(document);
        String processedHtml = document.body().html();

        String expectedHtml = """
                    <div>
                      <div>
                        <div style="color: grey; text-align: right; position: absolute; top: 0; right: 10px; font-size: 8px;">Reported by
                            <span class="polarion-no-style-cleanup">System Administrator</span>
                            <br/>
                            January 12, 2024 at 4:19:27 PM UTC
                        </div>
                      </div>
                    </div>
                """;
        assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedHtml), TestStringUtils.removeNonsensicalSymbols(processedHtml));
    }

    @Test
    @SneakyThrows
    void dontAdjustReportedFromTest() {
        // Artificial use case, just to be sure that if text is not "Reported by", HTML stays untouched
        String initialHtml = """
                    <div>
                      <div>
                        <div style="color: grey; text-align: right; position: absolute; top: 22px; right: 10px">Reported from
                            <span class="polarion-no-style-cleanup">System Administrator</span>
                            <br/>
                            January 12, 2024 at 4:19:27 PM UTC
                        </div>
                      </div>
                    </div>
                """;

        Document document = JSoupUtils.parseHtml(initialHtml);

        processor.adjustReportedBy(document);
        String processedHtml = document.body().html();

        String expectedHtml = """
                    <div>
                      <div>
                        <div style="color: grey; text-align: right; position: absolute; top: 22px; right: 10px">Reported from
                            <span class="polarion-no-style-cleanup">System Administrator</span>
                            <br/>
                            January 12, 2024 at 4:19:27 PM UTC
                        </div>
                      </div>
                    </div>
                """;
        assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedHtml), TestStringUtils.removeNonsensicalSymbols(processedHtml));
    }

    @Test
    @SneakyThrows
    void cutExportToPdfButtonTest() {
        String initialHtml = """
                    <aside>
                      <div>
                        <a onclick="PdfExporter.openPopup({context: 'report'})">
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
                      </div>
                    </aside>
                """;

        Document document = JSoupUtils.parseHtml(initialHtml);

        processor.cutExportToPdfButton(document);
        String processedHtml = document.body().html();

        String expectedHtml = """
                    <aside>
                      <div>
                        <a onclick="PdfExporter.openPopup({context: 'report'})"> </a>
                      </div>
                    </aside>
                """;
        assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedHtml), TestStringUtils.removeNonsensicalSymbols(processedHtml));
    }

    @Test
    @SneakyThrows
    void dontCutOtherButtonsTest() {
        String initialHtml = """
                    <aside>
                      <div>
                        <a onclick="PdfExporter.openPopup({context: 'report'})">
                          <div style="color:#5E5E5E; text-shadow:#FCFCFC 1px 1px; border-color:#C2C2C2; background-color:#F0F0F0; " class="polarion-TestsExecutionButton-buttons-pdf">
                            <div style="height: 10px;"></div>
                            <table class="polarion-TestsExecutionButton-buttons-content">
                              <tbody>
                              <tr>
                                <td class="polarion-TestsExecutionButton-buttons-content-labelCell polarion-TestsExecutionButton-buttons-content-labelCell-noSumText">
                                  <div class="polarion-TestsExecutionButton-labelTextNew">Some other button</div>
                                </td>
                              </tr>
                              <tr>
                                <td style="color:#5E5E5E; text-shadow:#FCFCFC 1px 1px; " class="polarion-TestsExecutionButton-sumText"></td>
                              </tr>
                              </tbody>
                            </table>
                          </div>
                        </a>
                      </div>
                    </aside>
                """;

        Document document = JSoupUtils.parseHtml(initialHtml);

        processor.cutExportToPdfButton(document);
        String processedHtml = document.body().html();

        String expectedHtml = """
                    <aside>
                      <div>
                        <a onclick="PdfExporter.openPopup({context: 'report'})">
                          <div style="color:#5E5E5E; text-shadow:#FCFCFC 1px 1px; border-color:#C2C2C2; background-color:#F0F0F0; " class="polarion-TestsExecutionButton-buttons-pdf">
                            <div style="height: 10px;"></div>
                            <table class="polarion-TestsExecutionButton-buttons-content">
                              <tbody>
                              <tr>
                                <td class="polarion-TestsExecutionButton-buttons-content-labelCell polarion-TestsExecutionButton-buttons-content-labelCell-noSumText">
                                  <div class="polarion-TestsExecutionButton-labelTextNew">Some other button</div>
                                </td>
                              </tr>
                              <tr>
                                <td style="color:#5E5E5E; text-shadow:#FCFCFC 1px 1px; " class="polarion-TestsExecutionButton-sumText"></td>
                              </tr>
                              </tbody>
                            </table>
                          </div>
                        </a>
                      </div>
                    </aside>
                """;
        assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedHtml), TestStringUtils.removeNonsensicalSymbols(processedHtml));
    }

    @Test
    @SneakyThrows
    void adjustColumnWidthInReportsTest() {
        String initialHtml = """
                    <table class="polarion-rp-column-layout" style="width: 1000px">
                      <tbody>
                        <tr>
                          <td class="polarion-rp-column-layout-cell" style="width: 100%">
                            <h1 id="polarion_hardcoded_0">PDF Tests</h1>
                            <p id="polarion_hardcoded_1">This Page has no content yet</p>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                """;

        Document document = JSoupUtils.parseHtml(initialHtml);
        processor.adjustColumnWidthInReports(document);
        String processedHtml = document.body().html();

        String expectedHtml = """
                    <table class="polarion-rp-column-layout" style="width: 100%;">
                      <tbody>
                        <tr>
                          <td class="polarion-rp-column-layout-cell" style="width: 100%">
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
        try (InputStream isInitialHtml = this.getClass().getResourceAsStream("/removeFloatLeftFromReportsBeforeProcessing.html");
             InputStream isExpectedHtml = this.getClass().getResourceAsStream("/removeFloatLeftFromReportsAfterProcessing.html")) {

            String initialHtml = new String(isInitialHtml.readAllBytes(), StandardCharsets.UTF_8);
            String expectedHtml = new String(isExpectedHtml.readAllBytes(), StandardCharsets.UTF_8);

            Document document = JSoupUtils.parseHtml(initialHtml);

            processor.removeFloatLeftFromReports(document);
            String processedHtml = document.body().html();

            // Spaces and new lines are removed to exclude difference in space characters
            assertEquals(TestStringUtils.removeNonsensicalSymbols(expectedHtml), TestStringUtils.removeNonsensicalSymbols(processedHtml));
        }
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

    @Test
    void processHtmlForPdfWithGenerationLogShouldRecordTimings() {
        when(localizationSettings.load(any(), any(SettingId.class)))
                .thenReturn(new LocalizationModel(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));

        String html = "<html><body><h2>Test</h2><p>Content</p></body></html>";
        ExportParams exportParams = getExportParams();
        PdfGenerationLog generationLog = new PdfGenerationLog();

        processor.processHtmlForPDF(html, exportParams, Collections.emptyList(), generationLog);

        // Verify that timings were recorded
        List<PdfGenerationLog.TimingEntry> entries = generationLog.getTimingEntries();
        assertFalse(entries.isEmpty(), "Timing entries should be recorded when generationLog is provided");

        // Check that expected stages are recorded
        List<String> stageNames = entries.stream()
                .map(PdfGenerationLog.TimingEntry::stageName)
                .toList();
        assertTrue(stageNames.contains("Parse HTML with JSoup"), "Should record JSoup parsing");
        assertTrue(stageNames.contains("Adjust document headings"), "Should record heading adjustment");
    }

    @Test
    void processHtmlForPdfWithoutGenerationLogShouldWork() {
        when(localizationSettings.load(any(), any(SettingId.class)))
                .thenReturn(new LocalizationModel(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));

        String html = "<html><body><h2>Test</h2><p>Content</p></body></html>";
        ExportParams exportParams = getExportParams();

        // Should work without throwing when generationLog is null
        String result = processor.processHtmlForPDF(html, exportParams, Collections.emptyList(), null);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void processHtmlForPDFWithInvalidCssGeneratesResultAndLogsWarnings() {
        // Document with invalid CSS (e.g. '%' in unexpected context) should still be processed successfully,
        // with warnings logged for unparseable CSS declarations
        when(localizationSettings.load(any(), any(SettingId.class)))
                .thenReturn(new LocalizationModel(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));

        String html = """
                <html><body>
                <h2>Chapter</h2>
                <table style="page-break-inside:avoid; width:50%%invalid">
                    <tr><td><p>Work item content</p></td></tr>
                </table>
                <table style="background: url('data:image/svg+xml,%%3Csvg%%3E'); page-break-inside:avoid">
                    <tr><td><p>Another work item</p></td></tr>
                </table>
                <p>Normal paragraph</p>
                </body></html>""";

        com.polarion.core.util.logging.Logger mockLogger = mock(com.polarion.core.util.logging.Logger.class);
        CssUtils.setLogger(mockLogger);
        try {
            String result = processor.processHtmlForPDF(html, getExportParams(), Collections.emptyList());

            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertTrue(result.contains("Work item content"));
            assertTrue(result.contains("Another work item"));
            assertTrue(result.contains("Normal paragraph"));

            verify(mockLogger, atLeastOnce()).warn(argThat((String msg) -> msg.contains("Failed to parse CSS")));
        } finally {
            CssUtils.setLogger(com.polarion.core.util.logging.Logger.getLogger(CssUtils.class));
        }
    }

    @Test
    void processPageBreakWidgetsInlineLandscapeTest() {
        // Inline placement: the marker and the content sit as siblings inside one rich-text cell
        String initialHtml = """
                <div class="content"><div class="polarion-rpe-view polarion-rpe-pdf polarion-rpe-content">
                  <table class="polarion-rp-column-layout"><tbody><tr><td>Block A</td></tr></tbody></table>
                  <table class="polarion-rp-column-layout"><tbody><tr><td class="polarion-rp-column-layout-cell">
                    <p>intro</p>
                    <div class="pdf-exporter-page-break pdf-exporter-page-break-landscape"><style>@media print{.pdf-exporter-page-break-label{display:none !important}}</style><span class="pdf-exporter-page-break-label">Page Break - Landscape</span></div>
                    <p>SOME CONTENT</p>
                    <p>MORE</p>
                  </td></tr></tbody></table>
                </div></div>
                """;

        Document document = JSoupUtils.parseHtml(initialHtml);
        processor.processPageBreakWidgets(document, ExportParams.builder().documentType(DocumentType.LIVE_REPORT).build());

        // The whole marker, including its visible label, must be gone after export processing
        assertTrue(document.select("div.pdf-exporter-page-break").isEmpty());
        assertTrue(document.select(".pdf-exporter-page-break-label").isEmpty());

        Element container = document.selectFirst("div.polarion-rpe-content");
        assertNotNull(container);

        // The content following the marker is lifted out of the cell into a body-level landscape section after the table
        Elements topChildren = container.children();
        assertEquals(3, topChildren.size());
        assertTrue(topChildren.get(0).text().contains("Block A"));
        assertEquals("table", topChildren.get(1).tagName());
        assertTrue(topChildren.get(1).text().contains("intro"));
        assertFalse(topChildren.get(1).text().contains("SOME CONTENT"));

        Element wrapper = topChildren.get(2);
        assertEquals("div", wrapper.tagName());
        assertTrue(wrapper.hasClass("sbb_page_break"));
        assertTrue(wrapper.hasClass("landA4"));
        assertTrue(wrapper.text().contains("SOME CONTENT"));
        assertTrue(wrapper.text().contains("MORE"));
        assertFalse(wrapper.text().contains("intro"));
        // The label belonged to the marker and must not be lifted into the section
        assertFalse(wrapper.text().contains("Page Break"), wrapper.html());
    }

    @Test
    void processPageBreakWidgetsInlineTwoSectionsTest() {
        // Two markers in one rich-text cell: landscape then portrait, segmenting the cell content
        String initialHtml = """
                <div class="content"><div class="polarion-rpe-view polarion-rpe-pdf polarion-rpe-content">
                  <table class="polarion-rp-column-layout"><tbody><tr><td class="polarion-rp-column-layout-cell">
                    <p>intro</p>
                    <div class="pdf-exporter-page-break pdf-exporter-page-break-landscape"></div>
                    <p>LAND CONTENT</p>
                    <div class="pdf-exporter-page-break"></div>
                    <p>PORT CONTENT</p>
                  </td></tr></tbody></table>
                </div></div>
                """;

        Document document = JSoupUtils.parseHtml(initialHtml);
        processor.processPageBreakWidgets(document, ExportParams.builder().documentType(DocumentType.LIVE_REPORT).build());

        assertTrue(document.select("div.pdf-exporter-page-break").isEmpty());

        Element container = document.selectFirst("div.polarion-rpe-content");
        Elements topChildren = container.children();
        // table (intro), landscape section, portrait section - in that order
        assertEquals(3, topChildren.size());
        assertTrue(topChildren.get(0).text().contains("intro"));

        Element landscape = topChildren.get(1);
        assertTrue(landscape.hasClass("sbb_page_break"));
        assertTrue(landscape.hasClass("landA4"));
        assertTrue(landscape.text().contains("LAND CONTENT"));
        assertFalse(landscape.text().contains("PORT CONTENT"));

        Element portrait = topChildren.get(2);
        assertTrue(portrait.hasClass("sbb_page_break"));
        assertTrue(portrait.hasClass("portA4"));
        assertTrue(portrait.text().contains("PORT CONTENT"));
    }

    @Test
    void processPageBreakWidgetsBlockPlacementTest() {
        // Standalone/block placement: the marker is alone in its own table; following report tables form the section
        String initialHtml = """
                <div class="content"><div class="polarion-rpe-view polarion-rpe-pdf polarion-rpe-content">
                  <table class="polarion-rp-column-layout"><tbody><tr><td>Block A</td></tr></tbody></table>
                  <table class="polarion-rp-column-layout"><tbody><tr><td><div class="pdf-exporter-page-break pdf-exporter-page-break-landscape"></div></td></tr></tbody></table>
                  <table class="polarion-rp-column-layout"><tbody><tr><td>Block B</td></tr></tbody></table>
                  <table class="polarion-rp-column-layout"><tbody><tr><td>Block C</td></tr></tbody></table>
                </div></div>
                """;

        Document document = JSoupUtils.parseHtml(initialHtml);
        processor.processPageBreakWidgets(document, ExportParams.builder().documentType(DocumentType.LIVE_REPORT).build());

        assertTrue(document.select("div.pdf-exporter-page-break").isEmpty());

        Element container = document.selectFirst("div.polarion-rpe-content");
        // Block A table, the (now empty) marker table, then the landscape wrapper holding Block B and Block C tables
        Element wrapper = container.selectFirst("div.sbb_page_break");
        assertNotNull(wrapper);
        assertTrue(wrapper.hasClass("landA4"));
        assertTrue(wrapper.text().contains("Block B"));
        assertTrue(wrapper.text().contains("Block C"));
        assertEquals(2, wrapper.select("table.polarion-rp-column-layout").size());
    }

    @Test
    void processPageBreakWidgetsNoMarkerTest() {
        String initialHtml = """
                <div class="content"><div class="polarion-rpe-view polarion-rpe-pdf polarion-rpe-content">
                  <table class="polarion-rp-column-layout"><tbody><tr><td>Block A</td></tr></tbody></table>
                </div></div>
                """;

        Document document = JSoupUtils.parseHtml(initialHtml);
        processor.processPageBreakWidgets(document, ExportParams.builder().documentType(DocumentType.LIVE_REPORT).build());

        assertTrue(document.select("div.sbb_page_break").isEmpty());
        assertEquals(1, document.selectFirst("div.polarion-rpe-content").children().size());
    }

    @Test
    void processPageBreakWidgetsTwoBlockSectionsTest() {
        // Two standalone markers in separate tables, with content tables between and after -> two oriented sections
        String initialHtml = """
                <div class="content"><div class="polarion-rpe-view polarion-rpe-pdf polarion-rpe-content">
                  <table class="polarion-rp-column-layout"><tbody><tr><td>Block A</td></tr></tbody></table>
                  <table class="polarion-rp-column-layout"><tbody><tr><td><div class="pdf-exporter-page-break pdf-exporter-page-break-landscape"></div></td></tr></tbody></table>
                  <table class="polarion-rp-column-layout"><tbody><tr><td>Block B</td></tr></tbody></table>
                  <table class="polarion-rp-column-layout"><tbody><tr><td><div class="pdf-exporter-page-break"></div></td></tr></tbody></table>
                  <table class="polarion-rp-column-layout"><tbody><tr><td>Block C</td></tr></tbody></table>
                </div></div>
                """;

        Document document = JSoupUtils.parseHtml(initialHtml);
        processor.processPageBreakWidgets(document, ExportParams.builder().documentType(DocumentType.LIVE_REPORT).build());

        assertTrue(document.select("div.pdf-exporter-page-break").isEmpty());

        Element container = document.selectFirst("div.polarion-rpe-content");
        Elements wrappers = container.select("div.sbb_page_break");
        assertEquals(2, wrappers.size());

        Element landscape = wrappers.get(0);
        assertTrue(landscape.hasClass("landA4"));
        assertTrue(landscape.text().contains("Block B"));
        assertFalse(landscape.text().contains("Block C"));

        Element portrait = wrappers.get(1);
        assertTrue(portrait.hasClass("portA4"));
        assertTrue(portrait.text().contains("Block C"));
        assertFalse(portrait.text().contains("Block B"));
    }

    @Test
    void processPageBreakWidgetsFallsBackToBodyWhenNoReportContainerTest() {
        // No polarion-rpe-content: the body becomes the container the section is lifted to
        String initialHtml = """
                <table class="polarion-rp-column-layout"><tbody><tr><td>Block A</td></tr></tbody></table>
                <table class="polarion-rp-column-layout"><tbody><tr><td><div class="pdf-exporter-page-break pdf-exporter-page-break-landscape"></div></td></tr></tbody></table>
                <table class="polarion-rp-column-layout"><tbody><tr><td>Block B</td></tr></tbody></table>
                """;

        Document document = JSoupUtils.parseHtml(initialHtml);
        processor.processPageBreakWidgets(document, ExportParams.builder().documentType(DocumentType.LIVE_REPORT).build());

        assertTrue(document.select("div.pdf-exporter-page-break").isEmpty());
        Element wrapper = document.body().selectFirst("div.sbb_page_break");
        assertNotNull(wrapper);
        assertTrue(wrapper.hasClass("landA4"));
        assertTrue(wrapper.text().contains("Block B"));
    }

    @Test
    void processPageBreakWidgetsDropsStrayMarkerOutsideContainerTest() {
        // A marker that is not inside the report content container is just dropped, no section is created
        String initialHtml = """
                <div class="content">
                  <div class="polarion-rpe-view polarion-rpe-pdf polarion-rpe-content"><table class="polarion-rp-column-layout"><tbody><tr><td>Block A</td></tr></tbody></table></div>
                  <div class="outside"><div class="pdf-exporter-page-break pdf-exporter-page-break-landscape"></div></div>
                </div>
                """;

        Document document = JSoupUtils.parseHtml(initialHtml);
        processor.processPageBreakWidgets(document, ExportParams.builder().documentType(DocumentType.LIVE_REPORT).build());

        assertTrue(document.select("div.pdf-exporter-page-break").isEmpty());
        assertTrue(document.select("div.sbb_page_break").isEmpty());
    }

    @Test
    void processPageBreakWidgetsNoContentAfterMarkerProducesNoSectionTest() {
        // A marker with nothing following it (neither in the cell nor after the block) yields no section
        String initialHtml = """
                <div class="content"><div class="polarion-rpe-view polarion-rpe-pdf polarion-rpe-content">
                  <table class="polarion-rp-column-layout"><tbody><tr><td>Block A</td></tr></tbody></table>
                  <table class="polarion-rp-column-layout"><tbody><tr><td><div class="pdf-exporter-page-break pdf-exporter-page-break-landscape"></div></td></tr></tbody></table>
                </div></div>
                """;

        Document document = JSoupUtils.parseHtml(initialHtml);
        processor.processPageBreakWidgets(document, ExportParams.builder().documentType(DocumentType.LIVE_REPORT).build());

        assertTrue(document.select("div.pdf-exporter-page-break").isEmpty());
        assertTrue(document.select("div.sbb_page_break").isEmpty());
    }

    @Test
    void processPageBreakWidgetsUsesConfiguredPaperSizeTest() {
        String initialHtml = """
                <div class="content"><div class="polarion-rpe-view polarion-rpe-pdf polarion-rpe-content">
                  <table class="polarion-rp-column-layout"><tbody><tr><td class="polarion-rp-column-layout-cell">
                    <div class="pdf-exporter-page-break pdf-exporter-page-break-landscape"></div>
                    <p>SOME CONTENT</p>
                  </td></tr></tbody></table>
                </div></div>
                """;

        ExportParams exportParams = mock(ExportParams.class);
        when(exportParams.getPaperSize()).thenReturn(PaperSize.LETTER);

        Document document = JSoupUtils.parseHtml(initialHtml);
        processor.processPageBreakWidgets(document, exportParams);

        Element wrapper = document.selectFirst("div.sbb_page_break");
        assertNotNull(wrapper);
        assertTrue(wrapper.hasClass("landLETTER"), wrapper.className());
    }

    @Test
    void processPageBreakWidgetsDefaultsToA4WhenPaperSizeNullTest() {
        String initialHtml = """
                <div class="content"><div class="polarion-rpe-view polarion-rpe-pdf polarion-rpe-content">
                  <table class="polarion-rp-column-layout"><tbody><tr><td class="polarion-rp-column-layout-cell">
                    <div class="pdf-exporter-page-break"></div>
                    <p>SOME CONTENT</p>
                  </td></tr></tbody></table>
                </div></div>
                """;

        ExportParams exportParams = mock(ExportParams.class);
        when(exportParams.getPaperSize()).thenReturn(null);

        Document document = JSoupUtils.parseHtml(initialHtml);
        processor.processPageBreakWidgets(document, exportParams);

        Element wrapper = document.selectFirst("div.sbb_page_break");
        assertNotNull(wrapper);
        assertTrue(wrapper.hasClass("portA4"), wrapper.className());
    }

    @Test
    void processHtmlForPDFAppliesPageBreakWidgetForLiveReport() {
        // Drives the full pipeline so the page-break step wired into the LIVE_REPORT branch of processHtmlForPDF runs
        String html = """
                <div class="content"><div class="polarion-rpe-view polarion-rpe-pdf polarion-rpe-content">
                  <table class="polarion-rp-column-layout"><tbody><tr><td class="polarion-rp-column-layout-cell">
                    <p>intro</p>
                    <div class="pdf-exporter-page-break pdf-exporter-page-break-landscape"></div>
                    <p>SOME CONTENT</p>
                  </td></tr></tbody></table>
                </div></div>
                """;

        ExportParams exportParams = ExportParams.builder()
                .projectId("test_project")
                .documentType(DocumentType.LIVE_REPORT)
                .language(Language.DE.name())
                .build();

        String result = processor.processHtmlForPDF(html, exportParams, List.of());

        // The marker is consumed and the following content ends up in a body-level landscape section
        assertFalse(result.contains("pdf-exporter-page-break"), result);
        assertTrue(result.contains("sbb_page_break"), result);
        assertTrue(result.contains("landA4"), result);
        assertTrue(result.contains("SOME CONTENT"), result);
    }

    private ExportParams getExportParams() {
        return ExportParams.builder()
                .projectId("test_project")
                .documentType(DocumentType.LIVE_DOC)
                .language(Language.DE.name())
                .build();
    }

}

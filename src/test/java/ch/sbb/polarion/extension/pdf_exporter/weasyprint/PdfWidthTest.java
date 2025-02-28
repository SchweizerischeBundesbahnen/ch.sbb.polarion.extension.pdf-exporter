package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.util.FileResourceProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.HtmlProcessor;
import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
import ch.sbb.polarion.extension.pdf_exporter.util.adjuster.PageWidthAdjuster;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BaseWeasyPrintTest;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class PdfWidthTest extends BaseWeasyPrintTest {

    public static final String FIXED_POSTFIX = "Fixed";
    private static final HtmlProcessor htmlProcessor = new HtmlProcessor(mock(FileResourceProvider.class), null, null, null);

    private static Stream<Arguments> testWidthViolationParams() {
        return Stream.of(
                Arguments.of("longWordsInAutoColumns", null),
                Arguments.of("nbspAfterSpan", (Function<String, String>) htmlProcessor::cutExtraNbsp),
                Arguments.of("wideImage", (Function<String, String>) html -> new PageWidthAdjuster(html).adjustImageSize().toHTML()),
                Arguments.of("wideTable", (Function<String, String>) html -> htmlProcessor.adjustTableSize(html, Orientation.PORTRAIT, PaperSize.A4)),
                Arguments.of("wideImagesInTable", (Function<String, String>) html -> htmlProcessor.adjustImageSizeInTables(html, Orientation.PORTRAIT, PaperSize.A4))
        );
    }

    @ParameterizedTest
    @MethodSource("testWidthViolationParams")
    @SneakyThrows
    void testWidthViolation(String fileName, Function<String, String> processFunction) {
        convertToPdfAndAssert(fileName, readHtmlResource(fileName), false);
        String fixedFileName = fileName + FIXED_POSTFIX;
        if (processFunction == null) { //if no process function specified - look for and check 'fixed' resource
            convertToPdfAndAssert(fixedFileName, readHtmlResource(fixedFileName), true);
        } else {
            String fixedHtml = processFunction.apply(readHtmlResource(fileName));
            try (FileWriter fileWriter = new FileWriter(REPORTS_FOLDER_PATH + fixedFileName + EXT_HTML)) {
                fileWriter.write(fixedHtml); //write 'fixed' html file to the reports folder
            }
            convertToPdfAndAssert(fixedFileName, fixedHtml, true);
        }
    }

    @SneakyThrows
    private void convertToPdfAndAssert(String fileName, String html, boolean allWhite) {
        List<BufferedImage> images = exportAndGetAsImages(fileName, html);
        assertThat(images).size().isEqualTo(1);
        assertEquals(allWhite, MediaUtils.checkAllRightPixelsAreWhite(images.get(0)));
    }
}

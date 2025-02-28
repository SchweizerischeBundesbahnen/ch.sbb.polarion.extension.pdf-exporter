package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ImageSizeInTablesAdjusterTest {

    @Test
    void getImageWidthBasedOnColumnsCountTest() {
        ImageSizeInTablesAdjuster imageSizeInTablesAdjuster = new ImageSizeInTablesAdjuster(mock(Document.class), ConversionParams.builder().build());
        assertEquals(-1, imageSizeInTablesAdjuster.getImageWidthBasedOnColumnsCount("<tr><img/></tr>", "<img/>"));
        assertEquals(-1, imageSizeInTablesAdjuster.getImageWidthBasedOnColumnsCount("<tr><td></td><td><img/></td><td></td>", "<img/>"));
        assertEquals(197, imageSizeInTablesAdjuster.getImageWidthBasedOnColumnsCount("<tr><td></td><td><img/></td><td></td></tr>", "<img/>"));

        assertEquals(437, new ImageSizeInTablesAdjuster(mock(Document.class), ConversionParams.builder().orientation(Orientation.LANDSCAPE).paperSize(PaperSize.A3).build()).getImageWidthBasedOnColumnsCount("<tr><td></td></tr><tr><td></td><td><img/></td><td></td></tr><tr><td></td></tr>", "<img/>"));
    }


    @Test
    void columnsCountTest() {
        ImageSizeInTablesAdjuster imageSizeInTablesAdjuster = new ImageSizeInTablesAdjuster(mock(Document.class), ConversionParams.builder().build());
        assertEquals(0, imageSizeInTablesAdjuster.columnsCount(""));
        assertEquals(0, imageSizeInTablesAdjuster.columnsCount("<div></div>"));
        assertEquals(3, imageSizeInTablesAdjuster.columnsCount("<div><td></td><td><span/></td><td></div>"));
    }

}

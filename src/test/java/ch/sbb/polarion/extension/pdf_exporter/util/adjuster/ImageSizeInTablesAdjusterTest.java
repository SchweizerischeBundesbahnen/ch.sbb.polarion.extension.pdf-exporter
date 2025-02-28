package ch.sbb.polarion.extension.pdf_exporter.util.adjuster;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ConversionParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ImageSizeInTablesAdjusterTest {

    @Test
    void getImageWidthBasedOnColumnsCountTest() {
        ImageSizeInTablesAdjuster imageSizeInTablesAdjuster = new ImageSizeInTablesAdjuster(mock(Document.class), ConversionParams.builder().build());
        assertEquals(-1, imageSizeInTablesAdjuster.getImageWidthBasedOnColumnsCount(Jsoup.parse("<table><tr><img id='test'></tr></table>").getElementById("test")));
        assertEquals(296, imageSizeInTablesAdjuster.getImageWidthBasedOnColumnsCount(Jsoup.parse("<table><tr><td></td><td><img id='test'></td></table>").getElementById("test")));
        assertEquals(197, imageSizeInTablesAdjuster.getImageWidthBasedOnColumnsCount(Jsoup.parse("<table><tr><td></td><td><img id='test'></td><td></td></tr></table>").getElementById("test")));

        assertEquals(437, new ImageSizeInTablesAdjuster(mock(Document.class), ConversionParams.builder().orientation(Orientation.LANDSCAPE).paperSize(PaperSize.A3).build()).getImageWidthBasedOnColumnsCount(Jsoup.parse("<table><tr><td></td></tr><tr><td></td><td><img id='test'></td><td></td></tr><tr><td></td></tr></table>").getElementById("test")));
    }


    @Test
    void columnsCountTest() {
        ImageSizeInTablesAdjuster imageSizeInTablesAdjuster = new ImageSizeInTablesAdjuster(mock(Document.class), ConversionParams.builder().build());
        assertEquals(0, imageSizeInTablesAdjuster.columnsCount(Jsoup.parse("<table id='test'></table>").getElementById("test")));
        assertEquals(3, imageSizeInTablesAdjuster.columnsCount(Jsoup.parse("<table id='test'><td></td><td><span/></td><td></table>").getElementById("test")));
        assertEquals(4, imageSizeInTablesAdjuster.columnsCount(Jsoup.parse("<table id='test'><td colspan='2'></td><td></td><td></table>").getElementById("test")));
        assertEquals(8, imageSizeInTablesAdjuster.columnsCount(Jsoup.parse("<table id='test'><td colspan='5'></td><td colspan='2'></td><td></table>").getElementById("test")));
    }

}

package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import com.polarion.alm.shared.rt.parts.impl.readonly.WikiBlockPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CustomWikiBlockPartTest {

    @Mock
    private WikiBlockPart wikiBlockPart;

    private CustomWikiBlockPart customWikiBlockPart;

    @BeforeEach
    void setup() {
        customWikiBlockPart = new CustomWikiBlockPart(wikiBlockPart);
    }

    @Test
    void testCallRender() {
        assertDoesNotThrow(() -> customWikiBlockPart.setRenderedContentHtml(""));
    }

    @Test
    void convertPd4mlPageBreakTagsTest() {
        // Test conversion of raw <pd4ml:page.break pageformat="..."> tags to markers
        String html = "<h1>Ch1</h1>" +
                "<pd4ml:page.break pageformat=\"rotate\">" +
                "<h2>Ch2</h2>" +
                "<pd4ml:page.break pageformat=\"reset\">" +
                "<h2>Ch3</h2>" +
                "<pd4ml:page.break>" +
                "<h2>Ch4</h2>" +
                "<pd4ml:page.break pageformat=\"unknown\">" +
                "<h2>Ch5</h2>";

        String converted = customWikiBlockPart.convertPd4mlPageBreakTags(html);

        assertEquals("<h1>Ch1</h1>" +
                "<!--PAGE_BREAK--><!--ROTATE_BELOW-->" +
                "<h2>Ch2</h2>" +
                "<!--PAGE_BREAK--><!--RESET_BELOW-->" +
                "<h2>Ch3</h2>" +
                "<!--PAGE_BREAK--><!--BREAK_BELOW-->" +
                "<h2>Ch4</h2>" +
                "<!--PAGE_BREAK--><!--BREAK_BELOW-->" +
                "<h2>Ch5</h2>", converted);
    }

    @Test
    void convertPd4mlPageBreakTagsCaseInsensitiveTest() {
        // Test that conversion is case-insensitive for pageformat value
        String html = "<pd4ml:page.break pageformat=\"ROTATE\"><pd4ml:page.break pageformat=\"Reset\">";

        String converted = customWikiBlockPart.convertPd4mlPageBreakTags(html);

        assertEquals("<!--PAGE_BREAK--><!--ROTATE_BELOW--><!--PAGE_BREAK--><!--RESET_BELOW-->", converted);
    }

}

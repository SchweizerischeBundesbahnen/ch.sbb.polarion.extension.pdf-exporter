package ch.sbb.polarion.extension.pdf_exporter.util;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The same cases as the empty last page tests of tests/test_docx_post_process.py in pandoc-service. */
class JSoupUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "<!--LANDSCAPE_ABOVE-->",
            "<br/>",
            "<p></p>",
            "<p><br/></p>",
            "<p>&nbsp;</p>",
            "<p> &nbsp;NBSP ZWS ZWNJ</p>",
            "<div><div></div></div>",
            "<span></span>",
            "<a id=\"anchor\"></a>",
            "</div></div>",
            "<style>p { color: red; }</style>",
            "<p style=\"color: red; font-weight: bold\">&nbsp;</p>",
            "<p style=\"background: none\"><br/></p>",
            "<p style=\"background-color: transparent\">&nbsp;</p>",
            "<p style=\"border: none\"><br/></p>",
            "<p style=\"border: 0\"></p>",
            "<p style=\"border: 0px solid black\"></p>",
            "<p style=\"text-decoration: none\">&nbsp;</p>",
            "<span style=\"background-color: #ffff00\"></span>",
            "<span style=\"background-color: #ffff00\"><br/></span>",
            "<span style=\"background-color: #ffff00\">ZWS</span>",
            "<div style=\"background-color: #ffff00\"></div>",
            "<div style=\"background-color: #ffff00\">\n  <span></span>\n</div>",
            "<u></u>",
            "<ul></ul>",
            "<ol></ol>",
    })
    void showsNothing(String html) {
        assertTrue(JSoupUtils.isVisuallyEmpty(html.replace("NBSP", "\u00A0").replace("ZWS", "\u200B").replace("ZWNJ", "\u200C")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "text",
            "<p>text</p>",
            "<div><div><span>text</span></div></div>",
            "<table><tr><td></td></tr></table>",
            "<table></table>",
            "<ul><li></li></ul>",
            "<ol><li><br/></li></ol>",
            "<hr/>",
            "<img src=\"diagram.png\"/>",
            "<svg></svg>",
            "<object></object>",
            "<canvas></canvas>",
            "<p><span style=\"background-color: #ffff00\">&nbsp;</span></p>",
            "<p><span style=\"background: yellow\">&nbsp;&nbsp;</span></p>",
            "<p><span style=\"BACKGROUND-COLOR: #FFFF00\"> </span></p>",
            "<p><span style=\"text-decoration: underline\">&nbsp;</span></p>",
            "<p><span style=\"text-decoration: line-through\">&nbsp;</span></p>",
            "<p><span style=\"text-decoration-line: underline\">&nbsp;</span></p>",
            "<p style=\"text-decoration: underline\"><span>&nbsp;</span></p>",
            "<p><span style=\"border: 1px solid black\">&nbsp;</span></p>",
            "<p><u>&nbsp;</u></p>",
            "<p><s>&nbsp;</s></p>",
            "<p><strike>&nbsp;</strike></p>",
            "<p><del>&nbsp;</del></p>",
            "<p><ins>&nbsp;</ins></p>",
            "<p><mark>&nbsp;</mark></p>",
            "<p style=\"background-color: #ffff00\"><br/></p>",
            "<p style=\"background-color: #ffff00\">&nbsp;</p>",
            "<div style=\"border: 1px solid black\"></div>",
            "<div style=\"border-left: 2px solid black\"></div>",
            "<p style=\"border-style: solid\"><br/></p>",
    })
    void shows(String html) {
        assertFalse(JSoupUtils.isVisuallyEmpty(html));
    }

    @Test
    void readsAnElementAsTheHtmlOfIt() {
        Document document = JSoupUtils.parseHtml("<p id=\"empty\"><br/></p><p id=\"painted\" style=\"background-color: #ffff00\"><br/></p>");

        assertTrue(JSoupUtils.isVisuallyEmpty(document.selectFirst("#empty")));
        assertFalse(JSoupUtils.isVisuallyEmpty(document.selectFirst("#painted")));
    }
}

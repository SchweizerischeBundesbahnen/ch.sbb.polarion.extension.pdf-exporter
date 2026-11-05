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
            "<p style=\"background: none repeat scroll 0% 0%\">&nbsp;</p>",
            "<p style=\"background: transparent none repeat scroll 0 0\"><br/></p>",
            "<p style=\"background: transparent no-repeat center / cover\">&nbsp;</p>",
            "<p style=\"background: none, none\">&nbsp;</p>",
            "<div style=\"height: 40px; background: none no-repeat left top\"></div>",
            "<p style=\"background-color: yellow; background-color: transparent\">&nbsp;</p>",
            "<p style=\"background-color: transparent !important; background-color: yellow\">&nbsp;</p>",
            "<p><span style=\"text-decoration: underline; text-decoration: none\">&nbsp;</span></p>",
            "<div style=\"height: 40px; height: 0; background-color: yellow\"></div>",
            "<div style=\"padding: 10px; padding: 0; background-color: yellow\"></div>",
            "<p style=\"border: none\"><br/></p>",
            "<p style=\"border: 0\"></p>",
            "<p style=\"border: 0px solid black\"></p>",
            "<p style=\"border: solid 0\"></p>",
            "<p style=\"border: red\"><br/></p>",
            "<p style=\"border: 1px\"><br/></p>",
            "<p style=\"border: 1px red\"><br/></p>",
            "<p style=\"border-top: 1px red\"><br/></p>",
            "<p style=\"border: 1px hidden red\"><br/></p>",
            "<p style=\"border-style: solid; border-width: 0\"><br/></p>",
            "<p style=\"border-width: 0px; border-style: solid\"><br/></p>",
            "<p style=\"border: 1px solid black; border-width: 0\"><br/></p>",
            "<p style=\"border: 1px solid black; border-style: none\"><br/></p>",
            "<p style=\"border-top: 1px solid black; border-top-width: 0\"><br/></p>",
            "<p style=\"border-style: solid none; border-width: 0 1px\"><br/></p>",
            "<p style=\"border-top-style: solid; border-top-width: 0\"><br/></p>",
            "<p style=\"border: 1px solid black; border-width: 0 !important\"><br/></p>",
            "<p style=\"border: 1px solid black !important; border-width: 0 !important\"><br/></p>",
            "<p style=\"border-width: 0 !important; border: 1px solid black\"><br/></p>",
            "<p style=\"border-style: none !important; border: 1px solid black\"><br/></p>",
            "<p style=\"text-decoration: none\">&nbsp;</p>",
            "<span style=\"background-color: #ffff00\"></span>",
            "<span style=\"background-color: #ffff00\"><br/></span>",
            "<span style=\"background-color: #ffff00\">ZWS</span>",
            "<div style=\"background-color: #ffff00\"></div>",
            "<div style=\"background-color: #ffff00\">\n  <span></span>\n</div>",
            "<div style=\"height: 40px\"></div>",
            "<div style=\"height: 0; background-color: #ffff00\"></div>",
            "<div style=\"height: auto; background-color: #ffff00\"></div>",
            "<div style=\"min-height: 0px; background-color: #ffff00\"></div>",
            "<div style=\"padding: 0; background-color: #ffff00\"></div>",
            "<div style=\"padding: 0 10px; background-color: #ffff00\"></div>",
            "<div style=\"padding-left: 10px; background-color: #ffff00\"></div>",
            "<span style=\"height: 40px; background-color: #ffff00\"></span>",
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
            "<p style=\"background-color: transparent; background-color: yellow\">&nbsp;</p>",
            "<p style=\"background-color: yellow !important; background-color: transparent\">&nbsp;</p>",
            "<p style=\"background-color: transparent; background-color: yellow !important; background-color: none\">&nbsp;</p>",
            "<p><span style=\"text-decoration: none; text-decoration: underline\">&nbsp;</span></p>",
            "<div style=\"height: 0; height: 40px; background-color: yellow\"></div>",
            "<div style=\"padding: 0; padding: 10px; background-color: yellow\"></div>",
            "<p style=\"background: yellow none repeat scroll 0% 0%\">&nbsp;</p>",
            "<p style=\"background: #ffff00 no-repeat\"><br/></p>",
            "<p style=\"background: rgb(255, 255, 0) none\">&nbsp;</p>",
            "<p style=\"background: transparent url(pattern.png) no-repeat\">&nbsp;</p>",
            "<div style=\"height: 40px; background-color: yellow\"></div>",
            "<div style=\"min-height: 40px; background-color: yellow\"></div>",
            "<div style=\"height: 0.5em; background-color: yellow\"></div>",
            "<div style=\"padding: 10px; background-color: yellow\"></div>",
            "<div style=\"padding: 0 0 10px; background-color: yellow\"></div>",
            "<div style=\"padding-top: 10px; background-color: yellow\"></div>",
            "<div style=\"padding-bottom: 10px; background-color: yellow\"></div>",
            "<div style=\"background-color: yellow\"><div style=\"height: 40px\"></div></div>",
            "<div style=\"border: 1px solid black\"></div>",
            "<div style=\"border-left: 2px solid black\"></div>",
            "<p style=\"border-style: solid\"><br/></p>",
            "<p style=\"border-style: none solid\"><br/></p>",
            "<p style=\"border: 1px; border-style: dashed\"><br/></p>",
            "<p style=\"border: solid\"><br/></p>",
            "<p style=\"border: 1px dotted red\"><br/></p>",
            "<p style=\"border: 1px solid rgb(0 0 0)\"><br/></p>",
            "<p style=\"border-style: solid; border-width: 0 0 1px 0\"><br/></p>",
            "<p style=\"border-style: solid; border-width: 0 1px\"><br/></p>",
            "<p style=\"border-style: solid; border-width: 0; border-left-width: 1px\"><br/></p>",
            "<p style=\"border-width: 0; border-top: 1px solid black\"><br/></p>",
            "<p style=\"border: none; border-left: 1px solid black\"><br/></p>",
            "<p style=\"border-top-style: solid\"><br/></p>",
            "<p style=\"border: 1px solid black !important; border-width: 0\"><br/></p>",
            "<p style=\"border: 1px solid black !important; border-style: none\"><br/></p>",
            "<p style=\"border: 1px solid black !important; border: none\"><br/></p>",
            "<p style=\"border-top: 1px solid black !important; border-width: 0\"><br/></p>",
            "<p style=\"border-style: solid !important; border-style: none; border-width: 1px\"><br/></p>",
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

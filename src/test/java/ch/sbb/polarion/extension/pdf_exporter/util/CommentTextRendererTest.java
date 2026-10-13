package ch.sbb.polarion.extension.pdf_exporter.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommentTextRendererTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "<p><br/></p>", "<p>\u200B\u200B</p>"})
    void emptyCommentTest(String richText) {
        assertEquals("", CommentTextRenderer.toInlineHtml(richText));
    }

    @Test
    void plainTextTest() {
        assertEquals("A comment", CommentTextRenderer.toInlineHtml("A comment"));
    }

    @Test
    void escapesTextTest() {
        assertEquals("a &lt; b &amp; c", CommentTextRenderer.toInlineHtml("a &lt; b &amp; c"));
    }

    @Test
    void richTextTest() {
        String richText = """
                <p>QWQEQEQ 11111</p>
                <ol>
                    <li><span style="font-weight: bold;">111</span></li>
                    <li><span style="font-weight: bold;">222</span></li>
                    <li><span style="font-weight: bold;">333</span></li>
                </ol>
                <p><br/>r435435435<br/><br/>\u200B\u200B</p>
                <ul>
                    <li>aaaa</li>
                    <li>bbbb</li>
                    <li>cccc</li>
                </ul>
                """;
        assertEquals("QWQEQEQ 11111<br/>1. 111<br/>2. 222<br/>3. 333<br/>r435435435<br/>\u2022 aaaa<br/>\u2022 bbbb<br/>\u2022 cccc",
                CommentTextRenderer.toInlineHtml(richText));
    }

    @Test
    void nestedListTest() {
        String richText = "<ol><li>one<ul><li>inner</li></ul></li><li>two</li></ol>";
        assertEquals("1. one<br/>\u2022 inner<br/>2. two", CommentTextRenderer.toInlineHtml(richText));
    }

    @Test
    void inlineFormattingTest() {
        assertEquals("bold and italic text", CommentTextRenderer.toInlineHtml("<b>bold</b> and <i>italic</i> <a href=\"#\">text</a>"));
    }

    @Test
    void listItemInParagraphTest() {
        String richText = "<ul><li><p>one</p></li><li><div>two</div><p>more</p></li></ul><ol><li><p><br/>first</p></li></ol>";
        assertEquals("\u2022 one<br/>\u2022 two<br/>more<br/>1. first", CommentTextRenderer.toInlineHtml(richText));
    }

    @Test
    void emptyListItemTest() {
        assertEquals("\u2022 two", CommentTextRenderer.toInlineHtml("<ul><li></li><li>two</li></ul>"));
    }

    @Test
    void emptyLastListItemTest() {
        assertEquals("1. one<br/>after", CommentTextRenderer.toInlineHtml("<ol><li>one</li><li></li></ol><p>after</p>"));
    }

    @Test
    void orderedListWithOtherElementsTest() {
        assertEquals("x<br/>1. a<br/>2. b", CommentTextRenderer.toInlineHtml("<ol><p>x</p><li>a</li><li>b</li></ol>"));
    }

    @Test
    void htmlCommentIsIgnoredTest() {
        assertEquals("before after", CommentTextRenderer.toInlineHtml("<p>before <!-- hidden --> after</p>"));
    }

    @Test
    void tableTest() {
        String richText = "<table><tr><th>h1</th><th>h2</th></tr><tr><td>a</td><td>b</td></tr><tr><td>c</td><td>d</td></tr></table>";
        assertEquals("h1 h2<br/>a b<br/>c d", CommentTextRenderer.toInlineHtml(richText));
    }
}

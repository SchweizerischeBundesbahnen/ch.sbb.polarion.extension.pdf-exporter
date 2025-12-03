package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.CommentsRenderType;
import com.polarion.alm.server.api.model.document.ProxyDocument;
import com.polarion.alm.shared.api.model.comment.CommentBase;
import com.polarion.alm.shared.api.model.comment.CommentBasesField;
import com.polarion.alm.shared.api.model.comment.CommentBasesTreeField;
import com.polarion.alm.shared.api.model.document.UpdatableDocumentFields;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveDocCommentsProcessorTest {

    @Test
    @SuppressWarnings("unchecked")
    void addLiveDocCommentsTest() {
        ProxyDocument document = mock(ProxyDocument.class, RETURNS_DEEP_STUBS);

        UpdatableDocumentFields fields = mock(UpdatableDocumentFields.class);

        CommentBasesTreeField<CommentBase> comments = mock(CommentBasesTreeField.class);
        doCallRealMethod().when(comments).forEach(any(Consumer.class));
        when(fields.comments()).thenReturn(comments);
        when(document.fields()).thenReturn(fields);

        String html = """
                <div>some content1</div>
                <span id="polarion-comment:1"></span>
                <div>some content2</div>
                <span id="polarion-comment:2"></span>
                <div>some content3</div>
                <img id="polarion-comment:3" class="polarion-dle-comment-icon"/>
                <div>some content4</div>
                """;

        List<CommentBase> commentBases = List.of(
                mockComment("1", "text1", "author1", false, true, true),
                mockComment("2", "text2", "author2", true, true, true),
                mockComment("3", "text3", "author3", false, true, false)
        );

        when(comments.iterator()).thenReturn(commentBases.iterator());

        // resolved comment isn't rendered
        assertEquals("""
                <div>some content1</div>
                [span class=comment level-0][span class=meta][span class=date]2025-03-13 16:21[/span][span class=details][span class=author]author1[/span][/span][/span][span class=text]text1[/span][/span][span class=comment level-1][span class=meta][span class=date]2025-03-13 16:21[/span][span class=details][span class=author]author1[/span][/span][/span][span class=text]Reply to text1[/span][/span]
                <div>some content2</div>

                <div>some content3</div>
                [span class=comment level-0][span class=meta][span class=date]2025-03-13 16:23[/span][span class=details][span class=author]author3[/span][/span][/span][span class=text]text3[/span][/span]
                <div>some content4</div>
                """, new LiveDocCommentsProcessor().addLiveDocComments(document, html, CommentsRenderType.OPEN, false));

        when(comments.iterator()).thenReturn(commentBases.iterator());

        // now resolved comment is here
        assertEquals("""
                <div>some content1</div>
                [span class=comment level-0][span class=meta][span class=date]2025-03-13 16:21[/span][span class=details][span class=author]author1[/span][/span][/span][span class=text]text1[/span][/span][span class=comment level-1][span class=meta][span class=date]2025-03-13 16:21[/span][span class=details][span class=author]author1[/span][/span][/span][span class=text]Reply to text1[/span][/span]
                <div>some content2</div>
                [span class=comment level-0][span class=meta][span class=date]2025-03-13 16:22[/span][span class=details][span class=status-resolved]Resolved[/span][span class=author]author2[/span][/span][/span][span class=text]text2[/span][/span][span class=comment level-1][span class=meta][span class=date]2025-03-13 16:22[/span][span class=details][span class=status-resolved]Resolved[/span][span class=author]author2[/span][/span][/span][span class=text]Reply to text2[/span][/span]
                <div>some content3</div>
                [span class=comment level-0][span class=meta][span class=date]2025-03-13 16:23[/span][span class=details][span class=author]author3[/span][/span][/span][span class=text]text3[/span][/span]
                <div>some content4</div>
                """, new LiveDocCommentsProcessor().addLiveDocComments(document, html, CommentsRenderType.ALL, false));

        // null commentsRenderType means remove comments
        assertEquals("""
                <div>some content1</div>

                <div>some content2</div>

                <div>some content3</div>

                <div>some content4</div>
                """, new LiveDocCommentsProcessor().addLiveDocComments(document, html, null, false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void addNativeLiveDocCommentsTest() {
        ProxyDocument document = mock(ProxyDocument.class, RETURNS_DEEP_STUBS);

        UpdatableDocumentFields fields = mock(UpdatableDocumentFields.class);

        CommentBasesTreeField<CommentBase> comments = mock(CommentBasesTreeField.class);
        doCallRealMethod().when(comments).forEach(any(Consumer.class));
        when(fields.comments()).thenReturn(comments);
        when(document.fields()).thenReturn(fields);

        String html = """
                <div>some content1</div>
                <span id="polarion-comment:1"></span>
                <div>some content2</div>
                <span id="polarion-comment:2"></span>
                <div>some content3</div>
                <img id="polarion-comment:3" class="polarion-dle-comment-icon"/>
                <div>some content4</div>
                """;

        List<CommentBase> commentBases = List.of(
                mockComment("1", "text1", "author1", false, true, true),
                mockComment("2", "text2", "author2", true, true, true),
                mockComment("3", "text3", "author3", false, true, false)
        );

        when(comments.iterator()).thenReturn(commentBases.iterator());

        // resolved comment isn't rendered
        LiveDocCommentsProcessor liveDocCommentsProcessor = spy(new LiveDocCommentsProcessor());

        // remove timezone rendering to have predictable output on all systems
        doAnswer(invocationOnMock -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(invocationOnMock.getArguments()[0]))
                .when(liveDocCommentsProcessor).formatIsoDate(any());

        assertEquals("""
                <div>some content1</div>
                [span class=sticky-note][span class=sticky-note-time]2025-03-13T16:21:00.000[/span][span class=sticky-note-username]author1[/span][span class=sticky-note-text]text1[/span][span class=sticky-note][span class=sticky-note-time]2025-03-13T16:21:00.000[/span][span class=sticky-note-username]author1[/span][span class=sticky-note-text]Reply to text1[/span][/span][/span]
                <div>some content2</div>

                <div>some content3</div>
                [span class=sticky-note][span class=sticky-note-time]2025-03-13T16:23:00.000[/span][span class=sticky-note-username]author3[/span][span class=sticky-note-text]text3[/span][/span]
                <div>some content4</div>
                """, liveDocCommentsProcessor.addLiveDocComments(document, html, CommentsRenderType.OPEN, true));

        when(comments.iterator()).thenReturn(commentBases.iterator());

        // now resolved comment is here
        assertEquals("""
                <div>some content1</div>
                [span class=sticky-note][span class=sticky-note-time]2025-03-13T16:21:00.000[/span][span class=sticky-note-username]author1[/span][span class=sticky-note-text]text1[/span][span class=sticky-note][span class=sticky-note-time]2025-03-13T16:21:00.000[/span][span class=sticky-note-username]author1[/span][span class=sticky-note-text]Reply to text1[/span][/span][/span]
                <div>some content2</div>
                [span class=sticky-note][span class=sticky-note-time]2025-03-13T16:22:00.000[/span][span class=sticky-note-username]author2[/span][span class=sticky-note-text]text2[/span][span class=sticky-note][span class=sticky-note-time]2025-03-13T16:22:00.000[/span][span class=sticky-note-username]author2[/span][span class=sticky-note-text]Reply to text2[/span][/span][/span]
                <div>some content3</div>
                [span class=sticky-note][span class=sticky-note-time]2025-03-13T16:23:00.000[/span][span class=sticky-note-username]author3[/span][span class=sticky-note-text]text3[/span][/span]
                <div>some content4</div>
                """, liveDocCommentsProcessor.addLiveDocComments(document, html, CommentsRenderType.ALL, true));

        // null commentsRenderType means remove comments
        assertEquals("""
                <div>some content1</div>

                <div>some content2</div>

                <div>some content3</div>

                <div>some content4</div>
                """, liveDocCommentsProcessor.addLiveDocComments(document, html, null, true));
    }

    @SneakyThrows
    private CommentBase mockComment(String id, String text, String author, boolean resolved, boolean rootComment, boolean addChildComment) {
        CommentBase comment = mock(CommentBase.class, RETURNS_DEEP_STUBS);
        lenient().when(comment.fields().parentComment().get()).thenReturn(rootComment ? null : mock(CommentBase.class));
        lenient().when(comment.fields().id().get()).thenReturn(id);
        lenient().when(comment.fields().created().get()).thenReturn(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2025-03-13 16:2%s".formatted(id)));
        lenient().when(comment.fields().text().persistedHtml()).thenReturn(text);
        lenient().when(Objects.requireNonNull(comment.fields().author().get()).fields().name().get()).thenReturn(author);
        CommentBasesField<CommentBase> childComment = addChildComment ? mockChildComment(id, text, author, resolved) : mockEmptyChildComment();
        lenient().when(comment.fields().childComments()).thenReturn(childComment);
        lenient().when(comment.fields().resolved().get()).thenReturn(resolved);
        return comment;
    }

    @SuppressWarnings("unchecked")
    private CommentBasesField<CommentBase> mockChildComment(String id, String text, String author, boolean resolved) {
        CommentBasesField<CommentBase> childComments = mock(CommentBasesField.class);
        CommentBase comment = mockComment(id + "_reply", "Reply to " + text, author, resolved, false, false);
        when(childComments.iterator()).thenAnswer(invocationOnMock -> List.of(comment).iterator());
        return childComments;
    }

    @SuppressWarnings("unchecked")
    private CommentBasesField<CommentBase> mockEmptyChildComment() {
        CommentBasesField<CommentBase> childComments = mock(CommentBasesField.class);
        lenient().when(childComments.iterator()).thenAnswer(invocationOnMock -> Collections.emptyIterator());
        lenient().when(childComments.isEmpty()).thenReturn(true);
        return childComments;
    }

    @Test
    @SuppressWarnings("unchecked")
    void addLiveDocCommentsWithVariousImgFormatsTest() {
        // Test that comment markers with different HTML formats are handled correctly:
        // - class attribute before id
        // - non-self-closing img tags
        // - resolved comment icon class (polarion-dle-comment-resolved-icon)
        ProxyDocument document = mock(ProxyDocument.class, RETURNS_DEEP_STUBS);

        UpdatableDocumentFields fields = mock(UpdatableDocumentFields.class);

        CommentBasesTreeField<CommentBase> comments = mock(CommentBasesTreeField.class);
        doCallRealMethod().when(comments).forEach(any(Consumer.class));
        when(fields.comments()).thenReturn(comments);
        when(document.fields()).thenReturn(fields);

        // HTML with various img tag formats that should all be matched
        String html = """
                <div>content1</div>
                <img id="polarion-comment:1" class="polarion-dle-comment-icon"/>
                <div>content2</div>
                <img class="polarion-dle-comment-icon" id="polarion-comment:2"/>
                <div>content3</div>
                <img id="polarion-comment:3" class="polarion-dle-comment-icon">
                <div>content4</div>
                <img class="polarion-dle-comment-icon" id="polarion-comment:4">
                <div>content5</div>
                <img id="polarion-comment:5" src="icon.png" class="polarion-dle-comment-icon"/>
                <div>content6</div>
                <img id="polarion-comment:6" class="polarion-dle-comment-resolved-icon"/>
                <div>content7</div>
                <img class="polarion-dle-comment-resolved-icon" id="polarion-comment:7"/>
                <div>content8</div>
                """;

        List<CommentBase> commentBases = List.of(
                mockComment("1", "text1", "author1", false, true, false),
                mockComment("2", "text2", "author2", false, true, false),
                mockComment("3", "text3", "author3", false, true, false),
                mockComment("4", "text4", "author4", false, true, false),
                mockComment("5", "text5", "author5", false, true, false),
                mockComment("6", "text6", "author6", true, true, false),
                mockComment("7", "text7", "author7", true, true, false)
        );

        when(comments.iterator()).thenReturn(commentBases.iterator());

        // Test with null commentsRenderType - all comment markers should be removed regardless of format
        assertEquals("""
                <div>content1</div>

                <div>content2</div>

                <div>content3</div>

                <div>content4</div>

                <div>content5</div>

                <div>content6</div>

                <div>content7</div>

                <div>content8</div>
                """, new LiveDocCommentsProcessor().addLiveDocComments(document, html, null, false));

        when(comments.iterator()).thenReturn(commentBases.iterator());

        // Test with CommentsRenderType.ALL - all comments should be rendered regardless of img format
        String result = new LiveDocCommentsProcessor().addLiveDocComments(document, html, CommentsRenderType.ALL, false);
        // Verify all comments are rendered (contain the comment text markers)
        assertEquals(7, countOccurrences(result, "[span class=comment level-0]"));
    }

    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    /**
     * Simulating real-world LiveDoc comment processing flow:
     * 1. First pass: process comments in raw document content (span markers)
     * 2. Document renderer produces workitem HTML with img comment markers
     * 3. Second pass: process comments in rendered content (img markers with various classes)
     *
     * Tests all three renderComments modes: null (remove), OPEN (only open), ALL (all comments)
     */
    @Test
    @SuppressWarnings("unchecked")
    void twoPassCommentProcessingIntegrationTest() {
        ProxyDocument document = mock(ProxyDocument.class, RETURNS_DEEP_STUBS);
        UpdatableDocumentFields fields = mock(UpdatableDocumentFields.class);
        CommentBasesTreeField<CommentBase> comments = mock(CommentBasesTreeField.class);
        doCallRealMethod().when(comments).forEach(any(Consumer.class));
        when(fields.comments()).thenReturn(comments);
        when(document.fields()).thenReturn(fields);

        // Comments in document: 5 = open, 6 = resolved
        // Comments in workitems: 1 = open (in EL-5118), 3 = resolved (in EL-5119)
        List<CommentBase> commentBases = List.of(
                mockComment("1", "OPEN in WI", "author1", false, true, false),
                mockComment("3", "RESOLVED in WI", "author3", true, true, false),
                mockComment("5", "open comment inside document", "author5", false, true, false),
                mockComment("6", "resolved comment inside document", "author6", true, true, false)
        );

        // Raw document content (before workitem rendering) - contains span markers for document comments
        String rawDocumentContent = """
                <h1 id="polarion_wiki macro name=module-workitem;params=id=EL-5120"></h1>\
                <div id="polarion_wiki macro name=module-workitem;params=id=EL-5118"></div>\
                <div id="polarion_wiki macro name=module-workitem;params=id=EL-5119"></div>\
                <p id="polarion_1">open<span id="polarion-comment:5"></span> comment inside document</p>\
                <p id="polarion_2">resolved<span id="polarion-comment:6"></span> comment inside document</p>""";

        // Simulated rendered content (after documentRenderer.render()) - workitems are expanded with img comment markers
        String simulatedRenderedContentTemplate = """
                <div class="polarion-dle-pdf"><h1 id="polarion_wiki macro name=module-workitem;params=id=EL-5120" title="Heading: EL-5120">Comments</h1>\
                <div id="polarion_wiki macro name=module-workitem;params=id=EL-5118" class="polarion-dle-workitem-basic-0" title="Requirement: EL-5118">\
                <span>EL-5118</span> - with open<img id="polarion-comment:1" title="OPEN in WI" src="http://localhost/polarion/ria/images/control/comment.png" class="polarion-dle-comment-icon"/> comment inside workitem</div>\
                <div id="polarion_wiki macro name=module-workitem;params=id=EL-5119" class="polarion-dle-workitem-basic-0" title="Requirement: EL-5119">\
                <span>EL-5119</span> - with resolved<img id="polarion-comment:3" title="RESOLVED in WI" src="http://localhost/polarion/ria/images/control/comment_resolved.png" class="polarion-dle-comment-resolved-icon"/> comment inside workitem</div>\
                %s</div>""";

        LiveDocCommentsProcessor processor = new LiveDocCommentsProcessor();

        // === Test 1: renderComments = null (remove all comments) ===
        when(comments.iterator()).thenReturn(commentBases.iterator());

        // First pass: process document comments (spans)
        String afterFirstPass = processor.addLiveDocComments(document, rawDocumentContent, null, false);
        // Document comments (spans) should be removed
        assertFalse(afterFirstPass.contains("polarion-comment:5"), "Open document comment span should be removed");
        assertFalse(afterFirstPass.contains("polarion-comment:6"), "Resolved document comment span should be removed");

        // Simulate renderer output with processed document content
        String documentParagraphs = "<p id=\"polarion_1\">open comment inside document</p><p id=\"polarion_2\">resolved comment inside document</p>";
        String renderedContent = simulatedRenderedContentTemplate.formatted(documentParagraphs);

        when(comments.iterator()).thenReturn(commentBases.iterator());

        // Second pass: process workitem comments (imgs)
        String finalResult = processor.addLiveDocComments(document, renderedContent, null, false);

        // All comment markers should be removed
        assertFalse(finalResult.contains("polarion-comment:"), "All comment markers should be removed when renderComments=null");
        assertFalse(finalResult.contains("polarion-dle-comment-icon"), "Open comment icon should be removed");
        assertFalse(finalResult.contains("polarion-dle-comment-resolved-icon"), "Resolved comment icon should be removed");
        assertTrue(finalResult.contains("with open comment inside workitem"), "Workitem content should remain");
        assertTrue(finalResult.contains("with resolved comment inside workitem"), "Workitem content should remain");

        // === Test 2: renderComments = OPEN (only open comments) ===
        when(comments.iterator()).thenReturn(commentBases.iterator());

        // First pass: process document comments
        afterFirstPass = processor.addLiveDocComments(document, rawDocumentContent, CommentsRenderType.OPEN, false);
        // Open comment should be rendered, resolved should be removed
        assertTrue(afterFirstPass.contains("[span class=comment level-0]"), "Open comment should be rendered");

        // Simulate renderer output
        String openCommentRendered = "[span class=comment level-0][span class=meta][span class=date]";
        documentParagraphs = "<p id=\"polarion_1\">open" + openCommentRendered + "</p><p id=\"polarion_2\">resolved comment inside document</p>";
        // Note: simplified - in reality would have full comment markup

        when(comments.iterator()).thenReturn(commentBases.iterator());

        // Second pass with fresh rendered content
        renderedContent = simulatedRenderedContentTemplate.formatted(
                "<p id=\"polarion_1\">open comment inside document</p><p id=\"polarion_2\">resolved comment inside document</p>");
        finalResult = processor.addLiveDocComments(document, renderedContent, CommentsRenderType.OPEN, false);

        // Open comment in workitem should be rendered
        assertTrue(finalResult.contains("[span class=comment level-0]"), "Open comment in workitem should be rendered");
        // Resolved comment in workitem should be removed
        assertFalse(finalResult.contains("polarion-comment:3"), "Resolved comment marker should be removed");
        assertFalse(finalResult.contains("polarion-dle-comment-resolved-icon"), "Resolved comment icon should be removed");

        // === Test 3: renderComments = ALL (all comments including resolved) ===
        when(comments.iterator()).thenReturn(commentBases.iterator());

        finalResult = processor.addLiveDocComments(document, renderedContent, CommentsRenderType.ALL, false);

        // Both comments should be rendered
        assertEquals(2, countOccurrences(finalResult, "[span class=comment level-0]"), "Both workitem comments should be rendered");
        // No raw comment markers should remain
        assertFalse(finalResult.contains("polarion-dle-comment-icon"), "Comment icon class should be replaced with rendered comment");
        assertFalse(finalResult.contains("polarion-dle-comment-resolved-icon"), "Resolved comment icon class should be replaced");
    }
}

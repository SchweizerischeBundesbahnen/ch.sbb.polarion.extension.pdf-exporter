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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}

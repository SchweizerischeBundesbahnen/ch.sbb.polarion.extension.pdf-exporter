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
        assertEquals("""
                <div>some content1</div>
                [span class=sticky-note]
                    [span class=sticky-note-time]2025-03-13T16:21:00.000+01:00[/span]
                    [span class=sticky-note-username]author1[/span]
                    [span class=sticky-note-text]text1[/span]
                [span class=sticky-note]
                    [span class=sticky-note-time]2025-03-13T16:21:00.000+01:00[/span]
                    [span class=sticky-note-username]author1[/span]
                    [span class=sticky-note-text]Reply to text1[/span]
                [/span][/span]
                <div>some content2</div>

                <div>some content3</div>
                [span class=sticky-note]
                    [span class=sticky-note-time]2025-03-13T16:23:00.000+01:00[/span]
                    [span class=sticky-note-username]author3[/span]
                    [span class=sticky-note-text]text3[/span]
                [/span]
                <div>some content4</div>
                """, new LiveDocCommentsProcessor().addLiveDocComments(document, html, CommentsRenderType.OPEN, true));

        when(comments.iterator()).thenReturn(commentBases.iterator());

        // now resolved comment is here
        assertEquals("""
                <div>some content1</div>
                [span class=sticky-note]
                    [span class=sticky-note-time]2025-03-13T16:21:00.000+01:00[/span]
                    [span class=sticky-note-username]author1[/span]
                    [span class=sticky-note-text]text1[/span]
                [span class=sticky-note]
                    [span class=sticky-note-time]2025-03-13T16:21:00.000+01:00[/span]
                    [span class=sticky-note-username]author1[/span]
                    [span class=sticky-note-text]Reply to text1[/span]
                [/span][/span]
                <div>some content2</div>
                [span class=sticky-note]
                    [span class=sticky-note-time]2025-03-13T16:22:00.000+01:00[/span]
                    [span class=sticky-note-username]author2[/span]
                    [span class=sticky-note-text]text2[/span]
                [span class=sticky-note]
                    [span class=sticky-note-time]2025-03-13T16:22:00.000+01:00[/span]
                    [span class=sticky-note-username]author2[/span]
                    [span class=sticky-note-text]Reply to text2[/span]
                [/span][/span]
                <div>some content3</div>
                [span class=sticky-note]
                    [span class=sticky-note-time]2025-03-13T16:23:00.000+01:00[/span]
                    [span class=sticky-note-username]author3[/span]
                    [span class=sticky-note-text]text3[/span]
                [/span]
                <div>some content4</div>
                """, new LiveDocCommentsProcessor().addLiveDocComments(document, html, CommentsRenderType.ALL, true));

        // null commentsRenderType means remove comments
        assertEquals("""
                <div>some content1</div>

                <div>some content2</div>

                <div>some content3</div>

                <div>some content4</div>
                """, new LiveDocCommentsProcessor().addLiveDocComments(document, html, null, true));
    }

    @SneakyThrows
    private CommentBase mockComment(String id, String text, String author, boolean resolved, boolean rootComment, boolean addChildComment) {
        CommentBase comment = mock(CommentBase.class, RETURNS_DEEP_STUBS);
        lenient().when(comment.fields().parentComment().get()).thenReturn(rootComment ? null : mock(CommentBase.class));
        lenient().when(comment.fields().id().get()).thenReturn(id);
        lenient().when(comment.fields().created().get()).thenReturn(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2025-03-13 16:2%s".formatted(id)));
        lenient().when(comment.fields().text().persistedHtml()).thenReturn(text);
        lenient().when(Objects.requireNonNull(comment.fields().author().get()).fields().name().get()).thenReturn(author);
        if (addChildComment) {
            CommentBasesField<CommentBase> childComments = mockChildComment(id, text, author, resolved);
            lenient().when(comment.fields().childComments()).thenReturn(childComments);
        }
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
}

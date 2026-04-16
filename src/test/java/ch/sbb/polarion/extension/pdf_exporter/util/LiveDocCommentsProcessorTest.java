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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
                <img id="polarion-comment:4" class="polarion-dle-comment-resolved-icon"/>
                <div>some content5</div>
                """;

        List<CommentBase> commentBases = List.of(
                mockComment("1", "text1", "author1", false, true, true),
                mockComment("2", "text2", "author2", true, true, true),
                mockComment("3", "text3", "author3", false, true, false),
                mockComment("4", "text4", "author4", true, true, false)
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

                <div>some content5</div>
                """, new LiveDocCommentsProcessor().addLiveDocComments(document, html, CommentsRenderType.OPEN, false, new HashSet<>()));

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
                [span class=comment level-0][span class=meta][span class=date]2025-03-13 16:24[/span][span class=details][span class=status-resolved]Resolved[/span][span class=author]author4[/span][/span][/span][span class=text]text4[/span][/span]
                <div>some content5</div>
                """, new LiveDocCommentsProcessor().addLiveDocComments(document, html, CommentsRenderType.ALL, false, new HashSet<>()));

        // null commentsRenderType means remove comments
        assertEquals("""
                <div>some content1</div>

                <div>some content2</div>

                <div>some content3</div>

                <div>some content4</div>

                <div>some content5</div>
                """, new LiveDocCommentsProcessor().addLiveDocComments(document, html, null, false, new HashSet<>()));
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
                <img id="polarion-comment:4" class="polarion-dle-comment-resolved-icon"/>
                <div>some content5</div>
                """;

        List<CommentBase> commentBases = List.of(
                mockComment("1", "text1", "author1", false, true, true),
                mockComment("2", "text2", "author2", true, true, true),
                mockComment("3", "text3", "author3", false, true, false),
                mockComment("4", "text4", "author4", true, true, false)
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

                <div>some content5</div>
                """, liveDocCommentsProcessor.addLiveDocComments(document, html, CommentsRenderType.OPEN, true, new HashSet<>()));

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
                [span class=sticky-note][span class=sticky-note-time]2025-03-13T16:24:00.000[/span][span class=sticky-note-username]author4[/span][span class=sticky-note-text]text4[/span][/span]
                <div>some content5</div>
                """, liveDocCommentsProcessor.addLiveDocComments(document, html, CommentsRenderType.ALL, true, new HashSet<>()));

        // null commentsRenderType means remove comments
        assertEquals("""
                <div>some content1</div>

                <div>some content2</div>

                <div>some content3</div>

                <div>some content4</div>

                <div>some content5</div>
                """, liveDocCommentsProcessor.addLiveDocComments(document, html, null, true, new HashSet<>()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void addLiveDocCommentsPopulatesUsedCommentIds() {
        ProxyDocument document = mock(ProxyDocument.class, RETURNS_DEEP_STUBS);

        UpdatableDocumentFields fields = mock(UpdatableDocumentFields.class);
        CommentBasesTreeField<CommentBase> comments = mock(CommentBasesTreeField.class);
        doCallRealMethod().when(comments).forEach(any(Consumer.class));
        when(fields.comments()).thenReturn(comments);
        when(document.fields()).thenReturn(fields);

        String html = """
                <div>content</div>
                <span id="polarion-comment:1"></span>
                <div>more</div>
                <img id="polarion-comment:3" class="polarion-dle-comment-icon"/>
                """;

        List<CommentBase> commentBases = List.of(
                mockComment("1", "text1", "author1", false, true, false),
                mockComment("3", "text3", "author3", false, true, false)
        );
        when(comments.iterator()).thenReturn(commentBases.iterator());

        Set<String> usedCommentIds = new HashSet<>();
        new LiveDocCommentsProcessor().addLiveDocComments(document, html, CommentsRenderType.OPEN, false, usedCommentIds);

        assertEquals(Set.of("1", "3"), usedCommentIds);
    }

    @Test
    @SuppressWarnings("unchecked")
    void addUnreferencedCommentsAppendsWhenUnreferencedExist() {
        ProxyDocument document = mock(ProxyDocument.class, RETURNS_DEEP_STUBS);

        UpdatableDocumentFields fields = mock(UpdatableDocumentFields.class);
        CommentBasesTreeField<CommentBase> comments = mock(CommentBasesTreeField.class);
        doCallRealMethod().when(comments).forEach(any(Consumer.class));
        when(fields.comments()).thenReturn(comments);
        when(document.fields()).thenReturn(fields);

        // Document has comments 1, 2, 3 but only 1 is referenced
        List<CommentBase> commentBases = List.of(
                mockComment("1", "text1", "author1", false, true, false),
                mockComment("2", "text2", "author2", false, true, false),
                mockComment("3", "text3", "author3", false, true, false)
        );
        when(comments.iterator()).thenReturn(commentBases.iterator());

        Set<String> usedCommentIds = new HashSet<>(Set.of("1"));
        String html = "<div>content</div>";

        String result = new LiveDocCommentsProcessor().addUnreferencedComments(document, html, CommentsRenderType.OPEN, false, usedCommentIds);

        assertTrue(result.startsWith("<div>content</div>"));
        assertTrue(result.contains("[span class=unreferenced-comments-delimiter][/span]"));
        assertTrue(result.contains("text2"));
        assertTrue(result.contains("text3"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void addUnreferencedCommentsNativeRendering() {
        ProxyDocument document = mock(ProxyDocument.class, RETURNS_DEEP_STUBS);

        UpdatableDocumentFields fields = mock(UpdatableDocumentFields.class);
        CommentBasesTreeField<CommentBase> comments = mock(CommentBasesTreeField.class);
        doCallRealMethod().when(comments).forEach(any(Consumer.class));
        when(fields.comments()).thenReturn(comments);
        when(document.fields()).thenReturn(fields);

        List<CommentBase> commentBases = List.of(
                mockComment("1", "text1", "author1", false, true, false),
                mockComment("2", "text2", "author2", false, true, false)
        );
        when(comments.iterator()).thenReturn(commentBases.iterator());

        Set<String> usedCommentIds = new HashSet<>(Set.of("1"));
        String html = "<div>content</div>";

        LiveDocCommentsProcessor processor = spy(new LiveDocCommentsProcessor());
        doAnswer(invocationOnMock -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(invocationOnMock.getArguments()[0]))
                .when(processor).formatIsoDate(any());

        String result = processor.addUnreferencedComments(document, html, CommentsRenderType.OPEN, true, usedCommentIds);

        assertTrue(result.contains("[span class=unreferenced-comments-delimiter][/span]"));
        assertTrue(result.contains("[span class=sticky-note]"));
        assertTrue(result.contains("text2"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void addUnreferencedCommentsReturnsOriginalWhenAllReferenced() {
        ProxyDocument document = mock(ProxyDocument.class, RETURNS_DEEP_STUBS);

        UpdatableDocumentFields fields = mock(UpdatableDocumentFields.class);
        CommentBasesTreeField<CommentBase> comments = mock(CommentBasesTreeField.class);
        doCallRealMethod().when(comments).forEach(any(Consumer.class));
        when(fields.comments()).thenReturn(comments);
        when(document.fields()).thenReturn(fields);

        List<CommentBase> commentBases = List.of(
                mockComment("1", "text1", "author1", false, true, false)
        );
        when(comments.iterator()).thenReturn(commentBases.iterator());

        Set<String> usedCommentIds = new HashSet<>(Set.of("1"));
        String html = "<div>content</div>";

        String result = new LiveDocCommentsProcessor().addUnreferencedComments(document, html, CommentsRenderType.OPEN, false, usedCommentIds);

        assertEquals(html, result);
    }

    @Test
    void addUnreferencedCommentsReturnsOriginalWhenNullRenderType() {
        ProxyDocument document = mock(ProxyDocument.class, RETURNS_DEEP_STUBS);

        Set<String> usedCommentIds = new HashSet<>();
        String html = "<div>content</div>";

        String result = new LiveDocCommentsProcessor().addUnreferencedComments(document, html, null, false, usedCommentIds);

        assertEquals(html, result);
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
}

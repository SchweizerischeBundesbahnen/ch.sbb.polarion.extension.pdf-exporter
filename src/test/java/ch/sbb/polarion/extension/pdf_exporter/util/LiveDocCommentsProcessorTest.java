package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.CommentsRenderType;
import com.polarion.alm.server.api.model.document.ProxyDocument;
import com.polarion.alm.shared.api.model.comment.CommentBase;
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
                mockComment("1", "text1", "author1", false),
                mockComment("2", "text2", "author2", true),
                mockComment("3", "text3", "author3", false)
        );

        when(comments.iterator()).thenReturn(commentBases.iterator());

        // resolved comment isn't rendered
        assertEquals("""
                <div>some content1</div>
                [span class=comment level-0][span class=meta][span class=date]2025-03-13 16:21[/span][span class=details][span class=author]author1[/span][/span][/span][span class=text]text1[/span][/span]
                <div>some content2</div>

                <div>some content3</div>
                [span class=comment level-0][span class=meta][span class=date]2025-03-13 16:23[/span][span class=details][span class=author]author3[/span][/span][/span][span class=text]text3[/span][/span]
                <div>some content4</div>
                """, new LiveDocCommentsProcessor().addLiveDocComments(document, html, CommentsRenderType.OPEN));

        when(comments.iterator()).thenReturn(commentBases.iterator());

        // now resolved comment is here
        assertEquals("""
                <div>some content1</div>
                [span class=comment level-0][span class=meta][span class=date]2025-03-13 16:21[/span][span class=details][span class=author]author1[/span][/span][/span][span class=text]text1[/span][/span]
                <div>some content2</div>
                [span class=comment level-0][span class=meta][span class=date]2025-03-13 16:22[/span][span class=details][span class=status-resolved]Resolved[/span][span class=author]author2[/span][/span][/span][span class=text]text2[/span][/span]
                <div>some content3</div>
                [span class=comment level-0][span class=meta][span class=date]2025-03-13 16:23[/span][span class=details][span class=author]author3[/span][/span][/span][span class=text]text3[/span][/span]
                <div>some content4</div>
                """, new LiveDocCommentsProcessor().addLiveDocComments(document, html, CommentsRenderType.ALL));

        // null commentsRenderType means remove comments
        assertEquals("""
                <div>some content1</div>

                <div>some content2</div>

                <div>some content3</div>

                <div>some content4</div>
                """, new LiveDocCommentsProcessor().addLiveDocComments(document, html, null));
    }

    @SneakyThrows
    private CommentBase mockComment(String id, String text, String author, boolean resolved) {
        CommentBase comment = mock(CommentBase.class, RETURNS_DEEP_STUBS);
        lenient().when(comment.fields().parentComment().get()).thenReturn(null);
        lenient().when(comment.fields().id().get()).thenReturn(id);
        lenient().when(comment.fields().created().get()).thenReturn(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2025-03-13 16:2%s".formatted(id)));
        lenient().when(comment.fields().text().persistedHtml()).thenReturn(text);
        lenient().when(Objects.requireNonNull(comment.fields().author().get()).fields().name().get()).thenReturn(author);
        when(comment.fields().resolved().get()).thenReturn(resolved);
        return comment;
    }
}

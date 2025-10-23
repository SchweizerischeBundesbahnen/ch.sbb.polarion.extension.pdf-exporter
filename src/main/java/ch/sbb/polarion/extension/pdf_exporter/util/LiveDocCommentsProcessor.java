package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import ch.sbb.polarion.extension.pdf_exporter.model.LiveDocComment;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.CommentsRenderType;
import com.polarion.alm.server.api.model.document.ProxyDocument;
import com.polarion.alm.shared.api.model.comment.CommentBase;
import com.polarion.alm.shared.api.model.comment.CommentBaseFields;
import com.polarion.alm.shared.api.model.comment.CommentBasesTreeField;
import com.polarion.alm.shared.api.model.document.UpdatableDocumentFields;
import com.polarion.alm.shared.api.model.fields.BooleanField;
import com.polarion.alm.shared.api.model.fields.DateField;
import com.polarion.alm.shared.api.model.fields.IdField;
import com.polarion.alm.shared.api.model.fields.RichTextField;
import com.polarion.alm.shared.api.model.fields.StringField;
import com.polarion.alm.shared.api.model.project.ProjectField;
import com.polarion.alm.shared.api.model.user.User;
import com.polarion.alm.shared.api.model.user.UserField;
import com.polarion.core.util.StringUtils;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("java:S1200")
public class LiveDocCommentsProcessor {

    private Map<String, LiveDocComment> getCommentsFromDocument(ProxyDocument document, boolean onlyOpen) {
        final UpdatableDocumentFields fields = document.fields();
        final CommentBasesTreeField<CommentBase> comments = fields.comments();
        Map<String, LiveDocComment> liveDocCommentMap = new HashMap<>();
        comments.forEach(commentBase -> {
            LiveDocComment liveDocComment = getCommentFromCommentBase(commentBase);
            if ((!onlyOpen || !liveDocComment.getResolved().get()) && commentBase.fields().parentComment().get() == null) {
                liveDocCommentMap.put(liveDocComment.getId().get(), liveDocComment);
            }
        });
        return liveDocCommentMap;
    }

    private void setChildComments(LiveDocComment comment, CommentBaseFields commentBaseFields) {
        if (!commentBaseFields.childComments().isEmpty()) {
            final Iterator<CommentBase> iterator = commentBaseFields.childComments().iterator();
            Map<String, LiveDocComment> map = new HashMap<>();
            while (iterator.hasNext()) {
                CommentBase next = iterator.next();
                LiveDocComment liveDocComment = getCommentFromCommentBase(next);
                map.put(liveDocComment.getId().get(), liveDocComment);
            }
            comment.setChildComments(map);
        }
    }

    private LiveDocComment getCommentFromCommentBase(CommentBase commentBase) {
        CommentBaseFields commentBaseFields = commentBase.fields();
        IdField id = commentBaseFields.id();
        StringField title = commentBaseFields.title();
        RichTextField text = commentBaseFields.text();
        ProjectField project = commentBaseFields.project();
        UserField author = commentBaseFields.author();
        DateField created = commentBaseFields.created();
        BooleanField resolved = commentBaseFields.resolved();
        final LiveDocComment build = LiveDocComment.builder()
                .title(title)
                .id(id)
                .text(text)
                .project(project)
                .author(author)
                .created(created)
                .resolved(resolved)
                .build();
        setChildComments(build, commentBaseFields);
        return build;
    }

    @NotNull
    public String addLiveDocComments(ProxyDocument document, @NotNull String html, @Nullable CommentsRenderType commentsRenderType, boolean renderNativeComments) {
        //Polarion document keeps comments position in span/img marked with id 'polarion-comment:commentId'.
        //<span id="polarion-comment:1"></span> or <img id="polarion-comment:1" class="polarion-dle-comment-icon"/> (for comments inside workitem description)
        //Following expression retrieves such entries.
        final Map<String, LiveDocComment> liveDocComments = commentsRenderType == null ? Map.of() : getCommentsFromDocument(document, commentsRenderType.equals(CommentsRenderType.OPEN));
        return RegexMatcher.get("(?s)((<img id=\"polarion-comment:(?<imgCommentId>\\d+)\"[^>]*class=\"polarion-dle-comment-icon\"/>)|(<span id=\"polarion-comment:(?<spanCommentId>\\d+)\"></span>))")
                .replace(html, regexEngine -> {
                    String commentId = Optional.ofNullable(regexEngine.group("imgCommentId")).orElse(regexEngine.group("spanCommentId"));
                    return renderComments(liveDocComments.get(commentId), renderNativeComments);
                });
    }

    private String renderComments(LiveDocComment liveDocComment, boolean renderNativeComments) {
        if (liveDocComment == null) {
            return "";
        }
        return renderNativeComments ? renderNativeComment(liveDocComment) : renderInlineComment(liveDocComment, 0);
    }

    private String renderInlineComment(LiveDocComment liveDocComment, int nestingLevel) {
        CommentData commentData = getCommentData(liveDocComment);
        StringBuilder commentSpan = new StringBuilder(getCommentSpan(commentData, nestingLevel));
        Map<String, LiveDocComment> childComments = liveDocComment.getChildComments();
        if (childComments != null) {
            nestingLevel++;
            for (LiveDocComment childComment : childComments.values()) {
                commentSpan.append(renderInlineComment(childComment, nestingLevel));
            }
        }
        return commentSpan.toString();
    }

    private String renderNativeComment(LiveDocComment liveDocComment) {
        CommentData commentData = getCommentData(liveDocComment);
        StringBuilder commentDiv = new StringBuilder("""
                [span class=sticky-note]
                    [span class=sticky-note-time]%s[/span]
                    [span class=sticky-note-username]%s[/span]
                    [span class=sticky-note-text]%s[/span]
                """.formatted(commentData.isoDate, StringUtils.getEmptyIfNull(commentData.author), commentData.text));
        Map<String, LiveDocComment> childComments = liveDocComment.getChildComments();
        if (childComments != null) {
            for (LiveDocComment childComment : childComments.values()) {
                commentDiv.append(renderNativeComment(childComment));
            }
        }
        commentDiv.append("[/span]");
        return commentDiv.toString();
    }

    private String getCommentSpan(CommentData commentData, int nestingLevel) {
        String dateSpan = String.format("[span class=date]%s[/span]", commentData.getDate());
        String statusSpan = commentData.isResolved() ? "[span class=status-resolved]Resolved[/span]" : "";
        String authorSpan = commentData.getAuthor() != null ? String.format("[span class=author]%s[/span]", commentData.getAuthor()) : "";
        String textSpan = String.format("[span class=text]%s[/span]", commentData.getText());
        return String.format("[span class=comment level-%d][span class=meta]%s[span class=details]%s%s[/span][/span]%s[/span]", nestingLevel, dateSpan, statusSpan, authorSpan, textSpan);
    }

    private CommentData getCommentData(LiveDocComment liveDocComment) {
        User user = liveDocComment.getAuthor().get();
        String authorName = user != null ? user.fields().name().get() : null;
        String commentText = liveDocComment.getText().persistedHtml();
        boolean resolved = liveDocComment.getResolved().get();
        return CommentData.builder()
                .date(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(liveDocComment.getCreated().get()))
                .isoDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(liveDocComment.getCreated().get()))
                .author(authorName)
                .text(commentText)
                .resolved(resolved)
                .build();
    }

    @Data
    @Builder
    private static class CommentData {
        private String date;
        private String isoDate;
        private String author;
        private String text;
        private boolean resolved;
    }

}

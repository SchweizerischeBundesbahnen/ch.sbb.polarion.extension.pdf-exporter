package ch.sbb.polarion.extension.pdf.exporter.util;

import ch.sbb.polarion.extension.pdf.exporter.model.LiveDocComment;
import ch.sbb.polarion.extension.pdf.exporter.util.regex.RegexMatcher;
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
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("java:S1200")
public class LiveDocCommentsProcessor {

    private Map<String, LiveDocComment> getCommentsFromDocument(ProxyDocument document) {
        final UpdatableDocumentFields fields = document.fields();
        final CommentBasesTreeField<CommentBase> comments = fields.comments();
        Map<String, LiveDocComment> liveDocCommentMap = new HashMap<>();
        comments.forEach(commentBase -> {
            LiveDocComment liveDocComment = getCommentFromCommentBase(commentBase);
            if (commentBase.fields().parentComment().get() == null) {
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
    public String addLiveDocComments(ProxyDocument document, @NotNull String html) {
        //Polarion document keeps comments position in spans marked with id 'polarion-comment:commentId'.
        //<span id="polarion-comment:1"></span>"
        //Following expression retrieves such spans.
        final Map<String, LiveDocComment> liveDocComments = getCommentsFromDocument(document);
        return RegexMatcher.get("(?s)(<span id=\"polarion-comment:(?<commentId>\\d+)\"></span>)").replace(html, regexEngine -> {
            String commentId = regexEngine.group("commentId");
            LiveDocComment liveDocComment = liveDocComments.get(commentId);
            return liveDocComment == null ? null : renderComment(liveDocComment, 0);
        });
    }

    private String renderComment(LiveDocComment liveDocComment, int nestingLevel) {
        CommentData commentData = getCommentData(liveDocComment);
        StringBuilder commentSpan = new StringBuilder(getCommentSpan(commentData, nestingLevel));
        Map<String, LiveDocComment> childComments = liveDocComment.getChildComments();
        if (childComments != null) {
            nestingLevel++;
            for (LiveDocComment childComment : childComments.values()) {
                commentSpan.append(renderComment(childComment, nestingLevel));
            }
        }
        return commentSpan.toString();
    }

    private String getCommentSpan(CommentData commentData, int nestingLevel) {
        String dateSpan = String.format("[span class=date]%s[/span]", commentData.getDate());
        String authorSpan = commentData.getAuthor() != null ? String.format("[span class=author]%s[/span]", commentData.getAuthor()) : "";
        String textSpan = String.format("[span class=text]%s[/span]", commentData.getText());
        return String.format("[span class=comment level-%d][span class=meta]%s%s[/span]%s[/span]", nestingLevel, dateSpan, authorSpan, textSpan);
    }

    private CommentData getCommentData(LiveDocComment liveDocComment) {
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(liveDocComment.getCreated().get());
        User user = liveDocComment.getAuthor().get();
        String authorName = user != null ? user.fields().name().get() : null;
        String commentText = liveDocComment.getText().persistedHtml();
        return CommentData.builder()
                .date(date)
                .author(authorName)
                .text(commentText)
                .build();
    }

    @Data
    @Builder
    private static class CommentData {
        private String date;
        private String author;
        private String text;
    }

}

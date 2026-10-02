package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.model.LiveDocComment;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.CommentsRenderType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.util.LiveDocCommentsProcessor;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import com.polarion.alm.server.api.model.document.ProxyDocument;
import com.polarion.alm.shared.api.model.comment.CommentBase;
import com.polarion.alm.shared.api.model.comment.CommentBasesField;
import com.polarion.alm.shared.api.model.comment.CommentBasesTreeField;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A comment of the document placed in the description of a work item which the document references from another one is
 * exported where it stands, as one in the text of the document or in the description of a work item of its own.
 * <p>
 * The document is given as the renderer gives it: Polarion renders the icon of a comment in the description of a
 * referenced work item, class {@code polarion-dle-workitem-basic-external}, as of any other work item once the export lets
 * it. Each comment is placed at its icon, and none is left for the comments the document does not place.
 * </p>
 */
class CommentsOfAReferencedWorkItemTest extends BasePdfConverterTest {

    /** The icon Polarion renders for a comment in the description of a work item. */
    private static final String ICON = "<img id=\"polarion-comment:%s\" title=\"%s\" contenteditable=\"false\" src=\"/polarion/ria/images/control/comment.png\" class=\"polarion-dle-comment-icon\"/>";

    private static final String RENDERED_DOCUMENT = """
            <h1>Comments of a referenced work item</h1>
            <p>Text of the document.<span id="polarion-comment:1"></span></p>
            <div id="polarion_wiki macro name=module-workitem;params=id=EL-2" class="polarion-dle-workitem-basic-0 polarion-dle-workitem-basic-internal" title="Requirement: EL-2">
            <span class="polarion-dle-workitem-title">EL-2 - A work item of the document</span>
            <p>Description of the work item of the document.%s</p>
            </div>
            <div id="polarion_wiki macro name=module-workitem;params=id=EL-3|external=true" class="polarion-dle-workitem-basic-0 polarion-dle-workitem-basic-external" title="Requirement: EL-3">
            <span class="polarion-dle-workitem-title">EL-3 - A work item the document references</span>
            <p>Description of the referenced work item.%s</p>
            </div>
            <p>Text after the work items.</p>
            """.formatted(ICON.formatted("2", "Comment in the work item"), ICON.formatted("3", "Review comment inside the referenced WI"));

    @Test
    void placesTheCommentOfAReferencedWorkItemWhereItStands() {
        LiveDocCommentsProcessor processor = new LiveDocCommentsProcessor();
        Map<String, LiveDocComment> comments = processor.getLiveDocComments(documentWith(
                comment("1", "Comment on the document"),
                comment("2", "Comment in the work item"),
                comment("3", "Review comment inside the referenced WI")), CommentsRenderType.ALL);
        Set<String> rendered = new LinkedHashSet<>();
        String content = processor.addLiveDocComments(RENDERED_DOCUMENT, comments, false, rendered);
        String withUnreferenced = processor.addUnreferencedComments(content, comments, false, rendered);

        assertThat(withUnreferenced).as("Every comment is placed where it stands, and none is left for the end of the document").isEqualTo(content);

        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .renderComments(CommentsRenderType.ALL)
                .includeUnreferencedComments(true)
                .build();
        byte[] pdf = exportLiveDoc("Comments of a referenced work item", withUnreferenced, params);
        boolean differ = compareContentUsingReferenceImages(getCurrentMethodName(), pdf);

        String text = textOf(pdf);
        assertThat(text).as("Each comment follows the text it is placed in")
                .containsSubsequence("Text of the document.", "Comment on the document",
                        "Description of the work item of the document.", "Comment in the work item",
                        "Description of the referenced work item.", "Review comment inside the referenced WI",
                        "Text after the work items.");
        assertFalse(differ, "The pages differ from the reference images");
    }

    @SuppressWarnings("unchecked")
    private static @NotNull ProxyDocument documentWith(@NotNull CommentBase... comments) {
        ProxyDocument document = mock(ProxyDocument.class, RETURNS_DEEP_STUBS);
        CommentBasesTreeField<CommentBase> field = mock(CommentBasesTreeField.class);
        when(document.fields().comments()).thenReturn(field);
        lenient().when(field.iterator()).thenAnswer(invocation -> List.of(comments).iterator());
        org.mockito.Mockito.doAnswer(invocation -> {
            java.util.function.Consumer<CommentBase> action = invocation.getArgument(0);
            List.of(comments).forEach(action);
            return null;
        }).when(field).forEach(org.mockito.ArgumentMatchers.any());
        return document;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private static @NotNull CommentBase comment(@NotNull String id, @NotNull String text) {
        CommentBase comment = mock(CommentBase.class, RETURNS_DEEP_STUBS);
        lenient().when(comment.fields().parentComment().get()).thenReturn(null);
        lenient().when(comment.fields().id().get()).thenReturn(id);
        lenient().when(comment.fields().created().get()).thenReturn(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2026-10-02 10:1" + id));
        lenient().when(comment.fields().text().persistedHtml()).thenReturn(text);
        lenient().when(Objects.requireNonNull(comment.fields().author().get()).fields().name().get()).thenReturn("Test User " + id);
        lenient().when(comment.fields().resolved().get()).thenReturn(false);
        CommentBasesField<CommentBase> noReplies = mock(CommentBasesField.class);
        lenient().when(noReplies.iterator()).thenAnswer(invocation -> Collections.emptyIterator());
        lenient().when(noReplies.isEmpty()).thenReturn(true);
        lenient().when(comment.fields().childComments()).thenReturn(noReplies);
        return comment;
    }

    @SneakyThrows
    private static @NotNull String textOf(byte @NotNull [] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            // In the order a reader sees it on the page, which a comment drawn as a block of its own is not in the stream
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document).replaceAll("\\s+", " ");
        }
    }
}

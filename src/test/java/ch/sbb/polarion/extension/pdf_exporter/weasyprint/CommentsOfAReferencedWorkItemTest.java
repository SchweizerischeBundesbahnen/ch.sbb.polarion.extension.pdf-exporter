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
 * exported where it stands, as one in the text of the document or in the description of a work item of its own. The
 * document is the one of the issue (#1118).
 * <p>
 * The document is given as the renderer gives it: Polarion renders the icon of a comment in the description of a
 * referenced work item, class {@code polarion-dle-workitem-basic-external}, as of any other work item once the export lets
 * it. Each comment is placed at its icon, and none is left for the comments the document does not place.
 * </p>
 */
class CommentsOfAReferencedWorkItemTest extends BasePdfConverterTest {

    /** The icon Polarion renders for a comment in the description of a work item. */
    private static final String ICON = "<img id=\"polarion-comment:%s\" title=\"%s\" contenteditable=\"false\" src=\"/polarion/ria/images/control/comment.png\" class=\"polarion-dle-comment-icon\"/>";

    /** The attribute table Polarion renders at the end of a work item, with its status and its type. */
    private static final String ATTRIBUTES = """
            <table class="polarion-dle-workitem-fields-end-table"><tbody>
            <tr><td class="polarion-dle-workitem-fields-end-table-label">Status</td><td class="polarion-dle-workitem-fields-end-table-value">%s</td></tr>
            <tr><td class="polarion-dle-workitem-fields-end-table-label">Type</td><td class="polarion-dle-workitem-fields-end-table-value">%s</td></tr>
            </tbody></table>""";

    /**
     * The document of the issue as the renderer gives it: a work item with a comment in its description, a comment in a
     * line of text, the referenced work item with a comment in its description, and a work item after it.
     */
    private static final String RENDERED_DOCUMENT = """
            <div id="polarion_wiki macro name=module-workitem;params=id=CHUD-30743" class="polarion-dle-workitem-basic-0 polarion-dle-workitem-basic-internal" title="Design Statement: CHUD-30743">
            <span class="polarion-dle-workitem-title">CHUD-30743 - Statement for the Another Req</span>
            <p>This is the Solution of the Another Req%s</p>%s
            </div>
            <p>A<span id="polarion-comment:2"></span> text</p>
            <div id="polarion_wiki macro name=module-workitem;params=id=CHUD-30741|external=true" class="polarion-dle-workitem-basic-0 polarion-dle-workitem-basic-external" title="Requirement: CHUD-30741">
            <span class="polarion-dle-workitem-title">CHUD-30741 - The refinement</span>
            <p>Refinement of the URS requirement%s</p>%s
            </div>
            <div id="polarion_wiki macro name=module-workitem;params=id=CHUD-30742" class="polarion-dle-workitem-basic-0 polarion-dle-workitem-basic-internal" title="Design Statement: CHUD-30742">
            <span class="polarion-dle-workitem-title">CHUD-30742 - Statement for the refinement</span>
            <p>Here is the solution for the Refinement</p>%s
            </div>
            """.formatted(
            ICON.formatted("1", "Review comment inside the WI"), ATTRIBUTES.formatted("Draft", "Design Statement"),
            ICON.formatted("3", "Review comment inside the referenced WI"), ATTRIBUTES.formatted("Ready For Review", "Requirement"),
            ATTRIBUTES.formatted("Draft", "Design Statement"));

    /** The comment of a work item which was deleted: the document places it nowhere, so it comes at the end. */
    private static final String UNREFERENCED = "This is a comment for the deleted WI. So it should be shown at the end of the document as unreferenced.";

    @Test
    void placesTheCommentOfAReferencedWorkItemWhereItStands() {
        LiveDocCommentsProcessor processor = new LiveDocCommentsProcessor();
        Map<String, LiveDocComment> comments = processor.getLiveDocComments(documentWith(
                comment("1", "Review comment inside the WI"),
                comment("2", "Review comment outside of WI"),
                comment("3", "Review comment inside the referenced WI"),
                comment("4", UNREFERENCED)), CommentsRenderType.ALL);
        Set<String> rendered = new LinkedHashSet<>();
        String content = processor.addLiveDocComments(RENDERED_DOCUMENT, comments, false, rendered);
        String withUnreferenced = processor.addUnreferencedComments(content, comments, false, rendered);

        assertThat(rendered).as("Every comment the document places is placed where it stands").containsExactly("1", "2", "3");

        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .renderComments(CommentsRenderType.ALL)
                .includeUnreferencedComments(true)
                .build();
        byte[] pdf = exportLiveDoc("Comments in a referenced WI", withUnreferenced, params);
        boolean differ = compareContentUsingReferenceImages(getCurrentMethodName(), pdf);

        assertThat(textOf(pdf)).as("Each comment follows the text it is placed in, and the unreferenced one comes last")
                .containsSubsequence("This is the Solution of the Another Req", "Review comment inside the WI",
                        "Review comment outside of WI", "text",
                        "Refinement of the URS requirement", "Review comment inside the referenced WI",
                        "Here is the solution for the Refinement",
                        "This is a comment for the deleted WI.");
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

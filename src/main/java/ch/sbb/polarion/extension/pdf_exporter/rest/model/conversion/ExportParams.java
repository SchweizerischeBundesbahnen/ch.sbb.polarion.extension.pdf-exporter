package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExportParams extends ConversionParams {
    public static final String URL_QUERY_PARAM_QUERY = "query";
    public static final String URL_QUERY_PARAM_LANGUAGE = "language";

    @Schema(description = "The unique identifier for the project", example = "elibrary")
    private @Nullable String projectId;

    @Schema(description = "Document path for export", example = "Specification/Product Specification")
    private @Nullable String locationPath;

    @Schema(description = "The specific revision of the provided baseline")
    private @Nullable String baselineRevision;

    @Schema(description = "The specific revision of the document")
    private @Nullable String revision;

    @Schema(description = "The type of document", example = "LIVE_DOC")
    private DocumentType documentType;

    @Schema(description = "Cover page settings name")
    private String coverPage;

    @Schema(description = "CSS settings name")
    private String css;

    @Schema(description = "Header/Footer settings name")
    private String headerFooter;

    @Schema(description = "Localization settings name")
    private String localization;

    @Schema(description = "Webhooks settings name")
    private String webhooks;

    @Schema(description = "Color to be used for headers in the document. By default dark blue color (Polarion's default)")
    private String headersColor;

    @Schema(description = "Which comments should be rendered in the exported document")
    private CommentsRenderType renderComments;

    @Schema(description = "Render native comments")
    private boolean renderNativeComments;

    @Schema(description = "Watermark content to be applied to the document")
    private boolean watermark;

    @Schema(description = "Referenced work items should be marked in the document")
    private boolean markReferencedWorkitems;

    @Schema(description = "Empty chapters should be removed from the document")
    private boolean cutEmptyChapters;

    @Schema(description = "Empty work item attributes should be removed from the document", defaultValue = "true")
    private boolean cutEmptyWIAttributes = true;

    @Schema(description = "Local Polarion URLs should be removed from the document")
    private boolean cutLocalUrls;

    @Schema(description = "SCC styles to be applied to numbered lists in the document")
    private String numberedListStyles;

    @Schema(description = "Specific higher level chapters")
    private List<String> chapters;

    @Schema(description = "CSV list of document field names or wildcard patterns to be exported as HTML meta (LiveDoc only)")
    private List<String> metadataFields;

    @Schema(description = "Language in the exported document")
    private String language;

    @Schema(description = "Specific Workitem roles", example = "[\"has parent\", \"depends on\"]")
    private List<String> linkedWorkitemRoles;

    @Schema(description = "Map of attributes extracted from the URL")
    private Map<String, String> urlQueryParameters;

    @Schema(description = "Filter for attachments to be downloaded, example: '*.pdf'")
    private String attachmentsFilter;

    @Schema(description = "A boolean testcase field ID. Attachments will be downloaded only from the testcases which have True value in the provided field. Leaving field empty will process all testcases.")
    private String testcaseFieldId;

    @Schema(description = "Internal content")
    private String internalContent;

    @Schema(description = "If attachments should be embedded into PDF")
    private boolean embedAttachments;

    @Schema(description = "Indicator that most parameters from this object should be ignored, best suitable style package should be found and properties from it should be taken instead")
    private boolean autoSelectStylePackage;

    public @NotNull DocumentType getDocumentType() {
        if (documentType == null) {
            documentType = DocumentType.LIVE_DOC;
        }
        return documentType;
    }

    public void overwriteByStylePackage(@NotNull StylePackageModel stylePackageModel) {
        coverPage = stylePackageModel.getCoverPage();
        headerFooter = stylePackageModel.getHeaderFooter();
        css = stylePackageModel.getCss();
        localization = stylePackageModel.getLocalization();
        webhooks = stylePackageModel.getWebhooks();
        headersColor = stylePackageModel.getHeadersColor();
        paperSize = PaperSize.fromString(stylePackageModel.getPaperSize());
        orientation = Orientation.fromString(stylePackageModel.getOrientation());
        setPdfVariant(PdfVariant.fromString(stylePackageModel.getPdfVariant()));
        imageDensity = ImageDensity.fromString(stylePackageModel.getImageDensity());
        fitToPage = stylePackageModel.isFitToPage();
        renderComments = stylePackageModel.getRenderComments();
        renderNativeComments = stylePackageModel.isRenderNativeComments();
        watermark = stylePackageModel.isWatermark();
        markReferencedWorkitems = stylePackageModel.isMarkReferencedWorkitems();
        setCutEmptyWIAttributes(stylePackageModel.isCutEmptyWorkitemAttributes());
        cutLocalUrls = stylePackageModel.isCutLocalURLs();
        cutEmptyChapters = stylePackageModel.isCutEmptyChapters();
        followHTMLPresentationalHints = stylePackageModel.isFollowHTMLPresentationalHints();
        metadataFields = stylePackageModel.getMetadataFields() != null ? Arrays.stream(stylePackageModel.getMetadataFields().split(",")).toList() : null;
        numberedListStyles = stylePackageModel.getCustomNumberedListStyles();
        chapters = stylePackageModel.getSpecificChapters() != null ? Arrays.stream(stylePackageModel.getSpecificChapters().split(",")).toList() : null;
        language = stylePackageModel.getLanguage();
        linkedWorkitemRoles = stylePackageModel.getLinkedWorkitemRoles();
        attachmentsFilter = stylePackageModel.getAttachmentsFilter();
        testcaseFieldId = stylePackageModel.getTestcaseFieldId();
        embedAttachments = stylePackageModel.isEmbedAttachments();
        setFullFonts(stylePackageModel.isFullFonts());
    }
}

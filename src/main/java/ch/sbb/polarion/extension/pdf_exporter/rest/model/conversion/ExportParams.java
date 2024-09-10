package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportParams {
    @Schema(name = "Project ID", example = "elibrary", description = "The unique identifier for the project")
    private String projectId;

    @Schema(name = "Location path", required = true, description = "Document path for export")
    private String locationPath;

    @Schema(description = "The specific revision of the document to export")
    private String revision;

    @Schema(description = "The type of document to export", example = "LIVE_DOC")
    private DocumentType documentType;

    @Schema(description = "Cover page settings name")
    private String coverPage;

    @Schema(description = "CSS settings name")
    private String css;

    @Schema(description = "Header Footer settings name")
    private String headerFooter;

    @Schema(description = "Localization settings name")
    private String localization;

    @Schema(description = "Webhooks settings name")
    private String webhooks;

    @Schema(description = "Color to be used for headers in the document. By default dark blue color (Polarion's default)")
    private String headersColor;

    @Schema(description = "The orientation of the exported document", defaultValue = "PORTRAIT")
    private Orientation orientation = Orientation.PORTRAIT;

    @Schema(description = "The paper size of the exported document", defaultValue = "A4")
    private PaperSize paperSize = PaperSize.A4;

    @Schema(description = "Content should be scaled to fit the page", defaultValue = "true")
    private boolean fitToPage = true;

    @Schema(description = "Comments should be rendered in the exported document", defaultValue = "true")
    private boolean enableCommentsRendering = true;

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

    @Schema(description = "HTML presentational hints should be followed", defaultValue = "true")
    private boolean followHTMLPresentationalHints = true;

    @Schema(description = "SCC styles to be applied to numbered lists in the document")
    private String numberedListStyles;

    @Schema(description = "Specific higher level chapters")
    private List<String> chapters;

    @Schema(description = "Language in the exported document")
    private String language;

    @Schema(description = "Specific Workitem roles", example = "[\"has parent\", \"depends on\"]")
    private List<String> linkedWorkitemRoles;

    @Schema(description = "Map of attributes extracted from the URL")
    private Map<String, String> urlQueryParameters;

    @Schema(description = "Internal content")
    private String internalContent;

    public DocumentType getDocumentType() {
        // Default to DOCUMENT
        if (documentType == null) {
            return DocumentType.LIVE_DOC;
        }
        return documentType;
    }
}

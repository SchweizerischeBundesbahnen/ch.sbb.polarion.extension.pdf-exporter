package ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion;

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
    private String projectId;
    private String locationPath;
    private String revision;
    private DocumentType documentType;
    private String coverPage;
    private String css;
    private String headerFooter;
    private String localization;
    private String headersColor;
    private Orientation orientation = Orientation.PORTRAIT;
    private PaperSize paperSize = PaperSize.A4;
    private boolean fitToPage = true;
    private boolean enableCommentsRendering = true;
    private boolean watermark;
    private boolean markReferencedWorkitems;
    private boolean cutEmptyChapters;
    private boolean cutEmptyWIAttributes = true;
    private boolean cutLocalUrls;
    private boolean followHTMLPresentationalHints = true;
    private String numberedListStyles;
    private List<String> chapters;
    private String language;
    private List<String> linkedWorkitemRoles;
    private Map<String, String> urlQueryParameters;
    private String internalContent; //overrides existing content in doc

    public DocumentType getDocumentType() {
        // Default to DOCUMENT
        if (documentType == null) {
            return DocumentType.DOCUMENT;
        }
        return documentType;
    }
}

package ch.sbb.polarion.extension.pdf_exporter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What the page-injected script needs to decide which toolbar buttons to draw for one project: the
 * configured appearance, plus whether this user may export at all. One answer instead of two requests,
 * because the script asks again on every move to another project.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolbarAppearance {
    private boolean liveDocButton;
    private boolean liveReportButton;
    private boolean liveReportAutoExpandTools;
    private boolean classicWikiButton;
    private boolean classicWikiAutoExpandTools;
    private boolean permitted;
}

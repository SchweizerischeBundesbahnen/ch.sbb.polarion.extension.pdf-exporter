package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.appearance;

import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.polarion.core.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Which "Export to PDF" buttons the extension puts on a Polarion page, per scope.
 *
 * A setting stored before this feature carries none of these entries, and an installation which never
 * opened the page has no setting at all. Both must keep the buttons which were there before, so every
 * missing entry falls back to the built-in default rather than to false.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppearanceModel extends SettingsModel {
    public static final boolean DEFAULT_DISPLAY = true;
    public static final boolean DEFAULT_AUTO_EXPAND_TOOLS = false;

    private static final String LIVE_DOC_BUTTON = "LIVE DOC BUTTON";
    private static final String LIVE_REPORT_BUTTON = "LIVE REPORT BUTTON";
    private static final String LIVE_REPORT_AUTO_EXPAND_TOOLS = "LIVE REPORT AUTO EXPAND TOOLS";
    private static final String CLASSIC_WIKI_BUTTON = "CLASSIC WIKI BUTTON";
    private static final String CLASSIC_WIKI_AUTO_EXPAND_TOOLS = "CLASSIC WIKI AUTO EXPAND TOOLS";

    /** The button in the document editor toolbar. */
    private boolean liveDocButton;
    /** The button in the Live Report toolbar, the one behind "Expand Tools". */
    private boolean liveReportButton;
    /** Keep the Live Report tools toolbar expanded, which Polarion itself collapses on every page open. */
    private boolean liveReportAutoExpandTools;
    /** The button in the Classic Wiki toolbar, the one behind "Expand Tools". */
    private boolean classicWikiButton;
    /** Keep the Classic Wiki tools toolbar expanded. */
    private boolean classicWikiAutoExpandTools;

    @Override
    protected String serializeModelData() {
        return serializeEntry(LIVE_DOC_BUTTON, liveDocButton) +
                serializeEntry(LIVE_REPORT_BUTTON, liveReportButton) +
                serializeEntry(LIVE_REPORT_AUTO_EXPAND_TOOLS, liveReportAutoExpandTools) +
                serializeEntry(CLASSIC_WIKI_BUTTON, classicWikiButton) +
                serializeEntry(CLASSIC_WIKI_AUTO_EXPAND_TOOLS, classicWikiAutoExpandTools);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        liveDocButton = deserializeFlag(LIVE_DOC_BUTTON, serializedString, DEFAULT_DISPLAY);
        liveReportButton = deserializeFlag(LIVE_REPORT_BUTTON, serializedString, DEFAULT_DISPLAY);
        liveReportAutoExpandTools = deserializeFlag(LIVE_REPORT_AUTO_EXPAND_TOOLS, serializedString, DEFAULT_AUTO_EXPAND_TOOLS);
        classicWikiButton = deserializeFlag(CLASSIC_WIKI_BUTTON, serializedString, DEFAULT_DISPLAY);
        classicWikiAutoExpandTools = deserializeFlag(CLASSIC_WIKI_AUTO_EXPAND_TOOLS, serializedString, DEFAULT_AUTO_EXPAND_TOOLS);
    }

    private boolean deserializeFlag(String entryName, String serializedString, boolean defaultValue) {
        String value = deserializeEntry(entryName, serializedString);
        return StringUtils.isEmptyTrimmed(value) ? defaultValue : Boolean.parseBoolean(value);
    }
}

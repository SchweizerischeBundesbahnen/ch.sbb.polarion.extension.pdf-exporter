package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage;

import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StylePackageModel extends SettingsModel {

    public static final Float DEFAULT_WEIGHT = 50f;
    public static final Float DEFAULT_INITIAL_WEIGHT = 0f; // used to initialize the 'Default' settings in the default/repository scope

    private static final String MATCHING_QUERY_ENTRY_NAME = "MATCHING QUERY";
    private static final String WEIGHT_ENTRY_NAME = "WEIGHT";
    private static final String EXPOSE_SETTINGS_ENTRY_NAME = "EXPOSE SETTINGS";
    private static final String COVER_PAGE_ENTRY_NAME = "COVER PAGE";
    private static final String HEADER_FOOTER_ENTRY_NAME = "HEADER FOOTER";
    private static final String CSS_ENTRY_NAME = "CSS";
    private static final String LOCALIZATION_ENTRY_NAME = "LOCALIZATION";
    private static final String WEBHOOKS_ENTRY_NAME = "WEBHOOKS";
    private static final String HEADERS_COLOR_ENTRY_NAME = "HEADERS COLOR";
    private static final String PAPER_SIZE_ENTRY_NAME = "PAPER SIZE";
    private static final String ORIENTATION_ENTRY_NAME = "ORIENTATION";
    private static final String FIT_TO_PAGE_ENTRY_NAME = "FIT TO PAGE";
    private static final String RENDER_COMMENTS_ENTRY_NAME = "RENDER COMMENTS";
    private static final String WATERMARK_ENTRY_NAME = "WATERMARK";
    private static final String MARK_REFERENCED_WORKITEMS_ENTRY_NAME = "MARK REFERENCED WORKITEMS";
    private static final String CUT_EMPTY_CHAPTERS_ENTRY_NAME = "CUT EMPTY CHAPTERS";
    private static final String CUT_EMPTY_WORKITEM_ATTRIBUTES_ENTRY_NAME = "CUT EMPTY WORKITEM ATTRIBUTES";
    private static final String CUT_LOCAL_URLS_ENTRY_NAME= "CUT LOCAL URLS";
    private static final String FOLLOW_HTML_PRESENTATIONAL_HINTS_ENTRY_NAME = "FOLLOW HTML PRESENTATIONAL HINTS";
    private static final String SPECIFIC_CHAPTERS_ENTRY_NAME = "SPECIFIC CHAPTERS";
    private static final String CUSTOM_NUMBERED_LIST_STYLES_ENTRY_NAME = "CUSTOM NUMBERED LIST STYLES";
    private static final String LANGUAGE_ENTRY_NAME = "LANGUAGE";
    private static final String LINKED_WORKITEM_ROLES_ENTRY_NAME = "LINKED WORKITEM ROLES";
    private static final String EXPOSE_PAGE_WIDTH_VALIDATION_ENTRY_NAME = "EXPOSE PAGE WIDTH VALIDATION";

    private String matchingQuery;
    private Float weight;
    private boolean exposeSettings;
    private String coverPage;
    private String headerFooter;
    private String css;
    private String localization;
    private String webhooks;
    private String headersColor;
    private String paperSize;
    private String orientation;
    private boolean fitToPage;
    private boolean renderComments;
    private boolean watermark;
    private boolean markReferencedWorkitems;
    private boolean cutEmptyChapters;
    private boolean cutEmptyWorkitemAttributes;
    private boolean cutLocalURLs;
    private boolean followHTMLPresentationalHints;
    private String specificChapters;
    private String customNumberedListStyles;
    private String language;
    private List<String> linkedWorkitemRoles;
    private boolean exposePageWidthValidation;

    @Override
    protected String serializeModelData() {
        return serializeEntry(MATCHING_QUERY_ENTRY_NAME, matchingQuery) +
                serializeEntry(WEIGHT_ENTRY_NAME, weight) +
                serializeEntry(EXPOSE_SETTINGS_ENTRY_NAME, exposeSettings) +
                serializeEntry(COVER_PAGE_ENTRY_NAME, coverPage) +
                serializeEntry(HEADER_FOOTER_ENTRY_NAME, headerFooter) +
                serializeEntry(CSS_ENTRY_NAME, css) +
                serializeEntry(LOCALIZATION_ENTRY_NAME, localization) +
                serializeEntry(WEBHOOKS_ENTRY_NAME, webhooks) +
                serializeEntry(HEADERS_COLOR_ENTRY_NAME, headersColor) +
                serializeEntry(PAPER_SIZE_ENTRY_NAME, paperSize) +
                serializeEntry(ORIENTATION_ENTRY_NAME, orientation) +
                serializeEntry(FIT_TO_PAGE_ENTRY_NAME, fitToPage) +
                serializeEntry(RENDER_COMMENTS_ENTRY_NAME, renderComments) +
                serializeEntry(WATERMARK_ENTRY_NAME, watermark) +
                serializeEntry(MARK_REFERENCED_WORKITEMS_ENTRY_NAME, markReferencedWorkitems) +
                serializeEntry(CUT_EMPTY_CHAPTERS_ENTRY_NAME, cutEmptyChapters) +
                serializeEntry(CUT_EMPTY_WORKITEM_ATTRIBUTES_ENTRY_NAME, cutEmptyWorkitemAttributes) +
                serializeEntry(CUT_LOCAL_URLS_ENTRY_NAME, cutLocalURLs) +
                serializeEntry(FOLLOW_HTML_PRESENTATIONAL_HINTS_ENTRY_NAME, followHTMLPresentationalHints) +
                serializeEntry(SPECIFIC_CHAPTERS_ENTRY_NAME, specificChapters) +
                serializeEntry(CUSTOM_NUMBERED_LIST_STYLES_ENTRY_NAME, customNumberedListStyles) +
                serializeEntry(LANGUAGE_ENTRY_NAME, language) +
                serializeEntry(LINKED_WORKITEM_ROLES_ENTRY_NAME, linkedWorkitemRoles) +
                serializeEntry(EXPOSE_PAGE_WIDTH_VALIDATION_ENTRY_NAME, exposePageWidthValidation);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        matchingQuery = deserializeEntry(MATCHING_QUERY_ENTRY_NAME, serializedString);
        weight = Optional.ofNullable(deserializeEntry(WEIGHT_ENTRY_NAME, serializedString)).map(Float::parseFloat)
                .orElse(NamedSettings.DEFAULT_NAME.equals(name) ? DEFAULT_INITIAL_WEIGHT : DEFAULT_WEIGHT);
        exposeSettings = Boolean.parseBoolean(deserializeEntry(EXPOSE_SETTINGS_ENTRY_NAME, serializedString));
        coverPage = deserializeEntry(COVER_PAGE_ENTRY_NAME, serializedString);
        headerFooter = deserializeEntry(HEADER_FOOTER_ENTRY_NAME, serializedString);
        css = deserializeEntry(CSS_ENTRY_NAME, serializedString);
        localization = deserializeEntry(LOCALIZATION_ENTRY_NAME, serializedString);
        webhooks = deserializeEntry(WEBHOOKS_ENTRY_NAME, serializedString);
        headersColor = deserializeEntry(HEADERS_COLOR_ENTRY_NAME, serializedString);
        paperSize = deserializeEntry(PAPER_SIZE_ENTRY_NAME, serializedString);
        orientation = deserializeEntry(ORIENTATION_ENTRY_NAME, serializedString);
        fitToPage = Boolean.parseBoolean(deserializeEntry(FIT_TO_PAGE_ENTRY_NAME, serializedString));
        renderComments = Boolean.parseBoolean(deserializeEntry(RENDER_COMMENTS_ENTRY_NAME, serializedString));
        watermark = Boolean.parseBoolean(deserializeEntry(WATERMARK_ENTRY_NAME, serializedString));
        markReferencedWorkitems = Boolean.parseBoolean(deserializeEntry(MARK_REFERENCED_WORKITEMS_ENTRY_NAME, serializedString));
        cutEmptyChapters = Boolean.parseBoolean(deserializeEntry(CUT_EMPTY_CHAPTERS_ENTRY_NAME, serializedString));
        cutEmptyWorkitemAttributes = Boolean.parseBoolean(deserializeEntry(CUT_EMPTY_WORKITEM_ATTRIBUTES_ENTRY_NAME, serializedString));
        cutLocalURLs = Boolean.parseBoolean(deserializeEntry(CUT_LOCAL_URLS_ENTRY_NAME, serializedString));
        followHTMLPresentationalHints = Boolean.parseBoolean(deserializeEntry(FOLLOW_HTML_PRESENTATIONAL_HINTS_ENTRY_NAME, serializedString));
        specificChapters = deserializeEntry(SPECIFIC_CHAPTERS_ENTRY_NAME, serializedString);
        customNumberedListStyles = deserializeEntry(CUSTOM_NUMBERED_LIST_STYLES_ENTRY_NAME, serializedString);
        language = deserializeEntry(LANGUAGE_ENTRY_NAME, serializedString);
        linkedWorkitemRoles = deserializeListEntry(LINKED_WORKITEM_ROLES_ENTRY_NAME, serializedString, String.class);
        exposePageWidthValidation = Boolean.parseBoolean(deserializeEntry(EXPOSE_PAGE_WIDTH_VALIDATION_ENTRY_NAME, serializedString));
    }
}

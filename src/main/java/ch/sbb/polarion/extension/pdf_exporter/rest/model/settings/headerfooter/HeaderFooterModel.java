package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter;

import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.polarion.core.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeaderFooterModel extends SettingsModel {

    public static final String DEFAULT_HASH_ENTRY_NAME = "DEFAULT HASH";
    public static final String USE_CUSTOM_VALUES_ENTRY_NAME = "USE CUSTOM VALUES";
    public static final String HEADER_LEFT = "HEADER LEFT";
    public static final String HEADER_CENTER = "HEADER CENTER";
    public static final String HEADER_RIGHT = "HEADER RIGHT";
    public static final String FOOTER_LEFT = "FOOTER LEFT";
    public static final String FOOTER_CENTER = "FOOTER CENTER";
    public static final String FOOTER_RIGHT = "FOOTER RIGHT";

    private boolean useCustomValues;
    private String headerLeft;
    private String headerCenter;
    private String headerRight;
    private String footerLeft;
    private String footerCenter;
    private String footerRight;

    /**
     * The hash of the built-in values the custom values were copied from, null when they were not copied.
     */
    private String defaultHash;

    /**
     * Whether the built-in values changed since the custom values were copied from them. Computed on reading, never stored.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean defaultChanged;

    @Override
    protected String serializeModelData() {
        return serializeEntry(USE_CUSTOM_VALUES_ENTRY_NAME, useCustomValues) +
                serializeEntry(HEADER_LEFT, headerLeft) +
                serializeEntry(HEADER_CENTER, headerCenter) +
                serializeEntry(HEADER_RIGHT, headerRight) +
                serializeEntry(FOOTER_LEFT, footerLeft) +
                serializeEntry(FOOTER_CENTER, footerCenter) +
                serializeEntry(FOOTER_RIGHT, footerRight) +
                serializeEntry(DEFAULT_HASH_ENTRY_NAME, defaultHash);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        String serializedUseCustomValues = deserializeEntry(USE_CUSTOM_VALUES_ENTRY_NAME, serializedString);
        if (StringUtils.isEmptyTrimmed(serializedUseCustomValues)) {
            useCustomValues = true; // Fallback to backwards compatibility with older versions when values were by default always custom
        } else {
            useCustomValues = Boolean.parseBoolean(deserializeEntry(USE_CUSTOM_VALUES_ENTRY_NAME, serializedString));
        }
        headerLeft = deserializeEntry(HEADER_LEFT, serializedString);
        headerCenter = deserializeEntry(HEADER_CENTER, serializedString);
        headerRight = deserializeEntry(HEADER_RIGHT, serializedString);
        footerLeft = deserializeEntry(FOOTER_LEFT, serializedString);
        footerCenter = deserializeEntry(FOOTER_CENTER, serializedString);
        footerRight = deserializeEntry(FOOTER_RIGHT, serializedString);
        String serializedDefaultHash = deserializeEntry(DEFAULT_HASH_ENTRY_NAME, serializedString);
        defaultHash = StringUtils.isEmptyTrimmed(serializedDefaultHash) ? null : serializedDefaultHash;
    }
}

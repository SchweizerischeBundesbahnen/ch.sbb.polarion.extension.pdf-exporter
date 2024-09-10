package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter;

import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import com.fasterxml.jackson.annotation.JsonInclude;
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

    public static final String HEADER_LEFT = "HEADER LEFT";
    public static final String HEADER_CENTER = "HEADER CENTER";
    public static final String HEADER_RIGHT = "HEADER RIGHT";
    public static final String FOOTER_LEFT = "FOOTER LEFT";
    public static final String FOOTER_CENTER = "FOOTER CENTER";
    public static final String FOOTER_RIGHT = "FOOTER RIGHT";

    private String headerLeft;
    private String headerCenter;
    private String headerRight;
    private String footerLeft;
    private String footerCenter;
    private String footerRight;

    @Override
    protected String serializeModelData() {
        return serializeEntry(HEADER_LEFT, headerLeft) +
                serializeEntry(HEADER_CENTER, headerCenter) +
                serializeEntry(HEADER_RIGHT, headerRight) +
                serializeEntry(FOOTER_LEFT, footerLeft) +
                serializeEntry(FOOTER_CENTER, footerCenter) +
                serializeEntry(FOOTER_RIGHT, footerRight);
    }

    @Override
    protected void deserializeModelData(String serializedString) {
        headerLeft = deserializeEntry(HEADER_LEFT, serializedString);
        headerCenter = deserializeEntry(HEADER_CENTER, serializedString);
        headerRight = deserializeEntry(HEADER_RIGHT, serializedString);
        footerLeft = deserializeEntry(FOOTER_LEFT, serializedString);
        footerCenter = deserializeEntry(FOOTER_CENTER, serializedString);
        footerRight = deserializeEntry(FOOTER_RIGHT, serializedString);
    }
}

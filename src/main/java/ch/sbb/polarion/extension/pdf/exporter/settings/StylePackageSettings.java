package ch.sbb.polarion.extension.pdf.exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.stylepackage.StylePackageModel;
import org.jetbrains.annotations.NotNull;

public class StylePackageSettings extends GenericNamedSettings<StylePackageModel> {
    public static final String FEATURE_NAME = "style-package";
    public static final String DEFAULT_HEADERS_COLOR = "#004d73";

    public StylePackageSettings() {
        super(FEATURE_NAME);
    }

    public StylePackageSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull StylePackageModel defaultValues() {
        return StylePackageModel.builder()
                .coverPage(DEFAULT_NAME)
                .headerFooter(DEFAULT_NAME)
                .css(DEFAULT_NAME)
                .localization(DEFAULT_NAME)
                .webhooks(DEFAULT_NAME)
                .headersColor(DEFAULT_HEADERS_COLOR)
                .paperSize(PaperSize.A4.name())
                .orientation(Orientation.PORTRAIT.name())
                .fitToPage(true)
                .renderComments(true)
                .cutEmptyWorkitemAttributes(true)
                .followHTMLPresentationalHints(true)
                .build();
    }

}

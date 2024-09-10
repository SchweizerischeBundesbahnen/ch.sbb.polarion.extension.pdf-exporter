package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.HeaderFooterModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.Placeholder;
import org.jetbrains.annotations.NotNull;

public class HeaderFooterSettings extends GenericNamedSettings<HeaderFooterModel> {

    public static final String FEATURE_NAME = "header-footer";

    public HeaderFooterSettings() {
        super(FEATURE_NAME);
    }

    public HeaderFooterSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull HeaderFooterModel defaultValues() {
        return HeaderFooterModel.builder()
                .headerLeft(wrapInPlaceholder(Placeholder.PROJECT_NAME.name()))
                .headerCenter("")
                .headerRight(
                        "<a href='https://www.sbb.ch/'>" + System.lineSeparator() +
                        "    <img src='/polarion/icons/group/sbb-headerlogo.png' " + System.lineSeparator() +
                        "         alt='Schweizerische Bundesbahnen' " + System.lineSeparator() +
                        "         style='height: 20px'>" + System.lineSeparator() +
                        "</a>")
                .footerLeft(wrapInPlaceholder(Placeholder.DOCUMENT_TITLE.name()) + " (rev. " + wrapInPlaceholder(Placeholder.REVISION.name()) + ")")
                .footerCenter(wrapInPlaceholder(Placeholder.TIMESTAMP.name()))
                .footerRight(wrapInPlaceholder(Placeholder.PAGE_NUMBER.name()) + "/" + wrapInPlaceholder(Placeholder.PAGES_TOTAL_COUNT.name()))
                .build();
    }

    private String wrapInPlaceholder(String name) {
        return String.format("{{ %s }}", name);
    }
}

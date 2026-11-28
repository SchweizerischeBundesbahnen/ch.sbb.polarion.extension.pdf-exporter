package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.appearance.AppearanceModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig("pdf-exporter")
class AppearanceSettingsTest {

    @Test
    void featureNameIsAppearance() {
        assertThat(new AppearanceSettings(mock(SettingsService.class)).getFeatureName()).isEqualTo("appearance");
    }

    @Test
    void defaultValuesShowEveryButton() {
        AppearanceModel defaults = new AppearanceSettings(mock(SettingsService.class)).defaultValues();
        assertThat(defaults.isLiveDocButton()).isTrue();
        assertThat(defaults.isLiveReportButton()).isTrue();
        assertThat(defaults.isClassicWikiButton()).isTrue();
        // Expanding a toolbar Polarion collapses is an opt-in, not what an installation starts with.
        assertThat(defaults.isLiveReportAutoExpandTools()).isFalse();
        assertThat(defaults.isClassicWikiAutoExpandTools()).isFalse();
    }

    @Test
    void everyFlagSurvivesSerialization() {
        AppearanceModel model = AppearanceModel.builder()
                .liveDocButton(false)
                .liveReportButton(true)
                .liveReportAutoExpandTools(true)
                .classicWikiButton(false)
                .classicWikiAutoExpandTools(true)
                .build();

        AppearanceModel read = new AppearanceModel();
        read.deserialize(model.serialize());

        assertThat(read.isLiveDocButton()).isFalse();
        assertThat(read.isLiveReportButton()).isTrue();
        assertThat(read.isLiveReportAutoExpandTools()).isTrue();
        assertThat(read.isClassicWikiButton()).isFalse();
        assertThat(read.isClassicWikiAutoExpandTools()).isTrue();
    }

    @Test
    void aSettingWithoutTheseEntriesKeepsTheButtonsItHadBefore() {
        // A setting stored by an older version carries none of the entries. Reading it as "every button
        // off" would take the buttons away from an installation which only upgraded.
        AppearanceModel read = new AppearanceModel();
        read.deserialize("-----BEGIN NAME-----\nDefault\n-----END NAME-----\n");

        assertThat(read.isLiveDocButton()).isTrue();
        assertThat(read.isLiveReportButton()).isTrue();
        assertThat(read.isClassicWikiButton()).isTrue();
        assertThat(read.isLiveReportAutoExpandTools()).isFalse();
        assertThat(read.isClassicWikiAutoExpandTools()).isFalse();
    }
}

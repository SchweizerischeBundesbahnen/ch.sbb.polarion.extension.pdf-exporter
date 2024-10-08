package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.localization.Language;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.localization.LocalizationModel;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig("pdf-exporter")
class LocalizationSettingsTest {

    @Test
    void testDefaultLocalizationEn() {
        final LocalizationModel localization = new LocalizationSettings(new SettingsService(null, null, null)).defaultValues();
        assertNotNull(localization);
        assertTrue(localization.getLocalizationMap(Language.EN.name()).isEmpty());
    }

    @Test
    void testDefaultLocalizationDe() {
        final LocalizationModel localization = new LocalizationSettings(new SettingsService(null, null, null)).defaultValues();
        assertNotNull(localization);
        assertNotNull(localization.getLocalizationMap(Language.DE.name()));
        assertFalse(localization.getLocalizationMap(Language.DE.name()).isEmpty());
    }

    @Test
    void testDefaultLocalizationFr() {
        final LocalizationModel localization = new LocalizationSettings(new SettingsService(null, null, null)).defaultValues();
        assertNotNull(localization);
        assertNotNull(localization.getLocalizationMap(Language.FR.name()));
        assertFalse(localization.getLocalizationMap(Language.FR.name()).isEmpty());
    }

    @Test
    void testDefaultLocalizationIt() {
        final LocalizationModel localization = new LocalizationSettings(new SettingsService(null, null, null)).defaultValues();
        assertNotNull(localization);
        assertNotNull(localization.getLocalizationMap(Language.IT.name()));
        assertFalse(localization.getLocalizationMap(Language.IT.name()).isEmpty());
    }

    @Test
    void testDefaultLocalizationLoad() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);

            String projectName = "test_project";
            // location for project 'test_project'
            ILocation mockProjectLocation = mock(ILocation.class);
            when(mockProjectLocation.append(anyString())).thenReturn(mockProjectLocation);
            when(mockedSettingsService.read(eq(mockProjectLocation), any())).thenReturn(null);
            mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(projectName)).thenCallRealMethod();
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/test_project/")).thenReturn(mockProjectLocation);
            // default location exists
            ILocation mockDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            mockScopeUtils.when(ScopeUtils::getDefaultLocation).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            LocalizationSettings localizationSettings = new LocalizationSettings(mockedSettingsService);
            LocalizationModel defaultLocalization = localizationSettings.defaultValues();
            when(mockedSettingsService.read(eq(mockDefaultLocation), any())).thenReturn(localizationSettings.toString(defaultLocalization));
            LocalizationModel loadedLocalization = localizationSettings.load(projectName, SettingId.fromName(NamedSettings.DEFAULT_NAME));
            assertEquals(defaultLocalization, loadedLocalization);
        }
    }
}

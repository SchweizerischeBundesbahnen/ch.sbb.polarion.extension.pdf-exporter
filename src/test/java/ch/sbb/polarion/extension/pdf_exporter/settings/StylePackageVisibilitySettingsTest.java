package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageVisibilityModel;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static ch.sbb.polarion.extension.generic.settings.NamedSettings.DEFAULT_NAME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig("pdf-exporter")
class StylePackageVisibilitySettingsTest {

    private static final String PROJECT_SCOPE = "project/test_project/";

    @Test
    void testDefaultValues() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            assertFalse(new StylePackageVisibilitySettings(mock(SettingsService.class)).defaultValues().isHideGlobalStylePackages());
        }
    }

    @Test
    void testGlobalStylePackagesNotHiddenWithoutSettings() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService settingsService = mock(SettingsService.class);
            Locations locations = mockLocations(mockScopeUtils);

            StylePackageVisibilitySettings stylePackageVisibilitySettings = new StylePackageVisibilitySettings(settingsService);

            // Neither scope has a settings folder, so nothing needs to be read at all
            assertFalse(stylePackageVisibilitySettings.isGlobalStylePackagesHidden(PROJECT_SCOPE));
            verify(settingsService, never()).read(eq(locations.project), any());
        }
    }

    @Test
    void testGlobalScopeNeverHidesItsOwnStylePackages() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService settingsService = mock(SettingsService.class);
            mockLocations(mockScopeUtils);

            assertFalse(new StylePackageVisibilitySettings(settingsService).isGlobalStylePackagesHidden(""));
        }
    }

    @Test
    void testFlagOfProjectIsRead() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService settingsService = mock(SettingsService.class);
            Locations locations = mockLocations(mockScopeUtils);

            persistFlag(settingsService, locations.project, true);

            assertTrue(new StylePackageVisibilitySettings(settingsService).isGlobalStylePackagesHidden(PROJECT_SCOPE));
        }
    }

    @Test
    void testFlagIsInheritedFromGlobalScope() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService settingsService = mock(SettingsService.class);
            Locations locations = mockLocations(mockScopeUtils);

            persistFlag(settingsService, locations.global, true);

            assertTrue(new StylePackageVisibilitySettings(settingsService).isGlobalStylePackagesHidden(PROJECT_SCOPE));
        }
    }

    @Test
    void testSaveForgetsWhatWasCachedForTheScope() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService settingsService = mock(SettingsService.class);
            Locations locations = mockLocations(mockScopeUtils);

            persistFlag(settingsService, locations.project, false);
            StylePackageVisibilitySettings settings = new StylePackageVisibilitySettings(settingsService);
            assertFalse(settings.isGlobalStylePackagesHidden(PROJECT_SCOPE));

            // The document now says the opposite while the folder still reports the revision it was read at,
            // which is what the save has to invalidate
            persistFlag(settingsService, locations.project, true);
            settings.save(PROJECT_SCOPE, SettingId.fromId("visibility_id"), model(true));

            assertTrue(settings.isGlobalStylePackagesHidden(PROJECT_SCOPE));
        }
    }

    private StylePackageVisibilityModel model(boolean hideGlobalStylePackages) {
        return StylePackageVisibilityModel.builder().hideGlobalStylePackages(hideGlobalStylePackages).build();
    }

    /** Makes the settings service answer the "Default" document of the given scope with the flag. */
    private void persistFlag(SettingsService settingsService, ILocation location, boolean hideGlobalStylePackages) {
        StylePackageVisibilityModel persisted = model(hideGlobalStylePackages);
        persisted.setName(DEFAULT_NAME);
        lenient().when(settingsService.getLastRevision(location)).thenReturn("42");
        lenient().when(settingsService.getPersistedSettingFileNames(location)).thenReturn(List.of("visibility_id"));
        lenient().when(settingsService.read(eq(location), any())).thenReturn(persisted.serialize());
    }

    private Locations mockLocations(MockedStatic<ScopeUtils> mockScopeUtils) {
        mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

        ILocation global = mock(ILocation.class);
        lenient().when(global.append(anyString())).thenReturn(global);
        mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(global);

        ILocation project = mock(ILocation.class);
        lenient().when(project.append(anyString())).thenReturn(project);
        mockScopeUtils.when(() -> ScopeUtils.getContextLocation(PROJECT_SCOPE)).thenReturn(project);

        return new Locations(global, project);
    }

    private record Locations(ILocation global, ILocation project) {
    }
}

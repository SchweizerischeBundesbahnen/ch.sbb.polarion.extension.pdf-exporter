package ch.sbb.polarion.extension.pdf.exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;

import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.stylepackage.StylePackageModel;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StylePackageSettingsTest {
    @Test
    void testLoadDefaultWhenSettingDoesNotExist() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            when(mockedSettingsService.exists(any())).thenReturn(true);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            StylePackageSettings stylePackageSettings = new StylePackageSettings(mockedSettingsService);

            String projectName = "test_project";

            ILocation mockProjectLocation = mock(ILocation.class);
            when(mockProjectLocation.append(anyString())).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            when(mockedSettingsService.read(eq(mockProjectLocation), any())).thenReturn(null);
            mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(projectName)).thenCallRealMethod();
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/test_project/")).thenReturn(mockProjectLocation);

            ILocation mockDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(ScopeUtils::getDefaultLocation).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);
            StylePackageModel model = stylePackageSettings.defaultValues();
            model.setBundleTimestamp("default");
            when(mockedSettingsService.read(eq(mockDefaultLocation), any())).thenReturn(model.serialize());

            StylePackageModel loadedModel = stylePackageSettings.load(projectName, SettingId.fromName("Any setting name"));
            assertEquals(model.getCss(), loadedModel.getCss());
            assertEquals(model.getCoverPage(), loadedModel.getCoverPage());
            assertEquals(model.getLanguage(), loadedModel.getLanguage());
            assertEquals(model.getOrientation(), loadedModel.getOrientation());
            assertEquals(model.getHeaderFooter(), loadedModel.getHeaderFooter());
            assertEquals(model.getPaperSize(), loadedModel.getPaperSize());
            assertEquals("default", loadedModel.getBundleTimestamp());
        }
    }

    @Test
    void testLoadCustomWhenSettingExists() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            GenericNamedSettings<StylePackageModel> stylePackageSettings = new StylePackageSettings(mockedSettingsService);

            String projectName = "test_project";

            ILocation mockDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);

            ILocation mockProjectLocation = mock(ILocation.class);
            when(mockProjectLocation.append(anyString())).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(projectName)).thenCallRealMethod();
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/test_project/")).thenReturn(mockProjectLocation);

            StylePackageModel customProjectModel = StylePackageModel.builder()
                    .css("customCSS")
                    .coverPage("customCoverPage")
                    .orientation("customOrientation")
                    .build();
            customProjectModel.setBundleTimestamp("custom");
            when(mockedSettingsService.read(eq(mockProjectLocation), any())).thenReturn(customProjectModel.serialize());

            StylePackageModel loadedModel = stylePackageSettings.load(projectName, SettingId.fromName("Any setting name"));
            assertEquals("customCSS", loadedModel.getCss());
            assertEquals("customCoverPage", loadedModel.getCoverPage());
            assertEquals("customOrientation", loadedModel.getOrientation());
            assertEquals("custom", loadedModel.getBundleTimestamp());
        }
    }

    @Test
    void testNamedSettings() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            GenericNamedSettings<StylePackageModel> settings = new StylePackageSettings(mockedSettingsService);

            String projectName = "test_project";
            String settingOne = "setting_one";
            String settingTwo = "setting_two";

            mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(projectName)).thenReturn("project/test_project/");

            ILocation mockDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);
            when(mockedSettingsService.getLastRevision(mockDefaultLocation)).thenReturn("some_revision");

            ILocation mockProjectLocation = mock(ILocation.class);
            when(mockedSettingsService.getPersistedSettingFileNames(mockProjectLocation)).thenReturn(List.of(settingOne, settingTwo));
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/test_project/")).thenReturn(mockProjectLocation);
            when(mockProjectLocation.append(contains("style"))).thenReturn(mockProjectLocation);
            ILocation settingOneLocation = mock(ILocation.class);
            when(mockProjectLocation.append(contains(settingOne))).thenReturn(settingOneLocation);
            ILocation settingTwoLocation = mock(ILocation.class);
            when(mockProjectLocation.append(contains(settingTwo))).thenReturn(settingTwoLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            when(mockedSettingsService.getLastRevision(mockProjectLocation)).thenReturn("some_revision");

            StylePackageModel settingOneModel = StylePackageModel.builder()
                    .css("setting_oneCSS")
                    .coverPage("setting_oneCoverPage")
                    .orientation(Orientation.LANDSCAPE.name())
                    .paperSize(PaperSize.valueOf("A4").name())
                    .build();
            settingOneModel.setBundleTimestamp("setting_one");
            when(mockedSettingsService.read(eq(settingOneLocation), any())).thenReturn(settingOneModel.serialize());

            StylePackageModel settingTwoModel = StylePackageModel.builder()
                    .css("setting_twoCSS")
                    .coverPage("setting_twoCoverPage")
                    .orientation(Orientation.LANDSCAPE.name())
                    .paperSize(PaperSize.valueOf("A3").name())
                    .build();
            settingTwoModel.setBundleTimestamp("setting_two");
            when(mockedSettingsService.read(eq(settingTwoLocation), any())).thenReturn(settingTwoModel.serialize());

            StylePackageModel loadedOneModel = settings.load(projectName, SettingId.fromName(settingOne));
            assertEquals("setting_oneCSS", loadedOneModel.getCss());
            assertEquals("setting_oneCoverPage", loadedOneModel.getCoverPage());
            assertEquals(Orientation.LANDSCAPE, Orientation.valueOf(loadedOneModel.getOrientation()));
            assertEquals(PaperSize.A4, PaperSize.valueOf(loadedOneModel.getPaperSize()));
            assertEquals("setting_one", loadedOneModel.getBundleTimestamp());

            StylePackageModel loadedTwoModel = settings.load(projectName, SettingId.fromName(settingTwo));
            assertEquals("setting_twoCSS", loadedTwoModel.getCss());
            assertEquals("setting_twoCoverPage", loadedTwoModel.getCoverPage());
            assertEquals(Orientation.LANDSCAPE, Orientation.valueOf(loadedTwoModel.getOrientation()));
            assertEquals(PaperSize.A3, PaperSize.valueOf(loadedTwoModel.getPaperSize()));
            assertEquals("setting_two", loadedTwoModel.getBundleTimestamp());
        }
    }
}

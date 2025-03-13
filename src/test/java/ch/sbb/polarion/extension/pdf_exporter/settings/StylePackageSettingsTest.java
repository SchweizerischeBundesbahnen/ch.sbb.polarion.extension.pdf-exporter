package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.CommentsRenderType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static ch.sbb.polarion.extension.generic.settings.NamedSettings.DEFAULT_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig("pdf-exporter")
class StylePackageSettingsTest {

    @Test
    void testSettingDoesNotExist() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            StylePackageSettings stylePackageSettings = new StylePackageSettings(mockedSettingsService);

            String projectName = "test_project";

            ILocation mockProjectLocation = mock(ILocation.class);
            when(mockProjectLocation.append(anyString())).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(projectName)).thenCallRealMethod();
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/test_project/")).thenReturn(mockProjectLocation);

            ILocation mockDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(ScopeUtils::getDefaultLocation).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);

            assertThrows(ObjectNotFoundException.class, () -> {
                StylePackageModel loadedModel = stylePackageSettings.load(projectName, SettingId.fromName("Any setting name"));
            });
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

            when(mockedSettingsService.getLastRevision(mockProjectLocation)).thenReturn("345");
            when(mockedSettingsService.getPersistedSettingFileNames(mockProjectLocation)).thenReturn(List.of("Any setting name"));

            StylePackageModel loadedModel = stylePackageSettings.load(projectName, SettingId.fromName("Any setting name"));
            assertEquals("customCSS", loadedModel.getCss());
            assertEquals("customCoverPage", loadedModel.getCoverPage());
            assertEquals("customOrientation", loadedModel.getOrientation());
            assertEquals("custom", loadedModel.getBundleTimestamp());
            assertNull(loadedModel.getRenderComments());

            // check 'render comments' backward boolean compatibility
            customProjectModel.setRenderComments(CommentsRenderType.OPEN);
            when(mockedSettingsService.read(eq(mockProjectLocation), any())).thenReturn(customProjectModel.serialize().replace("OPEN", "true"));
            assertEquals(CommentsRenderType.OPEN, stylePackageSettings.load(projectName, SettingId.fromName("Any setting name")).getRenderComments());
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

    @Test
    void testValidateWeight() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            StylePackageSettings stylePackageSettings = new StylePackageSettings(mockedSettingsService);
            StylePackageModel model = new StylePackageModel();

            model.setWeight(null);
            stylePackageSettings.adjustAndValidateWeight(model);
            assertEquals(StylePackageModel.DEFAULT_WEIGHT, model.getWeight());

            model.setWeight(0.0f);
            stylePackageSettings.adjustAndValidateWeight(model);
            assertEquals(0.0f, model.getWeight());

            model.setWeight(0.1f);
            stylePackageSettings.adjustAndValidateWeight(model);
            assertEquals(0.1f, model.getWeight());

            model.setWeight(0.10f);
            stylePackageSettings.adjustAndValidateWeight(model);
            assertEquals(0.1f, model.getWeight());

            model.setWeight(100.000f);
            stylePackageSettings.adjustAndValidateWeight(model);
            assertEquals(100f, model.getWeight());

            model.setWeight(-5.0f);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                stylePackageSettings.adjustAndValidateWeight(model);
            });
            assertEquals("Weight must be between 0 and 100 and have only one digit after the decimal point", exception.getMessage());

            model.setWeight(101.0f);
            exception = assertThrows(IllegalArgumentException.class, () -> {
                stylePackageSettings.adjustAndValidateWeight(model);
            });
            assertEquals("Weight must be between 0 and 100 and have only one digit after the decimal point", exception.getMessage());

            model.setWeight(50.11f);
            exception = assertThrows(IllegalArgumentException.class, () -> {
                stylePackageSettings.adjustAndValidateWeight(model);
            });
            assertEquals("Weight must be between 0 and 100 and have only one digit after the decimal point", exception.getMessage());

            model.setWeight(20.000001f);
            exception = assertThrows(IllegalArgumentException.class, () -> {
                stylePackageSettings.adjustAndValidateWeight(model);
            });
            assertEquals("Weight must be between 0 and 100 and have only one digit after the decimal point", exception.getMessage());
        }
    }

    @Test
    void testValidateMatchingQuery() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            StylePackageSettings stylePackageSettings = new StylePackageSettings(mockedSettingsService);
            StylePackageModel model = new StylePackageModel();

            model.setMatchingQuery("query");
            assertDoesNotThrow(() -> stylePackageSettings.validateMatchingQuery(model));

            model.setName(DEFAULT_NAME);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> stylePackageSettings.validateMatchingQuery(model));
            assertEquals("Matching query cannot be specified for a default style package", exception.getMessage());
        }
    }

}

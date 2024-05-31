package ch.sbb.polarion.extension.pdf.exporter.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.filename.FileNameTemplateModel;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileNameTemplateSettingsTest {
    @Test
    void testLoadDefaultWhenSettingDoesNotExist() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            when(mockedSettingsService.exists(any())).thenReturn(true);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            FileNameTemplateSettings fileNameTemplateSettings = new FileNameTemplateSettings(mockedSettingsService);

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
            FileNameTemplateModel model = fileNameTemplateSettings.defaultValues();
            model.setBundleTimestamp("default");
            when(mockedSettingsService.read(eq(mockDefaultLocation), any())).thenReturn(model.serialize());

            FileNameTemplateModel loadedModel = fileNameTemplateSettings.load(projectName, SettingId.fromName("Any setting name"));
            assertEquals(model.getDocumentNameTemplate(), loadedModel.getDocumentNameTemplate());
            assertEquals(model.getReportNameTemplate(), loadedModel.getReportNameTemplate());
            assertEquals("default", loadedModel.getBundleTimestamp());
        }
    }

    @Test
    void testLoadCustomWhenSettingExists() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            GenericNamedSettings<FileNameTemplateModel> exporterSettings = new FileNameTemplateSettings(mockedSettingsService);

            String projectName = "test_project";

            ILocation mockDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);

            ILocation mockProjectLocation = mock(ILocation.class);
            when(mockProjectLocation.append(anyString())).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(projectName)).thenCallRealMethod();
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/test_project/")).thenReturn(mockProjectLocation);

            FileNameTemplateModel customProjectModel = FileNameTemplateModel.builder()
                    .documentNameTemplate("customDocumentNameTemplate")
                    .reportNameTemplate("customReportTemplate")
                    .build();
            customProjectModel.setBundleTimestamp("custom");
            when(mockedSettingsService.read(eq(mockProjectLocation), any())).thenReturn(customProjectModel.serialize());

            FileNameTemplateModel defaultProjectModel = FileNameTemplateModel.builder()
                    .documentNameTemplate("defaultDocumentNameTemplate")
                    .reportNameTemplate("defaultReportTemplate")
                    .build();
            defaultProjectModel.setBundleTimestamp("default");

            FileNameTemplateModel loadedModel = exporterSettings.load(projectName, SettingId.fromName("Any setting name"));
            assertEquals("customDocumentNameTemplate", loadedModel.getDocumentNameTemplate());
            assertEquals("customReportTemplate", loadedModel.getReportNameTemplate());
            assertEquals("custom", loadedModel.getBundleTimestamp());
        }
    }

    @Test
    void testNamedSettings() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            GenericNamedSettings<FileNameTemplateModel> settings = new FileNameTemplateSettings(mockedSettingsService);

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
            when(mockProjectLocation.append(contains("template"))).thenReturn(mockProjectLocation);
            ILocation settingOneLocation = mock(ILocation.class);
            when(mockProjectLocation.append(contains(settingOne))).thenReturn(settingOneLocation);
            ILocation settingTwoLocation = mock(ILocation.class);
            when(mockProjectLocation.append(contains(settingTwo))).thenReturn(settingTwoLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            when(mockedSettingsService.getLastRevision(mockProjectLocation)).thenReturn("some_revision");

            FileNameTemplateModel settingOneModel = FileNameTemplateModel.builder()
                    .documentNameTemplate("setting_oneDocumentNameTemplate")
                    .reportNameTemplate("setting_oneReportTemplate")
                    .build();
            settingOneModel.setBundleTimestamp("setting_one");
            when(mockedSettingsService.read(eq(settingOneLocation), any())).thenReturn(settingOneModel.serialize());

            FileNameTemplateModel settingTwoModel = FileNameTemplateModel.builder()
                    .documentNameTemplate("setting_twoDocumentNameTemplate")
                    .reportNameTemplate("setting_twoReportTemplate")
                    .build();
            settingTwoModel.setBundleTimestamp("setting_two");
            when(mockedSettingsService.read(eq(settingTwoLocation), any())).thenReturn(settingTwoModel.serialize());

            FileNameTemplateModel loadedOneModel = settings.load(projectName, SettingId.fromName(settingOne));
            assertEquals("setting_oneDocumentNameTemplate", loadedOneModel.getDocumentNameTemplate());
            assertEquals("setting_oneReportTemplate", loadedOneModel.getReportNameTemplate());
            assertEquals("setting_one", loadedOneModel.getBundleTimestamp());

            FileNameTemplateModel loadedTwoModel = settings.load(projectName, SettingId.fromName(settingTwo));
            assertEquals("setting_twoDocumentNameTemplate", loadedTwoModel.getDocumentNameTemplate());
            assertEquals("setting_twoReportTemplate", loadedTwoModel.getReportNameTemplate());
            assertEquals("setting_two", loadedTwoModel.getBundleTimestamp());
        }
    }
}

package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.filename.FileNameTemplateModel;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig("pdf-exporter")
class FileNameTemplateSettingsTest {

    @Test
    void testSettingDoesNotExist() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            FileNameTemplateSettings fileNameTemplateSettings = new FileNameTemplateSettings(mockedSettingsService);

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

            SettingId anySettingName = SettingId.fromName("Any setting name");
            assertThrows(ObjectNotFoundException.class, () -> fileNameTemplateSettings.load(projectName, anySettingName));
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
                    .useCustomValues(true)
                    .documentNameTemplate("customDocumentNameTemplate")
                    .reportNameTemplate("customReportTemplate")
                    .testRunNameTemplate("customTestrunTemplate")
                    .wikiNameTemplate("customWikiTemplate")
                    .build();
            customProjectModel.setBundleTimestamp("custom");
            when(mockedSettingsService.read(eq(mockProjectLocation), any())).thenReturn(customProjectModel.serialize());

            when(mockedSettingsService.getLastRevision(mockProjectLocation)).thenReturn("345");
            when(mockedSettingsService.getPersistedSettingFileNames(mockProjectLocation)).thenReturn(List.of("Any setting name"));

            FileNameTemplateModel loadedModel = exporterSettings.load(projectName, SettingId.fromName("Any setting name"));
            assertTrue(loadedModel.isUseCustomValues());
            assertEquals("customDocumentNameTemplate", loadedModel.getDocumentNameTemplate());
            assertEquals("customReportTemplate", loadedModel.getReportNameTemplate());
            assertEquals("customTestrunTemplate", loadedModel.getTestRunNameTemplate());
            assertEquals("customWikiTemplate", loadedModel.getWikiNameTemplate());
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
                    .useCustomValues(true)
                    .documentNameTemplate("setting_oneDocumentNameTemplate")
                    .reportNameTemplate("setting_oneReportTemplate")
                    .testRunNameTemplate("setting_oneTestrunTemplate")
                    .wikiNameTemplate("setting_oneWikiTemplate")
                    .build();
            settingOneModel.setBundleTimestamp("setting_one");
            when(mockedSettingsService.read(eq(settingOneLocation), any())).thenReturn(settingOneModel.serialize());

            FileNameTemplateModel settingTwoModel = FileNameTemplateModel.builder()
                    .documentNameTemplate("setting_twoDocumentNameTemplate")
                    .reportNameTemplate("setting_twoReportTemplate")
                    .testRunNameTemplate("setting_twoTestrunTemplate")
                    .wikiNameTemplate("setting_twoWikiTemplate")
                    .build();
            settingTwoModel.setBundleTimestamp("setting_two");
            when(mockedSettingsService.read(eq(settingTwoLocation), any())).thenReturn(settingTwoModel.serialize());

            FileNameTemplateModel loadedOneModel = settings.load(projectName, SettingId.fromName(settingOne));
            assertTrue(loadedOneModel.isUseCustomValues());
            assertEquals("setting_oneDocumentNameTemplate", loadedOneModel.getDocumentNameTemplate());
            assertEquals("setting_oneReportTemplate", loadedOneModel.getReportNameTemplate());
            assertEquals("setting_oneTestrunTemplate", loadedOneModel.getTestRunNameTemplate());
            assertEquals("setting_oneWikiTemplate", loadedOneModel.getWikiNameTemplate());
            assertEquals("setting_one", loadedOneModel.getBundleTimestamp());

            FileNameTemplateModel loadedTwoModel = settings.load(projectName, SettingId.fromName(settingTwo));
            assertFalse(loadedTwoModel.isUseCustomValues());
            assertEquals("setting_twoDocumentNameTemplate", loadedTwoModel.getDocumentNameTemplate());
            assertEquals("setting_twoReportTemplate", loadedTwoModel.getReportNameTemplate());
            assertEquals("setting_twoTestrunTemplate", loadedTwoModel.getTestRunNameTemplate());
            assertEquals("setting_twoWikiTemplate", loadedTwoModel.getWikiNameTemplate());
            assertEquals("setting_two", loadedTwoModel.getBundleTimestamp());
        }
    }

    @Test
    void testInitialValuesAreEmptyAndNotInUse() {
        FileNameTemplateModel initial = new FileNameTemplateSettings(new SettingsService(null, null, null)).initialValues();
        assertFalse(initial.isUseCustomValues());
        assertEquals("", initial.getDocumentNameTemplate());
        assertEquals("", initial.getWikiNameTemplate());
    }

    @Test
    void testCopyOfBuiltInTemplatesIsReadAsEmptyUnlessInUse() {
        FileNameTemplateSettings settings = new FileNameTemplateSettings(new SettingsService(null, null, null));
        FileNameTemplateModel copy = settings.defaultValues();
        copy.setName("Default");

        FileNameTemplateModel read = settings.withoutBuiltInCopy(copy);
        assertEquals("", read.getDocumentNameTemplate());
        assertEquals("", read.getReportNameTemplate());
        assertEquals("Default", read.getName());

        FileNameTemplateModel inUse = settings.defaultValues();
        inUse.setUseCustomValues(true);
        assertEquals(FileNameTemplateSettings.DEFAULT_LIVE_DOC_NAME_TEMPLATE, settings.withoutBuiltInCopy(inUse).getDocumentNameTemplate());
    }

    @Test
    void testChangedDefaultOnlyForTemplatesInUse() {
        FileNameTemplateSettings settings = new FileNameTemplateSettings(new SettingsService(null, null, null));
        String page = "$projectName $page.pageNameWithSpace.replace(\" / \", \" \")";
        String formerHash = BuiltInValues.hash(FileNameTemplateSettings.DEFAULT_LIVE_DOC_NAME_TEMPLATE, page + " $page.revision",
                FileNameTemplateSettings.DEFAULT_TEST_RUN_NAME_TEMPLATE, page + " $page.revision");

        FileNameTemplateModel inUse = settings.initialValues();
        inUse.setUseCustomValues(true);
        inUse.setDefaultHash(formerHash);
        assertTrue(settings.withChangedDefault(inUse).isDefaultChanged());

        FileNameTemplateModel notInUse = settings.initialValues();
        notInUse.setDefaultHash(formerHash);
        assertFalse(settings.withChangedDefault(notInUse).isDefaultChanged());

        inUse.setDefaultHash(settings.defaultValues().getDefaultHash());
        assertFalse(settings.withChangedDefault(inUse).isDefaultChanged());
    }

    @Test
    void testLegacyCopyInUseOfFormerVersionIsNoticed() {
        FileNameTemplateSettings settings = new FileNameTemplateSettings(new SettingsService(null, null, null));
        String page = "$projectName $page.pageNameWithSpace.replace(\" / \", \" \")";
        FileNameTemplateModel legacyCopy = FileNameTemplateModel.builder()
                .useCustomValues(true)
                .documentNameTemplate(FileNameTemplateSettings.DEFAULT_LIVE_DOC_NAME_TEMPLATE)
                .reportNameTemplate(page + " $page.revision")
                .testRunNameTemplate(FileNameTemplateSettings.DEFAULT_TEST_RUN_NAME_TEMPLATE)
                .wikiNameTemplate(page + " $page.revision")
                .build();

        FileNameTemplateModel read = settings.withChangedDefault(settings.withLegacyBase(legacyCopy));
        assertNotNull(read.getDefaultHash());
        assertTrue(read.isDefaultChanged());
        assertEquals(page + " $page.revision", read.getReportNameTemplate());
    }

    @Test
    void testCopyOfFormerBuiltInTemplatesIsReadAsEmpty() {
        FileNameTemplateSettings settings = new FileNameTemplateSettings(new SettingsService(null, null, null));
        String page = "$projectName $page.pageNameWithSpace.replace(\" / \", \" \")";
        FileNameTemplateModel formerCopy = FileNameTemplateModel.builder()
                .documentNameTemplate(FileNameTemplateSettings.DEFAULT_LIVE_DOC_NAME_TEMPLATE)
                .reportNameTemplate(page + " $page.lastRevision")
                .testRunNameTemplate(FileNameTemplateSettings.DEFAULT_TEST_RUN_NAME_TEMPLATE)
                .wikiNameTemplate(page + " $page.lastRevision")
                .build();
        assertEquals("", settings.withoutBuiltInCopy(formerCopy).getReportNameTemplate());
    }
}

package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.css.CssModel;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig("pdf-exporter")
class CssSettingsTest {

    @Test
    void testDefaultCss() {
        final String css = new CssSettings(new SettingsService(null, null, null)).defaultValues().getCss();
        assertNotNull(css);
    }

    @Test
    void testSettingDoesNotExist() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            GenericNamedSettings<CssModel> cssSettings = new CssSettings(mockedSettingsService);

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
            assertThrows(ObjectNotFoundException.class, () -> cssSettings.load(projectName, anySettingName));
        }
    }

    @Test
    void testLoadCustomWhenSettingExists() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            GenericNamedSettings<CssModel> cssSettings = new CssSettings(mockedSettingsService);

            String projectName = "test_project";

            ILocation mockDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);

            ILocation mockProjectLocation = mock(ILocation.class);
            when(mockProjectLocation.append(anyString())).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(projectName)).thenCallRealMethod();
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/test_project/")).thenReturn(mockProjectLocation);

            CssModel customProjectModel = CssModel.builder().css("customCss").build();
            customProjectModel.setBundleTimestamp("custom");
            when(mockedSettingsService.read(eq(mockProjectLocation), any())).thenReturn(customProjectModel.serialize());

            when(mockedSettingsService.getLastRevision(mockProjectLocation)).thenReturn("345");
            when(mockedSettingsService.getPersistedSettingFileNames(mockProjectLocation)).thenReturn(List.of("Any setting name"));

            CssModel defaultProjectModel = CssModel.builder().css("defaultCss").build();
            defaultProjectModel.setBundleTimestamp("default");

            CssModel loadedModel = cssSettings.load(projectName, SettingId.fromName("Any setting name"));
            assertEquals("customCss", loadedModel.getCss());
            assertEquals("custom", loadedModel.getBundleTimestamp());
        }
    }

    @Test
    void testNamedSettings() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            GenericNamedSettings<CssModel> cssSettings = new CssSettings(mockedSettingsService);

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
            when(mockProjectLocation.append(contains("css"))).thenReturn(mockProjectLocation);
            ILocation settingOneLocation = mock(ILocation.class);
            when(mockProjectLocation.append(contains(settingOne))).thenReturn(settingOneLocation);
            ILocation settingTwoLocation = mock(ILocation.class);
            when(mockProjectLocation.append(contains(settingTwo))).thenReturn(settingTwoLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            when(mockedSettingsService.getLastRevision(mockProjectLocation)).thenReturn("some_revision");

            CssModel settingOneModel = CssModel.builder().css("setting_one").build();
            settingOneModel.setBundleTimestamp("setting_one");
            when(mockedSettingsService.read(eq(settingOneLocation), any())).thenReturn(settingOneModel.serialize());

            CssModel settingTwoModel = CssModel.builder().css("setting_two").build();
            settingTwoModel.setBundleTimestamp("setting_two");
            when(mockedSettingsService.read(eq(settingTwoLocation), any())).thenReturn(settingTwoModel.serialize());

            CssModel loadedOneModel = cssSettings.load(projectName, SettingId.fromName(settingOne));
            assertEquals("setting_one", loadedOneModel.getCss());
            assertEquals("setting_one", loadedOneModel.getBundleTimestamp());

            CssModel loadedTwoModel = cssSettings.load(projectName, SettingId.fromName(settingTwo));
            assertEquals("setting_two", loadedTwoModel.getCss());
            assertEquals("setting_two", loadedTwoModel.getBundleTimestamp());
        }
    }


    @Test
    void testInitialValuesHaveNoCustomCss() {
        CssModel initial = new CssSettings(new SettingsService(null, null, null)).initialValues();
        assertEquals("", initial.getCss());
        assertFalse(initial.isDisableDefaultCss());
    }

    @Test
    void testCopyOfDefaultCssIsReadAsNoCustomCss() {
        CssSettings cssSettings = new CssSettings(new SettingsService(null, null, null));
        String defaultCss = cssSettings.defaultValues().getCss();

        assertEquals("", cssSettings.withoutBuiltInCopy(CssModel.builder().css("  " + defaultCss + "\n").build()).getCss());
        // with the default CSS disabled the copy is the only CSS
        assertEquals(defaultCss, cssSettings.withoutBuiltInCopy(CssModel.builder().css(defaultCss).disableDefaultCss(true).build()).getCss());
        assertEquals("custom", cssSettings.withoutBuiltInCopy(CssModel.builder().css("custom").build()).getCss());
    }

    @Test
    void testUneditedCopyIsRecognizedByItsStoredHash() {
        CssSettings cssSettings = new CssSettings(new SettingsService(null, null, null));
        // a version shipped after the legacy values were frozen: only the stored hash tells it is a copy
        String laterVersion = "body { color: black; }";
        CssModel copy = CssModel.builder().css(laterVersion).defaultHash(BuiltInValues.hash(laterVersion)).build();
        assertEquals("", cssSettings.withoutBuiltInCopy(copy).getCss());

        CssModel edited = CssModel.builder().css(laterVersion + " h1 {}").defaultHash(BuiltInValues.hash(laterVersion)).build();
        assertEquals(laterVersion + " h1 {}", cssSettings.withoutBuiltInCopy(edited).getCss());
    }

    @Test
    void testLegacyCopyInUseGetsItsBase() {
        CssSettings cssSettings = new CssSettings(new SettingsService(null, null, null));
        CssModel defaultCss = cssSettings.defaultValues();

        CssModel legacyCopy = CssModel.builder().css(defaultCss.getCss()).disableDefaultCss(true).build();
        assertEquals(defaultCss.getDefaultHash(), cssSettings.withLegacyBase(legacyCopy).getDefaultHash());
        assertFalse(cssSettings.withChangedDefault(legacyCopy).isDefaultChanged());

        CssModel legacyEdited = CssModel.builder().css("h1 {}").disableDefaultCss(true).build();
        assertNull(cssSettings.withLegacyBase(legacyEdited).getDefaultHash());
    }

    @Test
    void testChangedDefaultOnlyForCustomCssAlone() throws Exception {
        CssSettings cssSettings = new CssSettings(new SettingsService(null, null, null));
        String currentHash = cssSettings.defaultValues().getDefaultHash();
        String formerHash = BuiltInValuesTest.formerHash(CssSettings.FEATURE_NAME, java.util.Set.of(currentHash));

        assertTrue(cssSettings.withChangedDefault(CssModel.builder().css("x").disableDefaultCss(true).defaultHash(formerHash).build()).isDefaultChanged());
        assertFalse(cssSettings.withChangedDefault(CssModel.builder().css("x").disableDefaultCss(true).defaultHash(currentHash).build()).isDefaultChanged());
        // with the default CSS enabled the export applies the current one
        assertFalse(cssSettings.withChangedDefault(CssModel.builder().css("x").defaultHash(formerHash).build()).isDefaultChanged());
        // a copy of a version this extension does not know is behind the current one as well
        assertTrue(cssSettings.withChangedDefault(CssModel.builder().css("x").disableDefaultCss(true).defaultHash("unknown").build()).isDefaultChanged());
        assertFalse(cssSettings.withChangedDefault(CssModel.builder().css("x").disableDefaultCss(true).build()).isDefaultChanged());
    }
}

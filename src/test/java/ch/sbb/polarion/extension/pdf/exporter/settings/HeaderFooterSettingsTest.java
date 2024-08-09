package ch.sbb.polarion.extension.pdf.exporter.settings;

import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.headerfooter.HeaderFooterModel;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeaderFooterSettingsTest {

    @Test
    void testSettingDoesNotExist() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            HeaderFooterSettings headerFooterSettings = new HeaderFooterSettings(mockedSettingsService);

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
                HeaderFooterModel loadedModel = headerFooterSettings.load(projectName, SettingId.fromName("Any setting name"));
            });
        }
    }

    @Test
    void testLoadCustomWhenSettingExists() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class, RETURNS_DEEP_STUBS)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            HeaderFooterSettings headerFooterSettings = new HeaderFooterSettings(mockedSettingsService);

            String projectName = "test_project";

            ILocation mockDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);

            ILocation mockProjectLocation = mock(ILocation.class);
            when(mockProjectLocation.append(anyString())).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(projectName)).thenCallRealMethod();
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/test_project/")).thenReturn(mockProjectLocation);

            HeaderFooterModel customProjectModel = getHeaderFooter("left", "center", "right", "left", "center", "right");
            customProjectModel.setBundleTimestamp("custom");
            when(mockedSettingsService.read(eq(mockProjectLocation), any())).thenReturn(customProjectModel.serialize());

            when(mockedSettingsService.getLastRevision(mockProjectLocation)).thenReturn("345");
            when(mockedSettingsService.getPersistedSettingFileNames(mockProjectLocation)).thenReturn(List.of("Any setting name"));

            HeaderFooterModel defaultProjectModel = getHeaderFooter("leftDefault", "centerDefault", "rightDefault", "leftDefault", "centerDefault", "rightDefault");
            defaultProjectModel.setBundleTimestamp("default");

            HeaderFooterModel loadedModel = headerFooterSettings.load(projectName, SettingId.fromName("Any setting name"));
            assertEquals("left", loadedModel.getHeaderLeft());
            assertEquals("center", loadedModel.getHeaderCenter());
            assertEquals("right", loadedModel.getHeaderRight());
            assertEquals("left", loadedModel.getFooterLeft());
            assertEquals("center", loadedModel.getFooterCenter());
            assertEquals("right", loadedModel.getFooterRight());
            assertEquals("custom", loadedModel.getBundleTimestamp());
        }
    }

    @Test
    void testNamedSettings() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class, RETURNS_DEEP_STUBS)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            HeaderFooterSettings headerFooterSettings = new HeaderFooterSettings(mockedSettingsService);

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
            when(mockProjectLocation.append(contains("header-footer"))).thenReturn(mockProjectLocation);
            ILocation settingOneLocation = mock(ILocation.class);
            when(mockProjectLocation.append(contains(settingOne))).thenReturn(settingOneLocation);
            ILocation settingTwoLocation = mock(ILocation.class);
            when(mockProjectLocation.append(contains(settingTwo))).thenReturn(settingTwoLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            when(mockedSettingsService.getLastRevision(mockProjectLocation)).thenReturn("some_revision");

            HeaderFooterModel settingOneModel = getHeaderFooter("left1", "center1", "right1", "left1", "center1", "right1");
            settingOneModel.setBundleTimestamp("setting_one");
            when(mockedSettingsService.read(eq(settingOneLocation), any())).thenReturn(settingOneModel.serialize());

            HeaderFooterModel settingTwoModel = getHeaderFooter("left2", "center2", "right2", "left2", "center2", "right2");
            settingTwoModel.setBundleTimestamp("setting_two");
            when(mockedSettingsService.read(eq(settingTwoLocation), any())).thenReturn(settingTwoModel.serialize());

            HeaderFooterModel loadedOneModel = headerFooterSettings.load(projectName, SettingId.fromName(settingOne));
            assertEquals("left1", loadedOneModel.getHeaderLeft());
            assertEquals("center1", loadedOneModel.getHeaderCenter());
            assertEquals("right1", loadedOneModel.getHeaderRight());
            assertEquals("left1", loadedOneModel.getFooterLeft());
            assertEquals("center1", loadedOneModel.getFooterCenter());
            assertEquals("right1", loadedOneModel.getFooterRight());
            assertEquals("setting_one", loadedOneModel.getBundleTimestamp());

            HeaderFooterModel loadedTwoModel = headerFooterSettings.load(projectName, SettingId.fromName(settingTwo));
            assertEquals("left2", loadedTwoModel.getHeaderLeft());
            assertEquals("center2", loadedTwoModel.getHeaderCenter());
            assertEquals("right2", loadedTwoModel.getHeaderRight());
            assertEquals("left2", loadedTwoModel.getFooterLeft());
            assertEquals("center2", loadedTwoModel.getFooterCenter());
            assertEquals("right2", loadedTwoModel.getFooterRight());
            assertEquals("setting_two", loadedTwoModel.getBundleTimestamp());
        }
    }

    private HeaderFooterModel getHeaderFooter(String topLeft, String topCenter, String topRight,
                                              String bottomLeft, String bottomCenter, String bottomRight) {
        return HeaderFooterModel.builder()
                .headerLeft(topLeft)
                .headerCenter(topCenter)
                .headerRight(topRight)
                .footerLeft(bottomLeft)
                .footerCenter(bottomCenter)
                .footerRight(bottomRight)
                .build();
    }

}
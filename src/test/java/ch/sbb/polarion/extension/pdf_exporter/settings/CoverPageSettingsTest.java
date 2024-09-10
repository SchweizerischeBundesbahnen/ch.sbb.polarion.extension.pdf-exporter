package ch.sbb.polarion.extension.pdf_exporter.settings;

import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.coverpage.CoverPageModel;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.util.MediaUtils;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoverPageSettingsTest {
    @Mock
    PdfExporterPolarionService mockedPdfExporterPolarionService;

    @Test
    void testDefaultCss() {
        CoverPageModel defaultModel = new CoverPageSettings(new SettingsService(null, null, null), mockedPdfExporterPolarionService).defaultValues();
        assertNotNull(defaultModel.getTemplateHtml());
        assertNotNull(defaultModel.getTemplateCss());
    }

    @Test
    void testSettingDoesNotExist() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            GenericNamedSettings<CoverPageModel> coverPageSettings = new CoverPageSettings(mockedSettingsService, mockedPdfExporterPolarionService);

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
                CoverPageModel loadedModel = coverPageSettings.load(projectName, SettingId.fromName("Any setting name"));
            });
        }
    }

    @Test
    void testLoadCustomWhenSettingExists() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            GenericNamedSettings<CoverPageModel> coverPageSettings = new CoverPageSettings(mockedSettingsService, mockedPdfExporterPolarionService);

            String projectName = "test_project";

            ILocation mockDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);

            ILocation mockProjectLocation = mock(ILocation.class);
            when(mockProjectLocation.append(anyString())).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(projectName)).thenCallRealMethod();
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/test_project/")).thenReturn(mockProjectLocation);

            CoverPageModel customModel = CoverPageModel.builder().templateHtml("customHtml").templateCss("customCss").build();
            customModel.setBundleTimestamp("custom");
            when(mockedSettingsService.read(eq(mockProjectLocation), any())).thenReturn(customModel.serialize());

            when(mockedSettingsService.getLastRevision(mockProjectLocation)).thenReturn("345");
            when(mockedSettingsService.getPersistedSettingFileNames(mockProjectLocation)).thenReturn(List.of("Any setting name"));

            CoverPageModel loadedModel = coverPageSettings.load(projectName, SettingId.fromName("Any setting name"));
            assertEquals(customModel.getTemplateHtml(), loadedModel.getTemplateHtml());
            assertEquals(customModel.getTemplateCss(), loadedModel.getTemplateCss());
            assertEquals("custom", loadedModel.getBundleTimestamp());
        }
    }

    @Test
    void testNamedSettings() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            GenericNamedSettings<CoverPageModel> coverPageSettings = new CoverPageSettings(mockedSettingsService, mockedPdfExporterPolarionService);

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
            when(mockProjectLocation.append(contains("cover-page"))).thenReturn(mockProjectLocation);
            ILocation settingOneLocation = mock(ILocation.class);
            when(mockProjectLocation.append(contains(settingOne))).thenReturn(settingOneLocation);
            ILocation settingTwoLocation = mock(ILocation.class);
            when(mockProjectLocation.append(contains(settingTwo))).thenReturn(settingTwoLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            when(mockedSettingsService.getLastRevision(mockProjectLocation)).thenReturn("some_revision");

            CoverPageModel settingOneModel = CoverPageModel.builder().templateHtml("html_one").templateCss("css_one").build();
            settingOneModel.setBundleTimestamp("setting_one");
            when(mockedSettingsService.read(eq(settingOneLocation), any())).thenReturn(settingOneModel.serialize());

            CoverPageModel settingTwoModel = CoverPageModel.builder().templateHtml("html_two").templateCss("css_two").build();
            settingTwoModel.setBundleTimestamp("setting_two");
            when(mockedSettingsService.read(eq(settingTwoLocation), any())).thenReturn(settingTwoModel.serialize());

            CoverPageModel loadedOneModel = coverPageSettings.load(projectName, SettingId.fromName(settingOne));
            assertEquals("html_one", loadedOneModel.getTemplateHtml());
            assertEquals("css_one", loadedOneModel.getTemplateCss());
            assertEquals("setting_one", loadedOneModel.getBundleTimestamp());

            CoverPageModel loadedTwoModel = coverPageSettings.load(projectName, SettingId.fromName(settingTwo));
            assertEquals("html_two", loadedTwoModel.getTemplateHtml());
            assertEquals("css_two", loadedTwoModel.getTemplateCss());
            assertEquals("setting_two", loadedTwoModel.getBundleTimestamp());
        }
    }

    @Test
    void testPredefinedTemplates() {
        try (MockedConstruction<ZipInputStream> mockZipInputStream = Mockito.mockConstruction(ZipInputStream.class,
                     (mock, context) -> when(mock.getNextEntry()).thenReturn(
                             new ZipEntry("default/cover-page/test_1/template.html"),
                             new ZipEntry("default/cover-page/test_2/template.html"),
                             new ZipEntry("default/cover-page/test_3/template.html"),
                             null
                     )
        )) {
            try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
                SettingsService mockedSettingsService = mock(SettingsService.class);
                mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

                CoverPageSettings coverPageSettings = new CoverPageSettings(mockedSettingsService, mockedPdfExporterPolarionService);

                Collection<String> templates = coverPageSettings.getPredefinedTemplates();
                assertEquals(3, templates.size());
                assertTrue(templates.contains("test_1"));
                assertTrue(templates.contains("test_2"));
                assertTrue(templates.contains("test_3"));
            }
        }
    }

    @Test
    void testTemplateCssImages() {
        String coverPageTemplate = "test_template";
        UUID coverPageUuid = UUID.randomUUID();
        Set<String> images = new HashSet<>(Arrays.asList("image1.jpg", "image2.jpg", "image3.jpg"));

        CoverPageSettings coverPageSettings = mock(CoverPageSettings.class);
        when(coverPageSettings.getTemplateImageFileNames(coverPageTemplate)).thenReturn(images);
        when(coverPageSettings.persistTemplateImage(coverPageTemplate, "scope", "image2.jpg", coverPageUuid)).thenReturn(String.format("path/%s", "image2.jpg"));
        CoverPageModel model = new CoverPageModel("html", ".test { background: templateImage('image2.jpg') no-repeat center 100%; }");

        doCallRealMethod().when(coverPageSettings).processImagePaths(model, coverPageTemplate, "scope", coverPageUuid);

        coverPageSettings.processImagePaths(model, coverPageTemplate, "scope", coverPageUuid);

        assertTrue(model.getTemplateCss().contains(String.format("{{ IMAGE: 'path/%s'}}", "image2.jpg")));
        assertFalse(model.getTemplateCss().contains("templateImage('image2.jpg')"));
    }

    @Test
    void testDeleteCoverPageImages() {
        SettingsService spiedSettingsService = spy(new SettingsService(mock(IRepositoryService.class), mock(IProjectService.class), mock(ITrackerService.class)));
        doNothing().when(spiedSettingsService).delete(any());
        CoverPageSettings spiedCoverPageSettings = spy(new CoverPageSettings(spiedSettingsService, mockedPdfExporterPolarionService));
        UUID coverPageUuid = UUID.randomUUID();
        doReturn(coverPageUuid.toString()).when(spiedCoverPageSettings).getIdByName("scope", true, "coverPageName");
        doReturn("settingsFolder").when(spiedCoverPageSettings).getSettingsFolder();
        try (MockedStatic<ScopeUtils> mockedScopeUtils = mockStatic(ScopeUtils.class)) {
            ILocation mockedScopeLocation = mock(Location.class);
            ILocation mockedFolderLocation = mock(Location.class);
            IRepositoryReadOnlyConnection mockedReadOnlyConnection = mock(IRepositoryReadOnlyConnection.class);
            when(mockedPdfExporterPolarionService.getReadOnlyConnection(mockedFolderLocation)).thenReturn(mockedReadOnlyConnection);
            ILocation fileLocation = Location.getLocation(String.format("path/%s_background.jpg", coverPageUuid));
            when(mockedReadOnlyConnection.getSubLocations(mockedFolderLocation, false)).thenReturn(Collections.singletonList(fileLocation));
            when(mockedScopeLocation.append("settingsFolder")).thenReturn(mockedFolderLocation);
            mockedScopeUtils.when(() -> ScopeUtils.getContextLocation("scope")).thenReturn(mockedScopeLocation);

            spiedCoverPageSettings.deleteCoverPageImages("coverPageName", "scope");

            verify(spiedSettingsService).delete(fileLocation);
        }
    }

    @Test
    void testProcessImagePlaceholders() {
        try (MockedStatic<MediaUtils> mockedMediaUtils = mockStatic(MediaUtils.class)) {
            String content = "test_content";
            byte[] contentBytes = content.getBytes();
            String contentEncoded = Base64.getEncoder().encodeToString(contentBytes);

            mockedMediaUtils.when(() -> MediaUtils.getBinaryFileFromSvn("/path/test.jpg")).thenReturn(contentBytes);
            mockedMediaUtils.when(() -> MediaUtils.getImageFormat("/path/test.jpg")).thenCallRealMethod();

            String result = new CoverPageSettings(mock(SettingsService.class), mock(PdfExporterPolarionService.class))
                    .processImagePlaceholders(".test { background: {{ IMAGE: '/path/test.jpg'}} no-repeat center 100%; }");

            assertFalse(result.contains("background: {{ IMAGE: '/path/test.jpg'}}"));
            assertTrue(result.contains(String.format("background: url('data:image/jpeg;base64,%s')", contentEncoded)));
        }
    }

}

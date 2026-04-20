package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.test_extensions.CustomExtensionMock;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.CommentsRenderType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.LinkRoleDirection;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.settings.CoverPageSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.HeaderFooterSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.WebhooksSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentFileNameHelper;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.UiContext;
import com.polarion.alm.shared.api.transaction.internal.InternalWriteTransaction;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.ui.server.forms.extensions.IFormExtensionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@SuppressWarnings("unused")
@ExtendWith({MockitoExtension.class, CurrentContextExtension.class, PlatformContextMockExtension.class, TransactionalExecutorExtension.class, PdfExporterExtensionConfigurationExtension.class})
class PdfExporterFormExtensionTest {

    @CustomExtensionMock
    private InternalWriteTransaction transaction;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IProject project;

    private MockedConstruction<PdfExporterPolarionService> mockedPolarionServiceConstruction;
    private MockedConstruction<DocumentFileNameHelper> mockedFileNameHelperConstruction;

    @BeforeEach
    void beforeEach() {
        mockedPolarionServiceConstruction = mockConstruction(PdfExporterPolarionService.class, (mock, context) -> {
            when(mock.getProject("testProject")).thenReturn(project);
        });
        mockedFileNameHelperConstruction = mockConstruction(DocumentFileNameHelper.class, (mock, context) -> {
            when(mock.getDocumentFileName(any())).thenReturn("testFileName.pdf");
        });
    }

    @AfterEach
    void afterEach() {
        NamedSettingsRegistry.INSTANCE.getAll().clear();
        mockedPolarionServiceConstruction.close();
        mockedFileNameHelperConstruction.close();
    }

    @Test
    void testRender() {
        PdfExporterFormExtension extension = new PdfExporterFormExtension();
        IFormExtensionContext context = mock(IFormExtensionContext.class, RETURNS_DEEP_STUBS);

        IModule module = mock(IModule.class, RETURNS_DEEP_STUBS);
        when(module.getProject().getId()).thenReturn("testProject");
        when(module.getModuleLocation().getLocationPath()).thenReturn("testSpace/testModule");

        when(context.object().getOldApi()).thenReturn(module);
        when(transaction.context()).thenReturn(mock(UiContext.class, RETURNS_DEEP_STUBS));

        StylePackageSettings packageSettings = mock(StylePackageSettings.class);
        when(packageSettings.getFeatureName()).thenReturn(StylePackageSettings.FEATURE_NAME);
        when(packageSettings.defaultValues()).thenReturn(StylePackageModel.builder()
                .headersColor("blue")
                .build());

        CoverPageSettings coverPageSettings = mock(CoverPageSettings.class);
        when(coverPageSettings.getFeatureName()).thenReturn(CoverPageSettings.FEATURE_NAME);

        CssSettings cssSettings = mock(CssSettings.class);
        when(cssSettings.getFeatureName()).thenReturn(CssSettings.FEATURE_NAME);

        HeaderFooterSettings headerFooterSettings = mock(HeaderFooterSettings.class);
        when(headerFooterSettings.getFeatureName()).thenReturn(HeaderFooterSettings.FEATURE_NAME);

        LocalizationSettings localizationSettings = mock(LocalizationSettings.class);
        when(localizationSettings.getFeatureName()).thenReturn(LocalizationSettings.FEATURE_NAME);

        WebhooksSettings webhooksSettings = mock(WebhooksSettings.class);
        when(webhooksSettings.getFeatureName()).thenReturn(WebhooksSettings.FEATURE_NAME);

        NamedSettingsRegistry.INSTANCE.register(List.of(packageSettings, coverPageSettings, cssSettings, headerFooterSettings, localizationSettings, webhooksSettings));

        assertDoesNotThrow(() -> extension.render(context));
    }

    @Test
    void testAdjustRenderComments() {
        PdfExporterFormExtension extension = new PdfExporterFormExtension();
        String form = "<input id='render-comments'/><div id='render-comments-options' style='display: none'>"
                + "<input id='include-unreferenced-comments'/><input id='render-native-comments'/></div>";
        StylePackageModel packageModel = new StylePackageModel();
        assertEquals(form, extension.adjustRenderComments(form, packageModel));

        packageModel.setRenderComments(CommentsRenderType.OPEN);
        assertEquals("<input id='render-comments' checked/><div id='render-comments-options' style='display: flex'>"
                + "<input id='include-unreferenced-comments'/><input id='render-native-comments'/></div>", extension.adjustRenderComments(form, packageModel));

        packageModel.setRenderNativeComments(true);
        assertEquals("<input id='render-comments' checked/><div id='render-comments-options' style='display: flex'>"
                + "<input id='include-unreferenced-comments'/><input id='render-native-comments' checked/></div>", extension.adjustRenderComments(form, packageModel));

        packageModel.setIncludeUnreferencedComments(true);
        assertEquals("<input id='render-comments' checked/><div id='render-comments-options' style='display: flex'>"
                + "<input id='include-unreferenced-comments' checked/><input id='render-native-comments' checked/></div>", extension.adjustRenderComments(form, packageModel));
    }

    @Test
    void testAdjustLinkRolesEmptyRoles() {
        PdfExporterFormExtension extension = new PdfExporterFormExtension();
        String form = "<div class='roles-fields'><input id='selected-roles'/>"
                + "<div id='roles-wrapper' style='display: none; flex-direction: column;'>"
                + "<select id='roles-direction-selector'>"
                + "<option value='BOTH'>Both directions</option>"
                + "<option value='DIRECT'>Direct only</option>"
                + "<option value='REVERSE'>Reverse only</option>"
                + "</select>{ROLES_OPTIONS}</div></div>";
        StylePackageModel packageModel = new StylePackageModel();

        String result = extension.adjustLinkRoles(form, Collections.emptyList(), packageModel);
        assertTrue(result.contains("class='roles-fields' style='display: none;'"));
    }

    @Test
    void testAdjustLinkRolesWithRolesNoSelection() {
        PdfExporterFormExtension extension = new PdfExporterFormExtension();
        String form = "<div class='roles-fields'><input id='selected-roles'/>"
                + "<div id='roles-wrapper' style='display: none; flex-direction: column;'>"
                + "<select id='roles-direction-selector'>"
                + "<option value='BOTH'>Both directions</option>"
                + "<option value='DIRECT'>Direct only</option>"
                + "<option value='REVERSE'>Reverse only</option>"
                + "</select>{ROLES_OPTIONS}</div></div>";
        StylePackageModel packageModel = new StylePackageModel();

        String result = extension.adjustLinkRoles(form, List.of("has parent", "depends on"), packageModel);
        // Checkbox should NOT be checked
        assertFalse(result.contains("id='selected-roles' checked"));
        // Roles wrapper should remain hidden
        assertTrue(result.contains("style='display: none;"));
        // Role options should be generated without 'selected'
        assertTrue(result.contains("<option value='has parent' >has parent</option>"));
        assertTrue(result.contains("<option value='depends on' >depends on</option>"));
        // Default direction BOTH should be preselected
        assertTrue(result.contains("<option value='BOTH' selected>Both directions</option>"));
        assertFalse(result.contains("<option value='DIRECT' selected"));
        assertFalse(result.contains("<option value='REVERSE' selected"));
    }

    @Test
    void testAdjustLinkRolesWithSelectedRolesDefaultDirection() {
        PdfExporterFormExtension extension = new PdfExporterFormExtension();
        String form = "<div class='roles-fields'><input id='selected-roles'/>"
                + "<div id='roles-wrapper' style='display: none; flex-direction: column;'>"
                + "<select id='roles-direction-selector'>"
                + "<option value='BOTH'>Both directions</option>"
                + "<option value='DIRECT'>Direct only</option>"
                + "<option value='REVERSE'>Reverse only</option>"
                + "</select>{ROLES_OPTIONS}</div></div>";
        StylePackageModel packageModel = StylePackageModel.builder()
                .linkedWorkitemRoles(List.of("has parent"))
                .build();

        String result = extension.adjustLinkRoles(form, List.of("has parent", "depends on"), packageModel);
        // Checkbox should be checked
        assertTrue(result.contains("id='selected-roles' checked"));
        // Roles wrapper should be visible
        assertTrue(result.contains("id='roles-wrapper' style='display: flex;"));
        // "has parent" should be selected, "depends on" should not
        assertTrue(result.contains("<option value='has parent' selected>has parent</option>"));
        assertTrue(result.contains("<option value='depends on' >depends on</option>"));
        // Default direction BOTH should be preselected
        assertTrue(result.contains("<option value='BOTH' selected>Both directions</option>"));
    }

    @Test
    void testAdjustLinkRolesWithDirectDirection() {
        PdfExporterFormExtension extension = new PdfExporterFormExtension();
        String form = "<div class='roles-fields'><input id='selected-roles'/>"
                + "<div id='roles-wrapper' style='display: none; flex-direction: column;'>"
                + "<select id='roles-direction-selector'>"
                + "<option value='BOTH'>Both directions</option>"
                + "<option value='DIRECT'>Direct only</option>"
                + "<option value='REVERSE'>Reverse only</option>"
                + "</select>{ROLES_OPTIONS}</div></div>";
        StylePackageModel packageModel = StylePackageModel.builder()
                .linkedWorkitemRoles(List.of("has parent"))
                .linkRoleDirection(LinkRoleDirection.DIRECT.name())
                .build();

        String result = extension.adjustLinkRoles(form, List.of("has parent", "depends on"), packageModel);
        // DIRECT should be preselected
        assertTrue(result.contains("<option value='DIRECT' selected>Direct only</option>"));
        // BOTH should NOT be selected
        assertFalse(result.contains("<option value='BOTH' selected"));
        assertFalse(result.contains("<option value='REVERSE' selected"));
    }

    @Test
    void testAdjustLinkRolesWithReverseDirection() {
        PdfExporterFormExtension extension = new PdfExporterFormExtension();
        String form = "<div class='roles-fields'><input id='selected-roles'/>"
                + "<div id='roles-wrapper' style='display: none; flex-direction: column;'>"
                + "<select id='roles-direction-selector'>"
                + "<option value='BOTH'>Both directions</option>"
                + "<option value='DIRECT'>Direct only</option>"
                + "<option value='REVERSE'>Reverse only</option>"
                + "</select>{ROLES_OPTIONS}</div></div>";
        StylePackageModel packageModel = StylePackageModel.builder()
                .linkedWorkitemRoles(List.of("depends on"))
                .linkRoleDirection(LinkRoleDirection.REVERSE.name())
                .build();

        String result = extension.adjustLinkRoles(form, List.of("has parent", "depends on"), packageModel);
        // REVERSE should be preselected
        assertTrue(result.contains("<option value='REVERSE' selected>Reverse only</option>"));
        // Others should NOT be selected
        assertFalse(result.contains("<option value='BOTH' selected"));
        assertFalse(result.contains("<option value='DIRECT' selected"));
        // "depends on" should be selected, "has parent" should not
        assertTrue(result.contains("<option value='depends on' selected>depends on</option>"));
        assertTrue(result.contains("<option value='has parent' >has parent</option>"));
    }

}

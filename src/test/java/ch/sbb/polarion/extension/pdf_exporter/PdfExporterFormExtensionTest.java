package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.test_extensions.CustomExtensionMock;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.CommentsRenderType;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        String form = "<input id='render-comments'/>\"<input id='render-native-comments-container' style='display: none'\"/><input id='render-native-comments'/>";
        StylePackageModel packageModel = new StylePackageModel();
        assertEquals(form, extension.adjustRenderComments(form, packageModel));

        packageModel.setRenderComments(CommentsRenderType.OPEN);
        assertEquals("<input id='render-comments' checked/>\"<input id='render-native-comments-container'\"/><input id='render-native-comments'/>", extension.adjustRenderComments(form, packageModel));

        packageModel.setRenderNativeComments(true);
        assertEquals("<input id='render-comments' checked/>\"<input id='render-native-comments-container'\"/><input id='render-native-comments' checked/>", extension.adjustRenderComments(form, packageModel));
    }

}

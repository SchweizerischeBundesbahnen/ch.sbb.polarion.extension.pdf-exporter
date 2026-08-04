package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.test_extensions.CustomExtensionMock;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import ch.sbb.polarion.extension.pdf_exporter.configuration.PdfExporterExtensionConfigurationExtension;
import com.polarion.alm.shared.UiContext;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.transaction.internal.InternalWriteTransaction;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.ui.server.forms.extensions.IFormExtensionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The form extension contributes the fragment which imports the React side panel, and nothing else. What
 * the panel then offers is covered by the panel's own suite (ui/test/SidePanel*.test.tsx); what is asserted
 * here is that the fragment reaches the editor, addresses the bundle the build emits, and is contributed
 * only for a document.
 */
@SuppressWarnings("unused")
@ExtendWith({MockitoExtension.class, CurrentContextExtension.class, PlatformContextMockExtension.class, TransactionalExecutorExtension.class, PdfExporterExtensionConfigurationExtension.class})
class PdfExporterFormExtensionTest {

    @CustomExtensionMock
    private InternalWriteTransaction transaction;

    private final PdfExporterFormExtension extension = new PdfExporterFormExtension();

    @Test
    void testRenderContributesTheFragmentForADocument() {
        IFormExtensionContext context = mock(IFormExtensionContext.class, RETURNS_DEEP_STUBS);
        when(context.object().getOldApi()).thenReturn(mock(IModule.class, RETURNS_DEEP_STUBS));
        UiContext uiContext = mock(UiContext.class, RETURNS_DEEP_STUBS);
        when(transaction.context()).thenReturn(uiContext);

        assertDoesNotThrow(() -> extension.render(context));

        verify(uiContext.createHtmlFragmentBuilderFor().gwt()).html(contains("id=\"pdf-exporter-panel\""));
    }

    @Test
    void testRenderContributesNothingForAnythingButADocument() {
        SharedContext context = mock(SharedContext.class, RETURNS_DEEP_STUBS);
        HtmlFragmentBuilder builder = context.createHtmlFragmentBuilderFor().gwt();

        extension.renderForm(context, mock(IWorkItem.class, RETURNS_DEEP_STUBS));

        verify(builder, never()).html(org.mockito.ArgumentMatchers.anyString());
        verify(builder).finished();
    }

    @Test
    void testFragmentMountsTheSidePanelBundle() {
        String fragment = extension.getSidePanelFragment();

        // The host the React app attaches its shadow root to, and the call that does it.
        assertTrue(fragment.contains("id=\"pdf-exporter-panel\""));
        assertTrue(fragment.contains("assets/side-panel.js"));
        assertTrue(fragment.contains("module.mountSidePanel(\"#pdf-exporter-panel\")"));
        // The trigger stylesheet whose onload fires that import; the panel's own styles are in the bundle.
        assertTrue(fragment.contains("ui/css/starter.css"));
    }

    @Test
    void testFragmentCarriesTheBundleVersion() {
        String fragment = extension.getSidePanelFragment();

        // Substituted, whatever the version is - unresolved, the browser would cache one bundle forever.
        assertFalse(fragment.contains("{BUNDLE_VERSION}"));
        assertTrue(fragment.contains("side-panel.js?v="));
        // No manifest in a unit test, so the fallback is what appears here.
        assertEquals(1, fragment.split("\\?v=", -1).length - 1);
    }
}

package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.CommentsRenderType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class PdfExporterFormExtensionTest {

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

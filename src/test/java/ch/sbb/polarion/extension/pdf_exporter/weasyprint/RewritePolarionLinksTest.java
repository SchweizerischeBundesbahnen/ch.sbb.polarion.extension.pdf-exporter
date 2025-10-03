package ch.sbb.polarion.extension.pdf_exporter.weasyprint;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.PaperSize;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentDataFactory;
import ch.sbb.polarion.extension.pdf_exporter.weasyprint.base.BasePdfConverterTest;
import com.polarion.alm.tracker.model.IModule;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

class RewritePolarionLinksTest extends BasePdfConverterTest {

    @Test
    @SneakyThrows
    void rewritePolarionLinks() {
        // Arrange
        ExportParams params = ExportParams.builder()
                .projectId("test")
                .locationPath("testLocation")
                .orientation(Orientation.PORTRAIT)
                .paperSize(PaperSize.A4)
                .cutLocalUrls(true)
                .build();

        DocumentData<IModule> liveDoc = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("Multi-page Watermarked Document")
                .content(readHtmlResource("rewritePolarionLinks"))
                .lastRevision("42")
                .revisionPlaceholder("42")
                .build();
        //Act
        documentDataFactoryMockedStatic.when(() -> DocumentDataFactory.getDocumentData(eq(params), anyBoolean())).thenReturn(liveDoc);
        List<String> links = new ArrayList<>();
        byte[] result = converter.convertToPdf(params, null);
        try (PDDocument resultPDF = Loader.loadPDF(result)) {
            for (PDPage page : resultPDF.getPages()) {
                for (PDAnnotation annotation : page.getAnnotations()) {
                    if (annotation instanceof PDAnnotationLink link) {
                        if (link.getAction() instanceof PDActionURI uriAction) {
                            links.add(uriAction.getURI());
                        } else if (link.getAction() instanceof PDActionGoTo goToAction) {
                            links.add(goToAction.getDestination().toString());
                        } else if (link.getDestination() != null) {
                            PDDestination dest = link.getDestination();
                            if (dest instanceof PDNamedDestination namedDest) {
                                links.add(namedDest.getNamedDestination());
                            }
                        }
                    }
                }
            }
        }

        //Assert
        assertFalse(links.isEmpty());
        assertTrue(links.contains("work-item-anchor-elibrary/EL-165"));
        assertFalse(links.contains("/polarion/#/project/elibrary/workitem?id=EL-149")); // This link should be removed because it is a local link without an anchor
    }
}

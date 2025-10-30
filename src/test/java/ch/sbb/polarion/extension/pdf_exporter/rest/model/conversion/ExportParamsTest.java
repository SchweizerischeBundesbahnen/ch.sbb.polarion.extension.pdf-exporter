package ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExportParamsTest {

    @Test
    @SuppressWarnings("java:S5961") // Suppress "Refactor this method to reduce the number of assertions" warning
    void testOverwriteByStylePackage() {
        ExportParams params = ExportParams.builder()
                .coverPage("coverPage1")
                .headerFooter("headerFooter1")
                .css("css1")
                .localization("localization1")
                .webhooks("webhooks1")
                .headersColor("headersColor1")
                .paperSize(PaperSize.A4)
                .orientation(Orientation.LANDSCAPE)
                .pdfVariant(PdfVariant.PDF_A_2B)
                .imageDensity(ImageDensity.DPI_96)
                .fitToPage(true)
                .renderComments(CommentsRenderType.ALL)
                .watermark(true)
                .markReferencedWorkitems(true)
                .cutEmptyChapters(true)
                .cutEmptyWIAttributes(true)
                .cutLocalUrls(true)
                .followHTMLPresentationalHints(true)
                .metadataFields(List.of("meta1", "meta2"))
                .numberedListStyles("numberedListStyle1")
                .chapters(List.of("1", "2"))
                .language("language1")
                .linkedWorkitemRoles(List.of("role1", "role2"))
                .attachmentsFilter("attachmentFilter1")
                .testcaseFieldId("testcaseFieldId1")
                .embedAttachments(true)
                .build();

        StylePackageModel stylePackageModel = StylePackageModel.builder()
                .coverPage("coverPage2")
                .headerFooter("headerFooter2")
                .css("css2")
                .localization("localization2")
                .webhooks("webhooks2")
                .headersColor("headersColor2")
                .paperSize("A5")
                .orientation("PORTRAIT")
                .pdfVariant("PDF_A_3B")
                .imageDensity("DPI_300")
                .metadataFields("meta3,meta4")
                .customNumberedListStyles("numberedListStyle2")
                .specificChapters("3,4")
                .language("language2")
                .linkedWorkitemRoles(List.of("role3", "role4"))
                .attachmentsFilter("attachmentFilter2")
                .testcaseFieldId("testcaseFieldId2")
                .build();

        params.overwriteByStylePackage(stylePackageModel);

        assertEquals("coverPage2", params.getCoverPage());
        assertEquals("headerFooter2", params.getHeaderFooter());
        assertEquals("css2", params.getCss());
        assertEquals("localization2", params.getLocalization());
        assertEquals("webhooks2", params.getWebhooks());
        assertEquals("headersColor2", params.getHeadersColor());
        assertEquals(PaperSize.A5, params.getPaperSize());
        assertEquals(Orientation.PORTRAIT, params.getOrientation());
        assertEquals(PdfVariant.PDF_A_3B, params.getPdfVariant());
        assertEquals(ImageDensity.DPI_300, params.getImageDensity());
        assertNull(params.getRenderComments());
        assertFalse(params.isFitToPage());
        assertFalse(params.isWatermark());
        assertFalse(params.isMarkReferencedWorkitems());
        assertFalse(params.isCutEmptyChapters());
        assertFalse(params.isCutEmptyWIAttributes());
        assertFalse(params.isCutLocalUrls());
        assertFalse(params.isFollowHTMLPresentationalHints());
        assertEquals(List.of("meta3", "meta4"), params.getMetadataFields());
        assertEquals("numberedListStyle2", params.getNumberedListStyles());
        assertEquals(List.of("3", "4"), params.getChapters());
        assertEquals("language2", params.getLanguage());
        assertEquals(List.of("role3", "role4"), params.getLinkedWorkitemRoles());
        assertEquals("attachmentFilter2", params.getAttachmentsFilter());
        assertEquals("testcaseFieldId2", params.getTestcaseFieldId());
        assertFalse(params.isEmbedAttachments());
    }
}

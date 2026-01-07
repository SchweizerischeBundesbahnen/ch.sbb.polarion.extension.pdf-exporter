package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.ExportMetaInfoCallback;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.WidthValidationResult;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.WorkItemRefData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import lombok.SneakyThrows;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.VisibleForTesting;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class PdfWidthValidationService {

    public static final String TEST_WI_MASK = "{TEST_WI:%s}";

    private final PdfConverter pdfConverter;

    public PdfWidthValidationService() {
        this.pdfConverter = new PdfConverter();
    }

    public PdfWidthValidationService(PdfConverter pdfConverter) {
        this.pdfConverter = pdfConverter;
    }

    public WidthValidationResult validateWidth(ExportParams exportParams, int maxResults) {
        ExportMetaInfoCallback metaInfoCallback = new ExportMetaInfoCallback();
        List<WidthValidationResult.PageInfo> invalidPages = findInvalidPages(pdfConverter.convertToPdf(exportParams, metaInfoCallback), maxResults);
        WidthValidationResult result = new WidthValidationResult();
        if (!invalidPages.isEmpty()) {
            result.setInvalidPages(invalidPages);
            exportParams.setChapters(null);
            exportParams.setInternalContent(generateValidationPreparedContent(metaInfoCallback.getLinkedWorkItems()));
            byte[] pdfBytes = pdfConverter.convertToPdf(exportParams, null);
            invalidPages = findInvalidPages(pdfBytes, Integer.MAX_VALUE);
            if (!invalidPages.isEmpty()) {
                List<Integer> wiPositions = getPreparedContentWiPositions(pdfBytes);
                for (WidthValidationResult.PageInfo page : invalidPages) {
                    Integer wiPos = wiPositions.get(page.getNumber());
                    if (wiPos != -1) {
                        result.getSuspiciousWorkItems().add(metaInfoCallback.getLinkedWorkItems().get(wiPos));
                    }
                }
            }
        }
        return result;
    }

    @SneakyThrows
    @VisibleForTesting
    public List<WidthValidationResult.PageInfo> findInvalidPages(byte[] pdf, int maxResults) {
        List<WidthValidationResult.PageInfo> result = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            int currentIssuesFound = 0;
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = MediaUtils.pdfPageToImage(document, page);
                if (!MediaUtils.checkAllRightPixelsAreWhite(image)) {
                    result.add(
                            WidthValidationResult.PageInfo.builder()
                                    .number(page)
                                    .content(Base64.getEncoder().encodeToString(MediaUtils.toPng(image)))
                                    .build());
                    if (++currentIssuesFound == maxResults) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Inserts special mark before and page brake after internal content of each work item.
     * This means that there will be no more than one work item on the page.
     */
    private String generateValidationPreparedContent(List<WorkItemRefData> items) {
        StringBuilder sb = new StringBuilder();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                sb.append(String.format(TEST_WI_MASK, i))
                        .append(items.get(i).toInternalContent())
                        .append(String.format("<div contentEditable=\"false\" data-is-landscape=\"false\" id=\"polarion_wiki macro name=page_break;params=uid=%s\"></div>", i + 1));
            }
        }
        return sb.toString();
    }

    /**
     * Returns an array where each its item shows the index of work item contained on it.
     * So, for example, the result {-1, -1, 0, 0, 0, 1, 1} means:
     * - there are 7 pages
     * - three first pages do not contain any work item
     * - pages 3, 4 & 5 contain work item N1
     * - pages 6 & 7 contain work item N2
     * Note: Work item number/index above is the work item order during parsing final html. Real work item id (e.g. EL-3671) isn't important at this moment, we will do this before sending the result to the client.
     */
    @SneakyThrows
    private List<Integer> getPreparedContentWiPositions(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            List<Integer> result = new ArrayList<>(Collections.nCopies(document.getNumberOfPages(), -1));
            int wiPos = -1;
            for (int i = 1; i <= result.size(); i++) {
                PDFTextStripper textStripper = new PDFTextStripper();
                textStripper.setStartPage(i);
                textStripper.setEndPage(i);
                String pageText = textStripper.getText(document);
                if (pageText.contains(String.format(TEST_WI_MASK, wiPos + 1))) {
                    wiPos++;
                }
                result.set(i - 1, wiPos);
            }
            return result;
        }
    }
}

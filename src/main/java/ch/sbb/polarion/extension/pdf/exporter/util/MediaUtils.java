package ch.sbb.polarion.extension.pdf.exporter.util;

import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf.exporter.service.PdfExporterPolarionService;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@UtilityClass
public class MediaUtils {
    private static final Logger logger = Logger.getLogger(MediaUtils.class);
    private static final int RIGHT_WHITE_AREA_PX = 30;
    private static final int PDF_TO_PNG_DPI = 72;
    private static final String IMG_FORMAT_PNG = "png";

    @SneakyThrows
    public BufferedImage pdfPageToImage(PDDocument document, int page) {
        return new PDFRenderer(document).renderImageWithDPI(page, PDF_TO_PNG_DPI);
    }

    @SuppressWarnings("squid:S109") // ignore 8, 16 & 255 constants creation proposal
    public boolean checkAllRightPixelsAreWhite(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = width - RIGHT_WHITE_AREA_PX; x < width; x++) {
                int pixel = img.getRGB(x, y);
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                if (red != 255 || green != 255 || blue != 255) {
                    return false;
                }
            }
        }
        return true;
    }

    @SneakyThrows
    public byte[] toPng(BufferedImage image) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, IMG_FORMAT_PNG, os);
        return os.toByteArray();
    }

    public String getImageFormat(@NotNull String imagePath) {
        if (imagePath.endsWith(".gif")) {
            return "image/gif";
        } else if (imagePath.endsWith(".png")) {
            return "image/png";
        } else {
            return "image/jpeg";
        }
    }

    public boolean compareImages(BufferedImage image1, BufferedImage image2) {
        if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) {
            return false;
        }

        for (int y = 0; y < image1.getHeight(); y++) {
            for (int x = 0; x < image1.getWidth(); x++) {
                if (image1.getRGB(x, y) != image2.getRGB(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }

    @SneakyThrows
    public byte[] overwriteFirstPageWithTitle(byte[] destinationPdf, byte[] titlePdf) {
        ByteArrayOutputStream modifiedTitleOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream modifiedContentOutputStream = new ByteArrayOutputStream();
        try (PDDocument titleDoc = Loader.loadPDF(titlePdf);
             PDDocument contentDoc = Loader.loadPDF(destinationPdf)) {
            while (titleDoc.getNumberOfPages() > 1) { //remove all pages except the first one from title pdf
                titleDoc.removePage(1);
            }
            titleDoc.save(modifiedTitleOutputStream);
            contentDoc.removePage(0);
            contentDoc.save(modifiedContentOutputStream);
        }

        ByteArrayOutputStream resultOutputStream = new ByteArrayOutputStream();
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.addSource(new RandomAccessReadBuffer(modifiedTitleOutputStream.toByteArray()));
        merger.addSource(new RandomAccessReadBuffer(modifiedContentOutputStream.toByteArray()));
        merger.setDestinationStream(resultOutputStream);
        merger.mergeDocuments(null);

        return resultOutputStream.toByteArray();
    }

    @SuppressWarnings("java:S1168")
    public byte[] getBinaryFileFromJar(@NotNull String filePath) {
        try (InputStream is = ScopeUtils.class.getClassLoader().getResourceAsStream(filePath)) {
            return is != null ? is.readAllBytes() : null;
        } catch (IOException e) {
            logger.error("Error reading template image content from: " + filePath, e);
            return null;
        }
    }

    @SuppressWarnings("java:S1168")
    public byte[] getBinaryFileFromSvn(@NotNull String path) {
        ILocation location = Location.getLocationWithRepository("default", path);
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            IRepositoryReadOnlyConnection readOnlyConnection = new PdfExporterPolarionService().getReadOnlyConnection(location);
            if (!readOnlyConnection.exists(location)) {
                logger.warn("Location does not exist: " + location.getLocationPath());
                return null;
            }

            try (InputStream inputStream = readOnlyConnection.getContent(location)) {
                return inputStream.readAllBytes();
            } catch (Exception e) {
                logger.error("Error reading content from: " + location.getLocationPath(), e);
                return null;
            }
        });
    }
}

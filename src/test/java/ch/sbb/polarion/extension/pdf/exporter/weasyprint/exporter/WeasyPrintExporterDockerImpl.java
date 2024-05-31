package ch.sbb.polarion.extension.pdf.exporter.weasyprint.exporter;

import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;

public class WeasyPrintExporterDockerImpl implements WeasyPrintExporter {

    private static final String DOCKER_IMAGE_NAME = "weasyprint_test";
    private static final String INPUT_FILE_PATH = "/tmp/input.html";
    private static final String OUTPUT_FILE_PATH = "/tmp/output.pdf";

    @Override
    public byte[] exportToPdf(@NotNull String html, @NotNull WeasyPrintOptions weasyPrintOptions) {
        ImageFromDockerfile image = new ImageFromDockerfile(DOCKER_IMAGE_NAME, false).withFileFromClasspath("Dockerfile", "/weasyprint/Dockerfile");
        try (GenericContainer<?> container = new GenericContainer<>(image)) {
            FileWaitStrategy fileWaitStrategy = new FileWaitStrategy(OUTPUT_FILE_PATH);
            container.withCommand(INPUT_FILE_PATH, OUTPUT_FILE_PATH)
                    .withCopyToContainer(Transferable.of(html), INPUT_FILE_PATH)
                    .waitingFor(fileWaitStrategy)
                    .start();
            return fileWaitStrategy.getPdfFileData();
        }
    }
}

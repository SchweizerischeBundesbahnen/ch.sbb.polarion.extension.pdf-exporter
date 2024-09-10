package ch.sbb.polarion.extension.pdf_exporter.util;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class PdfGenerationLog {

    private final StringBuilder builder = new StringBuilder();

    public void log(String message) {
        builder.append(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)).append(" ").append(message).append(System.lineSeparator());
    }

    public String getLog() {
        return builder.toString();
    }
}

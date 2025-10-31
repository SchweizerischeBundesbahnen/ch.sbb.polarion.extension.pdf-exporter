package ch.sbb.polarion.extension.pdf_exporter.exception;

public class WeasyPrintServiceHealthException extends RuntimeException {
    public WeasyPrintServiceHealthException(String message, Throwable cause) {
        super(message, cause);
    }
}

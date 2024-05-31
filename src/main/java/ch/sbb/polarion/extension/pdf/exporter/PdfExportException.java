package ch.sbb.polarion.extension.pdf.exporter;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PdfExportException extends RuntimeException {

    private final String command;
    private final int exitStatus;
    private final byte[] out;
    private final byte[] err;

    @Override
    public String getMessage() {
        return "Process (" + this.command + ") exited with status code " + this.exitStatus + ":" + System.lineSeparator() + new String(err);
    }
}

package ch.sbb.polarion.extension.pdf.exporter.util;

public interface FileResourceProvider {

    byte[] getResourceAsBytes(String resource);

}

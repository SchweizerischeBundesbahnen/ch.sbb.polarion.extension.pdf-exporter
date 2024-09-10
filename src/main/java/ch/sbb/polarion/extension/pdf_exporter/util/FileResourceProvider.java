package ch.sbb.polarion.extension.pdf_exporter.util;

public interface FileResourceProvider {

    byte[] getResourceAsBytes(String resource);

}

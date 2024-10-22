package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.util.configuration.WeasyPrintStatusProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionsUtils {

    public static @Nullable String getLatestCompatibleVersionWeasyPrintService() {
        try (InputStream input = WeasyPrintStatusProvider.class.getClassLoader().getResourceAsStream("versions.properties")) {
            if (input == null) {
                return null;
            }

            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty("weasyprint-service.version");
        } catch (IOException e) {
            return null;
        }
    }

}

package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@UtilityClass
public class VersionUtils {
    private static final String VERSION_FILE = "versions.properties";

    public static @Nullable String getLatestCompatibleVersionWeasyPrintService() {
        return getVersionProperty("weasyprint-service.version");
    }

    public static @Nullable String getCurrentCompatibleVersionPolarion() {
        return getVersionProperty("polarion.version");
    }

    private static @Nullable String getVersionProperty(String key) {
        try (InputStream input = VersionUtils.class.getClassLoader().getResourceAsStream(VERSION_FILE)) {
            if (input == null) {
                return null;
            }

            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty(key);
        } catch (IOException e) {
            return null;
        }
    }
}

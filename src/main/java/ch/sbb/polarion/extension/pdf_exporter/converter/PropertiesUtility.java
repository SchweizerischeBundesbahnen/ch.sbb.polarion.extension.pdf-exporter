package ch.sbb.polarion.extension.pdf_exporter.converter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtility {
    private static final String JOBS_PROPERTIES_FILE = "/pdf-converter-jobs.properties";
    private static final String TIMEOUT_FINISHED_JOBS = "jobs.timeout.finished.minutes";
    private static final String TIMEOUT_IN_PROGRESS_JOBS = "jobs.timeout.in-progress.minutes";

    private final Properties pdfConverterJobsProps;

    public PropertiesUtility() {
        pdfConverterJobsProps = loadProperties(JOBS_PROPERTIES_FILE);
    }

    public int getInProgressJobTimeout() {
        return getIntProperty(TIMEOUT_IN_PROGRESS_JOBS);
    }

    public int getFinishedJobTimeout() {
        return getIntProperty(TIMEOUT_FINISHED_JOBS);
    }

    private int getIntProperty(String propName) {
        String propValue = pdfConverterJobsProps.getProperty(propName);
        if (propValue == null) {
            throw new IllegalStateException("Missing property: " + propName);
        }
        return Integer.parseInt(propValue);
    }

    private Properties loadProperties(String resourcePath) {
        try(InputStream propsInputStream = PdfConverterJobsCleaner.class.getResourceAsStream(resourcePath)) {
            if (propsInputStream == null) {
                throw new IllegalStateException("Properties file is not found: " + JOBS_PROPERTIES_FILE);
            }
            Properties props = new Properties();
            props.load(propsInputStream);
            return props;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load properties file: " + JOBS_PROPERTIES_FILE, e);
        }
    }
}

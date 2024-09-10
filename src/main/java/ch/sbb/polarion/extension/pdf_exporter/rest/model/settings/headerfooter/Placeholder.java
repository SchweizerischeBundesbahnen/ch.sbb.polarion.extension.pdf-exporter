package ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter;

import java.util.Arrays;

public enum Placeholder {
    PROJECT_NAME,
    DOCUMENT_ID,
    DOCUMENT_TITLE,
    DOCUMENT_REVISION,
    REVISION,
    REVISION_AND_BASELINE_NAME,
    BASELINE_NAME,
    PAGE_NUMBER,
    PAGES_TOTAL_COUNT,
    PRODUCT_NAME,
    PRODUCT_VERSION,
    TIMESTAMP;

    public static boolean contains(String value) {
        return Arrays.stream(Placeholder.values()).anyMatch(placeholder -> placeholder.name().equals(value));
    }

}

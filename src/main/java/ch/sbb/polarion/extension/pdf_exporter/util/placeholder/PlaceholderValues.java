package ch.sbb.polarion.extension.pdf_exporter.util.placeholder;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.Placeholder;
import ch.sbb.polarion.extension.pdf_exporter.util.PolarionTypes;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.platform.persistence.IEnumOption;
import lombok.Builder;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@Builder
@ToString
public class PlaceholderValues {
    public static final String DOC_LANGUAGE_FIELD = "docLanguage";
    public static final String DOC_TIME_ZONE_FIELD = "docTimeZone";

    private String productName;
    private String productVersion;
    private String projectName;
    private String documentId;
    private String documentTitle;
    private String documentRevision;
    private String revision;
    private String revisionAndBaseLineName;
    private String baseLineName;
    private String sbbCustomRevision;

    @Builder.Default
    private String pageNumber = "<span class='page-number'><span class='number'></span>";
    @Builder.Default
    private String pagesTotalCount = "<span class='pages'></span></span>";
    @Builder.Default
    private String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(Instant.now());
    @Builder.Default
    private Map<String, String> customVariables = new HashMap<>();

    public @NotNull Map<String, String> getAllVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put(Placeholder.PROJECT_NAME.name(), projectName);
        variables.put(Placeholder.DOCUMENT_ID.name(), documentId);
        variables.put(Placeholder.DOCUMENT_TITLE.name(), documentTitle);
        variables.put(Placeholder.DOCUMENT_REVISION.name(), documentRevision);
        variables.put(Placeholder.REVISION.name(), revision);
        variables.put(Placeholder.REVISION_AND_BASELINE_NAME.name(), revisionAndBaseLineName);
        variables.put(Placeholder.BASELINE_NAME.name(), baseLineName);
        variables.put(Placeholder.PAGE_NUMBER.name(), pageNumber);
        variables.put(Placeholder.PAGES_TOTAL_COUNT.name(), pagesTotalCount);
        variables.put(Placeholder.PRODUCT_NAME.name(), productName);
        variables.put(Placeholder.PRODUCT_VERSION.name(), productVersion);
        variables.put(Placeholder.TIMESTAMP.name(), timestamp);

        variables.putAll(customVariables);
        return variables;
    }

    public @NotNull Map<String, String> getDefinedVariables() {
        Map<String, String> allVariables = getAllVariables();

        allVariables.entrySet()
                .removeIf(entry -> entry.getValue() == null);

        return allVariables;
    }

    @SuppressWarnings("java:S1166")
    public void addCustomVariables(@NotNull IModule document, @NotNull Set<String> customFields) {
        Locale locale = getDocumentLocale(document);
        TimeZone timeZone = getDocumentTimeZone(document);
        customFields.forEach(fieldName -> {
            Object fieldValue;
            try {
                fieldValue = document.getCustomField(fieldName);
            } catch (IllegalArgumentException e) {
                fieldValue = document.getValue(fieldName);
            }
            if (fieldValue != null) {
                customVariables.put(fieldName, PolarionTypes.convertFieldValueToString(fieldValue, locale, timeZone));
            } else {
                customVariables.put(fieldName, ""); // Replace "unknown" fields by blank string
            }
        });
    }

    private @NotNull Locale getDocumentLocale(@NotNull IModule document) {
        Object docLanguage = document.getCustomField(DOC_LANGUAGE_FIELD);
        if (docLanguage instanceof IEnumOption enumOption) {
            return Locale.forLanguageTag(enumOption.getId());
        } else {
            return Locale.getDefault();
        }
    }

    private @NotNull TimeZone getDocumentTimeZone(@NotNull IModule document) {
        Object timeZone = document.getCustomField(DOC_TIME_ZONE_FIELD);
        if (timeZone instanceof String string) {
            return TimeZone.getTimeZone(string);
        } else {
            return TimeZone.getDefault();
        }
    }

}

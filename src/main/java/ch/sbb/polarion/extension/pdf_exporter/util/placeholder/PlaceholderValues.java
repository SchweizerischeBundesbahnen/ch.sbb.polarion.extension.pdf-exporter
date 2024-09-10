package ch.sbb.polarion.extension.pdf_exporter.util.placeholder;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.headerfooter.Placeholder;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.internal.model.TypeOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.core.util.types.Currency;
import com.polarion.core.util.types.DateOnly;
import com.polarion.core.util.types.Text;
import com.polarion.core.util.types.TimeOnly;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.spi.CustomTypedList;
import lombok.Builder;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

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

    public Map<String, String> getAllVariables() {
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

    @SuppressWarnings("java:S1166")
    public void addCustomVariables(@NotNull IModule document, @NotNull Set<String> customFields) {
        Locale locale = getDocumentLocale(document);
        String timeZone = getDocumentTimeZone(document);
        customFields.forEach(fieldName -> {
            Object fieldValue;
            try {
                fieldValue = document.getCustomField(fieldName);
            } catch (IllegalArgumentException e) {
                fieldValue = document.getValue(fieldName);
            }
            if (fieldValue != null) {
                customVariables.put(fieldName, convertSingleValueToString(fieldValue, locale, timeZone));
            } else {
                customVariables.put(fieldName, ""); // Replace "unknown" fields by blank string
            }
        });
    }

    private Locale getDocumentLocale(@NotNull IModule document) {
        Object docLanguage = document.getCustomField(DOC_LANGUAGE_FIELD);
        if (docLanguage instanceof IEnumOption enumOption) {
            return Locale.forLanguageTag(enumOption.getId());
        } else {
            return Locale.getDefault();
        }
    }

    private String getDocumentTimeZone(@NotNull IModule document) {
        Object timeZone = document.getCustomField(DOC_TIME_ZONE_FIELD);
        return (timeZone instanceof String string) ? string : null;
    }

    private String convertSingleValueToString(Object fieldValue, Locale locale, String timeZone) {
        if (fieldValue instanceof TypeOpt typeOpt) {
            return typeOpt.getName();
        } else if (fieldValue instanceof Text text) {
            return text.convertToHTML().getContent();
        } else if (fieldValue instanceof DateOnly dateOnly) {
            return DateFormat.getDateInstance(DateFormat.LONG, locale).format(dateOnly.getDate());
        } else if (fieldValue instanceof TimeOnly timeOnly) {
            return convertToTime(timeOnly.getDate(), locale, timeZone);
        } else if (fieldValue instanceof Date date) {
            return convertToDateTime(date, locale, timeZone);
        } else if (fieldValue instanceof Currency currency) {
            return currency.getValue().toString();
        } else if (fieldValue instanceof IEnumOption enumOption) {
            return enumOption.getName() != null ? enumOption.getName() : enumOption.getId();
        } else if (fieldValue instanceof CustomTypedList customTypedList) {
            return convertListToString(customTypedList, locale, timeZone);
        } else if (fieldValue instanceof IUser user) {
            return user.getLabel();
        }
        return fieldValue.toString();
    }

    @NotNull
    private String convertToTime(Date fieldValue, Locale locale, String timeZone) {
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.LONG, locale);
        return formatForTimeZone(fieldValue, timeZone, timeFormat);
    }

    @NotNull
    private String convertToDateTime(Date fieldValue, Locale locale, String timeZone) {
        DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
        return formatForTimeZone(fieldValue, timeZone, dateTimeFormat);
    }

    @NotNull
    private String formatForTimeZone(Date fieldValue, String timeZone, DateFormat timeFormat) {
        if (timeZone != null) {
            timeFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
        }
        return timeFormat.format(fieldValue);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private String convertListToString(CustomTypedList list, Locale locale, String timeZone) {
        return (String) list.stream()
                .map(n -> convertSingleValueToString(n, locale, timeZone))
                .collect(Collectors.joining(", "));
    }
}

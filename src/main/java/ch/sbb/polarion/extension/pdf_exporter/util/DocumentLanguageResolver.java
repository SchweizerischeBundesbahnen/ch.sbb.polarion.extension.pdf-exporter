package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.persistence.IEnumOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Resolves the document language (ISO 639-1) used to fill the {@code lang} attribute of the exported HTML so
 * WeasyPrint can hyphenate. Centralizes the LiveDoc custom-field language reading that was otherwise duplicated across
 * the converter, the cover page and the utility endpoints.
 */
public final class DocumentLanguageResolver {

    // A permissive ISO 639-1 style language tag: 2-3 letters, optional subtags (e.g. 'de', 'en', 'de-CH').
    private static final Pattern ISO_LANGUAGE_TAG = Pattern.compile("^[A-Za-z]{2,3}(-[A-Za-z0-9]+)*$");
    private static final Logger logger = Logger.getLogger(DocumentLanguageResolver.class);

    private DocumentLanguageResolver() {
    }

    /**
     * Reads a custom field of the given module as a language code: the id of an enum option, or a non-blank plain
     * string value. Returns {@code null} when the field is unset or of another type.
     */
    public static @Nullable String readLanguageCode(@NotNull IModule module, @NotNull String fieldId) {
        Object value = module.getCustomField(fieldId);
        if (value instanceof IEnumOption enumOption) {
            return enumOption.getId();
        }
        return (value instanceof String string && !string.isBlank()) ? string : null;
    }

    /**
     * Resolves the document's language for injection into {@code <html lang>}. The feature is opt-in: the style
     * package must name the LiveDoc custom field to read ({@code languageCustomField}); when it is unset nothing is
     * read and {@code null} is returned, so existing exports are unaffected. The field value must be a valid ISO 639-1
     * style tag - any other value (free text, quotes, placeholder tokens) is ignored so it can never break or be
     * injected into the attribute. Reading is guarded so an unknown field id cannot fail the whole export.
     */
    public static @Nullable String resolve(@NotNull DocumentData<? extends IUniqueObject> documentData, @NotNull ExportParams exportParams) {
        String fieldId = exportParams.getLanguageCustomField();
        if (StringUtils.isEmpty(fieldId) || !(documentData.getDocumentObject() instanceof IModule module)) {
            return null;
        }
        String language;
        try {
            language = readLanguageCode(module, fieldId);
        } catch (Exception e) {
            logger.warn(String.format("Could not read document language custom field '%s': %s", fieldId, e.getMessage()));
            return null;
        }
        return language != null && ISO_LANGUAGE_TAG.matcher(language).matches() ? language : null;
    }
}

package ch.sbb.polarion.extension.pdf_exporter.util;

import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.handler.DoNothingCSSParseExceptionCallback;
import com.helger.css.reader.CSSReader;
import com.helger.css.reader.CSSReaderSettings;
import com.helger.css.reader.errorhandler.DoNothingCSSParseErrorHandler;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Reads the CSS of an export into a stylesheet for the rules of the page and of its content. The export embeds its fonts
 * and images as data URLs, megabytes which the parser would read through at every export and no rule here needs.
 */
@UtilityClass
public class ExportStylesheet {

    /** A resource embedded as a data URL, quoted or not. Base64 has no parenthesis and no quote. */
    private static final Pattern DATA_URL = Pattern.compile("url\\(\\s*(['\"]?)data:[^)'\"]*\\1\\s*\\)", Pattern.CASE_INSENSITIVE);

    /** The stylesheet of the CSS without the resources it embeds, each left as an empty {@code url()}, or nothing. */
    public @Nullable CascadingStyleSheet read(@NotNull String css) {
        return CSSReader.readFromStringReader(DATA_URL.matcher(css).replaceAll("url()"), new CSSReaderSettings()
                .setBrowserCompliantMode(true)
                .setCustomErrorHandler(new DoNothingCSSParseErrorHandler())
                .setCustomExceptionHandler(new DoNothingCSSParseExceptionCallback()));
    }
}

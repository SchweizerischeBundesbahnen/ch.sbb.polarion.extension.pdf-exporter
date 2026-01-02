package ch.sbb.polarion.extension.pdf_exporter.util.exporter;

import ch.sbb.polarion.extension.generic.regex.RegexMatcher;
import com.polarion.alm.shared.rt.document.PartIdGeneratorImpl;
import com.polarion.alm.shared.rt.parts.impl.readonly.WikiBlockPart;
import com.polarion.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import static ch.sbb.polarion.extension.pdf_exporter.util.exporter.Constants.*;

public class CustomWikiBlockPart extends WikiBlockPart {

    public CustomWikiBlockPart(@NotNull WikiBlockPart object) {
        super(object.getElement(), new PartIdGeneratorImpl());
    }

    @Override
    public void setRenderedContentHtml(@NotNull String renderedContentHtml) {
        super.setRenderedContentHtml(convertPd4mlPageBreakTags(renderedContentHtml));
    }

    @NotNull
    @VisibleForTesting
    String convertPd4mlPageBreakTags(@NotNull String html) {
        // Match <pd4ml:page.break> with optional pageformat="rotate" pageformat="reset" attributes
        // Case-insensitive for both attribute name and value (using (?i) flag)
        return RegexMatcher.get("(?i)<pd4ml:page\\.break(?:[^>]*pageformat\\s*=\\s*[\"']?(?<format>rotate|reset)[\"']?[^>]*)?[^>]*>")
                .replace(html, regexEngine -> {
                    String pageFormat = StringUtils.getEmptyIfNull(regexEngine.group("format")).toLowerCase();
                    if ("rotate".equals(pageFormat)) {
                        return PAGE_BREAK_MARK + ROTATE_BELOW_MARK;
                    } else if ("reset".equals(pageFormat)) {
                        return PAGE_BREAK_MARK + RESET_BELOW_MARK;
                    } else {
                        return PAGE_BREAK_MARK + BREAK_BELOW_MARK;
                    }
                });
    }
}

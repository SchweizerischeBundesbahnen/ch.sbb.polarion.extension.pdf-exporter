package ch.sbb.polarion.extension.pdf_exporter.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;

public class LiveReportTOCGenerator extends AbstractTOCGenerator {

    @Override
    protected @Nullable String getId(@NotNull Element heading) {
        return heading.id();
    }

    @Override
    protected @Nullable String getNumber(@NotNull Element heading) {
        return heading.text().split(" ")[0]; // extract chapter number, e.g., "1.1.1";
    }

    @Override
    protected @NotNull String getText(@NotNull Element heading) {
        String number = getNumber(heading);
        if (number == null) {
            return heading.text();
        }
        return heading.text().replaceFirst(number, "").trim(); // extract text without number
    }

}

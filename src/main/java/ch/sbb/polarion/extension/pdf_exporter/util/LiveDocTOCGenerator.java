package ch.sbb.polarion.extension.pdf_exporter.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.List;

public class LiveDocTOCGenerator extends AbstractTOCGenerator {

    @Override
    protected @Nullable String getId(@NotNull Element heading) {
        List<Node> childNodes = heading.childNodes();
        for (Node childNode : childNodes) {
            if (childNode instanceof Element childElement) {
                String childId = childElement.id();
                if (!childId.isEmpty()) {
                    return childId;
                }
            }
        }
        return null;
    }

    @Override
    protected @Nullable String getNumber(@NotNull Element heading) {
        String text = heading.text();
        int firstSpace = text.indexOf(' ');
        if (firstSpace != -1) {
            return text.substring(0, firstSpace);
        } else {
            return "";
        }
    }

    @Override
    protected @NotNull String getText(@NotNull Element heading) {
        String text = heading.text();
        int firstSpace = text.indexOf(' ');
        if (firstSpace != -1) {
            return text.substring(firstSpace + 1);
        } else {
            return "";
        }
    }

}

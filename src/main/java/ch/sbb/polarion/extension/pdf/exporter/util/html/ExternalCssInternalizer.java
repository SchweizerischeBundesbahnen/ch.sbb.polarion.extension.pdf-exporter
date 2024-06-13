package ch.sbb.polarion.extension.pdf.exporter.util.html;

import ch.sbb.polarion.extension.pdf.exporter.util.FileResourceProvider;

import java.util.Map;
import java.util.Optional;

public class ExternalCssInternalizer implements LinkInternalizer {
    private static final String DATA_PRECEDENCE = "data-precedence";
    private final FileResourceProvider fileResourceProvider;

    public ExternalCssInternalizer(FileResourceProvider fileResourceProvider) {
        this.fileResourceProvider = fileResourceProvider;
    }

    @Override
    public Optional<String> inline(Map<String, String> attributes) {
        if (!"stylesheet".equals(attributes.get("rel"))
                || !attributes.containsKey("href")) {
            return Optional.empty();
        }
        StringBuilder inlinedContent = new StringBuilder("<style");
        if (attributes.containsKey(DATA_PRECEDENCE)) {
            inlinedContent.append(" ")
                    .append(DATA_PRECEDENCE)
                    .append("=\"")
                    .append(attributes.get(DATA_PRECEDENCE))
                    .append("\"");
        }
        inlinedContent.append(">");
        String url = attributes.get("href");
        inlinedContent.append(new String(fileResourceProvider.getResourceAsBytes(url)));
        inlinedContent.append("</style>");

        return Optional.of(inlinedContent.toString());
    }
}

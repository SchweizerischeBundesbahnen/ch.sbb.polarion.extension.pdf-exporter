package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.core.config.IListStyleProvider;
import com.polarion.core.config.impl.SystemValueReader;
import com.polarion.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PdfExporterListStyleProvider implements IListStyleProvider {
    private static final int MAX_LEVELS = 9;

    private final SystemValueReader reader = SystemValueReader.getInstance();
    @Nullable
    private final String customConfiguration;

    public PdfExporterListStyleProvider(@Nullable String customConfiguration) {
        this.customConfiguration = customConfiguration;
    }

    @NotNull
    public String getStyle() {
        String property = this.getConfiguration();
        if (property == null) {
            return "";
        } else {
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < property.length(); ++i) {
                for (int j = 0; j < i + 1; ++j) {
                    result.append("ol ");
                }

                result.append("{list-style-type: ");
                char listType = property.charAt(i);
                switch (listType) {
                    case '1':
                        result.append("decimal");
                        break;
                    case 'A':
                        result.append("upper-alpha");
                        break;
                    case 'I':
                        result.append("upper-roman");
                        break;
                    case 'a':
                        result.append("lower-alpha");
                        break;
                    case 'i':
                        result.append("lower-roman");
                        break;
                    default:
                        result.append("none");
                }

                result.append(";}");
                result.append("\n");
            }

            return result.toString();
        }
    }

    @Nullable
    private String getConfiguration() {
        String property = StringUtils.isEmptyTrimmed(customConfiguration)
                ? reader.readString("com.siemens.polarion.document.listStyle", null)
                : customConfiguration;

        if (StringUtils.isEmptyTrimmed(property)) {
            return null;
        } else {
            StringBuilder result = new StringBuilder(property);
            while (result.length() < MAX_LEVELS) {
                result.append(property);
            }
            return result.substring(0, MAX_LEVELS);
        }
    }

    public char getType(int level) {
        String property = this.getConfiguration();
        return property != null && level < property.length() ? property.charAt(level) : '1';
    }
}

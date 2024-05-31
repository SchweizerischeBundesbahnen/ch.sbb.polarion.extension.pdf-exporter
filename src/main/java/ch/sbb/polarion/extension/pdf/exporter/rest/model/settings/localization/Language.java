package ch.sbb.polarion.extension.pdf.exporter.rest.model.settings.localization;

import com.polarion.core.util.StringUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum Language {
    EN("English"),
    DE("Deutsch"),
    FR("Français"),
    IT("Italiano");

    @Getter
    private final String value;

    Language(String value) {
        this.value = value;
    }

    @NotNull
    @SuppressWarnings("squid:S2259") //value checked for null inside isEmptyTrimmed
    public static Language valueOfIgnoreCase(@Nullable String value) {
        if (StringUtils.isEmptyTrimmed(value)) {
            return EN;
        }
        return valueOf(value.toUpperCase());
    }
}

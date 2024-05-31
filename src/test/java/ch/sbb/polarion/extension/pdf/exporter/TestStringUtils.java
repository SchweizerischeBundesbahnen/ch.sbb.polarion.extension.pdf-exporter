package ch.sbb.polarion.extension.pdf.exporter;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class TestStringUtils {

    public static @NotNull String removeLineEndings(@NotNull String input) {
        return input.replaceAll("\\r\\n|\\r|\\n", "");
    }

}

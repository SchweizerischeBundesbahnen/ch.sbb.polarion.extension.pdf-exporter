package ch.sbb.polarion.extension.pdf_exporter;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class TestStringUtils {

    public static @NotNull String removeLineEndings(@NotNull String input) {
        return input.replaceAll("\\r\\n|\\r|\\n", "");
    }

    public static @NotNull String removeSpaces(@NotNull String input) {
        return input.replaceAll(" ", "");
    }

    public static @NotNull String removeNonsensicalSymbols(@NotNull String input) {
        String result = removeLineEndings(input);
        return removeSpaces(result);
    }
}

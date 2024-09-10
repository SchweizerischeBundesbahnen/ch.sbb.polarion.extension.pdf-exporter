package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class ExceptionHandler {

    public static <T> T handleTransactionIllegalStateException(@NotNull IllegalStateException exception, T defaultValue) {
        if ("There is already a transaction.".equals(exception.getMessage())) {
            // This is an expected situation - if yet no settings were persisted we can't persist them here
            // because we need to open write transaction for this, but this is not possible being already in opened read only transaction
            return defaultValue;
        } else {
            // Other exceptions just re-throw
            throw exception;
        }
    }
}

package ch.sbb.polarion.extension.pdf_exporter.util;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExceptionHandlerTest {

    @Test
    void handleTransactionIllegalStateException() {
        assertThat(ExceptionHandler.handleTransactionIllegalStateException(new IllegalStateException("There is already a transaction."), Collections.emptyList())).isInstanceOf(Collections.emptyList().getClass());
        assertThat(ExceptionHandler.handleTransactionIllegalStateException(new IllegalStateException("There is already a transaction."), "")).isInstanceOf(String.class);
        IllegalStateException illegalStateException = new IllegalStateException("Test");
        assertThrows(IllegalStateException.class, () -> {
            ExceptionHandler.handleTransactionIllegalStateException(illegalStateException, "");
        });
    }
}
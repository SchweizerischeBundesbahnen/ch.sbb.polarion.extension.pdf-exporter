package ch.sbb.polarion.extension.pdf_exporter.service;

import ch.sbb.polarion.extension.pdf_exporter.exception.BaselineExecutionException;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.PolarionUtils;
import com.polarion.alm.shared.api.utils.RunnableWithResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolarionBaselineExecutorTest {

    @Test
    void testExecuteInBaseline() {
        PolarionUtils polarionUtils = new PolarionUtils() {
            @Override
            public @Nullable <T> T executeInBaseline(@NotNull String s, @NotNull RunnableWithResult<T> runnableWithResult) {
                return runnableWithResult.run();
            }

            @Override
            public @Nullable <T> T executeOutsideBaseline(@NotNull RunnableWithResult<T> runnableWithResult) {
                return null;
            }

            @Override
            public @NotNull String convertToAscii(@Nullable String s) {
                return "";
            }

            @Override
            public @NotNull String convertToAscii(@Nullable String s, @Nullable String s1) {
                return "";
            }
        };

        ReadOnlyTransaction mockReadOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(mockReadOnlyTransaction.utils()).thenReturn(polarionUtils);

        assertEquals("valueWithoutBaseline", PolarionBaselineExecutor.executeInBaseline(null, mockReadOnlyTransaction, () -> "valueWithoutBaseline"));

        assertEquals("valueInBaseline", PolarionBaselineExecutor.executeInBaseline("1234", mockReadOnlyTransaction, () -> "valueInBaseline"));
        assertThrows(BaselineExecutionException.class, () -> PolarionBaselineExecutor.executeInBaseline("5678", mockReadOnlyTransaction, this::testCallableWithException));
        assertThrows(NullPointerException.class, () -> PolarionBaselineExecutor.executeInBaseline("5678", mockReadOnlyTransaction, this::testCallableWithRuntimeException));
    }

    private String testCallableWithException() throws IOException {
        throw new IOException("io exception");
    }

    private String testCallableWithRuntimeException() {
        throw new NullPointerException("null pointer exception");
    }

}

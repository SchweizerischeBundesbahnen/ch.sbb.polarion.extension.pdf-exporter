package ch.sbb.polarion.extension.pdf_exporter.service;

import ch.sbb.polarion.extension.pdf_exporter.exception.BaselineExecutionException;
import ch.sbb.polarion.extension.pdf_exporter.test_extensions.CustomExtensionMock;
import ch.sbb.polarion.extension.pdf_exporter.test_extensions.TransactionalExecutorExtension;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(TransactionalExecutorExtension.class)
class PolarionBaselineExecutorTest {

    @CustomExtensionMock
    private InternalReadOnlyTransaction internalReadOnlyTransactionMock;

    @Test
    void testExecuteInBaseline() {
        assertEquals("valueWithoutBaseline", PolarionBaselineExecutor.executeInBaseline(null, internalReadOnlyTransactionMock, () -> "valueWithoutBaseline"));

        assertEquals("valueInBaseline", PolarionBaselineExecutor.executeInBaseline("1234", internalReadOnlyTransactionMock, () -> "valueInBaseline"));
        assertThrows(BaselineExecutionException.class, () -> PolarionBaselineExecutor.executeInBaseline("5678", internalReadOnlyTransactionMock, this::testCallableWithException));
        assertThrows(NullPointerException.class, () -> PolarionBaselineExecutor.executeInBaseline("5678", internalReadOnlyTransactionMock, this::testCallableWithRuntimeException));
    }

    private String testCallableWithException() throws IOException {
        throw new IOException("io exception");
    }

    private String testCallableWithRuntimeException() {
        throw new NullPointerException("null pointer exception");
    }

}

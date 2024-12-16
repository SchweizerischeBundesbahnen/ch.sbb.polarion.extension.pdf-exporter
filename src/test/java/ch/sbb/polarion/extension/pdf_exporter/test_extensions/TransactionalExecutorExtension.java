package ch.sbb.polarion.extension.pdf_exporter.test_extensions;

import com.polarion.alm.shared.api.transaction.RunnableInReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

public class TransactionalExecutorExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private InternalReadOnlyTransaction internalReadOnlyTransactionMock;

    private MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        internalReadOnlyTransactionMock = mock(InternalReadOnlyTransaction.class);

        transactionalExecutorMockedStatic = mockStatic(TransactionalExecutor.class);
        transactionalExecutorMockedStatic.when(() -> TransactionalExecutor.executeSafelyInReadOnlyTransaction(any()))
                .thenAnswer(invocation -> {
                    RunnableInReadOnlyTransaction<?> runnable = invocation.getArgument(0);
                    return runnable.run(internalReadOnlyTransactionMock);
                });

        CustomExtensionMockInjector.inject(context, internalReadOnlyTransactionMock);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (transactionalExecutorMockedStatic != null) {
            transactionalExecutorMockedStatic.close();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(ExtensionContext.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return extensionContext;
    }

}

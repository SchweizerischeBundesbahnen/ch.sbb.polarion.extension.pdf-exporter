package ch.sbb.polarion.extension.pdf_exporter.test_extensions;

import com.polarion.alm.shared.api.transaction.RunnableInReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.RunnableWithResult;
import com.polarion.alm.shared.api.utils.internal.InternalPolarionUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TransactionalExecutorExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private MockedStatic<TransactionalExecutor> transactionalExecutorMockedStatic;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        transactionalExecutorMockedStatic = mockStatic(TransactionalExecutor.class);

        InternalReadOnlyTransaction internalReadOnlyTransactionMock = mock(InternalReadOnlyTransaction.class);
        transactionalExecutorMockedStatic.when(() -> TransactionalExecutor.executeSafelyInReadOnlyTransaction(any())).thenAnswer(invocation -> {
            RunnableInReadOnlyTransaction<?> runnable = invocation.getArgument(0);
            return runnable.run(internalReadOnlyTransactionMock);
        });

        InternalPolarionUtils internalPolarionUtils = mock(InternalPolarionUtils.class);
        when(internalPolarionUtils.executeInBaseline(any(), any())).thenAnswer(invocation -> {
            RunnableWithResult<?> runnableWithResult = invocation.getArgument(1);
            return runnableWithResult.run();
        });
        when(internalReadOnlyTransactionMock.utils()).thenReturn(internalPolarionUtils);

        CustomExtensionMockInjector.inject(context, internalPolarionUtils);
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

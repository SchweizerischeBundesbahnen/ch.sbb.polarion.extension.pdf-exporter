package ch.sbb.polarion.extension.pdf_exporter.service;

import ch.sbb.polarion.extension.pdf_exporter.exception.BaselineExecutionException;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;

public class PolarionBaselineExecutor {

    public static <T> T executeInBaseline(@Nullable String baselineRevision, @NotNull ReadOnlyTransaction transaction, @NotNull Callable<T> callable) {
        if (baselineRevision == null) {
            return callCallable(callable);
        } else {
            return transaction.utils().executeInBaseline(baselineRevision, () -> callCallable(callable));
        }
    }

    private static <T> T callCallable(@NotNull Callable<T> callable) {
        try {
            return callable.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BaselineExecutionException("Error during callable execution", e);
        }
    }

}
package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.util.BundleJarsPrioritizingRunnable;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class MediaUtilsExecutorFailureTest {

    // Regression test for the same failure mode as ch.sbb.polarion.extension.docx-exporter issue #207:
    // when BundleJarsPrioritizingRunnableExecutor catches an internal exception (classloader / reflection / serialization failure),
    // it returns Map.of(ERROR_KEY, e) with no PARAM_RESULT. MediaUtils must degrade to null instead of triggering a
    // NullPointerException on Optional.orElse().
    @Test
    void getMimeTypeUsingTika_whenExecutorReturnsErrorMap_returnsNullWithoutNpe() {
        Map<String, Object> errorOnly = Map.of(BundleJarsPrioritizingRunnable.ERROR_KEY, new RuntimeException("executor failed"));

        try (MockedStatic<BundleJarsPrioritizingRunnable> mocked = mockStatic(BundleJarsPrioritizingRunnable.class)) {
            mocked.when(() -> BundleJarsPrioritizingRunnable.executeCached(any(), any())).thenReturn(errorOnly);

            assertNull(MediaUtils.getMimeTypeUsingTikaByContent("some.svg", new byte[]{1, 2, 3}));
            assertNull(MediaUtils.getMimeTypeUsingTikaByResourceName("some.svg", null));
        }
    }
}

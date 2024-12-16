package ch.sbb.polarion.extension.pdf_exporter.test_extensions;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Field;

public class CustomExtensionMockInjector {

    public static <T> void inject(ExtensionContext context, T what) throws IllegalAccessException {
        Object testInstance = context.getTestInstance().orElse(null);
        if (testInstance != null) {
            for (Field field : testInstance.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(CustomExtensionMock.class) && field.getType().isAssignableFrom(what.getClass())) {
                    field.setAccessible(true);
                    field.set(testInstance, what);
                }
            }
        }
    }

}

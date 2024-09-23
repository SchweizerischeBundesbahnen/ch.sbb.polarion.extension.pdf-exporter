package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CustomResourceUrlResolverTest {

    @Test
    @SneakyThrows
    void replaceImagesUrlUnderscoreAndSpaceReplacementTest() {
        CustomResourceUrlResolver resolver = mock(CustomResourceUrlResolver.class);
        InputStream is = mock(InputStream.class);
        when(resolver.resolve(any())).thenCallRealMethod();
        when(resolver.resolveImpl(any())).thenReturn(is);
        try (InputStream is1 = resolver.resolve("http://localhost/some path/img%5Fname.png")) {
            try (InputStream is2 = verify(resolver, times(1)).resolveImpl(new URL("http://localhost/some%20path/img_name.png"))){
                assertEquals(is, is1);
                assertNull(is2);
            }
        }
    }
}
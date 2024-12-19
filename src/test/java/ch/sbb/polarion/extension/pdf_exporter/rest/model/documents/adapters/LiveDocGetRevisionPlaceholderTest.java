package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters;

import com.polarion.alm.tracker.model.IModule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiveDocGetRevisionPlaceholderTest {

    private static Stream<Arguments> getRevisionPlaceholderParameters() {
        return Stream.of(
                Arguments.of(null, null, "lastRevision", "lastRevision"),
                Arguments.of("revision", null, "lastRevision", "revision"),
                Arguments.of("revision", "customFieldRevision", "lastRevision", "customFieldRevision"),
                Arguments.of(null, "customFieldRevision", "lastRevision", "customFieldRevision")
        );
    }

    @ParameterizedTest
    @MethodSource("getRevisionPlaceholderParameters")
    void testGetRevisionPlaceholder(String revision, String customFieldRevision, String lastRevision, String expectedRevisionPlaceholder) {
        IModule module = mock(IModule.class);
        when(module.getRevision()).thenReturn(revision);
        when(module.getLastRevision()).thenReturn(lastRevision);
        when(module.getCustomField("docRevision")).thenReturn(customFieldRevision);

        LiveDocAdapter liveDocAdapter = new LiveDocAdapter(module);
        String revisionPlaceholder = liveDocAdapter.getRevisionPlaceholder();

        assertEquals(expectedRevisionPlaceholder, revisionPlaceholder);
    }
}

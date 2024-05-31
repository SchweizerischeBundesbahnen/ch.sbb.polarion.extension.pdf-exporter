package ch.sbb.polarion.extension.pdf.exporter.util;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiveDocHelperTest {
    private LiveDocHelper liveDocHelper;

    @BeforeEach
    public void setup() {
        liveDocHelper = new LiveDocHelper(null);
    }

    @ParameterizedTest
    @MethodSource("getDocumentStatusParameters")
    void shouldGetDocumentStatus(String revision, String customFieldRevision, String lastRevision, String expectedStatus) {
        IModule module = mock(IModule.class);
        LiveDocHelper.DocumentData documentData = LiveDocHelper.DocumentData.builder()
                .lastRevision(lastRevision)
                .document(module)
                .build();
        when(module.getCustomField("docRevision")).thenReturn(customFieldRevision);

        String documentStatus = liveDocHelper.getDocumentStatus(revision, documentData);
        assertThat(documentStatus).isEqualTo(expectedStatus);
    }

    private static Stream<Arguments> getDocumentStatusParameters() {
        return Stream.of(
                Arguments.of("revision", "customFieldRevision", "lastRevision", "revision"),
                Arguments.of(null, "customFieldRevision",  "lastRevision", "customFieldRevision"),
                Arguments.of(null, null, "lastRevision", "lastRevision"));
    }


    @ParameterizedTest
    @MethodSource("getDocumentLocationParameters")
    void shouldGetDocumentLocation(String location, String revision) {
        ILocation documentLocation = liveDocHelper.getDocumentLocation(location, revision);
        assertThat(documentLocation.getLocationPath()).isEqualTo(location);
        assertThat(documentLocation.getRevision()).isEqualTo(revision);
    }

    @Test
    void shouldCreatePath() {
        assertThat(liveDocHelper.createPath("testProjectId", "testLocationPath")).isEqualTo("testProjectId/testLocationPath");
    }

    private static Stream<Arguments> getDocumentLocationParameters() {
        return Stream.of(
                Arguments.of("testLocation", "testRevision"),
                Arguments.of("testLocation", null));
    }
}
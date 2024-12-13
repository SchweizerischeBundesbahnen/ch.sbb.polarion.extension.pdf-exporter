package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.LiveDocId;
import com.polarion.alm.tracker.model.IModule;
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

class DocumentDataHelperTest {
    private DocumentDataHelper documentDataHelper;

    @BeforeEach
    public void setup() {
        documentDataHelper = new DocumentDataHelper(null);
    }

    @ParameterizedTest
    @MethodSource("getRevisionPlaceholderParameters")
    void shouldGetRevisionPlaceholder(String revision, String customFieldRevision, String lastRevision, String expectedStatus) {
        IModule module = mock(IModule.class);
        DocumentData<IModule> documentData = DocumentData.creator(DocumentType.LIVE_DOC, module)
                .id(LiveDocId.from("testProjectId", "_default", "testDocumentId"))
                .title("testTitle")
                .lastRevision(lastRevision)
                .revisionPlaceholder(lastRevision)
                .build();
        when(module.getCustomField("docRevision")).thenReturn(customFieldRevision);

        String documentStatus = documentDataHelper.getCalculatedPlaceholderRevision(revision, documentData);
        assertThat(documentStatus).isEqualTo(expectedStatus);
    }

    private static Stream<Arguments> getRevisionPlaceholderParameters() {
        return Stream.of(
                Arguments.of("revision", "customFieldRevision", "lastRevision", "revision"),
                Arguments.of(null, "customFieldRevision",  "lastRevision", "customFieldRevision"),
                Arguments.of(null, null, "lastRevision", "lastRevision"));
    }


    @ParameterizedTest
    @MethodSource("getDocumentLocationParameters")
    void shouldGetDocumentLocation(String location, String revision) {
        ILocation documentLocation = documentDataHelper.getDocumentLocation(location, revision);
        assertThat(documentLocation.getLocationPath()).isEqualTo(location);
        assertThat(documentLocation.getRevision()).isEqualTo(revision);
    }

    @Test
    void shouldCreatePath() {
        assertThat(documentDataHelper.createPath("testProjectId", "testLocationPath")).isEqualTo("testProjectId/testLocationPath");
    }

    private static Stream<Arguments> getDocumentLocationParameters() {
        return Stream.of(
                Arguments.of("testLocation", "testRevision"),
                Arguments.of("testLocation", null));
    }
}

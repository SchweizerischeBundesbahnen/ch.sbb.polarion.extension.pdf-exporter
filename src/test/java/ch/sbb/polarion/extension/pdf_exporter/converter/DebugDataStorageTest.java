package ch.sbb.polarion.extension.pdf_exporter.converter;

import ch.sbb.polarion.extension.pdf_exporter.model.DebugData;
import ch.sbb.polarion.extension.pdf_exporter.util.DebugDataStorage;
import com.polarion.platform.security.ISecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebugDataStorageTest {

    private static final String TEST_USER = "testUser";
    private static final String TEST_JOB_ID = "testJobId";

    @Mock
    private ISecurityService securityService;

    @BeforeEach
    void setUp() {
        DebugDataStorage.clear();
    }

    @AfterEach
    void tearDown() {
        DebugDataStorage.clear();
    }

    @Test
    void shouldSetAndGetCurrentJobId() {
        assertThat(DebugDataStorage.getCurrentJobId()).isNull();

        DebugDataStorage.setCurrentJobId(TEST_JOB_ID);
        assertThat(DebugDataStorage.getCurrentJobId()).isEqualTo(TEST_JOB_ID);

        DebugDataStorage.clearCurrentJobId();
        assertThat(DebugDataStorage.getCurrentJobId()).isNull();
    }

    @Test
    void shouldNotSetNullJobId() {
        DebugDataStorage.setCurrentJobId(TEST_JOB_ID);
        DebugDataStorage.setCurrentJobId(null);
        assertThat(DebugDataStorage.getCurrentJobId()).isNull();
    }

    @Test
    void shouldStoreAndRetrieveDebugData() {
        when(securityService.getCurrentUser()).thenReturn(TEST_USER);

        DebugData debugData = DebugData.builder()
                .originalHtml("<html>original</html>")
                .processedHtml("<html>processed</html>")
                .timingReport("timing report")
                .user(TEST_USER)
                .createdAt(Instant.now())
                .documentTitle("Test Document")
                .build();

        DebugDataStorage.store(TEST_JOB_ID, debugData);

        assertThat(DebugDataStorage.exists(TEST_JOB_ID)).isTrue();
        assertThat(DebugDataStorage.size()).isEqualTo(1);

        DebugData retrieved = DebugDataStorage.get(TEST_JOB_ID, securityService);
        assertThat(retrieved).isEqualTo(debugData);
    }

    @Test
    void shouldThrowWhenDebugDataNotFound() {
        assertThatThrownBy(() -> DebugDataStorage.get("unknownJobId", securityService))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("unknownJobId");
    }

    @Test
    void shouldThrowWhenUserMismatch() {
        when(securityService.getCurrentUser()).thenReturn("otherUser");

        DebugData debugData = DebugData.builder()
                .user(TEST_USER)
                .createdAt(Instant.now())
                .build();

        DebugDataStorage.store(TEST_JOB_ID, debugData);

        assertThatThrownBy(() -> DebugDataStorage.get(TEST_JOB_ID, securityService))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(TEST_JOB_ID);
    }

    @Test
    void shouldStoreForCurrentJob() {
        when(securityService.getCurrentUser()).thenReturn(TEST_USER);

        DebugDataStorage.setCurrentJobId(TEST_JOB_ID);
        DebugDataStorage.storeForCurrentJob(
                "<html>original</html>",
                "<html>processed</html>",
                "timing report",
                TEST_USER,
                "Test Document"
        );

        assertThat(DebugDataStorage.exists(TEST_JOB_ID)).isTrue();

        DebugData retrieved = DebugDataStorage.get(TEST_JOB_ID, securityService);
        assertThat(retrieved.originalHtml()).isEqualTo("<html>original</html>");
        assertThat(retrieved.processedHtml()).isEqualTo("<html>processed</html>");
        assertThat(retrieved.timingReport()).isEqualTo("timing report");
        assertThat(retrieved.documentTitle()).isEqualTo("Test Document");
    }

    @Test
    void shouldNotStoreWhenNoCurrentJobId() {
        DebugDataStorage.storeForCurrentJob(
                "<html>original</html>",
                "<html>processed</html>",
                "timing report",
                TEST_USER,
                "Test Document"
        );

        assertThat(DebugDataStorage.size()).isZero();
    }

    @Test
    void shouldRemoveDebugData() {
        DebugData debugData = DebugData.builder()
                .user(TEST_USER)
                .createdAt(Instant.now())
                .build();

        DebugDataStorage.store(TEST_JOB_ID, debugData);
        assertThat(DebugDataStorage.exists(TEST_JOB_ID)).isTrue();

        DebugDataStorage.remove(TEST_JOB_ID);
        assertThat(DebugDataStorage.exists(TEST_JOB_ID)).isFalse();
    }

    @Test
    void shouldCleanupExpiredData() {
        DebugData debugData = DebugData.builder()
                .user(TEST_USER)
                .createdAt(Instant.now().minusSeconds(120))
                .build();

        DebugDataStorage.store(TEST_JOB_ID, debugData);
        assertThat(DebugDataStorage.size()).isEqualTo(1);

        DebugDataStorage.cleanupExpired(1);

        assertThat(DebugDataStorage.exists(TEST_JOB_ID)).isFalse();
        assertThat(DebugDataStorage.size()).isZero();
    }

    @Test
    void shouldNotCleanupNonExpiredData() {
        DebugData debugData = DebugData.builder()
                .user(TEST_USER)
                .createdAt(Instant.now())
                .build();

        DebugDataStorage.store(TEST_JOB_ID, debugData);

        DebugDataStorage.cleanupExpired(60);

        assertThat(DebugDataStorage.exists(TEST_JOB_ID)).isTrue();
    }

    @Test
    void shouldClearAllData() {
        DebugDataStorage.setCurrentJobId(TEST_JOB_ID);
        DebugDataStorage.store(TEST_JOB_ID, DebugData.builder()
                .user(TEST_USER)
                .createdAt(Instant.now())
                .build());
        DebugDataStorage.store("job2", DebugData.builder()
                .user(TEST_USER)
                .createdAt(Instant.now())
                .build());

        assertThat(DebugDataStorage.size()).isEqualTo(2);
        assertThat(DebugDataStorage.getCurrentJobId()).isEqualTo(TEST_JOB_ID);

        DebugDataStorage.clear();

        assertThat(DebugDataStorage.size()).isZero();
        assertThat(DebugDataStorage.getCurrentJobId()).isNull();
    }
}

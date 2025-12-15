package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.generic.test_extensions.CustomExtensionMock;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.pdf_exporter.converter.DebugData;
import ch.sbb.polarion.extension.pdf_exporter.converter.DebugDataStorage;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.jobs.DebugDataResponse;
import com.polarion.platform.security.ISecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import javax.ws.rs.core.Response;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class ConverterDebugEndpointsTest {

    private static final String TEST_USER = "testUser";
    private static final String TEST_JOB_ID = "testJobId";

    @CustomExtensionMock
    private ISecurityService securityService;

    private ConverterInternalController controller;

    @BeforeEach
    void setUp() {
        controller = new ConverterInternalController(null, null, null, null, null);
        DebugDataStorage.clear();
    }

    private void setupUserMock() {
        when(securityService.getCurrentUser()).thenReturn(TEST_USER);
    }

    @AfterEach
    void tearDown() {
        DebugDataStorage.clear();
    }

    @Test
    void getDebugDataInfo_shouldReturnNotFoundWhenNoData() {
        try (Response response = controller.getDebugDataInfo("unknownJob")) {
            assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(response.getEntity()).isInstanceOf(DebugDataResponse.class);
            DebugDataResponse debugResponse = (DebugDataResponse) response.getEntity();
            assertThat(debugResponse.available()).isFalse();
            assertThat(debugResponse.message()).contains("Debug data not found");
        }
    }

    @Test
    void getDebugDataInfo_shouldReturnDataWhenAvailable() {
        setupUserMock();
        Instant now = Instant.now();
        DebugData debugData = DebugData.builder()
                .originalHtml("<html>original</html>")
                .processedHtml("<html>processed</html>")
                .timingReport("timing report")
                .user(TEST_USER)
                .createdAt(now)
                .documentTitle("Test Document")
                .build();
        DebugDataStorage.store(TEST_JOB_ID, debugData);

        try (Response response = controller.getDebugDataInfo(TEST_JOB_ID)) {
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getEntity()).isInstanceOf(DebugDataResponse.class);
            DebugDataResponse debugResponse = (DebugDataResponse) response.getEntity();
            assertThat(debugResponse.available()).isTrue();
            assertThat(debugResponse.documentTitle()).isEqualTo("Test Document");
            assertThat(debugResponse.hasOriginalHtml()).isTrue();
            assertThat(debugResponse.hasProcessedHtml()).isTrue();
            assertThat(debugResponse.hasTimingReport()).isTrue();
        }
    }

    @Test
    void getDebugDataInfo_shouldReturnNotFoundForDifferentUser() {
        setupUserMock();
        DebugData debugData = DebugData.builder()
                .user("otherUser")
                .createdAt(Instant.now())
                .build();
        DebugDataStorage.store(TEST_JOB_ID, debugData);

        try (Response response = controller.getDebugDataInfo(TEST_JOB_ID)) {
            assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    @Test
    void getDebugOriginalHtml_shouldReturnHtmlWhenAvailable() {
        setupUserMock();
        DebugData debugData = DebugData.builder()
                .originalHtml("<html>original content</html>")
                .user(TEST_USER)
                .createdAt(Instant.now())
                .build();
        DebugDataStorage.store(TEST_JOB_ID, debugData);

        try (Response response = controller.getDebugOriginalHtml(TEST_JOB_ID)) {
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getEntity()).isEqualTo("<html>original content</html>");
        }
    }

    @Test
    void getDebugOriginalHtml_shouldReturnNotFoundWhenEmpty() {
        setupUserMock();
        DebugData debugData = DebugData.builder()
                .originalHtml("")
                .user(TEST_USER)
                .createdAt(Instant.now())
                .build();
        DebugDataStorage.store(TEST_JOB_ID, debugData);

        try (Response response = controller.getDebugOriginalHtml(TEST_JOB_ID)) {
            assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(response.getEntity()).isEqualTo("Original HTML is not available for this job");
        }
    }

    @Test
    void getDebugOriginalHtml_shouldReturnNotFoundWhenNoData() {
        try (Response response = controller.getDebugOriginalHtml("unknownJob")) {
            assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(response.getEntity()).isEqualTo("Debug data not found");
        }
    }

    @Test
    void getDebugProcessedHtml_shouldReturnHtmlWhenAvailable() {
        setupUserMock();
        DebugData debugData = DebugData.builder()
                .processedHtml("<html>processed content</html>")
                .user(TEST_USER)
                .createdAt(Instant.now())
                .build();
        DebugDataStorage.store(TEST_JOB_ID, debugData);

        try (Response response = controller.getDebugProcessedHtml(TEST_JOB_ID)) {
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getEntity()).isEqualTo("<html>processed content</html>");
        }
    }

    @Test
    void getDebugProcessedHtml_shouldReturnNotFoundWhenNull() {
        setupUserMock();
        DebugData debugData = DebugData.builder()
                .user(TEST_USER)
                .createdAt(Instant.now())
                .build();
        DebugDataStorage.store(TEST_JOB_ID, debugData);

        try (Response response = controller.getDebugProcessedHtml(TEST_JOB_ID)) {
            assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(response.getEntity()).isEqualTo("Processed HTML is not available for this job");
        }
    }

    @Test
    void getDebugProcessedHtml_shouldReturnNotFoundWhenNoData() {
        try (Response response = controller.getDebugProcessedHtml("unknownJob")) {
            assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(response.getEntity()).isEqualTo("Debug data not found");
        }
    }

    @Test
    void getDebugTimingReport_shouldReturnReportWhenAvailable() {
        setupUserMock();
        DebugData debugData = DebugData.builder()
                .timingReport("Timing Report Content\nLine 2")
                .user(TEST_USER)
                .createdAt(Instant.now())
                .build();
        DebugDataStorage.store(TEST_JOB_ID, debugData);

        try (Response response = controller.getDebugTimingReport(TEST_JOB_ID)) {
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
            assertThat(response.getEntity()).isEqualTo("Timing Report Content\nLine 2");
        }
    }

    @Test
    void getDebugTimingReport_shouldReturnNotFoundWhenEmpty() {
        setupUserMock();
        DebugData debugData = DebugData.builder()
                .timingReport("")
                .user(TEST_USER)
                .createdAt(Instant.now())
                .build();
        DebugDataStorage.store(TEST_JOB_ID, debugData);

        try (Response response = controller.getDebugTimingReport(TEST_JOB_ID)) {
            assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(response.getEntity()).isEqualTo("Timing report is not available for this job");
        }
    }

    @Test
    void getDebugTimingReport_shouldReturnNotFoundWhenNoData() {
        try (Response response = controller.getDebugTimingReport("unknownJob")) {
            assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
            assertThat(response.getEntity()).isEqualTo("Debug data not found");
        }
    }
}

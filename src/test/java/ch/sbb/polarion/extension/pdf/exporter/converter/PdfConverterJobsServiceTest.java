package ch.sbb.polarion.extension.pdf.exporter.converter;

import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import ch.sbb.polarion.extension.pdf.exporter.converter.PdfConverterJobsService.JobState;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.ExportParams;
import com.polarion.platform.security.ISecurityService;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfConverterJobsServiceTest {
    @Mock
    private PdfConverter pdfConverter;

    @Mock
    private ISecurityService securityService;

    @Mock
    private Subject subject;

    @Mock
    ServletRequestAttributes requestAttributes;

    @InjectMocks
    private PdfConverterJobsService pdfConverterJobsService;

    @BeforeEach
    public void setup() {
        RequestContextHolder.setRequestAttributes(requestAttributes);
    }

    @AfterEach
    public void tearDown() {
        pdfConverterJobsService.cancelJobsAndCleanMap();
    }

    @Test
    void shouldStartJobAndGetStatus() {
        prepareSecurityServiceSubject(subject);
        when(requestAttributes.getAttribute(LogoutFilter.XSRF_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(Boolean.FALSE);
        when(requestAttributes.getAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(Boolean.TRUE);
        ExportParams exportParams = ExportParams.builder().build();
        when(pdfConverter.convertToPdf(exportParams, null)).thenReturn("test pdf".getBytes());

        String jobId = pdfConverterJobsService.startJob(exportParams, 60);

        assertThat(jobId).isNotBlank();
        waitToFinishJob(jobId);
        JobState jobState = pdfConverterJobsService.getJobState(jobId);
        assertThat(jobState.isCompletedExceptionally()).isFalse();
        assertThat(jobState.isCancelled()).isFalse();
        Optional<byte[]> jobResult = pdfConverterJobsService.getJobResult(jobId);
        assertThat(jobResult).isNotEmpty();
        assertThat(new String(jobResult.get())).isEqualTo("test pdf");

        assertThatThrownBy(() -> pdfConverterJobsService.getJobState(jobId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(jobId);
        verify(securityService).logout(subject);
    }

    @Test
    void shouldReturnFailInExceptionalCase() {
        prepareSecurityServiceSubject(subject);
        when(requestAttributes.getAttribute(LogoutFilter.XSRF_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(Boolean.FALSE);
        when(requestAttributes.getAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(Boolean.TRUE);
        ExportParams exportParams = ExportParams.builder().build();
        when(pdfConverter.convertToPdf(exportParams, null)).thenThrow(new RuntimeException("test error"));

        String jobId = pdfConverterJobsService.startJob(exportParams, 60);

        assertThat(jobId).isNotBlank();
        waitToFinishJob(jobId);
        JobState jobState = pdfConverterJobsService.getJobState(jobId);
        assertThat(jobState.isCompletedExceptionally()).isTrue();
        assertThat(jobState.isCancelled()).isFalse();
        assertThat(jobState.errorMessage()).contains("test error");
        verify(securityService).logout(subject);
    }

    @Test
    void shouldGetAllJobsStatuses() {
        prepareSecurityServiceSubject(subject);
        ExportParams exportParams = ExportParams.builder().build();
        lenient().when(pdfConverter.convertToPdf(exportParams, null)).thenReturn("test pdf".getBytes());

        String jobId1 = pdfConverterJobsService.startJob(exportParams, 60);
        String jobId2 = pdfConverterJobsService.startJob(exportParams, 60);

        Map<String, JobState> allJobsStates = pdfConverterJobsService.getAllJobsStates();
        assertThat(allJobsStates).containsOnlyKeys(jobId1, jobId2);
    }

    @Test
    void shouldAcceptNullSubject() {
        prepareSecurityServiceSubject(null);

        ExportParams exportParams = ExportParams.builder().build();
        lenient().when(pdfConverter.convertToPdf(exportParams, null)).thenReturn("test pdf".getBytes());

        String jobId = pdfConverterJobsService.startJob(exportParams, 60);

        assertThat(jobId).isNotBlank();
        waitToFinishJob(jobId);
        JobState jobState = pdfConverterJobsService.getJobState(jobId);
        assertThat(jobState.isCompletedExceptionally()).isFalse();
        assertThat(jobState.isCancelled()).isFalse();
        verify(securityService, never()).logout(null);
    }

    @ParameterizedTest
    @CsvSource({
            "true,true",
            "true,false",
            "false,false"
    })
    void shouldNotLogoutWithoutAsyncSkipLogoutProperty(boolean xsrfSkipLogout, boolean asyncSkipLogout) {
        prepareSecurityServiceSubject(subject);
        lenient().when(requestAttributes.getAttribute(LogoutFilter.XSRF_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(xsrfSkipLogout);
        lenient().when(requestAttributes.getAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST)).thenReturn(asyncSkipLogout);

        ExportParams exportParams = ExportParams.builder().build();
        when(pdfConverter.convertToPdf(exportParams, null)).thenReturn("test pdf".getBytes());

        String jobId = pdfConverterJobsService.startJob(exportParams, 60);

        assertThat(jobId).isNotBlank();
        waitToFinishJob(jobId);
        JobState jobState = pdfConverterJobsService.getJobState(jobId);
        assertThat(jobState.isCompletedExceptionally()).isFalse();
        assertThat(jobState.isCancelled()).isFalse();
        verify(securityService, never()).logout(subject);
    }

    @ParameterizedTest
    @CsvSource({"0,true", "1,false"})
    @SuppressWarnings({"unchecked", "java:S2925"})
    void shouldRespectInProgressTimeout(int timeout, boolean isTimeoutExpected) {
        ExportParams exportParams = ExportParams.builder().build();
        lenient().when(securityService.doAsUser(any(), any(PrivilegedAction.class))).thenAnswer(p -> {
            Thread.sleep(TimeUnit.MINUTES.toMillis(10));
            return null;
        });
        String jobId = pdfConverterJobsService.startJob(exportParams, timeout);

        await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
            JobState jobState = pdfConverterJobsService.getJobState(jobId);
            if (isTimeoutExpected) {
                assertThat(jobState.isDone()).isTrue();
                assertThat(jobState.isCompletedExceptionally()).isTrue();
                assertThat(jobState.errorMessage()).contains("Timeout after 0 min");
            } else {
                assertThat(jobState.isDone()).isFalse();
            }
        });
    }

    @ParameterizedTest
    @CsvSource({"0,0", "1,1"})
    void shouldCleanupTimedOutFinishedJobs(int timeout, int expectedJobsCount) {
        ExportParams exportParams = ExportParams.builder().build();
        String finishedJobId = pdfConverterJobsService.startJob(exportParams, 1);
        waitToFinishJob(finishedJobId);

        PdfConverterJobsService.cleanupExpiredJobs(timeout);

        assertThat(pdfConverterJobsService.getAllJobsStates()).hasSize(expectedJobsCount);
    }

    @Test
    @SuppressWarnings({"unchecked", "java:S2925"})
    void shouldCleanupTimedOutInProgressJobs() {
        ExportParams exportParams = ExportParams.builder().build();
        lenient().when(securityService.doAsUser(eq(null), any(PrivilegedAction.class))).thenAnswer(p -> {
            Thread.sleep(TimeUnit.MINUTES.toMillis(10));
            return null;
        });
        String jobId = pdfConverterJobsService.startJob(exportParams, 0);
        await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
            JobState jobState = pdfConverterJobsService.getJobState(jobId);
            assertThat(jobState.isDone()).isTrue();
            assertThat(jobState.isCompletedExceptionally()).isTrue();
            assertThat(jobState.errorMessage()).contains("Timeout after 0 min");
        });

        PdfConverterJobsService.cleanupExpiredJobs(0);

        assertThat(pdfConverterJobsService.getAllJobsStates()).isEmpty();
    }

    private void waitToFinishJob(String jobId1) {
        await().atMost(Durations.FIVE_SECONDS)
                .untilAsserted(() -> {
                    JobState jobState = pdfConverterJobsService.getJobState(jobId1);
                    assertThat(jobState.isDone()).isTrue();
                });
    }

    @SuppressWarnings("unchecked")
    private void prepareSecurityServiceSubject(Subject userSubject) {
        when(securityService.getCurrentSubject()).thenReturn(userSubject);
        when(securityService.doAsUser(eq(userSubject), any(PrivilegedAction.class))).thenAnswer(invocation ->
                ((PrivilegedAction<?>) invocation.getArgument(1)).run());
    }
}
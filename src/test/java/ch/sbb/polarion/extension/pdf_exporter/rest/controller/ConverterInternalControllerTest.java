package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverterJobsService;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverterJobsService.JobState;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.BulkMergeExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.jobs.ConverterJobDetails;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.jobs.ConverterJobStatus;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConverterInternalControllerTest {
    @Mock
    private PdfConverterJobsService pdfConverterJobService;
    @Mock
    private UriInfo uriInfo;
    @Mock
    private PdfExporterPolarionService pdfExporterPolarionService;

    @InjectMocks
    private ConverterInternalController internalController;

    @Test
    void startPdfConverterJob_success() {
        ExportParams params = ExportParams.builder()
                .projectId("testProjectId")
                .locationPath("testLocationPath")
                .build();
        when(pdfConverterJobService.startJob(params, 60)).thenReturn("testJobId");
        when(uriInfo.getRequestUri()).thenReturn(UriBuilder.fromUri("http://testHost:8090/polarion/pdf-exporter/rest/api/convert/jobs").build());
        try (Response response = internalController.startPdfConverterJob(params)) {
            assertThat(response.getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
            assertThat(response.getHeaderString(HttpHeaders.LOCATION)).isEqualTo("/polarion/pdf-exporter/rest/api/convert/jobs/testJobId");
        }
    }

    @ParameterizedTest
    @MethodSource("getWrongConverterExportParams")
    void startPdfConverterJob_badRequest(ExportParams exportParams, String expectedErrorMessage) {
        assertThatThrownBy(() -> internalController.startPdfConverterJob(exportParams))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(expectedErrorMessage);
    }

    public static Stream<Arguments> getWrongConverterExportParams() {
        return Stream.of(
                Arguments.of(null, "Missing export parameters"),
                Arguments.of(ExportParams.builder().documentType(DocumentType.LIVE_DOC).locationPath("test").build(), "projectId"),
                Arguments.of(ExportParams.builder().documentType(DocumentType.LIVE_DOC).projectId("test").build(), "locationPath"),
                Arguments.of(ExportParams.builder().documentType(DocumentType.LIVE_REPORT).build(), "locationPath"),
                Arguments.of(ExportParams.builder().documentType(DocumentType.WIKI_PAGE).build(), "locationPath")
        );
    }

    @Test
    void startPdfConverterJob_invalidWorkItemsQueryGivesBadRequest() {
        ExportParams params = ExportParams.builder()
                .documentType(DocumentType.LIVE_DOC)
                .projectId("test")
                .locationPath("space/doc")
                .urlQueryParameters(java.util.Map.of(ExportParams.URL_QUERY_PARAM_QUERY, "broken !@#"))
                .build();
        doThrow(new IllegalArgumentException("Invalid work items query: syntax error"))
                .when(pdfExporterPolarionService).validateWorkItemsQuery("broken !@#");

        assertThatThrownBy(() -> internalController.startPdfConverterJob(params))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid work items query");
    }

    @ParameterizedTest
    @MethodSource("getStatusParams")
    void getPdfConverterJobStatus_success(JobState jobState,
                                          HttpStatus expectedHttpStatus,
                                          ConverterJobStatus expectedJobStatus,
                                          String expectedLocationUrl,
                                          String expectedErrorMessage) {
        if (expectedLocationUrl != null) {
            when(uriInfo.getRequestUri()).thenReturn(UriBuilder.fromUri("http://testHost:8090/polarion/pdf-exporter/rest/api/convert/jobs/testJobId").build());
        }
        when(pdfConverterJobService.getJobState("testJobId")).thenReturn(jobState);
        try (Response response = internalController.getPdfConverterJobStatus("testJobId")) {
            assertThat(response.getStatus()).isEqualTo(expectedHttpStatus.value());
            assertThat(response.getEntity()).isInstanceOf(ConverterJobDetails.class);
            assertThat(((ConverterJobDetails) response.getEntity()).getStatus()).isEqualTo(expectedJobStatus);
            if (expectedErrorMessage != null) {
                assertThat(((ConverterJobDetails) response.getEntity()).getErrorMessage()).contains(expectedErrorMessage);
            } else {
                assertThat(((ConverterJobDetails) response.getEntity()).getErrorMessage()).isNull();
            }
            assertThat(response.getHeaderString(HttpHeaders.LOCATION)).isEqualTo(expectedLocationUrl);
        }
    }

    static Stream<Arguments> getStatusParams() {
        return Stream.of(
                Arguments.of(new JobState(false, false, false, null), HttpStatus.ACCEPTED, ConverterJobStatus.IN_PROGRESS, null, null),
                Arguments.of(new JobState(true, false, false, null), HttpStatus.SEE_OTHER, ConverterJobStatus.SUCCESSFULLY_FINISHED, "/polarion/pdf-exporter/rest/api/convert/jobs/testJobId/result", null),
                Arguments.of(new JobState(true, false, true, null), HttpStatus.CONFLICT, ConverterJobStatus.CANCELLED, null, null),
                Arguments.of(new JobState(true, true, false, "test error"), HttpStatus.CONFLICT, ConverterJobStatus.FAILED, null, "test error")
        );
    }

    @Test
    void getPdfConverterJobStatus_notFound() {
        when(pdfConverterJobService.getJobState(anyString())).thenAnswer(id -> {
            throw new NoSuchElementException("Job not found: " + id);
        });
        assertThatThrownBy(() -> internalController.getPdfConverterJobStatus("testJobIdUnknown"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("testJobIdUnknown");
    }

    @Test
    void getPdfConverterJobResult_success() {
        when(pdfConverterJobService.getJobResult("testJobId")).thenReturn(Optional.of("test pdf".getBytes()));
        when(pdfConverterJobService.getJobContext("testJobId")).thenReturn(PdfConverterJobsService.JobContext.builder().workItemIDsWithMissingAttachment(new ArrayList<String>()).build());
        Response jobResult = internalController.getPdfConverterJobResult("testJobId");

        assertThat(jobResult.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(jobResult.getEntity()).isEqualTo("test pdf".getBytes());
    }

    @Test
    void getPdfConverterJobResult_notFound() {
        when(pdfConverterJobService.getJobResult(anyString())).thenAnswer(id -> {
            throw new NoSuchElementException("Job not found: " + id);
        });
        assertThatThrownBy(() -> internalController.getPdfConverterJobResult("testJobIdUnknown"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("testJobIdUnknown");
    }

    @Test
    void getPdfConverterJobResult_illegalState() {
        when(pdfConverterJobService.getJobResult("testJobId")).thenThrow(new IllegalStateException("Job was cancelled or failed: testJobId"));

        assertThatThrownBy(() -> internalController.getPdfConverterJobResult("testJobId"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Job was cancelled or failed: testJobId");
    }

    @Test
    void getAllPdfConverterJobs() {
        when(pdfConverterJobService.getAllJobsStates()).thenReturn(
                Map.of(
                        "testJobId1", new JobState(true, false, false, null),
                        "testJobId2", new JobState(false, false, false, null),
                        "testJobId3", new JobState(true, true, false, "test error")
                )
        );

        Response response = internalController.getAllPdfConverterJobs();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getEntity()).isEqualTo(
                Map.of(
                        "testJobId1", ConverterJobDetails.builder().status(ConverterJobStatus.SUCCESSFULLY_FINISHED).build(),
                        "testJobId2", ConverterJobDetails.builder().status(ConverterJobStatus.IN_PROGRESS).build(),
                        "testJobId3", ConverterJobDetails.builder().status(ConverterJobStatus.FAILED).errorMessage("test error").build()
                )
        );
    }

    @Test
    void startMergeExportJob_success() {
        BulkMergeExportParams params = BulkMergeExportParams.builder()
                .documents(List.of(ExportParams.builder().projectId("proj1").build()))
                .build();
        when(pdfConverterJobService.startMergeJob(any(), anyInt())).thenReturn("mergeJobId");
        when(uriInfo.getRequestUri()).thenReturn(UriBuilder.fromUri("http://testHost:8090/polarion/pdf-exporter/rest/api/convert/merge/jobs").build());

        try (Response response = internalController.startMergeExportJob(params)) {
            assertThat(response.getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
            assertThat(response.getHeaderString(HttpHeaders.LOCATION)).contains("mergeJobId");
        }
    }

    @Test
    void startMergeExportJob_nullParams() {
        assertThatThrownBy(() -> internalController.startMergeExportJob(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("At least one document");
    }

    @Test
    void startMergeExportJob_emptyDocuments() {
        BulkMergeExportParams params = BulkMergeExportParams.builder()
                .documents(List.of())
                .build();
        assertThatThrownBy(() -> internalController.startMergeExportJob(params))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("At least one document");
    }

    @Test
    void startMergeExportJob_nullDocuments() {
        BulkMergeExportParams params = BulkMergeExportParams.builder()
                .documents(null)
                .build();
        assertThatThrownBy(() -> internalController.startMergeExportJob(params))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("At least one document");
    }
}

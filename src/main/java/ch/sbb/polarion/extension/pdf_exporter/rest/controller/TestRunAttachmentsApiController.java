package ch.sbb.polarion.extension.pdf_exporter.rest.controller;

import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.attachments.TestRunAttachment;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.List;

@Secured
@Path("/api")
public class TestRunAttachmentsApiController extends TestRunAttachmentsInternalController {

    private static final PolarionService polarionService = new PolarionService();

    @Override
    public List<TestRunAttachment> getTestRunAttachments(String projectId, String testRunId, String revision, String filter, String testCaseFilterFieldId) {
        return polarionService.callPrivileged(() -> super.getTestRunAttachments(projectId, testRunId, revision, filter, testCaseFilterFieldId));
    }

    @Override
    public TestRunAttachment getTesRunAttachment(String projectId, String testRunId, String attachmentId, String revision) {
        return polarionService.callPrivileged(() -> super.getTesRunAttachment(projectId, testRunId, attachmentId, revision));
    }

    @Override
    public Response getTestRunAttachmentContent(String projectId, String workItemId, String attachmentId, String revision) {
        return polarionService.callPrivileged(() -> super.getTestRunAttachmentContent(projectId, workItemId, attachmentId, revision));
    }
}

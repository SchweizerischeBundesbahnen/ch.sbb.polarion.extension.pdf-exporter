package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentId;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.DocumentProject;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.id.TestRuntId;
import ch.sbb.polarion.extension.pdf_exporter.service.PolarionBaselineExecutor;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.server.api.model.tr.ProxyTestRun;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.collections.StrictMapImpl;
import com.polarion.alm.shared.api.utils.html.RichTextRenderTarget;
import com.polarion.alm.shared.rpe.RpeModelAspect;
import com.polarion.alm.shared.rpe.RpeRenderer;
import com.polarion.alm.tracker.model.IAttachmentBase;
import com.polarion.alm.tracker.model.ITestRun;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class TestRunAdapter extends CommonUniqueObjectAdapter {
    private final @NotNull ITestRun testRun;

    public TestRunAdapter(@NotNull ITestRun testRun) {
        this.testRun = testRun;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IUniqueObject> @NotNull T getUniqueObject() {
        return (T) testRun;
    }

    @Override
    public @NotNull DocumentId getDocumentId() {
        return new TestRuntId(getDocumentProject(), testRun.getId());
    }

    private @NotNull DocumentProject getDocumentProject() {
        return new DocumentProject(testRun.getProject());
    }

    @Override
    public @NotNull DocumentType getDocumentType() {
        return DocumentType.TEST_RUN;
    }

    @Override
    public @NotNull String getTitle() {
        return testRun.getLabel();
    }

    @Override
    public @NotNull String getContent(@NotNull ExportParams exportParams, @NotNull ReadOnlyTransaction transaction) {
        return PolarionBaselineExecutor.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {
            ProxyTestRun proxyTestRun = new ProxyTestRun(testRun, (InternalReadOnlyTransaction) transaction);

            String html = RpeModelAspect.getPageHtml(proxyTestRun);
            RpeRenderer richPageRenderer = new RpeRenderer((InternalReadOnlyTransaction) transaction, html, RichTextRenderTarget.PDF_EXPORT, proxyTestRun.getReference(), proxyTestRun.getReference().scope(), new StrictMapImpl<>());
            return richPageRenderer.render(null);
        });
    }

    @Override
    public @Nullable List<Path> getAttachmentFiles(@NotNull ExportParams exportParams) throws IOException {
        if (exportParams.isEmbedAttachments()) {
            List<Path> attachmentFiles = new ArrayList<>();
            for (IAttachmentBase attachment : testRun.getAttachments()) {
                attachmentFiles.add(createAttachmentTempFile(attachment));
            }
            return attachmentFiles;
        } else {
            return null;
        }
    }

    @SuppressWarnings({"java:S5443"}) // Files created in a temp folder of a server are unlikely be accessible by non-authorised users
    private Path createAttachmentTempFile(IAttachmentBase attachment) throws IOException {
        String fileName = attachment.getFileName();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = (dotIndex != -1) ? fileName.substring(0, dotIndex) : fileName;
        String extension = (dotIndex != -1) ? fileName.substring(dotIndex) : "";
        Path tempFile = Files.createTempFile(baseName, extension);
        Files.copy(attachment.getDataStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        tempFile.toFile().deleteOnExit();
        return tempFile;
    }
}

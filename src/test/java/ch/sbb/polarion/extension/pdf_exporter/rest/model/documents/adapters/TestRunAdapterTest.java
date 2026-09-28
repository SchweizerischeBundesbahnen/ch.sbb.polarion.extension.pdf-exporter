package ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.adapters;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import com.polarion.alm.tracker.model.ITestRecord;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.ITestRunAttachment;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.spi.PObjectList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TestRunAdapterTest {

    /** An attachment whose content is its own file name, so an embedded file says which one it is. */
    private static ITestRunAttachment attachment(String fileName) {
        ITestRunAttachment attachment = mock(ITestRunAttachment.class);
        when(attachment.getFileName()).thenReturn(fileName);
        when(attachment.getDataStream()).thenReturn(new ByteArrayInputStream(fileName.getBytes(StandardCharsets.UTF_8)));
        return attachment;
    }

    private static ITestRun testRunWith(List<ITestRunAttachment> attachments, List<ITestRecord> records) {
        ITestRun testRun = mock(ITestRun.class);
        when(testRun.getAttachments()).thenReturn(new PObjectList(null, attachments));
        when(testRun.getAllRecords()).thenReturn(records);
        return testRun;
    }

    /** What was embedded, by the content of the temp files, which are removed afterwards. */
    private static List<String> embedded(List<Path> files) throws Exception {
        List<String> contents = new java.util.ArrayList<>();
        for (Path file : files) {
            contents.add(Files.readString(file));
            Files.deleteIfExists(file);
        }
        return contents.stream().sorted().toList();
    }

    @Test
    void embedsNothingWhereTheExportDoesNotAskForIt() throws Exception {
        TestRunAdapter adapter = new TestRunAdapter(testRunWith(List.of(attachment("report.pdf")), List.of()));

        assertNull(adapter.getAttachmentFiles(ExportParams.builder().embedAttachments(false).build()));
    }

    @Test
    void embedsOnlyTheAttachmentsTheMaskNames() throws Exception {
        // The mask is the one the export downloads by where the attachments are not embedded. It was not read
        // here, so switching the embedding on took every attachment of the test run; reported in #1071.
        TestRunAdapter adapter = new TestRunAdapter(
                testRunWith(List.of(attachment("report.pdf"), attachment("log.txt"), attachment("evidence.pdf")), List.of()));

        List<Path> files = adapter.getAttachmentFiles(
                ExportParams.builder().embedAttachments(true).attachmentsFilter("*.pdf").build());

        assertEquals(List.of("evidence.pdf", "report.pdf"), embedded(files));
    }

    @Test
    void embedsEveryAttachmentWithoutAMask() throws Exception {
        TestRunAdapter adapter = new TestRunAdapter(testRunWith(List.of(attachment("report.pdf"), attachment("log.txt")), List.of()));

        List<Path> files = adapter.getAttachmentFiles(ExportParams.builder().embedAttachments(true).build());

        assertEquals(List.of("log.txt", "report.pdf"), embedded(files));
    }

    @Test
    void embedsOnlyTheAttachmentsOfTheTestCasesTheFieldIsTrueFor() throws Exception {
        // The same rule the download follows: a test case the field is false for brings no attachment along.
        ITestRunAttachment ofTheRun = attachment("run.pdf");
        ITestRunAttachment ofTheExcludedCase = attachment("excluded.pdf");

        IWorkItem excludedCase = mock(IWorkItem.class);
        when(excludedCase.getValue("embed")).thenReturn(Boolean.FALSE);
        ITestRecord record = mock(ITestRecord.class);
        when(record.getTestCase()).thenReturn(excludedCase);
        when(record.getAttachments()).thenReturn(List.of(ofTheExcludedCase));
        when(record.getTestStepResults()).thenReturn(List.of());

        ITestRun testRun = testRunWith(List.of(ofTheRun, ofTheExcludedCase), List.of(record));
        when(testRun.getValue("embed")).thenReturn(Boolean.TRUE);
        TestRunAdapter adapter = new TestRunAdapter(testRun);

        List<Path> files = adapter.getAttachmentFiles(
                ExportParams.builder().embedAttachments(true).attachmentsFilter("*.*").testcaseFieldId("embed").build());

        assertEquals(List.of("run.pdf"), embedded(files));
    }
}

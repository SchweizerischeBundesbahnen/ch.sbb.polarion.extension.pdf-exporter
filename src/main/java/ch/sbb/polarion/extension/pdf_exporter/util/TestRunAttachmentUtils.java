package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.ITestRunAttachment;
import com.polarion.core.util.StringUtils;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Which attachments of a test run an export takes: those whose file name matches the mask, and, where a boolean
 * test case field is named, only those of the test cases the field is true for.
 * <p>
 * Both ways an export delivers them read it - downloaded next to the PDF, and embedded into it - so the mask and
 * the field mean the same thing whichever way is chosen.
 * </p>
 */
@UtilityClass
public class TestRunAttachmentUtils {

    public @NotNull List<ITestRunAttachment> selectAttachments(@NotNull ITestRun testRun, @Nullable String filter, @Nullable String testCaseFilterFieldId) {
        List<ITestRunAttachment> attachments = new ArrayList<>(testRun.getAttachments()); // initially take all attachments
        if (!StringUtils.isEmpty(testCaseFilterFieldId)) {
            // the value of the test run itself, which a test case leaving the field empty inherits
            boolean testRunFieldValue = testRun.getValue(testCaseFilterFieldId) instanceof Boolean b && b;
            // filter out attachments from test records that do not match the test case filter
            testRun.getAllRecords().stream()
                    .filter(testRecord -> testRecord.getTestCase() != null)
                    .filter(testRecord -> {
                        Object value = testRecord.getTestCase().getValue(testCaseFilterFieldId);
                        boolean testCaseValue = value instanceof Boolean b && b;
                        return !Objects.equals(Boolean.TRUE, value != null ? testCaseValue : testRunFieldValue);
                    })
                    .forEach(testRecord -> {
                        // attachments on the test record itself (the last summary step)
                        attachments.removeAll(testRecord.getAttachments());
                        // attachments on the test steps
                        attachments.removeAll(testRecord.getTestStepResults().stream().flatMap(res -> res.getAttachments().stream()).toList());
                    });
        }
        return attachments.stream().filter(a -> filter == null || WildcardUtils.matches(a.getFileName(), filter)).toList();
    }
}

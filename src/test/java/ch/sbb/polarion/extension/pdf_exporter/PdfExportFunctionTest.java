package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.pdf_exporter.converter.PdfConverter;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageSettings;
import com.polarion.alm.projects.model.IFolder;
import com.polarion.alm.tracker.internal.workflow.Arguments;
import com.polarion.alm.tracker.model.IAttachment;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IStatusOpt;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.core.util.exceptions.UserFriendlyRuntimeException;
import com.polarion.core.util.types.Text;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
class PdfExportFunctionTest {

    PdfExporterPolarionService pdfExporterPolarionService;
    PdfConverter pdfConverter;
    PdfExportFunction pdfExportFunction;
    ICallContext<? extends IWorkflowObject> context;
    IModule module;
    IFolder moduleFolder;
    ITrackerProject trackerProject;
    @Mock
    private SettingsService settingsService;

    @BeforeEach
    public void setup() {
        pdfExporterPolarionService = mock(PdfExporterPolarionService.class);
        pdfConverter = mock(PdfConverter.class);
        pdfExportFunction = spy(new PdfExportFunction(pdfExporterPolarionService, pdfConverter));
        context = (ICallContext<? extends IWorkflowObject>) mock(ICallContext.class);
        trackerProject = mock(ITrackerProject.class);
        lenient().when(pdfExporterPolarionService.getTrackerProject(anyString())).thenReturn(trackerProject);
        module = mock(IModule.class);
        ILocation moduleLocation = mock(ILocation.class);
        lenient().when(module.getModuleLocation()).thenReturn(moduleLocation);
        moduleFolder = mock(IFolder.class);
        lenient().when(module.getFolder()).thenReturn(moduleFolder);
        IStatusOpt moduleStatus = mock(IStatusOpt.class);
        lenient().when(moduleStatus.getId()).thenReturn("currentStatusId");
        lenient().when(module.getStatus()).thenReturn(moduleStatus);
        lenient().when(context.getTarget()).thenAnswer((Answer<?>) invocationOnMock -> module);
    }

    @Test
    void testNotIModuleMustBeSkipped() {
        when(context.getTarget()).thenAnswer((Answer<?>) invocationOnMock -> mock(IWorkItem.class));
        IArguments args = mock(IArguments.class);

        assertDoesNotThrow(() -> pdfExportFunction.execute(context, args));
    }

    @Test
    void testDefaultStylePackageUsed() {
        IArguments args = new Arguments(Map.of());
        NamedSettingsRegistry.INSTANCE.register(List.of(new StylePackageSettings(settingsService)));

        assertEquals("Default", pdfExportFunction.getExportParams(module, args).getCss());
    }

    @Test
    void testSpecificStylePackageNotFound() {
        IArguments args = new Arguments(Map.of("style_package", "Specific"));
        NamedSettingsRegistry.INSTANCE.register(List.of(new StylePackageSettings(settingsService)));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> pdfExportFunction.getExportParams(module, args));
        assertEquals("Styled package 'Specific' is unavailable. Please contact system administrator.", exception.getMessage());
    }

    @Test
    void testLastBaselineUsed() {
        IArguments args = new Arguments(Map.of());
        NamedSettingsRegistry.INSTANCE.register(List.of(new StylePackageSettings(settingsService)));
        doReturn("271").when(pdfExportFunction).getLastBaselineRevision(any());

        // call without arg
        assertNull(pdfExportFunction.getExportParams(module, args).getBaselineRevision());

        // use baseline arg set
        args = new Arguments(Map.of("prefer_last_baseline", "true"));
        assertEquals("271", pdfExportFunction.getExportParams(module, args).getRevision());
    }

    @Test
    void testWorkItemCreationFailedRequiredParametersNotProvided() {
        Arguments args = new Arguments(Map.of());
        ExportParams params = ExportParams.builder().projectId("projectId").build();

        UserFriendlyRuntimeException exception = assertThrows(UserFriendlyRuntimeException.class, () -> pdfExportFunction.savePdfAsWorkItemAttachment(module, params, "TargetStatus", args, new byte[0]));
        assertEquals("Workflow function isn't configured properly. Please contact system administrator.", exception.getMessage());
    }

    @Test
    void testWorkItemData() {
        ExportParams params = ExportParams.builder().projectId("projectId").build();
        IWorkItem newWorkItem = mock(IWorkItem.class);
        when(trackerProject.createWorkItem(anyString())).thenReturn(newWorkItem);
        when(newWorkItem.createAttachment(anyString(), anyString(), any())).thenReturn(mock(IAttachment.class));
        when(module.getTitleWithSpace()).thenReturn("Some space / Document Title");
        doReturn("Status name").when(pdfExportFunction).getStatusName(any(), anyString());
        doReturn("Attach Title.pdf").when(pdfExportFunction).getDocumentFileName(eq(params));

        Arguments args = new Arguments(Map.of("create_wi_type_id", "someType"));
        pdfExportFunction.savePdfAsWorkItemAttachment(module, params, "TargetStatus", args, new byte[0]);
        verify(pdfExporterPolarionService, times(1)).getTrackerProject("projectId");
        verify(trackerProject, times(1)).createWorkItem("someType");
        verify(newWorkItem, times(1)).createAttachment(eq("Attach Title.pdf"), eq("Attach Title"), any());
        verify(newWorkItem, times(1)).setTitle("Some space / Document Title -> Status name");
        verify(newWorkItem, times(1)).setDescription(Text.html("This item was created automatically. Check 'Attachments' section for the generated PDF document."));

        // custom project, attachment title & description
        args = new Arguments(Map.of("create_wi_type_id", "someType", "project_id", "proj1", "attachment_title", "Custom title", "create_wi_description", "Custom description"));
        pdfExportFunction.savePdfAsWorkItemAttachment(module, params, "TargetStatus", args, new byte[0]);
        verify(pdfExporterPolarionService, times(1)).getTrackerProject("proj1");
        verify(newWorkItem, times(1)).createAttachment(eq("Attach Title.pdf"), eq("Custom title"), any());
        verify(newWorkItem, times(1)).setDescription(Text.html("Custom description"));

        // use existing work item
        args = new Arguments(Map.of("existing_wi_id", "ID-123"));
        IWorkItem foundWorkItem = mock(IWorkItem.class);
        when(foundWorkItem.createAttachment(anyString(), anyString(), any())).thenReturn(mock(IAttachment.class));
        when(pdfExporterPolarionService.getWorkItem("projectId", "ID-123")).thenReturn(foundWorkItem);
        pdfExportFunction.savePdfAsWorkItemAttachment(module, params, "TargetStatus", args, new byte[0]);
        verify(foundWorkItem, times(1)).createAttachment(any(), any(), any());

        // use existing work item from specific project
        args = new Arguments(Map.of("existing_wi_id", "ID-345", "project_id", "proj2"));
        foundWorkItem = mock(IWorkItem.class);
        when(foundWorkItem.createAttachment(anyString(), anyString(), any())).thenReturn(mock(IAttachment.class));
        when(pdfExporterPolarionService.getWorkItem("proj2", "ID-345")).thenReturn(foundWorkItem);
        pdfExportFunction.savePdfAsWorkItemAttachment(module, params, "TargetStatus", args, new byte[0]);
        verify(foundWorkItem, times(1)).createAttachment(any(), any(), any());
    }
}

package ch.sbb.polarion.extension.pdf.exporter.service;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.core.util.StringUtils;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import org.jetbrains.annotations.NotNull;

public class PdfExporterPolarionService extends PolarionService {

    public PdfExporterPolarionService() {
        super();
    }

    public PdfExporterPolarionService(@NotNull ITrackerService trackerService, @NotNull IProjectService projectService, @NotNull ISecurityService securityService,
                                      @NotNull IPlatformService platformService, @NotNull IRepositoryService repositoryService) {
        super(trackerService, projectService, securityService, platformService, repositoryService);
    }

    public ITrackerProject getProjectFromScope(String scope) {
        ITrackerProject project = null;

        if (!StringUtils.isEmpty(scope)) {
            String projectId = ScopeUtils.getProjectFromScope(scope);
            if (projectId == null) {
                throw new IllegalArgumentException(String.format("Wrong scope format: %s. Should be of form 'project/{projectId}/'", scope));
            } else {
                project = getTrackerProject(projectId);
            }
        }
        return project;
    }
}

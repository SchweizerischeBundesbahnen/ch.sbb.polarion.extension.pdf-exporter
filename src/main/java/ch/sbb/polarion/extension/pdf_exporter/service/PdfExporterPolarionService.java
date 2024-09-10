package ch.sbb.polarion.extension.pdf_exporter.service;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageSettings;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.core.util.StringUtils;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

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

    public Collection<SettingName> getSuitableStylePackages(@NotNull String projectId, @NotNull String spaceId, @NotNull String documentName) {
        StylePackageSettings stylePackageSettings = (StylePackageSettings) NamedSettingsRegistry.INSTANCE.getByFeatureName(StylePackageSettings.FEATURE_NAME);
        Collection<SettingName> stylePackageNames = stylePackageSettings.readNames(ScopeUtils.getScopeFromProject(projectId));
        return stylePackageNames.stream().filter(stylePackageName -> isStylePackageSuitable(projectId, spaceId, documentName, stylePackageSettings, stylePackageName)).toList();
    }

    @SuppressWarnings("unchecked")
    private boolean isStylePackageSuitable(@NotNull String projectId, @NotNull String spaceId, @NotNull String documentName,
                                           @NotNull StylePackageSettings stylePackageSettings, @NotNull SettingName stylePackageName) {
        StylePackageModel model = stylePackageSettings.read(ScopeUtils.getScopeFromProject(projectId), SettingId.fromName(stylePackageName.getName()), null);

        if (StringUtils.isEmpty(model.getMatchingQuery())) {
            return true;
        } else {
            IDataService dataService = getTrackerService().getDataService();
            IPObjectList<IModule> suitableDocuments =  dataService.searchInstances(dataService.getPrototype("Module"), model.getMatchingQuery(), "name");
            for (IModule suitableDocument : suitableDocuments) {
                if (sameDocument(projectId, spaceId, documentName, suitableDocument)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean sameDocument(@NotNull String projectId, @NotNull String spaceId, @NotNull String documentName, @NotNull IModule document) {
        return projectId.equals(document.getProjectId()) && String.format("%s/%s", spaceId, documentName).equals(document.getModuleLocation().getLocationPath());
    }

}

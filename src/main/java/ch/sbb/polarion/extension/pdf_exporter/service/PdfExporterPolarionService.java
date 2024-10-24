package ch.sbb.polarion.extension.pdf_exporter.service;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageWeightInfo;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PdfExporterPolarionService extends PolarionService {

    public PdfExporterPolarionService() {
        super();
    }

    public PdfExporterPolarionService(@NotNull ITrackerService trackerService, @NotNull IProjectService projectService, @NotNull ISecurityService securityService,
                                      @NotNull IPlatformService platformService, @NotNull IRepositoryService repositoryService) {
        super(trackerService, projectService, securityService, platformService, repositoryService);
    }

    public @Nullable ITrackerProject getProjectFromScope(@Nullable String scope) {
        ITrackerProject project = null;

        if (scope != null && !scope.isEmpty()) {
            String projectId = ScopeUtils.getProjectFromScope(scope);
            if (projectId == null) {
                throw new IllegalArgumentException(String.format("Wrong scope format: %s. Should be of form 'project/{projectId}/'", scope));
            } else {
                project = getTrackerProject(projectId);
            }
        }
        return project;
    }

    public Collection<StylePackageWeightInfo> getStylePackagesWeights(@Nullable String scope) {
        StylePackageSettings stylePackageSettings = (StylePackageSettings) NamedSettingsRegistry.INSTANCE.getByFeatureName(StylePackageSettings.FEATURE_NAME);
        Collection<SettingName> stylePackageNames = stylePackageSettings.readNames(scope == null ? "" : scope);
        Collection<StylePackageWeightInfo> stylePackageWeightInfos = new ArrayList<>();
        for (SettingName settingName : stylePackageNames) {
            stylePackageWeightInfos.add(StylePackageWeightInfo.builder()
                    .name(settingName.getName())
                    .scope(settingName.getScope())
                    .weight(stylePackageSettings.read(settingName.getScope(), SettingId.fromName(settingName.getName()), null).getWeight())
                    .build());
        }
        return stylePackageWeightInfos;
    }

    public void updateStylePackagesWeights(@NotNull List<StylePackageWeightInfo> weightInfos) {
        StylePackageSettings stylePackageSettings = (StylePackageSettings) NamedSettingsRegistry.INSTANCE.getByFeatureName(StylePackageSettings.FEATURE_NAME);
        for (StylePackageWeightInfo weightInfo : weightInfos) {
            StylePackageModel model = stylePackageSettings.read(weightInfo.getScope(), SettingId.fromName(weightInfo.getName()), null);
            if (!Objects.equals(model.getWeight(), weightInfo.getWeight())) { // skip unnecessary updates
                model.setWeight(weightInfo.getWeight());
                stylePackageSettings.save(weightInfo.getScope(), SettingId.fromName(weightInfo.getName()), model);
            }
        }
    }

    public Collection<SettingName> getSuitableStylePackages(@Nullable String projectId, @NotNull String spaceId, @NotNull String documentName) {
        StylePackageSettings stylePackageSettings = (StylePackageSettings) NamedSettingsRegistry.INSTANCE.getByFeatureName(StylePackageSettings.FEATURE_NAME);
        Collection<SettingName> stylePackageNames = stylePackageSettings.readNames(ScopeUtils.getScopeFromProject(projectId));
        List<SettingName> names = stylePackageNames.stream().filter(stylePackageName -> isStylePackageSuitable(projectId, spaceId, documentName, stylePackageSettings, stylePackageName)).toList();
        Map<SettingName, Float> weightsMap = new HashMap<>();
        names.forEach(name -> weightsMap.put(name, stylePackageSettings.read(name.getScope(), SettingId.fromName(name.getName()), null).getWeight()));
        return names.stream().sorted((o1, o2) -> {
            int compareResult = weightsMap.get(o2).compareTo(weightsMap.get(o1));
            return compareResult == 0 ? o1.getName().compareToIgnoreCase(o2.getName()) : compareResult;
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private boolean isStylePackageSuitable(@Nullable String projectId, @NotNull String spaceId, @NotNull String documentName,
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

    private boolean sameDocument(@Nullable String projectId, @NotNull String spaceId, @NotNull String documentName, @NotNull IModule document) {
        if (projectId == null) {
            return document.getProjectId() == null && String.format("%s/%s", spaceId, documentName).equals(document.getModuleLocation().getLocationPath());
        } else {
            return projectId.equals(document.getProjectId()) && String.format("%s/%s", spaceId, documentName).equals(document.getModuleLocation().getLocationPath());
        }
    }
}

package ch.sbb.polarion.extension.pdf_exporter.service;

import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.attachments.TestRunAttachment;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.DocIdentifier;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageWeightInfo;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.WildcardUtils;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.ITestRunAttachment;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.core.util.StringUtils;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.portal.internal.server.navigation.TestManagementServiceAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PdfExporterPolarionService extends PolarionService {

    protected final ITestManagementService testManagementService;

    public PdfExporterPolarionService() {
        super();
        this.testManagementService = new TestManagementServiceAccessor().getTestingService();
    }

    public PdfExporterPolarionService(
            @NotNull ITrackerService trackerService,
            @NotNull IProjectService projectService,
            @NotNull ISecurityService securityService,
            @NotNull IPlatformService platformService,
            @NotNull IRepositoryService repositoryService,
            @NotNull ITestManagementService testManagementService
    ) {
        super(trackerService, projectService, securityService, platformService, repositoryService);
        this.testManagementService = testManagementService;
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

    public Collection<SettingName> getSuitableStylePackages(@NotNull List<DocIdentifier> docIdentifiers) {
        if (docIdentifiers.isEmpty()) {
            return new ArrayList<>();
        }
        StylePackageSettings stylePackageSettings = (StylePackageSettings) NamedSettingsRegistry.INSTANCE.getByFeatureName(StylePackageSettings.FEATURE_NAME);
        // if user mixes items from different projects then we can use only 'default'-level style packages
        String stylePackageScope = ScopeUtils.getScopeFromProject(docIdentifiers.stream().map(DocIdentifier::getProjectId).distinct().count() == 1 ? docIdentifiers.get(0).getProjectId() : GenericNamedSettings.DEFAULT_SCOPE);
        Collection<SettingName> stylePackageNames = stylePackageSettings.readNames(stylePackageScope);
        List<SettingName> names = stylePackageNames.stream().filter(stylePackageName -> docIdentifiers.stream().allMatch(
                i -> isStylePackageSuitable(i.getProjectId(), i.getSpaceId(), i.getDocumentName(), stylePackageSettings, stylePackageScope, stylePackageName))).toList();
        Map<SettingName, Float> weightsMap = new HashMap<>();
        names.forEach(name -> weightsMap.put(name, stylePackageSettings.read(name.getScope(), SettingId.fromName(name.getName()), null).getWeight()));
        return names.stream().sorted((o1, o2) -> {
            int compareResult = weightsMap.get(o2).compareTo(weightsMap.get(o1));
            return compareResult == 0 ? o1.getName().compareToIgnoreCase(o2.getName()) : compareResult;
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private boolean isStylePackageSuitable(@Nullable String projectId, @NotNull String spaceId, @NotNull String documentName,
                                           @NotNull StylePackageSettings stylePackageSettings, @NotNull String stylePackageScope, @NotNull SettingName stylePackageName) {
        StylePackageModel model = stylePackageSettings.read(stylePackageScope, SettingId.fromName(stylePackageName.getName()), null);

        if (StringUtils.isEmpty(model.getMatchingQuery())) {
            return true;
        } else {
            IDataService dataService = getTrackerService().getDataService();
            IPObjectList<IModule> suitableDocuments = dataService.searchInstances(IModule.PROTO, model.getMatchingQuery(), "name");
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

    public @NotNull ITestRun getTestRun(@NotNull String projectId, @NotNull String testRunId, @Nullable String revision) {
        ITestRun testRun = testManagementService.getTestRun(projectId, testRunId, revision);
        if (testRun.isUnresolvable()) {
            throw new IllegalArgumentException("Test run with id '%s' not found in project '%s'".formatted(testRunId, projectId));
        }
        return testRun;
    }

    public @NotNull List<TestRunAttachment> getTestRunAttachments(@NotNull String projectId, @NotNull String testRunId, @Nullable String revision, @Nullable String filter, @Nullable String testCaseFilterFieldId) {
        ITestRun testRun = getTestRun(projectId, testRunId, revision);

        Boolean testRunFieldValue;
        if (testCaseFilterFieldId != null) {
            Object testRunFieldObj = testRun.getValue(testCaseFilterFieldId);
            testRunFieldValue = testRunFieldObj instanceof Boolean b && b;
        } else {
            testRunFieldValue = false;
        }

        List<ITestRunAttachment> attachments = new ArrayList<>(testRun.getAttachments()); // initially take all attachments
        if (!StringUtils.isEmpty(testCaseFilterFieldId)) {
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
        return attachments.stream().filter(a -> filter == null || WildcardUtils.matches(a.getFileName(), filter))
                .map(TestRunAttachment::fromAttachment)
                .toList();
    }

    public @NotNull ITestRunAttachment getTestRunAttachment(@NotNull String projectId, @NotNull String testRunId, @NotNull String attachmentId, @Nullable String revision) {
        ITestRun testRun = getTestRun(projectId, testRunId, revision);
        ITestRunAttachment testRunAttachment = testRun.getAttachment(attachmentId);
        if (testRunAttachment == null) {
            throw new IllegalArgumentException("Attachment with id '%s' not found in test run '%s/%s'".formatted(attachmentId, projectId, testRunId));
        }
        return testRunAttachment;
    }
}

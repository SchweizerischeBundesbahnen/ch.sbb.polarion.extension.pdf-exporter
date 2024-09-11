package ch.sbb.polarion.extension.pdf_exporter;

import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.DocumentType;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.localization.Language;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.settings.stylepackage.StylePackageModel;
import ch.sbb.polarion.extension.pdf_exporter.service.PdfExporterPolarionService;
import ch.sbb.polarion.extension.pdf_exporter.settings.CoverPageSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.CssSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.HeaderFooterSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.LocalizationSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.StylePackageSettings;
import ch.sbb.polarion.extension.pdf_exporter.settings.WebhooksSettings;
import ch.sbb.polarion.extension.pdf_exporter.util.DocumentFileNameHelper;
import ch.sbb.polarion.extension.pdf_exporter.util.EnumValuesProvider;
import ch.sbb.polarion.extension.pdf_exporter.util.ExceptionHandler;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.ui.server.forms.extensions.IFormExtension;
import com.polarion.alm.ui.server.forms.extensions.IFormExtensionContext;
import com.polarion.alm.ui.shared.CollectionUtils;
import com.polarion.core.util.StringUtils;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.model.IPObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ch.sbb.polarion.extension.pdf_exporter.util.placeholder.PlaceholderValues.DOC_LANGUAGE_FIELD;

public class PdfExporterFormExtension implements IFormExtension {

    private static final String OPTION_TEMPLATE = "<option value='%s' %s>%s</option>";
    private static final String OPTION_VALUE = "<option value='%s'";
    private static final String OPTION_SELECTED = "<option value='%s' selected";
    private static final String SELECTED = "selected";

    private final PdfExporterPolarionService polarionService = new PdfExporterPolarionService();

    @Override
    @Nullable
    public String render(@NotNull IFormExtensionContext context) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(
                transaction -> renderForm(transaction.context(), context.object().getOldApi()));
    }

    public String renderForm(@NotNull SharedContext context, @NotNull IPObject object) {
        HtmlFragmentBuilder builder = context.createHtmlFragmentBuilderFor().gwt();

        if (object instanceof IModule module) {

            String form = ScopeUtils.getFileContent("webapp/pdf-exporter/html/sidePanelContent.html");

            String scope = ScopeUtils.getScopeFromProject(module.getProject().getId());
            form = form.replace("{SCOPE_VALUE}", scope);

            Collection<SettingName> stylePackageNames = getSuitableStylePackages(module);
            SettingName stylePackageNameToSelect = getStylePackageNameToSelect(stylePackageNames); // Either default (if exists) or first from list

            form = form.replace("{STYLE_PACKAGE_OPTIONS}", generateSelectOptions(stylePackageNames, stylePackageNameToSelect != null ? stylePackageNameToSelect.getName() : null));

            StylePackageModel selectedStylePackage = getSelectedStylePackage(stylePackageNameToSelect, scope); // Implicitly default one will be returned if no style package persisted (whatever the reason)

            if (!selectedStylePackage.isExposeSettings()) {
                form = form.replace("<div id='style-package-content'", "<div id='style-package-content' class='hidden'"); // Hide settings pane if style package settings not exposed to end users
            }

            form = adjustCoverPage(scope, form, selectedStylePackage);
            form = adjustCss(scope, form, selectedStylePackage);
            form = adjustHeaderFooter(scope, form, selectedStylePackage);
            form = adjustLocalization(scope, form, selectedStylePackage);
            form = adjustWebhooks(scope, form, selectedStylePackage);
            form = form.replace("{HEADERS_COLOR_VALUE}", selectedStylePackage.getHeadersColor());
            form = adjustPaperSize(form, selectedStylePackage);
            form = adjustOrientation(form, selectedStylePackage);
            form = adjustFitToPage(form, selectedStylePackage);
            form = adjustCommentsRendering(form, selectedStylePackage);
            form = adjustWatermark(form, selectedStylePackage);
            form = adjustMarkReferencedWorkitems(form, selectedStylePackage);
            form = adjustCutEmptyChapters(form, selectedStylePackage);
            form = adjustCutEmptyWorkitemAttributes(form, selectedStylePackage);
            form = adjustCutLocalURLs(form, selectedStylePackage);
            form = adjustPresentationalHints(form, selectedStylePackage);
            form = adjustCustomNumberedListsStyles(form, selectedStylePackage);
            form = adjustChapters(form, selectedStylePackage);
            form = adjustLocalizeEnums(form, selectedStylePackage, module.getCustomField(DOC_LANGUAGE_FIELD));
            form = adjustLinkRoles(form, EnumValuesProvider.getAllLinkRoleNames(module.getProject()), selectedStylePackage);
            form = adjustFilename(form, module);
            form = adjustButtons(form, module, selectedStylePackage);

            builder.html(form);
        }

        builder.finished();
        return builder.toString();
    }

    private SettingName getStylePackageNameToSelect(Collection<SettingName> stylePackageNames) {
        return stylePackageNames.stream()
                .filter(stylePackageName -> !NamedSettings.DEFAULT_NAME.equals(stylePackageName.getName()))
                .findFirst()
                .orElse(stylePackageNames.stream()
                        .findFirst()
                        .orElse(null)
                );
    }

    private StylePackageModel getSelectedStylePackage(SettingName defaultStylePackageName, String scope) {
        StylePackageSettings stylePackageSettings = (StylePackageSettings) NamedSettingsRegistry.INSTANCE.getByFeatureName(StylePackageSettings.FEATURE_NAME);
        return defaultStylePackageName != null
                ? stylePackageSettings.read(scope, SettingId.fromName(defaultStylePackageName.getName()), null)
                : stylePackageSettings.defaultValues();
    }

    private String adjustCoverPage(String scope, String form, StylePackageModel stylePackage) {
        Collection<SettingName> options = getSettingNames(CoverPageSettings.FEATURE_NAME, scope);
        boolean noCoverPage = StringUtils.isEmpty(stylePackage.getCoverPage());
        String coverPageOptions = generateSelectOptions(options, noCoverPage ? NamedSettings.DEFAULT_NAME : stylePackage.getCoverPage());
        form = form.replace("{COVER_PAGE_OPTIONS}", coverPageOptions);
        form = form.replace("{COVER_PAGE_DISPLAY}", noCoverPage ? "none" : "inline-block");
        return form.replace("{COVER_PAGE_SELECTED}", noCoverPage ? "" : "checked");
    }

    private String adjustHeaderFooter(String scope, String form, StylePackageModel stylePackage) {
        Collection<SettingName> headerFooterNames = getSettingNames(HeaderFooterSettings.FEATURE_NAME, scope);
        String headerFooterOptions = generateSelectOptions(headerFooterNames, stylePackage.getHeaderFooter());
        return form.replace("{HEADER_FOOTER_OPTIONS}", headerFooterOptions);
    }

    private String adjustCss(String scope, String form, StylePackageModel stylePackage) {
        Collection<SettingName> cssNames = getSettingNames(CssSettings.FEATURE_NAME, scope);
        String cssOptions = generateSelectOptions(cssNames, stylePackage.getCss());
        return form.replace("{CSS_OPTIONS}", cssOptions);
    }

    private String adjustLocalization(String scope, String form, StylePackageModel stylePackage) {
        Collection<SettingName> localizationNames = getSettingNames(LocalizationSettings.FEATURE_NAME, scope);
        String localizationOptions = generateSelectOptions(localizationNames, stylePackage.getLocalization());
        return form.replace("{LOCALIZATION_OPTIONS}", localizationOptions);
    }

    private String adjustWebhooks(String scope, String form, StylePackageModel stylePackage) {
        Collection<SettingName> webhooksNames = getSettingNames(WebhooksSettings.FEATURE_NAME, scope);
        boolean noHooks = StringUtils.isEmpty(stylePackage.getWebhooks());
        String webhooksOptions = generateSelectOptions(webhooksNames, noHooks ? NamedSettings.DEFAULT_NAME : stylePackage.getWebhooks());
        form = form.replace("{WEBHOOKS_DISPLAY}", PdfExporterExtensionConfiguration.getInstance().getWebhooksEnabled() ? "" : "hidden");
        form = form.replace("{WEBHOOKS_OPTIONS}", webhooksOptions);
        form = form.replace("{WEBHOOKS_SELECTOR_DISPLAY}", noHooks ? "none" : "inline-block");
        return form.replace("{WEBHOOKS_SELECTED}", noHooks ? "" : "checked");
    }

    private Collection<SettingName> getSuitableStylePackages(@NotNull IModule module) {
        String locationPath = module.getModuleLocation().getLocationPath();
        String spaceId = "";
        final String documentName;
        if (locationPath.contains("/")) {
            spaceId = locationPath.substring(0, locationPath.lastIndexOf('/'));
            documentName = locationPath.substring(locationPath.lastIndexOf('/') + 1);
        } else {
            documentName = locationPath;
        }
        return polarionService.getSuitableStylePackages(module.getProject().getId(), spaceId, documentName);
    }

    private Collection<SettingName> getSettingNames(@NotNull String featureName, @NotNull String scope) {
        try {
            return NamedSettingsRegistry.INSTANCE.getByFeatureName(featureName).readNames(scope);
        } catch (IllegalStateException ex) {
            return ExceptionHandler.handleTransactionIllegalStateException(ex, Collections.emptyList());
        }
    }

    private String generateSelectOptions(Collection<SettingName> settingNames, String defaultName) {
        if (!settingNames.isEmpty()) {
             final String nameToPreselect;
             if (defaultName != null && settingNames.stream().map(SettingName::getName).anyMatch(name -> name.equals(defaultName))) {
                 nameToPreselect = defaultName;
             } else {
                 nameToPreselect = NamedSettings.DEFAULT_NAME;
             }

            return settingNames.stream()
                    .map(settingName -> String.format(OPTION_TEMPLATE,
                            settingName.getName(), settingName.getName().equals(nameToPreselect) ? SELECTED : "", settingName.getName()))
                    .collect(Collectors.joining());
        } else {
            return String.format(OPTION_TEMPLATE, NamedSettings.DEFAULT_NAME, SELECTED, NamedSettings.DEFAULT_NAME);
        }
    }

    private String adjustPaperSize(String form, StylePackageModel stylePackage) {
        return form.replace(String.format(OPTION_VALUE, stylePackage.getPaperSize()), String.format(OPTION_SELECTED, stylePackage.getPaperSize()));
    }

    private String adjustOrientation(String form, StylePackageModel stylePackage) {
        return form.replace(String.format(OPTION_VALUE, stylePackage.getOrientation()), String.format(OPTION_SELECTED, stylePackage.getOrientation()));
    }

    private String adjustFitToPage(String form, StylePackageModel stylePackage) {
        return stylePackage.isFitToPage() ? form.replace("<input id='fit-to-page'", "<input id='fit-to-page' checked") : form;
    }

    private String adjustCommentsRendering(String form, StylePackageModel stylePackage) {
        return stylePackage.isRenderComments() ? form.replace("<input id='enable-comments-rendering'", "<input id='enable-comments-rendering' checked") : form;
    }

    private String adjustWatermark(String form, StylePackageModel stylePackage) {
        return stylePackage.isWatermark() ? form.replace("<input id='watermark'", "<input id='watermark' checked") : form;
    }

    private String adjustMarkReferencedWorkitems(String form, StylePackageModel stylePackage) {
        return stylePackage.isMarkReferencedWorkitems() ? form.replace("<input id='mark-referenced-workitems'", "<input id='mark-referenced-workitems' checked") : form;
    }

    private String adjustCutEmptyChapters(String form, StylePackageModel stylePackage) {
        return stylePackage.isCutEmptyChapters() ? form.replace("<input id='cut-empty-chapters'", "<input id='cut-empty-chapters' checked") : form;
    }

    private String adjustCutEmptyWorkitemAttributes(String form, StylePackageModel stylePackage) {
        return stylePackage.isCutEmptyWorkitemAttributes() ? form.replace("<input id='cut-empty-wi-attributes'", "<input id='cut-empty-wi-attributes' checked") : form;
    }

    private String adjustCutLocalURLs(String form, StylePackageModel stylePackage) {
        return stylePackage.isCutLocalURLs() ? form.replace("<input id='cut-urls'", "<input id='cut-urls' checked") : form;
    }

    private String adjustPresentationalHints(String form, StylePackageModel stylePackage) {
        return stylePackage.isFollowHTMLPresentationalHints() ? form.replace("<input id='presentational-hints'", "<input id='presentational-hints' checked") : form;
    }

    private String adjustCustomNumberedListsStyles(String form, StylePackageModel stylePackage) {
        if (!StringUtils.isEmpty(stylePackage.getCustomNumberedListStyles())) {
            form = form.replace("<input id='custom-list-styles'", "<input id='custom-list-styles' checked");
            form = form.replace("id='numbered-list-styles' style='display: none;", String.format("<input id='numbered-list-styles' value='%s' style='", stylePackage.getCustomNumberedListStyles()));
        }
        return form;
    }

    private String adjustChapters(String form, StylePackageModel stylePackage) {
        if (!StringUtils.isEmpty(stylePackage.getSpecificChapters())) {
            form = form.replace("<input id='specific-chapters'", "<input id='specific-chapters' checked");
            form = form.replace("<input id='chapters' style='display: none;", String.format("<input id='chapters' value='%s' style='", stylePackage.getSpecificChapters()));
        }
        return form;
    }

    private String adjustLocalizeEnums(String form, StylePackageModel stylePackage, Object documentLanguageField) {
        String documentLanguage = (documentLanguageField instanceof IEnumOption enumOption) ? enumOption.getId() : "";

        if (!StringUtils.isEmpty(stylePackage.getLanguage())) {
            form = form.replace("<input id='localization'", "<input id='localization' checked");
            form = form.replace("id='language' style='display: none'", "id='language'");

            String languageToPreselect = null;
            for (Language language : Language.values()) {
                if (language.name().equalsIgnoreCase(documentLanguage) || language.getValue().equalsIgnoreCase(documentLanguage)) {
                    languageToPreselect = language.name().toLowerCase();
                    break;
                }
            }
            if (!stylePackage.isExposeSettings() || StringUtils.isEmpty(languageToPreselect)) {
                languageToPreselect = stylePackage.getLanguage();
            }

            form = form.replace(String.format(OPTION_VALUE, languageToPreselect), String.format(OPTION_SELECTED, languageToPreselect));
        }
        return form.replace("{DOCUMENT_LANGUAGE}", documentLanguage);
    }

    private String adjustLinkRoles(@NotNull String form, @NotNull List<String> roleEnumValues, @NotNull StylePackageModel stylePackage) {
        if (!roleEnumValues.isEmpty()) {
            if (!CollectionUtils.isEmpty(stylePackage.getLinkedWorkitemRoles())) {
                form = form.replace("<input id='selected-roles'", "<input id='selected-roles' checked");
                form = form.replace("id='roles-wrapper' style='display: none'", "id='roles-wrapper'");
            }

            String rolesOptions = roleEnumValues.stream()
                    .map(roleEnumValue -> String.format(OPTION_TEMPLATE,
                            roleEnumValue,
                            !CollectionUtils.isEmpty(stylePackage.getLinkedWorkitemRoles()) && stylePackage.getLinkedWorkitemRoles().contains(roleEnumValue) ? SELECTED : "",
                            roleEnumValue)
                    ).collect(Collectors.joining());
            return form.replace("{ROLES_OPTIONS}", rolesOptions);
        } else {
            return form.replace("class='roles-fields'", "class='roles-fields' style='display: none;'"); // Hide roles fields when no roles obtained
        }
    }

    private String adjustFilename(@NotNull String form, @NotNull IModule module) {
        String filename = getFilename(module);
        return form.replace("{FILENAME}", filename).replace("{DATA_FILENAME}", filename);
    }

    private String adjustButtons(@NotNull String form, @NotNull IModule module, @NotNull StylePackageModel stylePackage) {
        IProject project = module.getProject();
        String moduleLocationPath = module.getModuleLocation().getLocationPath();
        String params = fillParams(project.getId(), moduleLocationPath);
        form = form.replace("{LOAD_PDF_PARAMS}", params);

        form = form.replace("{VALIDATE_PDF_PARAMS}", params);

        if (!stylePackage.isExposePageWidthValidation()) {
            form = form.replace("id='page-width-validation'", "id='page-width-validation' style='display: none;'");
        }

        return form;
    }

    private String getFilename(@NotNull IModule module) {
        DocumentFileNameHelper documentFileNameHelper = new DocumentFileNameHelper(new PdfExporterPolarionService());

        ExportParams exportParams = ExportParams.builder()
                .projectId(module.getProject().getId())
                .locationPath(module.getModuleLocation().getLocationPath())
                .revision(module.getRevision())
                .documentType(DocumentType.LIVE_DOC)
                .build();

        return documentFileNameHelper.getDocumentFileName(exportParams);
    }

    @Override
    @Nullable
    public String getIcon(@NotNull IPObject object, @Nullable Map<String, String> attributes) {
        return null;
    }

    @Override
    @Nullable
    public String getLabel(@NotNull IPObject object, @Nullable Map<String, String> attributes) {
        return "PDF Exporter";
    }

    private String fillParams(String... params) {
        return Arrays.stream(params).map(p -> p == null ? null : "\"" + p + "\"").collect(Collectors.joining(","));
    }
}

import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';
import ConfigurationsPane from '../../ui/generic/js/modules/ConfigurationsPane.js';
import CustomSelect from '../../ui/generic/js/modules/CustomSelect.js';
import StylePackageUtils from './style-package-utils.js';

const DEFAULT_SETTING_NAME = "Default";

const ctx = new ExtensionContext({
    extension: 'pdf-exporter',
    setting: 'style-package',
    scopeFieldId: 'scope'
});

const conf = new ConfigurationsPane({
    ctx: ctx,
    label: 'style package',
    setConfigurationContentCallback: setStylePackage,
    newConfigurationCallback: newConfigurationCreated
});

ctx.onClick(
    'save-toolbar-button', saveStylePackage,
    'cancel-toolbar-button', ctx.cancelEdit,
    'default-toolbar-button', revertToDefault,
    'revisions-toolbar-button', ctx.toggleRevisions
);

ctx.onBlur(
    'style-package-weight', StylePackageUtils.adjustWeight
);

const ChildConfigurations = {
    cssSelect: new CustomSelect({
        selectContainer: ctx.getElementById("css-select"),
        label: ctx.getElementById("css-select-label")
    }),
    headerFooterSelect: new CustomSelect({
        selectContainer: ctx.getElementById("header-footer-select"),
        label: ctx.getElementById("header-footer-select-label")
    }),
    localizationSelect: new CustomSelect({
        selectContainer: ctx.getElementById("localization-select"),
        label: ctx.getElementById("localization-select-label")
    }),
    coverPageSelect: new CustomSelect({
        selectContainer: ctx.getElementById("cover-page-select")
    }),
    webhooksSelect: new CustomSelect({
        selectContainer: ctx.getElementById("webhooks-select")
    }),

    load: function () {
        ctx.getElementById("child-configs-load-error").style.display = "none";

        return new Promise((resolve) => {
            Promise.all([
                this.loadSettingNames("css", this.cssSelect),
                this.loadSettingNames("header-footer", this.headerFooterSelect),
                this.loadSettingNames("localization", this.localizationSelect),
                this.loadSettingNames("cover-page", this.coverPageSelect),
                this.loadSettingNames("webhooks", this.webhooksSelect)
            ]).then(() => {
                resolve();
            }).catch(() => {
                ctx.getElementById("child-configs-load-error").style.display = "block";
            });
        });
    },

    loadSettingNames: function(setting, select) {
        return new Promise((resolve, reject) => {
            ctx.callAsync({
                method: 'GET',
                url: `/polarion/${ctx.extension}/rest/internal/settings/${setting}/names?scope=${ctx.scope}`,
                contentType: 'application/json',
                onOk: (responseText) => {
                    let namesCount = 0;
                    for (let name of JSON.parse(responseText)) {
                        namesCount++;

                        const addedOption = select.addOption(name.name);
                        if (name.scope !== ctx.scope) {
                            addedOption.checkbox.classList.add('parent');
                            addedOption.label.classList.add('parent');
                        }
                    }
                    if (namesCount === 0) {
                        reject();
                    } else {
                        resolve();
                    }
                },
                onError: () => reject()
            });
        });
    }
}

const LinkRoles = {
    rolesSelect: new CustomSelect({
        selectContainer: ctx.getElementById("roles-select"),
        multiselect: true
    }),

    load: function () {
        ctx.getElementById("link-roles-load-error").style.display = "none";

        return new Promise((resolve, reject) => {
            ctx.callAsync({
                method: 'GET',
                url: `/polarion/${ctx.extension}/rest/internal/link-role-names?scope=${ctx.scope}`,
                contentType: 'application/json',
                onOk: (responseText) => {
                    for (let name of JSON.parse(responseText)) {
                        this.rolesSelect.addOption(name);
                    }
                    resolve();
                },
                onError: () => {
                    ctx.getElementById("link-roles-load-error").style.display = "block";
                    reject();
                }
            });
        });
    }
}

const PaperSizes = {
    paperSizeSelect: new CustomSelect({
        selectContainer: ctx.getElementById("paper-size-select"),
        label: ctx.getElementById("paper-size-label")
    }),

    init: function () {
        this.paperSizeSelect.addOption('A5', 'A5');
        this.paperSizeSelect.addOption('A4', 'A4');
        this.paperSizeSelect.addOption('A3', 'A3');
        this.paperSizeSelect.addOption('B5', 'B5');
        this.paperSizeSelect.addOption('B4', 'B4');
        this.paperSizeSelect.addOption('JIS_B5', 'JIS-B5');
        this.paperSizeSelect.addOption('JIS_B4', 'JIS-B4');
        this.paperSizeSelect.addOption('LETTER', 'Letter');
        this.paperSizeSelect.addOption('LEGAL', 'Legal');
        this.paperSizeSelect.addOption('LEDGER', 'Ledger');
    }
}

const Orientations = {
    orientationSelect: new CustomSelect({
        selectContainer: ctx.getElementById("orientation-select"),
        label: ctx.getElementById("orientation-label")
    }),

    init: function () {
        this.orientationSelect.addOption('PORTRAIT', 'Portrait');
        this.orientationSelect.addOption('LANDSCAPE', 'Landscape');
    }
}

const PdfVariants = {
    pdfVariantSelect: new CustomSelect({
        selectContainer: ctx.getElementById("pdf-variant-select"),
        label: ctx.getElementById("pdf-variant-label")
    }),

    init: function () {
        this.pdfVariantSelect.addOption('PDF_A_1B', 'pdf/a-1b');
        this.pdfVariantSelect.addOption('PDF_A_2B', 'pdf/a-2b');
        this.pdfVariantSelect.addOption('PDF_A_3B', 'pdf/a-3b');
        this.pdfVariantSelect.addOption('PDF_A_4B', 'pdf/a-4b');
        this.pdfVariantSelect.addOption('PDF_A_2U', 'pdf/a-2u');
        this.pdfVariantSelect.addOption('PDF_A_3U', 'pdf/a-3u');
        this.pdfVariantSelect.addOption('PDF_A_4U', 'pdf/a-4u');
        this.pdfVariantSelect.addOption('PDF_UA_1', 'pdf/ua-1');
    }
}

const RenderComments = {
    renderCommentsSelect: new CustomSelect({
        selectContainer: ctx.getElementById("render-comments-select"),
        label: ctx.getElementById("render-comments-label")
    }),

    init: function () {
        this.renderCommentsSelect.addOption('OPEN', 'Open');
        this.renderCommentsSelect.addOption('ALL', 'All');
    }
}

const Languages = {
    languageSelect: new CustomSelect({
        selectContainer: ctx.getElementById("language-select")
    }),

    init: function () {
        this.languageSelect.addOption('de', 'Deutsch');
        this.languageSelect.addOption('fr', new DOMParser().parseFromString(`Fran&ccedil;ais`, 'text/html').body.textContent);
        this.languageSelect.addOption('it', 'Italiano');
    }
}

function saveStylePackage() {
    ctx.hideActionAlerts();

    ctx.callAsync({
        method: 'PUT',
        url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${conf.getSelectedConfiguration()}/content?scope=${ctx.scope}`,
        contentType: 'application/json',
        body: JSON.stringify({
            'matchingQuery': ctx.getValueById('matching-query'),
            'weight': ctx.getValueById('style-package-weight'),
            'exposeSettings': ctx.getCheckboxValueById('exposeSettings'),
            'coverPage': ctx.getCheckboxValueById('cover-page-checkbox') ? ChildConfigurations.coverPageSelect.getSelectedValue() : null,
            'css': ChildConfigurations.cssSelect.getSelectedValue(),
            'headerFooter': ChildConfigurations.headerFooterSelect.getSelectedValue(),
            'localization': ChildConfigurations.localizationSelect.getSelectedValue(),
            'webhooks': ctx.getCheckboxValueById('webhooks-checkbox') ? ChildConfigurations.webhooksSelect.getSelectedValue() : null,
            'headersColor': ctx.getValueById('headers-color'),
            'paperSize': PaperSizes.paperSizeSelect.getSelectedValue(),
            'orientation': Orientations.orientationSelect.getSelectedValue(),
            'pdfVariant': PdfVariants.pdfVariantSelect.getSelectedValue(),
            'fitToPage': ctx.getCheckboxValueById('fit-to-page'),
            'renderComments': ctx.getCheckboxValueById('render-comments') ? RenderComments.renderCommentsSelect.getSelectedValue() : null,
            'watermark': ctx.getCheckboxValueById('watermark'),
            'markReferencedWorkitems': ctx.getCheckboxValueById('mark-referenced-workitems'),
            'cutEmptyChapters': ctx.getCheckboxValueById('cut-empty-chapters'),
            'cutEmptyWorkitemAttributes': ctx.getCheckboxValueById('cut-empty-wi-attributes'),
            'cutLocalURLs': ctx.getCheckboxValueById('cut-urls'),
            'followHTMLPresentationalHints': ctx.getCheckboxValueById('presentational-hints'),
            'specificChapters': ctx.getCheckboxValueById('specific-chapters') ? ctx.getValueById('chapters') : null,
            'customNumberedListStyles': ctx.getCheckboxValueById('custom-list-styles') ? ctx.getValueById('numbered-list-styles') : null,
            'language': ctx.getCheckboxValueById('localization') ? Languages.languageSelect.getSelectedValue() : null,
            'linkedWorkitemRoles': ctx.getCheckboxValueById('selected-roles') ? LinkRoles.rolesSelect.getSelectedValue() : null,
            'exposePageWidthValidation': ctx.getCheckboxValueById('expose-page-width-validation'),
            'attachmentsFilter': ctx.getCheckboxValueById('download-attachments') ? ctx.getValueById('attachments-filter') : null,
            'testcaseFieldId': ctx.getCheckboxValueById('download-attachments') ? ctx.getValueById('testcase-field-id') : null,
            'embedAttachments': ctx.getCheckboxValueById('download-attachments') && ctx.getCheckboxValueById('embed-attachments'),
        }),
        onOk: () => {
            ctx.showSaveSuccessAlert();
            ctx.setNewerVersionNotificationVisible(false);
            conf.loadConfigurationNames();
        },
        onError: () => ctx.showSaveErrorAlert()
    });
}

function revertToDefault() {
    if (confirm("Are you sure you want to return the default value?")) {
        ctx.setLoadingErrorNotificationVisible(false);
        ctx.hideActionAlerts();

        ctx.callAsync({
            method: 'GET',
            url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/default-content`,
            contentType: 'application/json',
            onOk: (responseText) => {
                ctx.showRevertedToDefaultAlert();
                setStylePackage(responseText);
            },
            onError: () => ctx.setLoadingErrorNotificationVisible(true)
        });
    }
}

function setStylePackage(content) {
    const stylePackage = JSON.parse(content);

    ctx.setValueById('matching-query', stylePackage.matchingQuery || "");
    ctx.setValueById('style-package-weight', stylePackage.weight);
    ctx.getElementById('matching-query-container').style.display = DEFAULT_SETTING_NAME === conf.getSelectedConfiguration() ? "none" : "flex";

    ctx.setCheckboxValueById('exposeSettings', stylePackage.exposeSettings);
    ctx.setCheckboxValueById('cover-page-checkbox', !!stylePackage.coverPage);
    ctx.getElementById('cover-page-checkbox').dispatchEvent(new Event('change'));
    ChildConfigurations.coverPageSelect.selectValue(ChildConfigurations.coverPageSelect.containsOption(stylePackage.coverPage) ? stylePackage.coverPage : DEFAULT_SETTING_NAME);
    ChildConfigurations.cssSelect.selectValue(ChildConfigurations.cssSelect.containsOption(stylePackage.css) ? stylePackage.css : DEFAULT_SETTING_NAME);
    ChildConfigurations.headerFooterSelect.selectValue(ChildConfigurations.headerFooterSelect.containsOption(stylePackage.headerFooter) ? stylePackage.headerFooter : DEFAULT_SETTING_NAME);
    ChildConfigurations.localizationSelect.selectValue(ChildConfigurations.localizationSelect.containsOption(stylePackage.localization) ? stylePackage.localization : DEFAULT_SETTING_NAME);
    ctx.setCheckboxValueById('webhooks-checkbox', !!stylePackage.webhooks);
    ctx.getElementById('webhooks-checkbox').dispatchEvent(new Event('change'));
    ChildConfigurations.webhooksSelect.selectValue(ChildConfigurations.webhooksSelect.containsOption(stylePackage.webhooks) ? stylePackage.webhooks : DEFAULT_SETTING_NAME);

    ctx.setValueById('headers-color', stylePackage.headersColor);
    PaperSizes.paperSizeSelect.selectValue(stylePackage.paperSize || 'A4');
    Orientations.orientationSelect.selectValue(stylePackage.orientation || 'PORTRAIT');
    PdfVariants.pdfVariantSelect.selectValue(stylePackage.pdfVariant || 'PDF_A_2B');

    ctx.setCheckboxValueById('fit-to-page', stylePackage.fitToPage);

    ctx.setCheckboxValueById('render-comments', !!stylePackage.renderComments);
    ctx.getElementById('render-comments').dispatchEvent(new Event('change'));
    RenderComments.renderCommentsSelect.selectValue(stylePackage.renderComments || 'OPEN');

    ctx.setCheckboxValueById('watermark', stylePackage.watermark);
    ctx.setCheckboxValueById('mark-referenced-workitems', stylePackage.markReferencedWorkitems);

    ctx.setCheckboxValueById('cut-empty-chapters', stylePackage.cutEmptyChapters);
    ctx.setCheckboxValueById('cut-empty-wi-attributes', stylePackage.cutEmptyWorkitemAttributes);
    ctx.setCheckboxValueById('cut-urls', stylePackage.cutLocalURLs);
    ctx.setCheckboxValueById('presentational-hints', stylePackage.followHTMLPresentationalHints);

    ctx.setCheckboxValueById('specific-chapters', !!stylePackage.specificChapters);
    ctx.getElementById('specific-chapters').dispatchEvent(new Event('change'));
    ctx.setValueById('chapters', stylePackage.specificChapters || "");

    ctx.setCheckboxValueById('custom-list-styles', !!stylePackage.customNumberedListStyles);
    ctx.getElementById('custom-list-styles').dispatchEvent(new Event('change'));
    ctx.setValueById('numbered-list-styles', stylePackage.customNumberedListStyles || "");

    ctx.setCheckboxValueById('localization', !!stylePackage.language);
    ctx.getElementById('localization').dispatchEvent(new Event('change'));
    Languages.languageSelect.selectValue(stylePackage.language);

    const rolesProvided = stylePackage.linkedWorkitemRoles && stylePackage.linkedWorkitemRoles.length && stylePackage.linkedWorkitemRoles.length > 0;
    ctx.setCheckboxValueById('selected-roles', rolesProvided);
    ctx.getElementById('selected-roles').dispatchEvent(new Event('change'));
    LinkRoles.rolesSelect.selectMultipleValues(stylePackage.linkedWorkitemRoles);

    ctx.setCheckboxValueById('download-attachments', !!stylePackage.attachmentsFilter || !!stylePackage.testcaseFieldId);
    ctx.getElementById('download-attachments').dispatchEvent(new Event('change'));
    ctx.setValueById('attachments-filter', stylePackage.attachmentsFilter || "");
    ctx.setValueById('testcase-field-id', stylePackage.testcaseFieldId || "");
    ctx.setCheckboxValueById('embed-attachments', (!!stylePackage.attachmentsFilter || !!stylePackage.testcaseFieldId) && stylePackage.embedAttachments);

    ctx.setCheckboxValueById('expose-page-width-validation', stylePackage.exposePageWidthValidation);

    ctx.setNewerVersionNotificationVisible(stylePackage.bundleTimestamp && stylePackage.bundleTimestamp !== ctx.getValueById('bundle-timestamp'));
}

function newConfigurationCreated() {
    ctx.setValueById('style-package-weight', 50);
}

PaperSizes.init();
Orientations.init();
PdfVariants.init();
RenderComments.init();
Languages.init();
Promise.all([
    LinkRoles.load(),
    ChildConfigurations.load()
]).then(() => {
    conf.loadConfigurationNames();
});

const DEFAULT_SETTING_NAME = "Default";

SbbCommon.init({
    extension: 'pdf-exporter',
    setting: 'style-package',
    scope: SbbCommon.getValueById('scope')
});
Configurations.init({
    label: 'style package',
    setConfigurationContentCallback: setStylePackage,
    newConfigurationCallback: newConfigurationCreated
});

const ChildConfigurations = {
    cssSelect: new SbbCustomSelect({
        selectContainer: document.getElementById("css-select"),
        label: document.getElementById("css-select-label")
    }),
    headerFooterSelect: new SbbCustomSelect({
        selectContainer: document.getElementById("header-footer-select"),
        label: document.getElementById("header-footer-select-label")
    }),
    localizationSelect: new SbbCustomSelect({
        selectContainer: document.getElementById("localization-select"),
        label: document.getElementById("localization-select-label")
    }),
    coverPageSelect: new SbbCustomSelect({
        selectContainer: document.getElementById("cover-page-select")
    }),
    webhooksSelect: new SbbCustomSelect({
        selectContainer: document.getElementById("webhooks-select")
    }),

    load: function () {
        document.getElementById("child-configs-load-error").style.display = "none";

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
                document.getElementById("child-configs-load-error").style.display = "block";
            });
        });
    },

    loadSettingNames: function(setting, select) {
        return new Promise((resolve, reject) => {
            SbbCommon.callAsync({
                method: 'GET',
                url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${setting}/names?scope=${SbbCommon.scope}`,
                contentType: 'application/json',
                onOk: (responseText) => {
                    let namesCount = 0;
                    for (let name of JSON.parse(responseText)) {
                        namesCount++;

                        const addedOption = select.addOption(name.name);
                        if (name.scope !== SbbCommon.scope) {
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
    rolesSelect: new SbbCustomSelect({
        selectContainer: document.getElementById("roles-select"),
        multiselect: true
    }),

    load: function () {
        document.getElementById("link-roles-load-error").style.display = "none";

        return new Promise((resolve, reject) => {
            SbbCommon.callAsync({
                method: 'GET',
                url: `/polarion/${SbbCommon.extension}/rest/internal/link-role-names?scope=${SbbCommon.scope}`,
                contentType: 'application/json',
                onOk: (responseText) => {
                    for (let name of JSON.parse(responseText)) {
                        this.rolesSelect.addOption(name);
                    }
                    resolve();
                },
                onError: () => {
                    document.getElementById("link-roles-load-error").style.display = "block";
                    reject();
                }
            });
        });
    }
}

const PaperSizes = {
    paperSizeSelect: new SbbCustomSelect({
        selectContainer: document.getElementById("paper-size-select"),
        label: document.getElementById("paper-size-label")
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
    orientationSelect: new SbbCustomSelect({
        selectContainer: document.getElementById("orientation-select"),
        label: document.getElementById("orientation-label")
    }),

    init: function () {
        this.orientationSelect.addOption('PORTRAIT', 'Portrait');
        this.orientationSelect.addOption('LANDSCAPE', 'Landscape');
    }
}

const Languages = {
    languageSelect: new SbbCustomSelect({
        selectContainer: document.getElementById("language-select")
    }),

    init: function () {
        this.languageSelect.addOption('de', 'Deutsch');
        this.languageSelect.addOption('fr', new DOMParser().parseFromString(`Fran&ccedil;ais`, 'text/html').body.textContent);
        this.languageSelect.addOption('it', 'Italiano');
    }
}

function saveStylePackage() {
    SbbCommon.hideActionAlerts();

    SbbCommon.callAsync({
        method: 'PUT',
        url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${Configurations.getSelectedConfiguration()}/content?scope=${SbbCommon.scope}`,
        contentType: 'application/json',
        body: JSON.stringify({
            'matchingQuery': SbbCommon.getValueById('matching-query'),
            'weight': SbbCommon.getValueById('style-package-weight'),
            'exposeSettings': SbbCommon.getCheckboxValueById('exposeSettings'),
            'coverPage': SbbCommon.getCheckboxValueById('cover-page-checkbox') ? ChildConfigurations.coverPageSelect.getSelectedValue() : null,
            'css': ChildConfigurations.cssSelect.getSelectedValue(),
            'headerFooter': ChildConfigurations.headerFooterSelect.getSelectedValue(),
            'localization': ChildConfigurations.localizationSelect.getSelectedValue(),
            'webhooks': SbbCommon.getCheckboxValueById('webhooks-checkbox') ? ChildConfigurations.webhooksSelect.getSelectedValue() : null,
            'headersColor': SbbCommon.getValueById('headers-color'),
            'paperSize': PaperSizes.paperSizeSelect.getSelectedValue(),
            'orientation': Orientations.orientationSelect.getSelectedValue(),
            'fitToPage': SbbCommon.getCheckboxValueById('fit-to-page'),
            'renderComments': SbbCommon.getCheckboxValueById('enable-comments-rendering'),
            'watermark': SbbCommon.getCheckboxValueById('watermark'),
            'markReferencedWorkitems': SbbCommon.getCheckboxValueById('mark-referenced-workitems'),
            'cutEmptyChapters': SbbCommon.getCheckboxValueById('cut-empty-chapters'),
            'cutEmptyWorkitemAttributes': SbbCommon.getCheckboxValueById('cut-empty-wi-attributes'),
            'cutLocalURLs': SbbCommon.getCheckboxValueById('cut-urls'),
            'followHTMLPresentationalHints': SbbCommon.getCheckboxValueById('presentational-hints'),
            'specificChapters': SbbCommon.getCheckboxValueById('specific-chapters') ? SbbCommon.getValueById('chapters') : null,
            'customNumberedListStyles': SbbCommon.getCheckboxValueById('custom-list-styles') ? SbbCommon.getValueById('numbered-list-styles') : null,
            'language': SbbCommon.getCheckboxValueById('localization') ? Languages.languageSelect.getSelectedValue() : null,
            'linkedWorkitemRoles': SbbCommon.getCheckboxValueById('selected-roles') ? LinkRoles.rolesSelect.getSelectedValue() : null,
            'exposePageWidthValidation': SbbCommon.getCheckboxValueById('expose-page-width-validation'),
            'attachmentsFilter': SbbCommon.getCheckboxValueById('download-attachments') ? SbbCommon.getValueById('attachments-filter') : null,
        }),
        onOk: () => {
            SbbCommon.showSaveSuccessAlert();
            SbbCommon.setNewerVersionNotificationVisible(false);
            Configurations.loadConfigurationNames();
        },
        onError: () => SbbCommon.showSaveErrorAlert()
    });
}

function revertToDefault() {
    if (confirm("Are you sure you want to return the default value?")) {
        SbbCommon.setLoadingErrorNotificationVisible(false);
        SbbCommon.hideActionAlerts();

        SbbCommon.callAsync({
            method: 'GET',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/default-content`,
            contentType: 'application/json',
            onOk: (responseText) => {
                SbbCommon.showRevertedToDefaultAlert();
                setStylePackage(responseText);
            },
            onError: () => SbbCommon.setLoadingErrorNotificationVisible(true)
        });
    }
}

function setStylePackage(content) {
    const stylePackage = JSON.parse(content);

    SbbCommon.setValueById('matching-query', stylePackage.matchingQuery || "");
    SbbCommon.setValueById('style-package-weight', stylePackage.weight);
    document.getElementById('matching-query-container').style.display = DEFAULT_SETTING_NAME === Configurations.getSelectedConfiguration() ? "none" : "flex";

    SbbCommon.setCheckboxValueById('exposeSettings', stylePackage.exposeSettings);
    SbbCommon.setCheckboxValueById('cover-page-checkbox', !!stylePackage.coverPage);
    document.getElementById('cover-page-checkbox').dispatchEvent(new Event('change'));
    ChildConfigurations.coverPageSelect.selectValue(ChildConfigurations.coverPageSelect.containsOption(stylePackage.coverPage) ? stylePackage.coverPage : DEFAULT_SETTING_NAME);
    ChildConfigurations.cssSelect.selectValue(ChildConfigurations.cssSelect.containsOption(stylePackage.css) ? stylePackage.css : DEFAULT_SETTING_NAME);
    ChildConfigurations.headerFooterSelect.selectValue(ChildConfigurations.headerFooterSelect.containsOption(stylePackage.headerFooter) ? stylePackage.headerFooter : DEFAULT_SETTING_NAME);
    ChildConfigurations.localizationSelect.selectValue(ChildConfigurations.localizationSelect.containsOption(stylePackage.localization) ? stylePackage.localization : DEFAULT_SETTING_NAME);
    SbbCommon.setCheckboxValueById('webhooks-checkbox', !!stylePackage.webhooks);
    document.getElementById('webhooks-checkbox').dispatchEvent(new Event('change'));
    ChildConfigurations.webhooksSelect.selectValue(ChildConfigurations.webhooksSelect.containsOption(stylePackage.webhooks) ? stylePackage.webhooks : DEFAULT_SETTING_NAME);

    SbbCommon.setValueById('headers-color', stylePackage.headersColor);
    PaperSizes.paperSizeSelect.selectValue(stylePackage.paperSize || 'A4');
    Orientations.orientationSelect.selectValue(stylePackage.orientation);

    SbbCommon.setCheckboxValueById('fit-to-page', stylePackage.fitToPage);
    SbbCommon.setCheckboxValueById('enable-comments-rendering', stylePackage.renderComments);
    SbbCommon.setCheckboxValueById('watermark', stylePackage.watermark);
    SbbCommon.setCheckboxValueById('mark-referenced-workitems', stylePackage.markReferencedWorkitems);

    SbbCommon.setCheckboxValueById('cut-empty-chapters', stylePackage.cutEmptyChapters);
    SbbCommon.setCheckboxValueById('cut-empty-wi-attributes', stylePackage.cutEmptyWorkitemAttributes);
    SbbCommon.setCheckboxValueById('cut-urls', stylePackage.cutLocalURLs);
    SbbCommon.setCheckboxValueById('presentational-hints', stylePackage.followHTMLPresentationalHints);

    SbbCommon.setCheckboxValueById('specific-chapters', !!stylePackage.specificChapters);
    document.getElementById('specific-chapters').dispatchEvent(new Event('change'));
    SbbCommon.setValueById('chapters', stylePackage.specificChapters || "");

    SbbCommon.setCheckboxValueById('custom-list-styles', !!stylePackage.customNumberedListStyles);
    document.getElementById('custom-list-styles').dispatchEvent(new Event('change'));
    SbbCommon.setValueById('numbered-list-styles', stylePackage.customNumberedListStyles || "");

    SbbCommon.setCheckboxValueById('localization', !!stylePackage.language);
    document.getElementById('localization').dispatchEvent(new Event('change'));
    Languages.languageSelect.selectValue(stylePackage.language);

    SbbCommon.setCheckboxValueById('selected-roles', !!stylePackage.linkedWorkitemRoles);
    document.getElementById('selected-roles').dispatchEvent(new Event('change'));
    LinkRoles.rolesSelect.selectMultipleValues(stylePackage.linkedWorkitemRoles);

    SbbCommon.setCheckboxValueById('download-attachments', !!stylePackage.attachmentsFilter);
    document.getElementById('download-attachments').dispatchEvent(new Event('change'));
    SbbCommon.setValueById('attachments-filter', stylePackage.attachmentsFilter || "");

    SbbCommon.setCheckboxValueById('expose-page-width-validation', stylePackage.exposePageWidthValidation);

    if (stylePackage.bundleTimestamp !== SbbCommon.getValueById('bundle-timestamp')) {
        SbbCommon.setNewerVersionNotificationVisible(true);
    }
}

function newConfigurationCreated() {
    SbbCommon.setValueById('style-package-weight', 50);
}

PaperSizes.init();
Orientations.init();
Languages.init();
Promise.all([
    LinkRoles.load(),
    ChildConfigurations.load()
]).then(() => {
    Configurations.loadConfigurationNames();
});

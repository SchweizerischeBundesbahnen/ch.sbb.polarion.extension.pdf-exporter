import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';

const DEFAULT_SETTING_NAME = "Default";

const ctx = new ExtensionContext({
    extension: 'pdf-exporter',
    setting: 'filename-template',
    scopeFieldId: 'scope',
    initCodeInput: true
});

ctx.getElementById("default-toolbar-button").style.display = "none";

ctx.onClick(
    'save-toolbar-button', saveSettings,
    'cancel-toolbar-button', ctx.cancelEdit,
    'revisions-toolbar-button', ctx.toggleRevisions
);

let defaultSettings = null;

function saveSettings() {
    ctx.hideActionAlerts();

    const useCustomValues = ctx.getCheckboxValueById('use-custom-values');
    const requestBody = useCustomValues
        ? JSON.stringify({
            'useCustomValues' : true,
            'documentNameTemplate': ctx.getValueById('custom-document-name-template'),
            'reportNameTemplate': ctx.getValueById('custom-report-name-template'),
            'testRunNameTemplate': ctx.getValueById('custom-testrun-name-template')
        })
        : JSON.stringify({
            'useCustomValues' : false,
            'documentNameTemplate': '',
            'reportNameTemplate': '',
            'testRunNameTemplate': ''
        });

    ctx.callAsync({
        method: 'PUT',
        url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${DEFAULT_SETTING_NAME}/content?scope=${ctx.scope}`,
        contentType: 'application/json',
        body: requestBody,
        onOk: () => {
            ctx.showSaveSuccessAlert();
            ctx.setNewerVersionNotificationVisible(false);
            readAndFillRevisions();
        },
        onError: () => ctx.showSaveErrorAlert()
    });
}

function setSettings(content) {
    const settings = JSON.parse(content);
    ctx.setCheckboxValueById('use-custom-values', settings.useCustomValues);
    ctx.getElementById("use-custom-values").dispatchEvent(new Event('change'));
    if (!settings.useCustomValues) {
        ctx.getElementById("default-templates").checked = true; // Preselect default templates tab if usage of custom templates wasn't activated
    }
    ctx.setValueById('custom-document-name-template', settings.documentNameTemplate);
    ctx.setValueById('custom-report-name-template', settings.reportNameTemplate);
    ctx.setValueById('custom-testrun-name-template', settings.testRunNameTemplate);

    loadDefaultContent().then((defaultSettings) => {
        ctx.setValueById('default-document-name-template', defaultSettings.documentNameTemplate);
        ctx.setValueById('default-report-name-template', defaultSettings.reportNameTemplate);
        ctx.setValueById('default-testrun-name-template', defaultSettings.testRunNameTemplate);
    });
}

function readSettings() {
    ctx.callAsync({
        method: 'GET',
        url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${DEFAULT_SETTING_NAME}/content?scope=${ctx.scope}`,
        contentType: 'application/json',
        onOk: (responseText) => {
            setSettings(responseText)
        },
        onError: () => ctx.setLoadingErrorNotificationVisible(true)
    });
}

function readAndFillRevisions() {
    ctx.readAndFillRevisions({
        revertToRevisionCallback: (responseText) => setSettings(responseText)
    });
}

function loadDefaultContent() {
    if (defaultSettings != null) {
        return Promise.resolve(defaultSettings);
    } else {
        return new Promise((resolve, reject) => {
            ctx.setLoadingErrorNotificationVisible(false);
            ctx.hideActionAlerts();

            ctx.callAsync({
                method: 'GET',
                url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/default-content`,
                contentType: 'application/json',
                onOk: (responseText) => {
                    defaultSettings = JSON.parse(responseText);
                    resolve(defaultSettings);
                },
                onError: () => {
                    ctx.setLoadingErrorNotificationVisible(true);
                    reject();
                }
            });
        });
    }
}

ctx.getElementById("use-custom-values").addEventListener("change", (event) => {
    if (event.target.checked) {
        ctx.getElementById("custom-templates").disabled = false;
    } else {
        ctx.getElementById("default-templates").checked = true;
        ctx.getElementById("custom-templates").disabled = true;
    }
});

readSettings();

import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';

const DEFAULT_SETTING_NAME = "Default";

const ctx = new ExtensionContext({
    extension: 'pdf-exporter',
    setting: 'filename-template',
    scopeFieldId: 'scope',
    initCodeInput: true
});

ctx.onClick(
    'save-toolbar-button', saveSettings,
    'cancel-toolbar-button', ctx.cancelEdit,
    'default-toolbar-button', revertToDefault,
    'revisions-toolbar-button', ctx.toggleRevisions
);

function saveSettings() {
    ctx.hideActionAlerts();

    ctx.callAsync({
        method: 'PUT',
        url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${DEFAULT_SETTING_NAME}/content?scope=${ctx.scope}`,
        contentType: 'application/json',
        body: JSON.stringify({
            'documentNameTemplate': ctx.getValueById('document-name-template'),
            'reportNameTemplate': ctx.getValueById('report-name-template'),
            'testRunNameTemplate': ctx.getValueById('testrun-name-template')
        }),
        onOk: () => {
            ctx.showSaveSuccessAlert();
            ctx.setNewerVersionNotificationVisible(false);
            readAndFillRevisions();
        },
        onError: () => ctx.showSaveErrorAlert()
    });
}

function revertToDefault() {
    if (confirm("Are you sure you want to return the default values?")) {
        ctx.setLoadingErrorNotificationVisible(false);
        ctx.hideActionAlerts();

        ctx.callAsync({
            method: 'GET',
            url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/default-content`,
            contentType: 'application/json',
            onOk: (responseText) => {
                ctx.showRevertedToDefaultAlert();
                setSettings(responseText);
            },
            onError: () => ctx.setLoadingErrorNotificationVisible(true)
        });
    }
}

function setSettings(content) {
    const settings = JSON.parse(content);
    ctx.setValueById('document-name-template', settings.documentNameTemplate);
    ctx.setValueById('report-name-template', settings.reportNameTemplate);
    ctx.setValueById('testrun-name-template', settings.testRunNameTemplate);

    if (settings.bundleTimestamp !== ctx.getValueById('bundle-timestamp')) {
        ctx.setNewerVersionNotificationVisible(true);
    }
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

readSettings();

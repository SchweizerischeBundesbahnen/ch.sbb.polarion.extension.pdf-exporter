const DEFAULT_SETTING_NAME = "Default";

SbbCommon.init({
    extension: 'pdf-exporter',
    setting: 'filename-template',
    scope: SbbCommon.getValueById('scope'),
    initCodeInput: true
});

function saveSettings() {
    SbbCommon.hideActionAlerts();

    SbbCommon.callAsync({
        method: 'PUT',
        url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${DEFAULT_SETTING_NAME}/content?scope=${SbbCommon.scope}`,
        contentType: 'application/json',
        body: JSON.stringify({
            'documentNameTemplate': SbbCommon.getValueById('document-name-template'),
            'reportNameTemplate': SbbCommon.getValueById('report-name-template'),
            'testrunNameTemplate': SbbCommon.getValueById('testrun-name-template')
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
    if (confirm("Are you sure you want to return the default values?")) {
        SbbCommon.setLoadingErrorNotificationVisible(false);
        SbbCommon.hideActionAlerts();

        SbbCommon.callAsync({
            method: 'GET',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/default-content`,
            contentType: 'application/json',
            onOk: (responseText) => {
                SbbCommon.showRevertedToDefaultAlert();
                setSettings(responseText);
            },
            onError: () => SbbCommon.setLoadingErrorNotificationVisible(true)
        });
    }
}

function setSettings(content) {
    const settings = JSON.parse(content);
    SbbCommon.setValueById('document-name-template', settings.documentNameTemplate);
    SbbCommon.setValueById('report-name-template', settings.reportNameTemplate);
    SbbCommon.setValueById('testrun-name-template', settings.testrunNameTemplate);

    if (settings.bundleTimestamp !== SbbCommon.getValueById('bundle-timestamp')) {
        SbbCommon.setNewerVersionNotificationVisible(true);
    }
}

function readSettings() {
    SbbCommon.callAsync({
        method: 'GET',
        url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${DEFAULT_SETTING_NAME}/content?scope=${SbbCommon.scope}`,
        contentType: 'application/json',
        onOk: (responseText) => {
            setSettings(responseText)
        },
        onError: () => SbbCommon.setLoadingErrorNotificationVisible(true)
    });
}

readSettings();

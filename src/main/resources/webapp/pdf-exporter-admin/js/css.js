SbbCommon.init({
    extension: 'pdf-exporter',
    setting: 'css',
    scope: SbbCommon.getValueById('scope'),
    initCodeInput: true
});
Configurations.init({
    setConfigurationContentCallback: setCss
});

function saveCss() {
    SbbCommon.hideActionAlerts();

    SbbCommon.callAsync({
        method: 'PUT',
        url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${Configurations.getSelectedConfiguration()}/content?scope=${SbbCommon.scope}`,
        contentType: 'application/json',
        body: JSON.stringify({
            'css': SbbCommon.getValueById('css-input')
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
        loadDefaultContent()
            .then((responseText) => {
                setCss(responseText);
                SbbCommon.showRevertedToDefaultAlert();
            })
    }
}

function setCss(text) {
    const cssModel = JSON.parse(text);
    SbbCommon.setValueById('css-input', cssModel.css);
    if (cssModel.bundleTimestamp !== SbbCommon.getValueById('bundle-timestamp')) {
        loadDefaultContent()
            .then((responseText) => {
                const defaultCssModel = JSON.parse(responseText);
                SbbCommon.setNewerVersionNotificationVisible(cssModel.css && defaultCssModel.css
                    && (cssModel.css.length !== defaultCssModel.css.length || cssModel.css !== defaultCssModel.css));
            })
    }
}

function loadDefaultContent() {
    return new Promise((resolve, reject) => {
        SbbCommon.setLoadingErrorNotificationVisible(false);
        SbbCommon.hideActionAlerts();

        SbbCommon.callAsync({
            method: 'GET',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/default-content`,
            contentType: 'application/json',
            onOk: (responseText) => resolve(responseText),
            onError: () => {
                SbbCommon.setLoadingErrorNotificationVisible(true);
                reject();
            }
        });
    });
}

Configurations.loadConfigurationNames();

SbbCommon.init({
    extension: 'pdf-exporter',
    setting: 'header-footer',
    scope: SbbCommon.getValueById('scope'),
    initCodeInput: true
});
Configurations.init({
    setConfigurationContentCallback: setHeaderFooterContent
});

function saveHeaderFooter() {
    SbbCommon.hideActionAlerts();

    SbbCommon.callAsync({
        method: 'PUT',
        url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${Configurations.getSelectedConfiguration()}/content?scope=${SbbCommon.scope}`,
        contentType: 'application/json',
        body: JSON.stringify({
            'headerLeft': SbbCommon.getValueById('top-left'),
            'headerCenter': SbbCommon.getValueById('top-center'),
            'headerRight': SbbCommon.getValueById('top-right'),
            'footerLeft': SbbCommon.getValueById('bottom-left'),
            'footerCenter': SbbCommon.getValueById('bottom-center'),
            'footerRight': SbbCommon.getValueById('bottom-right')
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
                setHeaderFooterContent(responseText);
            },
            onError: () => SbbCommon.setLoadingErrorNotificationVisible(true)
        });
    }
}

function setHeaderFooterContent(content) {
    const headerFooter = JSON.parse(content);
    SbbCommon.setValueById('top-left', headerFooter.headerLeft);
    SbbCommon.setValueById('top-center', headerFooter.headerCenter);
    SbbCommon.setValueById('top-right', headerFooter.headerRight);
    SbbCommon.setValueById('bottom-left', headerFooter.footerLeft);
    SbbCommon.setValueById('bottom-center', headerFooter.footerCenter);
    SbbCommon.setValueById('bottom-right', headerFooter.footerRight);

    if (headerFooter.bundleTimestamp !== SbbCommon.getValueById('bundle-timestamp')) {
        SbbCommon.setNewerVersionNotificationVisible(true);
    }
}

Configurations.loadConfigurationNames();
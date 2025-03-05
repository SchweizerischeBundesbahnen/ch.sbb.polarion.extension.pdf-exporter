import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';
import ConfigurationsPane from '../../ui/generic/js/modules/ConfigurationsPane.js';

const ctx = new ExtensionContext({
    extension: 'pdf-exporter',
    setting: 'header-footer',
    scopeFieldId: 'scope',
    initCodeInput: true
});

const conf = new ConfigurationsPane({
    ctx: ctx,
    setConfigurationContentCallback: setHeaderFooterContent,
});

ctx.onClick(
    'save-toolbar-button', saveHeaderFooter,
    'cancel-toolbar-button', ctx.cancelEdit,
    'default-toolbar-button', revertToDefault,
    'revisions-toolbar-button', ctx.toggleRevisions,
);

function saveHeaderFooter() {
    ctx.hideActionAlerts();

    ctx.callAsync({
        method: 'PUT',
        url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${conf.getSelectedConfiguration()}/content?scope=${ctx.scope}`,
        contentType: 'application/json',
        body: JSON.stringify({
            'headerLeft': ctx.getValueById('top-left'),
            'headerCenter': ctx.getValueById('top-center'),
            'headerRight': ctx.getValueById('top-right'),
            'footerLeft': ctx.getValueById('bottom-left'),
            'footerCenter': ctx.getValueById('bottom-center'),
            'footerRight': ctx.getValueById('bottom-right')
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
    if (confirm("Are you sure you want to return the default values?")) {
        ctx.setLoadingErrorNotificationVisible(false);
        ctx.hideActionAlerts();

        ctx.callAsync({
            method: 'GET',
            url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/default-content`,
            contentType: 'application/json',
            onOk: (responseText) => {
                ctx.showRevertedToDefaultAlert();
                setHeaderFooterContent(responseText);
            },
            onError: () => ctx.setLoadingErrorNotificationVisible(true)
        });
    }
}

function setHeaderFooterContent(content) {
    const headerFooter = JSON.parse(content);
    ctx.setValueById('top-left', headerFooter.headerLeft);
    ctx.setValueById('top-center', headerFooter.headerCenter);
    ctx.setValueById('top-right', headerFooter.headerRight);
    ctx.setValueById('bottom-left', headerFooter.footerLeft);
    ctx.setValueById('bottom-center', headerFooter.footerCenter);
    ctx.setValueById('bottom-right', headerFooter.footerRight);

    if (headerFooter.bundleTimestamp !== ctx.getValueById('bundle-timestamp')) {
        ctx.setNewerVersionNotificationVisible(true);
    }
}

conf.loadConfigurationNames();

import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';
import ConfigurationsPane from '../../ui/generic/js/modules/ConfigurationsPane.js';

const ctx = new ExtensionContext({
    extension: 'pdf-exporter',
    setting: 'css',
    scopeFieldId: 'scope',
    initCodeInput: true
});

const conf = new ConfigurationsPane({
    ctx: ctx,
    setConfigurationContentCallback: setCss,
});

ctx.onClick(
    'save-toolbar-button', saveCss,
    'cancel-toolbar-button', ctx.cancelEdit,
    'default-toolbar-button', revertToDefault,
    'revisions-toolbar-button', ctx.toggleRevisions,
);

function saveCss() {
    ctx.hideActionAlerts();

    ctx.callAsync({
        method: 'PUT',
        url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${conf.getSelectedConfiguration()}/content?scope=${ctx.scope}`,
        contentType: 'application/json',
        body: JSON.stringify({
            'css': ctx.getValueById('css-input')
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
        loadDefaultContent()
            .then((responseText) => {
                setCss(responseText);
                ctx.showRevertedToDefaultAlert();
            })
    }
}

function setCss(text) {
    const cssModel = JSON.parse(text);
    ctx.setValueById('css-input', cssModel.css);
    if (cssModel.bundleTimestamp !== ctx.getValueById('bundle-timestamp')) {
        loadDefaultContent()
            .then((responseText) => {
                const defaultCssModel = JSON.parse(responseText);
                ctx.setNewerVersionNotificationVisible(cssModel.css && defaultCssModel.css
                    && (cssModel.css.length !== defaultCssModel.css.length || cssModel.css !== defaultCssModel.css));
            })
    }
}

function loadDefaultContent() {
    return new Promise((resolve, reject) => {
        ctx.setLoadingErrorNotificationVisible(false);
        ctx.hideActionAlerts();

        ctx.callAsync({
            method: 'GET',
            url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/default-content`,
            contentType: 'application/json',
            onOk: (responseText) => resolve(responseText),
            onError: () => {
                ctx.setLoadingErrorNotificationVisible(true);
                reject();
            }
        });
    });
}

conf.loadConfigurationNames();

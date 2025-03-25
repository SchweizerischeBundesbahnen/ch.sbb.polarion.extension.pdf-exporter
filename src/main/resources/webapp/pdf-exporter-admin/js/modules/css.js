import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';
import ConfigurationsPane from '../../ui/generic/js/modules/ConfigurationsPane.js';

const ctx = new ExtensionContext({
    extension: 'pdf-exporter',
    setting: 'css',
    scopeFieldId: 'scope',
    initCodeInput: true
});

ctx.getElementById("default-toolbar-button").style.display = "none";

ctx.onClick(
    'save-toolbar-button', saveCss,
    'cancel-toolbar-button', ctx.cancelEdit,
    'revisions-toolbar-button', ctx.toggleRevisions,
);

const conf = new ConfigurationsPane({
    ctx: ctx,
    setConfigurationContentCallback: setCss,
});

let defaultCss = null;

function saveCss() {
    ctx.hideActionAlerts();

    const disableDefaultCss = ctx.getCheckboxValueById('disable-default-css');
    const requestBody = {
        'disableDefaultCss': disableDefaultCss,
        'css': ctx.getValueById('custom-css-input')
    };

    ctx.callAsync({
        method: 'PUT',
        url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${conf.getSelectedConfiguration()}/content?scope=${ctx.scope}`,
        contentType: 'application/json',
        body: JSON.stringify(requestBody),
        onOk: () => {
            ctx.showSaveSuccessAlert();
            conf.loadConfigurationNames();
        },
        onError: () => ctx.showSaveErrorAlert()
    });
}

function setCss(text) {
    const cssModel = JSON.parse(text);
    ctx.setCheckboxValueById('disable-default-css', cssModel.disableDefaultCss);
    ctx.setValueById('custom-css-input', cssModel.css);

    loadDefaultContent().then((defaultCss) => { ctx.setValueById('default-css-input', defaultCss); });
}

function loadDefaultContent() {
    if (defaultCss != null) {
        return Promise.resolve(defaultCss);
    } else {
        return new Promise((resolve, reject) => {
            ctx.setLoadingErrorNotificationVisible(false);
            ctx.hideActionAlerts();

            ctx.callAsync({
                method: 'GET',
                url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/default-content`,
                contentType: 'application/json',
                onOk: (responseText) => {
                    defaultCss = JSON.parse(responseText).css;
                    resolve(defaultCss);
                },
                onError: () => {
                    ctx.setLoadingErrorNotificationVisible(true);
                    reject();
                }
            });
        });
    }
}

conf.loadConfigurationNames();

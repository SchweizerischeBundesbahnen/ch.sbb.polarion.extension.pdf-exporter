import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';
import ConfigurationsPane from '../../ui/generic/js/modules/ConfigurationsPane.js';

const ctx = new ExtensionContext({
    extension: 'pdf-exporter',
    setting: 'header-footer',
    scopeFieldId: 'scope',
    initCodeInput: true
});

ctx.getElementById("default-toolbar-button").style.display = "none";

ctx.onClick(
    'save-toolbar-button', saveHeaderFooter,
    'cancel-toolbar-button', ctx.cancelEdit,
    'revisions-toolbar-button', ctx.toggleRevisions,
);

const conf = new ConfigurationsPane({
    ctx: ctx,
    setConfigurationContentCallback: setHeaderFooterContent,
});

let defaultSettings = null;

function saveHeaderFooter() {
    ctx.hideActionAlerts();

    const useCustomValues = ctx.getCheckboxValueById('use-custom-values');

    const requestBody = useCustomValues
        ? {
            'useCustomValues' : true,
            'headerLeft': ctx.getValueById('custom-top-left'),
            'headerCenter': ctx.getValueById('custom-top-center'),
            'headerRight': ctx.getValueById('custom-top-right'),
            'footerLeft': ctx.getValueById('custom-bottom-left'),
            'footerCenter': ctx.getValueById('custom-bottom-center'),
            'footerRight': ctx.getValueById('custom-bottom-right')
        }
        : {
            'useCustomValues' : false,
            'headerLeft': '',
            'headerCenter': '',
            'headerRight': '',
            'footerLeft': '',
            'footerCenter': '',
            'footerRight': ''
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

function setHeaderFooterContent(content) {
    const settings = JSON.parse(content);
    ctx.setCheckboxValueById('use-custom-values', settings.useCustomValues);
    ctx.getElementById("use-custom-values").dispatchEvent(new Event('change'));
    if (settings.useCustomValues) {
        ctx.getElementById("custom-templates").checked = true;
    } else {
        ctx.getElementById("default-templates").checked = true;
    }

    ctx.setValueById('custom-top-left', settings.headerLeft);
    ctx.setValueById('custom-top-center', settings.headerCenter);
    ctx.setValueById('custom-top-right', settings.headerRight);
    ctx.setValueById('custom-bottom-left', settings.footerLeft);
    ctx.setValueById('custom-bottom-center', settings.footerCenter);
    ctx.setValueById('custom-bottom-right', settings.footerRight);

    loadDefaultContent().then((defaultSettings) => {
        ctx.setValueById('default-top-left', defaultSettings.headerLeft);
        ctx.setValueById('default-top-center', defaultSettings.headerCenter);
        ctx.setValueById('default-top-right', defaultSettings.headerRight);
        ctx.setValueById('default-bottom-left', defaultSettings.footerLeft);
        ctx.setValueById('default-bottom-center', defaultSettings.footerCenter);
        ctx.setValueById('default-bottom-right', defaultSettings.footerRight);
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
        ctx.getElementById("custom-templates").checked = true;
    } else {
        ctx.getElementById("default-templates").checked = true;
        ctx.getElementById("custom-templates").disabled = true;
    }
});

conf.loadConfigurationNames();

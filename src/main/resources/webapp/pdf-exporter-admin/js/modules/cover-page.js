import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';
import ConfigurationsPane from '../../ui/generic/js/modules/ConfigurationsPane.js';
import CustomSelect from '../../ui/generic/js/modules/CustomSelect.js';

const ctx = new ExtensionContext({
    extension: 'pdf-exporter',
    setting: 'cover-page',
    scopeFieldId: 'scope',
    initCodeInput: true
});

ctx.getElementById("default-toolbar-button").style.display = "none";

ctx.onClick(
    'save-toolbar-button', saveCoverPage,
    'cancel-toolbar-button', ctx.cancelEdit,
    'revisions-toolbar-button', ctx.toggleRevisions,
);

const conf = new ConfigurationsPane({
    ctx: ctx,
    setConfigurationContentCallback: setCoverPageContent,
    preDeleteCallback: coverPagePreDeleteRoutine
});

let defaultSettings = null;

const Templates = {
    templatesSelect: new CustomSelect({
        selectContainer: document.getElementById("templates-select"),
        label: document.getElementById("templates-label")
    }),

    load: function () {
        this.hideAlerts();

        ctx.onClick(
            'persist-selected-template', () => this.persistSelected()
        );

        ctx.callAsync({
            method: 'GET',
            url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/templates`,
            contentType: 'application/json',
            onOk: (responseText) => {
                let templatesDefined = false;
                for (let name of JSON.parse(responseText)) {
                    this.templatesSelect.addOption(name);
                    templatesDefined = true;
                }
                if (templatesDefined) {
                    document.getElementById("templates-pane").style.display = "block";
                }
            },
            onError: () => Templates.showAlert("templates-load-error")
        });
    },

    persistSelected: function () {
        this.hideAlerts();

        ctx.callAsync({
            method: 'POST',
            url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/templates/${this.templatesSelect.getSelectedValue()}?scope=${ctx.scope}`,
            contentType: 'application/json',
            onOk: () => {
                Templates.showAlert("template-save-success");
                conf.loadConfigurationNames();
            },
            onError: () => Templates.showAlert("template-save-error")
        });
    },

    showAlert: function (containerId) {
        document.getElementById(containerId).style.display = "block";
        setTimeout(function () {
            Templates.hideAlerts();
        }, 5000);
    },

    hideAlerts: function () {
        document.querySelectorAll('#templates-pane .action-alerts .alert').forEach(alertDiv => {
            alertDiv.style.display = 'none';
        });
    }
}

function saveCoverPage() {
    ctx.hideActionAlerts();

    const useCustomValues = ctx.getCheckboxValueById('use-custom-values');

    const requestBody = useCustomValues
        ? JSON.stringify({
            'useCustomValues' : true,
            'templateHtml': ctx.getValueById('custom-template-html-input'),
            'templateCss': ctx.getValueById('custom-template-css-input')
        })
        : JSON.stringify({
            'useCustomValues' : false,
            'templateHtml': '',
            'templateCss': ''
        });

    ctx.callAsync({
        method: 'PUT',
        url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${conf.getSelectedConfiguration()}/content?scope=${ctx.scope}`,
        contentType: 'application/json',
        body: requestBody,
        onOk: () => {
            ctx.showSaveSuccessAlert();
            conf.loadConfigurationNames();
        },
        onError: () => ctx.showSaveErrorAlert()
    });
}

function setCoverPageContent(content) {
    const settings = JSON.parse(content);
    ctx.setCheckboxValueById('use-custom-values', settings.useCustomValues);
    ctx.getElementById("use-custom-values").dispatchEvent(new Event('change'));
    if (settings.useCustomValues) {
        ctx.getElementById("custom-template").checked = true;
    } else {
        ctx.getElementById("default-template").checked = true;
    }

    ctx.setValueById('custom-template-html-input', settings.templateHtml);
    ctx.setValueById('custom-template-css-input', settings.templateCss);

    loadDefaultContent().then((defaultSettings) => {
        ctx.setValueById('default-template-html-input', defaultSettings.templateHtml);
        ctx.setValueById('default-template-css-input', defaultSettings.templateCss);
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

function coverPagePreDeleteRoutine(coverPageName) {
    return new Promise((resolve, reject) => {
        ctx.callAsync({
            method: 'DELETE',
            url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${coverPageName}/images?scope=${ctx.scope}`,
            contentType: 'application/json',
            onOk: () => resolve(),
            onError: () => reject()
        });
    });
}

ctx.getElementById("use-custom-values").addEventListener("change", (event) => {
    if (event.target.checked) {
        ctx.getElementById("custom-template").disabled = false;
    } else {
        ctx.getElementById("default-template").checked = true;
        ctx.getElementById("custom-template").disabled = true;
    }
});

conf.loadConfigurationNames();
Templates.load();

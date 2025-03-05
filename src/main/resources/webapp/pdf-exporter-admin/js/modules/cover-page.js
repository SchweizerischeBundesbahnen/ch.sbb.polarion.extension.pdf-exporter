import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';
import ConfigurationsPane from '../../ui/generic/js/modules/ConfigurationsPane.js';
import CustomSelect from '../../ui/generic/js/modules/CustomSelect.js';

const ctx = new ExtensionContext({
    extension: 'pdf-exporter',
    setting: 'cover-page',
    scopeFieldId: 'scope',
    initCodeInput: true
});

const conf = new ConfigurationsPane({
    ctx: ctx,
    setConfigurationContentCallback: setCoverPageContent,
    preDeleteCallback: coverPagePreDeleteRoutine
});

ctx.onClick(
    'save-toolbar-button', saveCoverPage,
    'cancel-toolbar-button', ctx.cancelEdit,
    'default-toolbar-button', revertToDefault,
    'revisions-toolbar-button', ctx.toggleRevisions,
);

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

    ctx.callAsync({
        method: 'PUT',
        url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${conf.getSelectedConfiguration()}/content?scope=${ctx.scope}`,
        contentType: 'application/json',
        body: JSON.stringify({
            'templateHtml': ctx.getValueById('template-html-input'),
            'templateCss': ctx.getValueById('template-css-input')
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
                setCoverPageContent(responseText);
            },
            onError: () => ctx.setLoadingErrorNotificationVisible(true)
        });
    }
}

function setCoverPageContent(content) {
    const model = JSON.parse(content);
    ctx.setValueById('template-html-input', model.templateHtml);
    ctx.setValueById('template-css-input', model.templateCss);
    if (model.bundleTimestamp !== ctx.getValueById('bundle-timestamp')) {
        ctx.setNewerVersionNotificationVisible(true);
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

conf.loadConfigurationNames();
Templates.load();

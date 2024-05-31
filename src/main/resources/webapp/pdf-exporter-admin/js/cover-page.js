SbbCommon.init({
    extension: 'pdf-exporter',
    setting: 'cover-page',
    scope: SbbCommon.getValueById('scope'),
    initCodeInput: true
});
Configurations.init({
    setConfigurationContentCallback: setCoverPageContent,
    preDeleteCallback: coverPagePreDeleteRoutine
});

const Templates = {
    templatesSelect: new SbbCustomSelect({
        selectContainer: document.getElementById("templates-select"),
        label: document.getElementById("templates-label")
    }),

    load: function () {
        this.hideAlerts();

        SbbCommon.callAsync({
            method: 'GET',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/templates`,
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

        SbbCommon.callAsync({
            method: 'POST',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/templates/${this.templatesSelect.getSelectedValue()}?scope=${SbbCommon.scope}`,
            contentType: 'application/json',
            onOk: () => {
                Templates.showAlert("template-save-success");
                Configurations.loadConfigurationNames();
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
    SbbCommon.hideActionAlerts();

    SbbCommon.callAsync({
        method: 'PUT',
        url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${Configurations.getSelectedConfiguration()}/content?scope=${SbbCommon.scope}`,
        contentType: 'application/json',
        body: JSON.stringify({
            'templateHtml': SbbCommon.getValueById('template-html-input'),
            'templateCss': SbbCommon.getValueById('template-css-input')
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
                setCoverPageContent(responseText);
            },
            onError: () => SbbCommon.setLoadingErrorNotificationVisible(true)
        });
    }
}

function setCoverPageContent(content) {
    const model = JSON.parse(content);
    SbbCommon.setValueById('template-html-input', model.templateHtml);
    SbbCommon.setValueById('template-css-input', model.templateCss);
    if (model.bundleTimestamp !== SbbCommon.getValueById('bundle-timestamp')) {
        SbbCommon.setNewerVersionNotificationVisible(true);
    }
}

function coverPagePreDeleteRoutine(coverPageName) {
    return new Promise((resolve, reject) => {
        SbbCommon.callAsync({
            method: 'DELETE',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${coverPageName}/images?scope=${SbbCommon.scope}`,
            contentType: 'application/json',
            onOk: () => resolve(),
            onError: () => reject()
        });
    });
}

Configurations.loadConfigurationNames();
Templates.load();
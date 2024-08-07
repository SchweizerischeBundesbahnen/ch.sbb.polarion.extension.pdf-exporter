const WebHooks = {

    saveHooks : function () {
        SbbCommon.hideActionAlerts();

        SbbCommon.callAsync({
            method: 'PUT',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${Configurations.getSelectedConfiguration()}/content?scope=${SbbCommon.scope}`,
            contentType: 'application/json',
            body: JSON.stringify({
                'webhooks': Array.from(document.getElementsByClassName("webhook")).map(input => input.value).filter(value => value),
            }),
            onOk: () => {
                SbbCommon.showSaveSuccessAlert();
                SbbCommon.setNewerVersionNotificationVisible(false);
                Configurations.loadConfigurationNames();
            },
            onError: () => SbbCommon.showSaveErrorAlert()
        });
    },

    setHooks: function(text) {
        document.getElementById('webhooks-table').innerHTML = ""; // Remove all records currently displayed first
        const webhooksModel = JSON.parse(text);
        if (webhooksModel.webhooks) {
            webhooksModel.webhooks.forEach(hook => {
                WebHooks.addHook(hook);
            });
        }
        if (webhooksModel.bundleTimestamp !== SbbCommon.getValueById('bundle-timestamp')) {
            WebHooks.loadDefaultContent()
                .then((responseText) => {
                    const defaultHooksModel = JSON.parse(responseText);
                    SbbCommon.setNewerVersionNotificationVisible(webhooksModel.webhooks && defaultHooksModel.webhooks
                        && (webhooksModel.webhooks.length !== defaultHooksModel.webhooks.length
                            || webhooksModel.webhooks.every(function(value, index) { return value === defaultHooksModel.hooks[index]})));
                })
        }
    },

    revertToDefault: function() {
        if (confirm("Are you sure you want to return the default value?")) {
            WebHooks.loadDefaultContent()
                .then((responseText) => {
                    WebHooks.setHooks(responseText);
                    SbbCommon.showRevertedToDefaultAlert();
                })
        }
    },

    loadDefaultContent: function() {
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
    },

    addHook: function(value) {
        const table = document.getElementById('webhooks-table');
        const tableRow = document.createElement('tr');
        tableRow.classList.add('webhook-row');

        const buttonCell = document.createElement('td');
        const removeButton = document.createElement('div');
        removeButton.classList.add('webhook-button');
        removeButton.setAttribute('title', 'Delete this webhook');
        const image = document.createElement('img');
        image.setAttribute('src', '/polarion/ria/images/control/tableMinus.png');
        removeButton.appendChild(image);
        removeButton.addEventListener('click', function () {
            tableRow.remove();
        })
        buttonCell.appendChild(removeButton);
        tableRow.appendChild(buttonCell);

        const labelCell = document.createElement('td');
        const fieldLabel = document.createElement('label');
        fieldLabel.innerHTML = 'Webhook URL: ';
        labelCell.appendChild(fieldLabel);
        tableRow.appendChild(labelCell);

        const fieldCell = document.createElement('td');
        const field = document.createElement('input');
        field.classList.add('fs-14', 'webhook');
        if (value) {
            field.setAttribute("value", value);
        }
        fieldCell.appendChild(field);

        const invalidUrlError = document.createElement('div');
        invalidUrlError.innerHTML = "WARNING: Entered value doesn't seem to be a valid URL";
        invalidUrlError.classList.add('invalid-webhook', 'hidden');
        field.addEventListener("keyup", (event) => {
            const urlPattern = /^(http(s)?:\/\/.)[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)$/g;
            if (!event.target.value || urlPattern.test(event.target.value)) {
                invalidUrlError.classList.add('hidden');
            } else {
                invalidUrlError.classList.remove('hidden');
            }
        });
        fieldCell.appendChild(invalidUrlError);

        tableRow.appendChild(fieldCell);

        table.appendChild(tableRow);
    },
}

SbbCommon.init({
    extension: 'pdf-exporter',
    setting: 'webhooks',
    scope: SbbCommon.getValueById('scope')
});

Configurations.init({
    setConfigurationContentCallback: WebHooks.setHooks
});

Configurations.loadConfigurationNames();
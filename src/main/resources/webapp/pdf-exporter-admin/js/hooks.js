const WebHooks = {
    saveHooks : function () {
        SbbCommon.hideActionAlerts();

        SbbCommon.callAsync({
            method: 'PUT',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${Configurations.getSelectedConfiguration()}/content?scope=${SbbCommon.scope}`,
            contentType: 'application/json',
            body: JSON.stringify({
                'hooks': Array.from(document.getElementsByClassName("hook")).map(input => input.value).filter(value => value),
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
        document.getElementById('hooks-table').innerHTML = ""; // Remove all records currently displayed first
        const hooksModel = JSON.parse(text);
        if (hooksModel.hooks) {
            hooksModel.hooks.forEach(hook => {
                WebHooks.addHook(hook);
            });
        }
        if (hooksModel.bundleTimestamp !== SbbCommon.getValueById('bundle-timestamp')) {
            WebHooks.loadDefaultContent()
                .then((responseText) => {
                    const defaultHooksModel = JSON.parse(responseText);
                    SbbCommon.setNewerVersionNotificationVisible(hooksModel.hooks && defaultHooksModel.hooks
                        && (hooksModel.hooks.length !== defaultHooksModel.hooks.length
                            || hooksModel.hooks.every(function(value, index) { return value === defaultHooksModel.hooks[index]})));
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
        const table = document.getElementById('hooks-table');
        const tableRow = document.createElement('tr');
        tableRow.classList.add('hook-row');

        const buttonCell = document.createElement('td');
        const removeButton = document.createElement('div');
        removeButton.classList.add('hook-button');
        removeButton.setAttribute('title', 'Delete this hook');
        const image = document.createElement('img');
        image.setAttribute('src', '/polarion/ria/images/control/tableMinus.png');
        removeButton.appendChild(image);
        removeButton.addEventListener('click', function () {
            tableRow.remove();
        })
        buttonCell.appendChild(removeButton);
        tableRow.appendChild(buttonCell);

        const fieldCell = document.createElement('td');
        const fieldLabel = document.createElement('label');
        fieldLabel.innerHTML = 'Hook URL: ';
        fieldCell.appendChild(fieldLabel);

        const field = document.createElement('input');
        field.classList.add('fs-14', 'hook');
        if (value) {
            field.setAttribute("value", value);
        }
        fieldCell.appendChild(field);
        tableRow.appendChild(fieldCell);

        table.appendChild(tableRow);
    },

}

SbbCommon.init({
    extension: 'pdf-exporter',
    setting: 'hooks',
    scope: SbbCommon.getValueById('scope')
});

Configurations.init({
    setConfigurationContentCallback: WebHooks.setHooks
});

Configurations.loadConfigurationNames();

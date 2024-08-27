const Webhooks = {

    saveWebhooks: function () {
        SbbCommon.hideActionAlerts();

        const webhookRows = Array.from(document.querySelectorAll('#webhooks-table tr'));
        const webhookConfigs = webhookRows
            .map(row => {
                return {
                    url: row.querySelector('input[name="url"]')?.value,
                    authType: row.querySelector('input[name="auth"]')?.checked ? row.querySelector('select[name="auth_type"]')?.value : null,
                    authTokenName: row.querySelector('input[name="auth"]')?.checked ? row.querySelector('input[name="auth_token_name"]')?.value : null
                };
            });

        SbbCommon.callAsync({
            method: 'PUT',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${Configurations.getSelectedConfiguration()}/content?scope=${SbbCommon.scope}`,
            contentType: 'application/json',
            body: JSON.stringify({
                'webhookConfigs': webhookConfigs,
            }),
            onOk: () => {
                SbbCommon.showSaveSuccessAlert();
                SbbCommon.setNewerVersionNotificationVisible(false);
                Configurations.loadConfigurationNames();
            },
            onError: () => SbbCommon.showSaveErrorAlert()
        });
    },

    setWebhooks: function (text) {
        document.getElementById('webhooks-table').innerHTML = ""; // Remove all records currently displayed first
        const webhooksModel = JSON.parse(text);
        if (webhooksModel.webhookConfigs) {
            webhooksModel.webhookConfigs.forEach(webhookConfig => {
                Webhooks.addWebhook(webhookConfig);
            });
        }
        if (webhooksModel.bundleTimestamp !== SbbCommon.getValueById('bundle-timestamp')) {
            Webhooks.loadDefaultContent()
                .then((responseText) => {
                    const defaultHooksModel = JSON.parse(responseText);
                    SbbCommon.setNewerVersionNotificationVisible(webhooksModel.webhooks && defaultHooksModel.webhooks
                        && (webhooksModel.webhooks.length !== defaultHooksModel.webhooks.length
                            || webhooksModel.webhooks.every(function (value, index) {
                                return value === defaultHooksModel.hooks[index]
                            })));
                })
        }
    },

    revertToDefault: function () {
        if (confirm('Are you sure you want to return the default value?')) {
            Webhooks.loadDefaultContent()
                .then((responseText) => {
                    Webhooks.setWebhooks(responseText);
                    SbbCommon.showRevertedToDefaultAlert();
                })
        }
    },

    loadDefaultContent: function () {
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

    addWebhook: function (webhookConfig) {
        function createTableRow() {
            const tableRow = document.createElement('tr');
            tableRow.classList.add('webhook-row');
            return tableRow;
        }

        function createControlButtonCell() {
            const cell = document.createElement('td');
            const removeButton = document.createElement('button');
            removeButton.classList.add('toolbar-button', 'webhook-button');
            removeButton.setAttribute('title', 'Delete this webhook');
            const removeButtonImage = document.createElement('img');
            removeButtonImage.setAttribute('src', '/polarion/ria/images/control/tableMinus.png');
            removeButton.appendChild(removeButtonImage);
            removeButton.addEventListener('click', function () {
                tableRow.remove();
            })
            cell.appendChild(removeButton);
            return cell;
        }

        function createUrlLabelCell() {
            const cell = document.createElement('td');
            const fieldLabel = document.createElement('label');
            fieldLabel.innerHTML = 'URL: ';
            cell.appendChild(fieldLabel);
            return cell;
        }

        function createUrlInputCell() {
            const cell = document.createElement('td');

            const urlInput = document.createElement('input');
            urlInput.classList.add('fs-14', 'webhook');
            urlInput.setAttribute('name', 'url');
            urlInput.setAttribute('placeholder', 'https://my.domain.com/my-webhook');
            if (webhookConfig?.url) {
                urlInput.setAttribute('value', webhookConfig.url);
            }

            const invalidUrlError = document.createElement('div');
            invalidUrlError.innerHTML = 'WARNING: Entered value doesn\'t seem to be a valid URL';
            invalidUrlError.classList.add('invalid-webhook', 'hidden');
            urlInput.addEventListener("keyup", (event) => {
                const urlPattern = /^(http(s)?:\/\/.)[-a-zA-Z0-9@:%._+~#=]{2,256}\b([-a-zA-Z0-9@:%_+.~#?&/=]*)$/g;
                if (!event.target.value || urlPattern.test(event.target.value)) {
                    invalidUrlError.classList.add('hidden');
                } else {
                    invalidUrlError.classList.remove('hidden');
                }
            });

            cell.appendChild(urlInput);
            cell.appendChild(invalidUrlError);
            return cell;
        }

        function createAuthCheckboxCell() {
            const cell = document.createElement('td');

            const authCheckbox = document.createElement('input');
            authCheckbox.setAttribute('type', 'checkbox');
            authCheckbox.setAttribute('name', 'auth');
            if (webhookConfig?.authType) {
                authCheckbox.setAttribute('checked', 'checked');
            }

            authCheckbox.addEventListener('change', (event) => {
                const authTypeCombobox = event.target.parentElement.parentElement.querySelector('select[name="auth_type"]');
                const authTokenNameField = event.target.parentElement.parentElement.querySelector('input[name="auth_token_name"]');
                if (event.target.checked) {
                    authTypeCombobox.style.display = 'block';
                    authTokenNameField.style.display = 'block';
                } else {
                    authTypeCombobox.style.display = 'none';
                    authTokenNameField.style.display = 'none';
                }
            });

            const authLabel = document.createElement('label');
            authLabel.setAttribute('for', 'auth');
            authLabel.innerHTML = 'Auth';

            cell.appendChild(authCheckbox);
            cell.appendChild(authLabel);
            return cell;
        }

        function createAuthTypeComboboxCell() {
            const cell = document.createElement('td');

            const authTypeCombobox = document.createElement('select');
            authTypeCombobox.setAttribute('name', 'auth_type');
            authTypeCombobox.innerHTML = '<option value="BEARER_TOKEN">Bearer Token</option><option value="XSRF_TOKEN">XSRF Token</option>';
            if (webhookConfig?.authType) {
                switch (webhookConfig.authType) {
                    case 'BEARER_TOKEN': authTypeCombobox.selectedIndex = 0; break;
                    case 'XSRF_TOKEN': authTypeCombobox.selectedIndex = 1; break;
                }
            } else {
                authTypeCombobox.style.display = 'none';
            }

            cell.appendChild(authTypeCombobox);
            return cell;
        }

        function createAuthTokenNameInput() {
            const cell = document.createElement('td');

            const authTokenNameField = document.createElement('input');
            authTokenNameField.setAttribute('name', 'auth_token_name');
            if (webhookConfig?.authType) {
                authTokenNameField.setAttribute('value', webhookConfig.authTokenName ? webhookConfig.authTokenName : '');
            } else {
                authTokenNameField.style.display = 'none';
            }

            cell.appendChild(authTokenNameField);
            return cell;
        }

        const table = document.getElementById('webhooks-table');
        const tableRow = createTableRow();
        tableRow.appendChild(createControlButtonCell());
        tableRow.appendChild(createUrlLabelCell());
        tableRow.appendChild(createUrlInputCell());
        tableRow.appendChild(createAuthCheckboxCell());
        tableRow.appendChild(createAuthTypeComboboxCell());
        tableRow.appendChild(createAuthTokenNameInput());
        table.appendChild(tableRow);
    },
}

SbbCommon.init({
    extension: 'pdf-exporter',
    setting: 'webhooks',
    scope: SbbCommon.getValueById('scope')
});

Configurations.init({
    setConfigurationContentCallback: Webhooks.setWebhooks
});

Configurations.loadConfigurationNames();
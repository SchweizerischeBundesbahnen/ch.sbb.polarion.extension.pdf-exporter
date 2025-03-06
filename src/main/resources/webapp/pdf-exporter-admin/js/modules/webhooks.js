import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';
import ConfigurationsPane from '../../ui/generic/js/modules/ConfigurationsPane.js';

const ctx = new ExtensionContext({
    extension: 'pdf-exporter',
    setting: 'webhooks',
    scopeFieldId: 'scope'
});

const conf = new ConfigurationsPane({
    ctx: ctx,
    setConfigurationContentCallback: setWebhooks,
});

ctx.onClick(
    'add-webhook-button', addWebhook,
    'save-toolbar-button', saveWebhooks,
    'cancel-toolbar-button', ctx.cancelEdit,
    'default-toolbar-button', revertToDefault,
    'revisions-toolbar-button', ctx.toggleRevisions,
);

function saveWebhooks() {
    ctx.hideActionAlerts();

    const webhookRows = Array.from(document.querySelectorAll('#webhooks-table tr'));
    const webhookConfigs = webhookRows
        .map(row => {
            return {
                url: row.querySelector('input[name="url"]')?.value,
                authType: row.querySelector('input[name="auth"]')?.checked ? row.querySelector('select[name="auth_type"]')?.value : null,
                authTokenName: row.querySelector('input[name="auth"]')?.checked ? row.querySelector('input[name="auth_token_name"]')?.value : null
            };
        });

    ctx.callAsync({
        method: 'PUT',
        url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${conf.getSelectedConfiguration()}/content?scope=${ctx.scope}`,
        contentType: 'application/json',
        body: JSON.stringify({
            'webhookConfigs': webhookConfigs,
        }),
        onOk: () => {
            ctx.showSaveSuccessAlert();
            ctx.setNewerVersionNotificationVisible(false);
            conf.loadConfigurationNames();
        },
        onError: () => ctx.showSaveErrorAlert()
    });
}

function setWebhooks(text) {
    document.getElementById('webhooks-table').innerHTML = ""; // Remove all records currently displayed first
    const webhooksModel = JSON.parse(text);
    if (webhooksModel.webhookConfigs) {
        webhooksModel.webhookConfigs.forEach(webhookConfig => {
            addWebhook(webhookConfig);
        });
    }
    if (webhooksModel.bundleTimestamp !== ctx.getValueById('bundle-timestamp')) {
        loadDefaultContent()
            .then((responseText) => {
                const defaultWebhooksModel = JSON.parse(responseText);
                ctx.setNewerVersionNotificationVisible(webhooksModel.webhooks && defaultWebhooksModel.webhooks
                    && (webhooksModel.webhooks.length !== defaultWebhooksModel.webhooks.length
                        || webhooksModel.webhooks.every(function (value, index) {
                            return value === defaultWebhooksModel.hooks[index]
                        })));
            })
    }
}

function revertToDefault() {
    if (confirm('Are you sure you want to return the default value?')) {
        loadDefaultContent()
            .then((responseText) => {
                setWebhooks(responseText);
                ctx.showRevertedToDefaultAlert();
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

function addWebhook(webhookConfig) {
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
        authTypeCombobox.innerHTML = '<option value="BEARER_TOKEN">Bearer Token</option><option value="BASIC_AUTH">Basic</option>';
        if (webhookConfig?.authType) {
            switch (webhookConfig.authType) {
                case 'BEARER_TOKEN': authTypeCombobox.selectedIndex = 0; break;
                case 'BASIC_AUTH': authTypeCombobox.selectedIndex = 1; break;
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
        authTokenNameField.setAttribute('placeholder', 'Polarion Vault entry name');
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
}

conf.loadConfigurationNames();

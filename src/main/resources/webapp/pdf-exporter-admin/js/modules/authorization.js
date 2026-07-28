import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';

const ctx = new ExtensionContext({
    extension: 'pdf-exporter',
    setting: 'authorization',
    scopeFieldId: 'scope'
});

ctx.onClick(
    'save-toolbar-button', saveSettings,
    'cancel-toolbar-button', ctx.cancelEdit,
    'default-toolbar-button', revertToDefault,
    'revisions-toolbar-button', ctx.toggleRevisions
);

function getSelectedRoles(containerId) {
    const result = [];
    const checkboxes = ctx.querySelectorAll(`div#${containerId} input[type=checkbox]:checked`);
    checkboxes.forEach((checkbox) => result.push(checkbox.id));
    return result;
}

function setSelectedRoles(containerId, allowedRoles) {
    const roles = allowedRoles || [];
    const checkboxes = ctx.querySelectorAll(`div#${containerId} input[type=checkbox]`);
    checkboxes.forEach((checkbox) => {
        checkbox.checked = roles.includes(checkbox.id);
    });
}

function parseAndSetSettings(text) {
    const settings = JSON.parse(text);
    setSelectedRoles('global_roles', settings.globalRoles);
    setSelectedRoles('project_roles', settings.projectRoles);
    ctx.setNewerVersionNotificationVisible(settings.bundleTimestamp !== ctx.getValueById('bundle-timestamp'));
}

function saveSettings() {
    ctx.hideActionAlerts();

    ctx.callAsync({
        method: 'PUT',
        url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${ExtensionContext.DEFAULT}/content?scope=${ctx.scope}`,
        contentType: 'application/json',
        body: JSON.stringify({
            'globalRoles': getSelectedRoles('global_roles'),
            'projectRoles': getSelectedRoles('project_roles')
        }),
        onOk: () => {
            ctx.showSaveSuccessAlert();
            ctx.setNewerVersionNotificationVisible(false);
            readAndFillRevisions();
        },
        onError: () => ctx.showSaveErrorAlert()
    });
}

function readSettings() {
    ctx.setLoadingErrorNotificationVisible(false);

    ctx.callAsync({
        method: 'GET',
        url: `/polarion/${ctx.extension}/rest/internal/settings/${ctx.setting}/names/${ExtensionContext.DEFAULT}/content?scope=${ctx.scope}`,
        contentType: 'application/json',
        onOk: (responseText) => {
            parseAndSetSettings(responseText);
            readAndFillRevisions();
        },
        onError: () => ctx.setLoadingErrorNotificationVisible(true)
    });
}

function readAndFillRevisions() {
    ctx.readAndFillRevisions({
        revertToRevisionCallback: (responseText) => parseAndSetSettings(responseText)
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
                parseAndSetSettings(responseText);
                ctx.showRevertedToDefaultAlert();
            },
            onError: () => ctx.setLoadingErrorNotificationVisible(true)
        });
    }
}

readSettings();

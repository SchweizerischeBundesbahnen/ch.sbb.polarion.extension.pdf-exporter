SbbCommon.init({
    extension: 'pdf-exporter',
    setting: 'localization',
    scope: SbbCommon.getValueById('scope')
});
Configurations.init({
    setConfigurationContentCallback: composeTranslationsTable,
    setContentAreaEnabledCallback: setContentAreaEnabled
});

function saveLocalizations() {
    SbbCommon.hideActionAlerts();

    SbbCommon.callAsync({
        method: 'PUT',
        url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${Configurations.getSelectedConfiguration()}/content?scope=${SbbCommon.scope}`,
        contentType: 'application/json',
        body: JSON.stringify({
            'translations': Object.fromEntries(getTranslationsMap()),
        }),
        onOk: () => {
            SbbCommon.showSaveSuccessAlert();
            Configurations.loadConfigurationNames();
        },
        onError: () => SbbCommon.showSaveErrorAlert()
    });
}

function getTranslationsMap() {
    const tableRows = getTableRows();
    const translations = new Map();
    Array.from(tableRows).forEach((tableRow) => {
        let translationEntries = [];
        getLanguages().forEach((language) => {
            const lang = language.key;
            const value = tableRow.querySelector('[data-language=' + lang + ']').value.trim();
            translationEntries.push({language: lang, value: value});
        })
        const enValue = translationEntries.shift().value;
        if (enValue !== '') {
            translations.set(enValue, translationEntries);
        }
    })
    return translations;
}

function downloadLocalization(language) {
    const xhr = new XMLHttpRequest();
    xhr.open('GET',
        `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${Configurations.getSelectedConfiguration()}/download?language=${language}&scope=${SbbCommon.scope}`,
        true);
    xhr.responseType = 'blob';
    xhr.onload = () => {
        if (xhr.status === 200) {
            const objectURL = (window.URL ? window.URL : window.webkitURL).createObjectURL(xhr.response);
            const anchorElement = document.createElement('a');
            anchorElement.href = objectURL;
            anchorElement.download = language + '.xlf';
            anchorElement.target = '_blank';
            anchorElement.click();
            anchorElement.remove();
            setTimeout(() => URL.revokeObjectURL(objectURL), 100);
        } else {
            //display error body content
            console.log('error occurred');
        }
    };
    xhr.onerror = () => SbbCommon.showActionAlert({
        containerId: 'action-error',
        message: 'Error downloading translations file for language ' + language
    });
    xhr.send();
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
                composeTranslationsTable(responseText);
            },
            onError: () => SbbCommon.setLoadingErrorNotificationVisible(true)
        });
    }
}

function composeTranslationsTable(translations) {
    // Remove old table
    const tableContainer = document.getElementById('translation-table');
    let table = tableContainer.getElementsByTagName('table')[0];
    if (table !== undefined && table !== null) {
        table.remove();
    }

    // ... and create new one
    table = document.createElement('table');
    table.style.width = '100%'
    const header = createTableHeader(getLanguages())
    table.appendChild(header);
    if (translations !== undefined && translations.length) {
        const content = createTableContent(translations);
        table.appendChild(content);
    }
    tableContainer.appendChild(table);
}

function createTableHeader(languages) {
    const thead = document.createElement('thead');
    const tr = document.createElement('tr');
    languages.forEach((language) => {
        const th = document.createElement('th');
        th.appendChild(document.createTextNode(language.value));
        tr.appendChild(th)
    })
    thead.appendChild(tr);
    return thead;
}

function createTableContent(content) {
    const tbody = document.createElement('tbody');
    for (const [key, translationEntries] of Object.entries(JSON.parse(content).translations)) {
        const row = createEditableTableRow();
        setInputValues(row, key, translationEntries);
        tbody.appendChild(row);
    }
    return tbody;
}

function createEditableTableRow() {
    const tr = document.createElement('tr');
    getLanguages().forEach((language) => {
        const td = document.createElement('td');
        td.style.width = '24%';
        const input = createWrappedInput(language, 'text');
        td.appendChild(input);
        tr.appendChild(td)
    })
    createActionColumn(tr);
    return tr;
}

function setInputValues(row, key, translationEntries) {
    setTranslationTextForLanguage('en', row, key);
    translationEntries.forEach(translationEntry => {
        setTranslationTextForLanguage(translationEntry.language, row, translationEntry.value);
    })
}

function setTranslationTextForLanguage(language, row, text) {
    const input = row.querySelector('[data-language=' + language + ']');
    input.value = text;
    if (input.value.trim().length === 0) {
        input.classList.add('red-border');
    } else {
        input.classList.remove('red-border');
    }
}

function createActionColumn(row) {
    const td = row.insertCell();
    td.classList.add('action');
    td.setAttribute('title', 'Delete');
    const image = document.createElement('img');
    image.setAttribute('src', '/polarion/ria/images/control/tableMinus.png');
    td.appendChild(image);
    td.addEventListener('click', function () {
        row.remove();
    })
}

function createEmptyTableRow() {
    const translationTable = document.getElementById('translation-table');
    const tbody = translationTable.getElementsByTagName('tbody')[0];
    const row = createEditableTableRow();
    tbody.appendChild(row);
    return row;
}

function getTableRows() {
    const translationTable = document.getElementById('translation-table');
    const tbody = translationTable.getElementsByTagName('tbody')[0];
    return tbody.getElementsByTagName('tr');
}

function createWrappedInput(language, type) {
    const div = document.createElement('div');
    div.classList.add('w-100');
    const input = document.createElement('input');
    input.setAttribute('type', type);
    input.value = '';
    input.dataset.language = language.key;
    input.classList.add('monospace');
    input.classList.add('w-100');
    input.classList.add('red-border');
    createInputColorChangeListener(input);
    div.appendChild(input);
    return div;
}

function createInputColorChangeListener(input) {
    input.addEventListener('blur', function () {
        this.setAttribute('value', this.value);
        if (this.value.trim().length === 0) {
            this.classList.add('red-border');
        } else {
            this.classList.remove('red-border');
        }
    });
}

function getLanguages() {
    return [{key: 'en', value: 'English'}, {key: 'de', value: 'German'}, {key: 'fr', value: 'French'}, {key: 'it', value: 'Italian'}];
}

function attachUploadActions() {
    attachUploadActionsForLanguage('de');
    attachUploadActionsForLanguage('fr');
    attachUploadActionsForLanguage('it');
}

function attachUploadActionsForLanguage(language) {
    document.getElementById('file-' + language).onclick = function () {
        this.value = '';
        SbbCommon.hideActionAlerts();
    };
    document.getElementById('file-' + language).onchange = function () {
        uploadLocalization(language);
    };
}

function uploadLocalization(language) {
    const file = document.getElementById('file-' + language).files[0];
    const formData = new FormData();
    formData.append('file', file);
    SbbCommon.callAsync({
        method: 'POST',
        url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/upload?language=${language}&scope=${SbbCommon.scope}`,
        body: formData,
        onOk: (response) => {
            replaceTranslationsForLanguage(language, JSON.parse(response));
            SbbCommon.showActionAlert({
                containerId: 'action-success',
                message: `Translation for language ${language} successfully uploaded. Don't forget to save the data before leaving.`
            });
        },
        onError: () => SbbCommon.showActionAlert({
            containerId: 'action-error',
            message: `Error occurred while uploading translation file for language ${language}`
        })
    });
}
function replaceTranslationsForLanguage(language, translations) {
    const translationTable = document.getElementById('translation-table');
    const tBody = translationTable.getElementsByTagName('tbody')[0];
    const rows = tBody.getElementsByTagName('tr');
    const translationKeysMap = getTranslationKeys(rows);
    for (const [key, translationEntry] of Object.entries(translations)) {
        const index = translationKeysMap.get(key);
        if (index > -1) {
            setTranslationTextForLanguage(language, rows[index], translationEntry);
        } else {
            const values = [];
            const languages = getLanguages();
            for (let i = 1; i < languages.length; i++) {
                const lang = languages[i].key;
                let value;
                if (lang === language) {
                    value = translationEntry;
                } else {
                    value = '';
                }
                values.push({language: lang, value: value});
            }
            const row = createEditableTableRow();
            setInputValues(row, key, values);
            tBody.appendChild(row);
        }
    }
    for (const key of translationKeysMap.keys()) {
        if (!translations.hasOwnProperty(key)) {
            const index = translationKeysMap.get(key);
            const tableRow = rows[index];
            const input = tableRow.querySelector('[data-language=' + language + ']');
            input.value = '';
            input.classList.add('red-border');
        }
    }
}

function getTranslationKeys(tableRows) {
    const translations = new Map();
    for (let row = 0; row < tableRows.length; row++) {
        const translationKeyColumn = tableRows[row].getElementsByTagName('td')[0];
        translations.set(translationKeyColumn.getElementsByTagName('input')[0].value, row);
    }
    return translations;
}

function setContentAreaEnabled(enabled) {
    if (enabled) {
        document.getElementById('translation-table').style.display = "block";
        document.getElementById('export-import-table').style.display = "table";
    } else {
        document.getElementById('translation-table').style.display = "none";
        document.getElementById('export-import-table').style.display = "none";
    }
}

attachUploadActions();
Configurations.loadConfigurationNames();
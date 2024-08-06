
const ExportCommon = {

    DOC_PDF_CONVERSION_PULL_INTERVAL: 1000,
    DEFAULT_SETTING_NAME: "Default",

    setCheckbox: function (elementId, value) {
        document.getElementById(elementId).checked = !!value;
    },

    setValue: function (elementId, value) {
        document.getElementById(elementId).value = value;
    },

    setSelector: function (elementId, value) {
        const selector = document.getElementById(elementId);
        selector.value = this.containsOption(selector, value) ? value : this.DEFAULT_SETTING_NAME;
    },

    displayIf: function (elementId, condition, displayStyle = "block") {
        document.getElementById(elementId).style.display = condition ? displayStyle : "none";
    },

    visibleIf: function (elementId, condition) {
        document.getElementById(elementId).style.visibility = condition ? "visible" : "hidden";
    },

    containsOption: function (selectElement, option) {
        return [...selectElement.options].map(o => o.value).includes(option);
    },

    asyncConvertPdf: async function (request, successCallback, errorCallback) {
        SbbCommon.callAsync({
            method: "POST",
            url: "/polarion/pdf-exporter/rest/internal/convert/jobs",
            contentType: "application/json",
            responseType: "blob",
            body: request,
            onOk: (responseText, request) => {
                this.pullAndGetResultPdf(request.getResponseHeader("Location"), successCallback, errorCallback);
            },
            onError: (status, errorMessage, request) => {
                errorCallback(request.response);
            }
        });
    },

    pullAndGetResultPdf: async function (url, successCallback, errorCallback) {
        await new Promise(resolve => setTimeout(resolve, this.DOC_PDF_CONVERSION_PULL_INTERVAL));
        SbbCommon.callAsync({
            method: "GET",
            url: url,
            responseType: "blob",
            onOk: (responseText, request) => {
                if (request.status === 202) {
                    console.log('Async PDF conversion: still in progress, retrying...');
                    this.pullAndGetResultPdf(url, successCallback, errorCallback);
                } else if (request.status === 200) {
                    successCallback(request.response);
                }
            },
            onError: (status, errorMessage, request) => {
                errorCallback(request.response);
            }
        });
    },
}

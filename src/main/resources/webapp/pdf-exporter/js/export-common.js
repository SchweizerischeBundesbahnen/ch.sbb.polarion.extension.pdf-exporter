
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
                    successCallback(request.response, request.getResponseHeader("Export-Filename"));
                }
            },
            onError: (status, errorMessage, request) => {
                errorCallback(request.response);
            }
        });
    },

    downloadTestRunAttachments: function (projectId, testRunId, revision = null, filter = null) {
        let url = `/polarion/pdf-exporter/rest/internal/projects/${projectId}/testruns/${testRunId}/attachments?`;
        if (revision) url += `&revision=${revision}`;
        if (filter) url += `&filter=${filter}`;

        SbbCommon.callAsync({
            method: "GET",
            url: url,
            responseType: "json",
            onOk: (responseText, request) => {
                for (const attachment of request.response) {
                    this.downloadAttachmentContent(projectId, testRunId, attachment.id, revision);
                }
            },
            onError: (status, errorMessage, request) => {
                console.error('Error fetching attachments:', request.response);
            }
        });
    },

    downloadAttachmentContent: function (projectId, testRunId, attachmentId, revision = null) {
        let url = `/polarion/pdf-exporter/rest/internal/projects/${projectId}/testruns/${testRunId}/attachments/${attachmentId}/content?`;
        if (revision) url += `&revision=${revision}`;

        SbbCommon.callAsync({
            method: "GET",
            url: url,
            responseType: "blob",
            onOk: (responseText, request) => {
                this.downloadBlob(request.response, request.getResponseHeader("Filename"));
            },
            onError: (status, errorMessage, request) => {
                console.error(`Error downloading attachment ${attachmentId}:`, request.response);
            }
        });
    },

    downloadBlob: function(blob, fileName) {
        const objectURL = (window.URL ? window.URL : window.webkitURL).createObjectURL(blob);
        const link = document.createElement("a");
        link.href = objectURL;
        link.download = fileName
        link.target = "_blank";
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(objectURL), 100);
    },

    downloadTestRunAttachments: function (projectId, testRunId, revision = null, filter = null) {
        let url = `/polarion/pdf-exporter/rest/internal/projects/${projectId}/testruns/${testRunId}/attachments?`;
        if (revision) url += `&revision=${revision}`;
        if (filter) url += `&filter=${filter}`;

        SbbCommon.callAsync({
            method: "GET",
            url: url,
            responseType: "json",
            onOk: (responseText, request) => {
                for (const attachment of request.response) {
                    this.downloadAttachmentContent(projectId, testRunId, attachment.id, revision);
                }
            },
            onError: (status, errorMessage, request) => {
                console.error('Error fetching attachments:', request.response);
            }
        });
    },

    downloadAttachmentContent: function (projectId, testRunId, attachmentId, revision = null) {
        let url = `/polarion/pdf-exporter/rest/internal/projects/${projectId}/testruns/${testRunId}/attachments/${attachmentId}/content?`;
        if (revision) url += `&revision=${revision}`;

        SbbCommon.callAsync({
            method: "GET",
            url: url,
            responseType: "blob",
            onOk: (responseText, request) => {
                this.downloadBlob(request.response, request.getResponseHeader("Filename"));
            },
            onError: (status, errorMessage, request) => {
                console.error(`Error downloading attachment ${attachmentId}:`, request.response);
            }
        });
    },

    downloadBlob: function(blob, fileName) {
        const objectURL = (window.URL ? window.URL : window.webkitURL).createObjectURL(blob);
        const link = document.createElement("a");
        link.href = objectURL;
        link.download = fileName
        link.target = "_blank";
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(objectURL), 100);
    },

    downloadCollectionItems: function (exportParams, collectionId, onComplete, onError) {
        let url = `/polarion/pdf-exporter/rest/internal/projects/${exportParams.projectId}/collections/${collectionId}`;
        SbbCommon.callAsync({
            method: "GET",
            url: url,
            responseType: "json",
            onOk: (responseText, request) => {
                const collectionItems = request.response;

                if (!collectionItems || collectionItems.length === 0) {
                    console.warn("No items found in the collection.");
                    onComplete && onComplete();
                    return;
                }

                let completedCount = 0;
                let hasErrors = false;

                const handleItem = (item) => {
                    exportParams["locationPath"] = item.moduleNameWithSpace;
                    exportParams["revision"] = item.revision;
                    exportParams["documentType"] = ExportParams.DocumentType.LIVE_DOC;

                    this.asyncConvertPdf(
                        exportParams.toJSON(),
                        (responseBody, fileName) => {
                            const downloadFileName = fileName || "downloaded_document.pdf";
                            this.downloadBlob(responseBody, downloadFileName);

                            completedCount++;
                            if (completedCount === collectionItems.length && !hasErrors) {
                                onComplete && onComplete();
                            }
                        },
                        (errorResponse) => {
                            console.error("Error converting item:", errorResponse);
                            hasErrors = true;
                            completedCount++;
                            if (completedCount === collectionItems.length) {
                                onError && onError(errorResponse);
                            }
                        }
                    );
                };

                collectionItems.forEach(handleItem);
            },
            onError: (status, errorMessage, request) => {
                console.error("Error loading collection items:", request.response);
                onError && onError(errorMessage);
            }
        });
    },
}


const ExportCommon = {

    TOP_SELECTOR: ".pdf-exporter",
    DOC_PDF_CONVERSION_PULL_INTERVAL: 1000,
    DEFAULT_SETTING_NAME: "Default",

    getElementById: function (elementId) {
        return document.querySelector(`${this.TOP_SELECTOR} #${elementId}`);
    },

    getJQueryElement: function (selector) {
        return $(`${this.TOP_SELECTOR} ${selector}`);
    },


    querySelector: function (selector) {
        return document.querySelector(`${this.TOP_SELECTOR} ${selector}`);
    },

    querySelectorAll: function (selector) {
        return document.querySelectorAll(`${this.TOP_SELECTOR} ${selector}`);
    },

    setCheckbox: function (elementId, value) {
        this.getElementById(elementId).checked = !!value;
    },

    setValue: function (elementId, value) {
        this.getElementById(elementId).value = value;
    },

    setSelector: function (elementId, value) {
        const selector = this.getElementById(elementId);
        selector.value = this.containsOption(selector, value) ? value : this.DEFAULT_SETTING_NAME;
    },

    displayIf: function (elementId, condition, displayStyle = "block") {
        this.getElementById(elementId).style.display = condition ? displayStyle : "none";
    },

    visibleIf: function (elementId, condition) {
        this.getElementById(elementId).style.visibility = condition ? "visible" : "hidden";
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
                    let warningMessage;
                    let count = request.getResponseHeader("Missing-WorkItem-Attachments-Count");
                    if (count > 0) {
                        let attachment = request.getResponseHeader("WorkItem-IDs-With-Missing-Attachment")
                        warningMessage = `${count} image(s) in WI(s) ${attachment} were not exported. They were replaced with an image containing 'This image is not accessible'.`;
                    }
                    successCallback({
                        response: request.response,
                        warning: warningMessage
                    });
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

    convertCollectionDocuments: function (exportParams, collectionId, onComplete, onError) {
        let url = `/polarion/pdf-exporter/rest/internal/projects/${exportParams.projectId}/collections/${collectionId}/documents`;
        SbbCommon.callAsync({
            method: "GET",
            url: url,
            responseType: "json",
            onOk: (responseText, request) => {
                const collectionDocuments = request.response;

                if (!collectionDocuments || collectionDocuments.length === 0) {
                    console.warn("No documents found in the collection.");
                    onComplete && onComplete();
                    return;
                }

                let completedCount = 0;
                let hasErrors = false;

                const convertCollectionDocument = (collectionDocument) => {
                    exportParams["projectId"] = collectionDocument.projectId;
                    exportParams["locationPath"] = collectionDocument.spaceId + "/" + collectionDocument.documentName;
                    exportParams["revision"] = collectionDocument.revision;
                    exportParams["documentType"] = ExportParams.DocumentType.LIVE_DOC;

                    this.asyncConvertPdf(
                        exportParams.toJSON(),
                        (result, fileName) => {
                            const downloadFileName = fileName || `${collectionDocument.projectId}_${collectionDocument.spaceId}_${collectionDocument.documentName}.pdf`;
                            this.downloadBlob(result.response, downloadFileName);

                            completedCount++;
                            if (completedCount === collectionDocuments.length && !hasErrors) {
                                onComplete && onComplete();
                            }
                        },
                        (errorResponse) => {
                            console.error("Error converting collection document:", errorResponse);
                            hasErrors = true;
                            completedCount++;
                            if (completedCount === collectionDocuments.length) {
                                onError && onError(errorResponse);
                            }
                        }
                    );
                };

                collectionDocuments.forEach(convertCollectionDocument);
            },
            onError: (status, errorMessage, request) => {
                console.error("Error loading collection documents:", request.response);
                onError && onError(errorMessage);
            }
        });
    },
}

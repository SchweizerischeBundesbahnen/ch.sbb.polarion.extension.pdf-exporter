import ExtensionContext from "/polarion/pdf-exporter/ui/generic/js/modules/ExtensionContext.js";
import ExportParams from "./ExportParams.js";

export default class ExportContext extends ExtensionContext {
    static PULL_INTERVAL = 1000;
    projectId = undefined;
    locationPath = undefined;
    baselineRevision = undefined;
    revision = undefined;
    documentType = undefined;
    exportType = undefined;
    urlQueryParameters = undefined;

    constructor({
                    documentType = ExportParams.DocumentType.LIVE_DOC,
                    exportType = ExportParams.ExportType.SINGLE,
                    polarionLocationHash = window.location.hash,
                    exportPages = false,
                    rootComponentSelector}) {
        const urlPathAndSearchParams = getPathAndQueryParams(polarionLocationHash);
        const normalizedPolarionLocationHash = urlPathAndSearchParams.path;
        const scope = getScope(normalizedPolarionLocationHash);
        super({extension: "pdf-exporter", setting: "pdf-exporter", rootComponentSelector: rootComponentSelector, scope: scope});

        this.documentType = documentType;
        this.exportType = exportType;
        this.projectId = getProjectId(scope);
        this.exportPages = exportPages;

        const baseline = getBaseline(normalizedPolarionLocationHash);
        if (baseline) {
            this.baselineRevision = getBaselineRevision(baseline);
        }

        if (this.exportType !== ExportParams.ExportType.BULK) {
            this.locationPath = getPath(normalizedPolarionLocationHash, scope);

            // if "testrun" or "testruns" is present return undefined
            if (this.locationPath?.startsWith("testrun")) {
                this.documentType = ExportParams.DocumentType.TEST_RUN;
                this.locationPath = undefined;
            }
        }

        const searchParameters = urlPathAndSearchParams.searchParameters;
        this.urlQueryParameters = getQueryParams(searchParameters);
        this.revision = this.urlQueryParameters?.revision;

        function getPathAndQueryParams(polarionLocationHash) {
            const result = {
                path: undefined,
                searchParameters: undefined
            };

            if (typeof window !== 'undefined' && polarionLocationHash.endsWith("/testruns")) {
                // TestRun opened from the list doesn't have proper URL, so we attempt to fetch it from the specific tag
                // WARNING: the way we get this URL isn't convenient and may stop working in the future, but it seems the only way to do it atm
                polarionLocationHash = window.document.querySelector('.polarion-TestRunLabelWidget-container a').getAttribute('href');
            }

            if (polarionLocationHash.includes("?")) {
                const pathAndQueryParams = decodeURI(polarionLocationHash.substring(2));
                const pathAndQueryParamsArray = pathAndQueryParams.split("?");
                result.path = pathAndQueryParamsArray[0];
                result.searchParameters = pathAndQueryParamsArray[1];
            } else {
                result.path = decodeURI(polarionLocationHash.substring(2));
            }

            return result;
        }

        function getBaseline(locationHash) {
            const baselinePattern = /baseline\/([^/]+)\//;
            const baselineMatch = baselinePattern.exec(locationHash);
            return baselineMatch ? `baseline/${baselineMatch[1]}/` : undefined;
        }

        function getBaselineRevision(baselineScope) {
            const foundValues = /baseline\/(.*)\//.exec(baselineScope);
            return foundValues !== null ? foundValues[1] : null;
        }

        function getScope(locationHash) {
            const projectPattern = /project\/([^/]+)\//;
            const projectMatch = projectPattern.exec(locationHash);
            return projectMatch ? `project/${projectMatch[1]}/` : "";
        }

        function getProjectId(scope) {
            const foundValues = /project\/(.*)\//.exec(scope);
            return foundValues !== null ? foundValues[1] : null;
        }

        function getPath(locationHash, scope) {
            if (scope) {
                const pathPattern = /project\/(.+)\/(wiki\/([^?#]+)|testruns|testrun)/;
                const pathMatch = pathPattern.exec(locationHash);
                const extractedPath = pathMatch ? (pathMatch[3] || pathMatch[2]) : undefined;
                return pathMatch ? addDefaultSpaceIfRequired(extractedPath) : undefined;
            } else {
                const globalPathPattern = /wiki\/([^?#]+)/;
                const pathMatch = globalPathPattern.exec(locationHash);
                return pathMatch ? addDefaultSpaceIfRequired(pathMatch[1]) : undefined;
            }
        }

        function addDefaultSpaceIfRequired(extractedPath) {
            if (!extractedPath) {
                return "";
            }
            // if "testrun" or "testruns" is present return undefined
            if (extractedPath.startsWith("testrun")) {
                return extractedPath;
            }
            // if contains a '/' return it as it is
            if (extractedPath.includes("/")) {
                return extractedPath;
            }
            // otherwise, prepend '_default/' to the path
            return `_default/${extractedPath}`;
        }

        function getQueryParams(searchParams) {
            if (!searchParameters) {
                return undefined;
            }

            const urlSearchParams = new URLSearchParams(searchParams);
            return Object.fromEntries([...urlSearchParams]);
        }

    }

    getProjectId() {
        return this.projectId;
    }

    getLocationPath() {
        return this.locationPath;
    }

    getBaselineRevision() {
        return this.baselineRevision;
    }

    getRevision() {
        return this.revision;
    }

    getDocumentType() {
        return this.documentType;
    }

    getExportType() {
        return this.exportType;
    }

    getUrlQueryParameters() {
        return this.urlQueryParameters;
    }

    getScope() {
        return this.projectId ? `project/${this.projectId}/` : "";
    }

    getSpaceId() {
        if (this.locationPath?.includes("/")) {
            const pathParts = this.locationPath.split("/");
            return pathParts && pathParts.length > 0 && pathParts[0];
        } else {
            return undefined;
        }
    }

    getDocumentName() {
        if (this.locationPath?.includes("/")) {
            const pathParts = this.locationPath.split("/");
            return pathParts && pathParts.length > 1 && pathParts[1];
        } else {
            return undefined;
        }
    }

    toExportParams() {
        return new ExportParams.Builder(this.documentType)
            .setProjectId(this.projectId)
            .setLocationPath(this.locationPath)
            .setBaselineRevision(this.baselineRevision)
            .setRevision(this.revision)
            .setUrlQueryParameters(this.urlQueryParameters)
            .build();
    }

    async asyncConvertPdf(request, successCallback, errorCallback) {
        this.callAsync({
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
    }

    async pullAndGetResultPdf(url, successCallback, errorCallback) {
        await new Promise(resolve => setTimeout(resolve, ExportContext.PULL_INTERVAL));
        this.callAsync({
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
                        warning: warningMessage,
                        fileName: request.getResponseHeader("Export-Filename")
                    });
                }
            },
            onError: (status, errorMessage, request) => {
                errorCallback(request.response);
            }
        });
    }

    downloadTestRunAttachments(projectId, testRunId, revision = null, filter = null, testCaseFieldId) {
        let url = `/polarion/pdf-exporter/rest/internal/projects/${projectId}/testruns/${testRunId}/attachments?`;
        if (revision) url += `&revision=${revision}`;
        if (filter) url += `&filter=${filter}`;
        if (testCaseFieldId) url += `&testCaseFilterFieldId=${testCaseFieldId}`;

        this.callAsync({
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
    }

    downloadAttachmentContent(projectId, testRunId, attachmentId, revision = null) {
        let url = `/polarion/pdf-exporter/rest/internal/projects/${projectId}/testruns/${testRunId}/attachments/${attachmentId}/content?`;
        if (revision) url += `&revision=${revision}`;

        this.callAsync({
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
    }

    convertCollectionDocuments(exportParams, collectionId, onComplete, onError) {
        let url = `/polarion/pdf-exporter/rest/internal/projects/${exportParams.projectId}/collections/${collectionId}/documents`;
        this.callAsync({
            method: "GET",
            url: url,
            responseType: "json",
            onOk: (responseText, request) => {
                let collectionDocuments = request.response;

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
                    exportParams["documentType"] = collectionDocument.documentType;

                    this.asyncConvertPdf(
                        exportParams.toJSON(),
                        result => {
                            const downloadFileName = collectionDocument.fileName || `${collectionDocument.projectId}_${collectionDocument.spaceId}_${collectionDocument.documentName}.pdf`;
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

                if (!this.exportPages) {
                    collectionDocuments = collectionDocuments.filter(doc => doc.documentType !== ExportParams.DocumentType.LIVE_REPORT);
                }
                collectionDocuments.forEach(convertCollectionDocument);
            },
            onError: (status, errorMessage, request) => {
                console.error("Error loading collection documents:", request.response);
                onError && onError(errorMessage);
            }
        });
    }
}

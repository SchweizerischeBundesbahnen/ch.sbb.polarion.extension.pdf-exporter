import ExportParams from "./ExportParams.js";

export default class ExportContext {
    projectId = undefined;
    locationPath = undefined;
    baselineRevision = undefined;
    revision = undefined;
    documentType = undefined;
    exportType = undefined;
    urlQueryParameters = undefined;
    bulkExportWidget = undefined;

    constructor({documentType = ExportParams.DocumentType.LIVE_DOC, exportType = ExportParams.ExportType.SINGLE, polarionLocationHash = window.location.hash, bulkExportWidget}) {
        this.documentType = documentType;
        this.exportType = exportType;

        const urlPathAndSearchParams = getPathAndQueryParams(polarionLocationHash);
        const normalizedPolarionLocationHash = urlPathAndSearchParams.path;
        const searchParameters = urlPathAndSearchParams.searchParameters;

        const baseline = getBaseline(normalizedPolarionLocationHash);
        if (baseline) {
            this.baselineRevision = getBaselineRevision(baseline);
        }

        const scope = getScope(normalizedPolarionLocationHash);
        this.projectId = getProjectId(scope);

        if (this.exportType !== ExportParams.ExportType.BULK) {
            this.locationPath = getPath(normalizedPolarionLocationHash, scope);

            // if "testrun" or "testruns" is present return undefined
            if (this.locationPath?.startsWith("testrun")) {
                this.documentType = ExportParams.DocumentType.TEST_RUN;
                this.locationPath = undefined;
            }
        }

        this.urlQueryParameters = getQueryParams(searchParameters);
        this.revision = this.urlQueryParameters?.revision;

        this.bulkExportWidget = bulkExportWidget;

        // print the context to console only in browser
        if (isWindowDefined()) {
            console.log(this);
        }

        function getPathAndQueryParams(polarionLocationHash) {
            const result = {
                path: undefined,
                searchParameters: undefined
            };

            if (isWindowDefined() && polarionLocationHash.endsWith("/testruns")) {
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
        };

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

    getBulkExportWidget() {
        return this.bulkExportWidget;
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
}

// expose ExportContext to the global scope
if (isWindowDefined()) {
    window.ExportContext = ExportContext;
}

function isWindowDefined() {
    return typeof window !== 'undefined';
}

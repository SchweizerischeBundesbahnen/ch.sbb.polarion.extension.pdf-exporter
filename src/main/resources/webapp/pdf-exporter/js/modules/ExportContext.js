import ExportParams from "./ExportParams.js";

export default class ExportContext {
    projectId = undefined;
    locationPath = undefined;
    revision = undefined;
    documentType = undefined;
    urlQueryParameters = undefined;

    constructor(documentType = ExportParams.DocumentType.LIVE_DOC, polarionLocationHash = window.location.hash) {
        this.documentType = documentType;

        const urlPathAndSearchParams = getPathAndQueryParams(polarionLocationHash);
        const normalizedPolarionLocationHash = urlPathAndSearchParams.path;
        const searchParameters = urlPathAndSearchParams.searchParameters;

        const scope = getScope(normalizedPolarionLocationHash);
        this.projectId = getProjectId(scope);
        this.locationPath = getPath(normalizedPolarionLocationHash, scope);

        if (this.locationPath === "testrun") {
            this.documentType = ExportParams.DocumentType.TEST_RUN;
        }

        this.urlQueryParameters = getQueryParams(searchParameters);
        this.revision = this.urlQueryParameters?.revision;

        console.log(this);

        function getPathAndQueryParams(polarionLocationHash) {
            const result = {
                path: undefined,
                searchParameters: undefined
            };

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
                const pathPattern = /project\/[^/]+\/(wiki\/([^?#]+)|testrun)/;
                const pathMatch = pathPattern.exec(locationHash);
                return pathMatch ? addDefaultSpaceIfRequired(pathMatch[2] || "testrun") : "";
            } else {
                const globalPathPattern = /wiki\/([^/?#]+)/;
                const pathMatch = globalPathPattern.exec(locationHash);
                return pathMatch ? addDefaultSpaceIfRequired(pathMatch[1]) : "";
            }
        }

        function addDefaultSpaceIfRequired(extractedPath) {
            if (!extractedPath) {
                return "";
            }
            // if contains a '/' or is exactly 'testrun', return it as it is
            if (extractedPath.includes("/") || extractedPath === "testrun") {
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

    getRevision() {
        return this.revision;
    }

    getDocumentType() {
        return this.documentType;
    }

    getUrlQueryParameters() {
        return this.urlQueryParameters;
    }

    getScope() {
        return this.projectId ? `project/${this.projectId}/` : "";
    }

    getSpaceId() {
        const pathParts = this.locationPath.split("/");
        return pathParts && pathParts.length > 0 && pathParts[0];
    }

    getDocumentName() {
        const pathParts = this.locationPath.split("/");
        return pathParts && pathParts.length > 1 && pathParts[1];
    }

    toExportParams() {
        return new ExportParams.Builder(this.documentType)
            .setProjectId(this.projectId)
            .setLocationPath(this.locationPath)
            .setRevision(this.revision)
            .setUrlQueryParameters(this.urlQueryParameters)
            .build();
    }
}

// expose ExportContext to the global scope
if (typeof window !== 'undefined') {
    window.ExportContext = ExportContext;
}

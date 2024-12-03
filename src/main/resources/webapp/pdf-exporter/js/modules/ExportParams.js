export default class ExportParams {
    static DocumentType = {
        LIVE_DOC: 'LIVE_DOC',
        LIVE_REPORT: 'LIVE_REPORT',
        TEST_RUN: 'TEST_RUN',
        WIKI_PAGE: 'WIKI_PAGE',
        BASELINE_COLLECTION: 'BASELINE_COLLECTION',
        MIXED: 'MIXED',
    };

    static Orientation = {
        PORTRAIT: 'PORTRAIT',
        LANDSCAPE: 'LANDSCAPE',
    };

    static PaperSize = {
        A5: 'A5',
        A4: 'A4',
        A3: 'A3',
        B5: 'B5',
        B4: 'B4',
        JIS_B5: 'JIS_B5',
        JIS_B4: 'JIS_B4',
        LETTER: 'LETTER',
        LEGAL: 'LEGAL',
        LEDGER: 'LEDGER',
    };

    constructor(builder) {
        this.projectId = builder.projectId;
        this.locationPath = builder.locationPath;
        this.revision = builder.revision;
        this.documentType = builder.documentType;
        this.coverPage = builder.coverPage;
        this.css = builder.css;
        this.headerFooter = builder.headerFooter;
        this.localization = builder.localization;
        this.webhooks = builder.webhooks;
        this.headersColor = builder.headersColor;
        this.orientation = builder.orientation;
        this.paperSize = builder.paperSize;
        this.fitToPage = builder.fitToPage;
        this.enableCommentsRendering = builder.enableCommentsRendering;
        this.watermark = builder.watermark;
        this.markReferencedWorkitems = builder.markReferencedWorkitems;
        this.cutEmptyChapters = builder.cutEmptyChapters;
        this.cutEmptyWIAttributes = builder.cutEmptyWIAttributes;
        this.cutLocalUrls = builder.cutLocalUrls;
        this.followHTMLPresentationalHints = builder.followHTMLPresentationalHints;
        this.numberedListStyles = builder.numberedListStyles;
        this.chapters = builder.chapters;
        this.language = builder.language;
        this.linkedWorkitemRoles = builder.linkedWorkitemRoles;
        this.fileName = builder.fileName;
        this.urlQueryParameters = builder.urlQueryParameters;
        this.attachmentsFilter = builder.attachmentsFilter;
        this.internalContent = builder.internalContent;
    }

    toJSON() {
        const filteredObject = Object.keys(this)
            .filter(key => this[key] !== undefined && this[key] !== null)
            .reduce((obj, key) => {
                obj[key] = this[key];
                return obj;
            }, {});
        return JSON.stringify(filteredObject, null, 2);
    }

    static get Builder() {
        return class {
            constructor(documentType) {
                // required field in constructor
                if (!documentType) {
                    throw new Error("documentType is mandatory");
                }
                this.documentType = documentType;

                // initialize all other values as undefined
                this.projectId = undefined;
                this.locationPath = undefined;
                this.revision = undefined;
                this.coverPage = undefined;
                this.css = undefined;
                this.headerFooter = undefined;
                this.localization = undefined;
                this.webhooks = undefined;
                this.headersColor = undefined;
                this.orientation = undefined;
                this.paperSize = undefined;
                this.fitToPage = undefined;
                this.enableCommentsRendering = undefined;
                this.watermark = undefined;
                this.markReferencedWorkitems = undefined;
                this.cutEmptyChapters = undefined;
                this.cutEmptyWIAttributes = undefined;
                this.cutLocalUrls = undefined;
                this.followHTMLPresentationalHints = undefined;
                this.numberedListStyles = undefined;
                this.chapters = undefined;
                this.language = undefined;
                this.linkedWorkitemRoles = undefined;
                this.fileName = undefined;
                this.urlQueryParameters = undefined;
                this.attachmentsFilter = undefined;
                this.internalContent = undefined;
            }

            setProjectId(projectId) {
                this.projectId = projectId;
                return this;
            }

            setLocationPath(locationPath) {
                this.locationPath = locationPath;
                return this;
            }

            setRevision(revision) {
                this.revision = revision;
                return this;
            }

            setCoverPage(coverPage) {
                this.coverPage = coverPage;
                return this;
            }

            setCss(css) {
                this.css = css;
                return this;
            }

            setHeaderFooter(headerFooter) {
                this.headerFooter = headerFooter;
                return this;
            }

            setLocalization(localization) {
                this.localization = localization;
                return this;
            }

            setWebhooks(webhooks) {
                this.webhooks = webhooks;
                return this;
            }

            setHeadersColor(headersColor) {
                this.headersColor = headersColor;
                return this;
            }

            setOrientation(orientation) {
                this.orientation = orientation;
                return this;
            }

            setPaperSize(paperSize) {
                this.paperSize = paperSize;
                return this;
            }

            setFitToPage(fitToPage) {
                this.fitToPage = fitToPage;
                return this;
            }

            setEnableCommentsRendering(enableCommentsRendering) {
                this.enableCommentsRendering = enableCommentsRendering;
                return this;
            }

            setWatermark(watermark) {
                this.watermark = watermark;
                return this;
            }

            setMarkReferencedWorkitems(markReferencedWorkitems) {
                this.markReferencedWorkitems = markReferencedWorkitems;
                return this;
            }

            setCutEmptyChapters(cutEmptyChapters) {
                this.cutEmptyChapters = cutEmptyChapters;
                return this;
            }

            setCutEmptyWIAttributes(cutEmptyWIAttributes) {
                this.cutEmptyWIAttributes = cutEmptyWIAttributes;
                return this;
            }

            setCutLocalUrls(cutLocalUrls) {
                this.cutLocalUrls = cutLocalUrls;
                return this;
            }

            setFollowHTMLPresentationalHints(followHTMLPresentationalHints) {
                this.followHTMLPresentationalHints = followHTMLPresentationalHints;
                return this;
            }

            setNumberedListStyles(numberedListStyles) {
                this.numberedListStyles = numberedListStyles;
                return this;
            }

            setChapters(chapters) {
                this.chapters = chapters;
                return this;
            }

            setLanguage(language) {
                this.language = language;
                return this;
            }

            setLinkedWorkitemRoles(linkedWorkitemRoles) {
                this.linkedWorkitemRoles = linkedWorkitemRoles;
                return this;
            }

            setFileName(fileName) {
                this.fileName = fileName;
                return this;
            }

            setUrlQueryParameters(urlQueryParameters) {
                this.urlQueryParameters = urlQueryParameters;
                return this;
            }

            setAttachmentsFilter(attachmentsFilter) {
                this.attachmentsFilter = attachmentsFilter;
                return this;
            }

            build() {
                return new ExportParams(this);
            }
        };
    }
}

// expose ExportParams to the global scope
if (typeof window !== 'undefined') {
    window.ExportParams = ExportParams;
}

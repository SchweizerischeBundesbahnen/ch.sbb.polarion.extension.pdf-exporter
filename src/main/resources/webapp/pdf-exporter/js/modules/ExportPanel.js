import ExportParams from "./ExportParams.js";
import ExportContext from "./ExportContext.js";

export default class ExportPanel {

    constructor(rootComponentSelector) {
        this.ctx = new ExportContext({rootComponentSelector: rootComponentSelector});

        this.ctx.onChange('style-package-select', () => {
            this.stylePackageChanged()
        });
        this.ctx.onClick('export-pdf', () => {
            this.loadPdf()
        });
        this.ctx.onClick('validate-pdf', () => {
            this.validatePdf()
        });
    }

    stylePackageChanged() {
        const selectedStylePackage = this.ctx.getElementById("style-package-select").value;
        const scope = this.ctx.querySelector("input[name='scope']").value;
        if (selectedStylePackage && scope) {
            this.ctx.querySelectorAll('button').forEach(actionButton => {
                actionButton.disabled = true;
            });

            const $stylePackageError = this.ctx.getJQueryElement("#style-package-error");
            $stylePackageError.empty();

            this.ctx.callAsync({
                method: "GET",
                url: `/polarion/pdf-exporter/rest/internal/settings/style-package/names/${selectedStylePackage}/content?scope=${scope}`,
                responseType: "json",
                onOk: (responseText, request) => {
                    this.stylePackageSelected(request.response);
                    this.ctx.querySelectorAll('button').forEach(actionButton => {
                        actionButton.disabled = false;
                    });
                },
                onError: () => {
                    $stylePackageError.append("There was an error loading style package settings. Please, contact administrator");
                }
            });
        }
    }

    stylePackageSelected(stylePackage) {
        if (!stylePackage) {
            return;
        }
        const documentLanguage = this.ctx.getElementById("document-language").value;

        this.ctx.setCheckbox("cover-page-checkbox", stylePackage.coverPage);

        this.ctx.setSelector("cover-page-selector", stylePackage.coverPage);
        this.ctx.displayIf("cover-page-selector", stylePackage.coverPage, "inline-block")

        this.ctx.setSelector("css-selector", stylePackage.css);
        this.ctx.setSelector("header-footer-selector", stylePackage.headerFooter);
        this.ctx.setSelector("localization-selector", stylePackage.localization);

        this.ctx.setCheckbox("webhooks-checkbox", !!stylePackage.webhooks);
        this.ctx.setSelector("webhooks-selector", stylePackage.webhooks);
        this.ctx.displayIf("webhooks-selector", !!stylePackage.webhooks, "inline-block")

        this.ctx.setValue("paper-size-selector", stylePackage.paperSize || 'A4');
        this.ctx.setValue("headers-color", stylePackage.headersColor);
        this.ctx.setValue("orientation-selector", stylePackage.orientation || 'PORTRAIT');
        this.ctx.setValue("pdf-variant-selector", stylePackage.pdfVariant || 'PDF_A_2B');
        this.ctx.setCheckbox("fit-to-page", stylePackage.fitToPage);

        this.ctx.setCheckbox("render-comments", !!stylePackage.renderComments);
        this.ctx.setValue("render-comments-selector", stylePackage.renderComments  || 'OPEN');
        this.ctx.displayIf("render-comments-selector", !!stylePackage.renderComments)

        this.ctx.setCheckbox("watermark", stylePackage.watermark);
        this.ctx.setCheckbox("mark-referenced-workitems", stylePackage.markReferencedWorkitems);
        this.ctx.setCheckbox("cut-empty-chapters", stylePackage.cutEmptyChapters);
        this.ctx.setCheckbox("cut-empty-wi-attributes", stylePackage.cutEmptyWorkitemAttributes);
        this.ctx.setCheckbox("cut-urls", stylePackage.cutLocalURLs);
        this.ctx.setCheckbox("presentational-hints", stylePackage.followHTMLPresentationalHints);

        this.ctx.setCheckbox("custom-list-styles", stylePackage.customNumberedListStyles);
        this.ctx.setValue("numbered-list-styles", stylePackage.customNumberedListStyles || "");
        this.ctx.displayIf("numbered-list-styles", stylePackage.customNumberedListStyles);

        this.ctx.setCheckbox("specific-chapters", stylePackage.specificChapters);
        this.ctx.setValue("chapters", stylePackage.specificChapters || "");
        this.ctx.displayIf("chapters", stylePackage.specificChapters);

        this.ctx.setCheckbox("metadata-fields", stylePackage.metadataFields);
        this.ctx.setValue("metadata-fields-input", stylePackage.metadataFields || "");
        this.ctx.displayIf("metadata-fields-input", stylePackage.metadataFields);

        this.ctx.setCheckbox("localization", stylePackage.language);
        this.ctx.setValue("language", (stylePackage.exposeSettings && stylePackage.language && documentLanguage) ? documentLanguage : stylePackage.language);
        this.ctx.displayIf("language", stylePackage.language);

        this.ctx.setCheckbox("selected-roles", stylePackage.linkedWorkitemRoles);
        this.ctx.querySelectorAll(`#roles-selector option`).forEach(roleOption => {
            roleOption.selected = false;
        });
        if (stylePackage.linkedWorkitemRoles) {
            for (const role of stylePackage.linkedWorkitemRoles) {
                this.ctx.querySelectorAll(`#roles-selector option[value='${role}']`).forEach(roleOption => {
                    roleOption.selected = true;
                });
            }
        }
        this.ctx.displayIf("roles-wrapper", stylePackage.linkedWorkitemRoles);

        this.ctx.displayIf("style-package-content", stylePackage.exposeSettings);
        this.ctx.displayIf("page-width-validation", stylePackage.exposePageWidthValidation);
    }

    setClass(elementId, className) {
        this.ctx.getElementById(elementId).className = className;
    }

    prepareRequest(projectId, locationPath, baselineRevision, revision, fileName) {
        let selectedChapters = null;
        if (this.ctx.getElementById("specific-chapters").checked) {
            selectedChapters = this.getSelectedChapters();
            this.setClass("chapters", selectedChapters ? "" : "error");
            if (!selectedChapters) {
                this.ctx.getJQueryElement("#export-error").append("Please, provide comma separated list of integer values in 'Specific higher level chapters' field");
                return undefined;
            }
        }

        let selectedMetadataFields = null;
        if (this.ctx.getElementById("metadata-fields").checked) {
            selectedMetadataFields = this.getSelectedMetadataFields();
            this.setClass("metadata-fields-input", selectedMetadataFields ? "" : "error");
            if (!selectedMetadataFields) {
                this.ctx.getJQueryElement("#export-error").append("Please, provide comma separated list of values in 'Metadata fields' field");
                return undefined;
            }
        }

        let numberedListStyles = null;
        if (this.ctx.getElementById("custom-list-styles").checked) {
            numberedListStyles = this.ctx.getElementById("numbered-list-styles").value;
            const error = this.validateNumberedListStyles(numberedListStyles);
            this.setClass("numbered-list-styles", error ? "error" : "");
            if (error) {
                this.ctx.getJQueryElement("#export-error").append(error);
                return undefined;
            }
        }

        const selectedRoles = [];
        if (this.ctx.getElementById("selected-roles").checked) {
            const selectedOptions = Array.from(this.ctx.getElementById("roles-selector").options).filter(opt => opt.selected);
            selectedRoles.push(...selectedOptions.map(opt => opt.value));
        }

        return this.buildRequestJson(projectId, locationPath, baselineRevision, revision, selectedChapters, selectedMetadataFields, numberedListStyles, selectedRoles, fileName);
    }

    buildRequestJson(projectId, locationPath, baselineRevision, revision, selectedChapters, selectedMetadataFields, numberedListStyles, selectedRoles, fileName) {
        const live_doc = this.ctx.getDocumentType() === ExportParams.DocumentType.LIVE_DOC;
        const test_run = this.ctx.getDocumentType() === ExportParams.DocumentType.TEST_RUN;
        const urlSearchParams = new URL(window.location.href.replace('#', '/')).searchParams;
        return new ExportParams.Builder(ExportParams.DocumentType.LIVE_DOC)
            .setProjectId(projectId)
            .setLocationPath(locationPath)
            .setBaselineRevision(baselineRevision)
            .setRevision(revision)
            .setCoverPage(this.ctx.getElementById("cover-page-checkbox").checked ? this.ctx.getElementById("cover-page-selector").value : null)
            .setCss(this.ctx.getElementById("css-selector").value)
            .setHeaderFooter(this.ctx.getElementById("header-footer-selector").value)
            .setLocalization(this.ctx.getElementById("localization-selector").value)
            .setWebhooks(this.ctx.getElementById("webhooks-checkbox").checked ? this.ctx.getElementById("webhooks-selector").value : null)
            .setHeadersColor(this.ctx.getElementById("headers-color").value)
            .setPaperSize(this.ctx.getElementById("paper-size-selector").value)
            .setOrientation(this.ctx.getElementById("orientation-selector").value)
            .setPdfVariant(this.ctx.getElementById("pdf-variant-selector").value)
            .setFitToPage((live_doc || test_run) && this.ctx.getElementById('fit-to-page').checked)
            .setRenderComments(this.ctx.getElementById('render-comments').checked ? this.ctx.getElementById("render-comments-selector").value : null)
            .setWatermark(this.ctx.getElementById("watermark").checked)
            .setMarkReferencedWorkitems(live_doc && this.ctx.getElementById("mark-referenced-workitems").checked)
            .setCutEmptyChapters(live_doc && this.ctx.getElementById("cut-empty-chapters").checked)
            .setCutEmptyWIAttributes(live_doc && this.ctx.getElementById('cut-empty-wi-attributes').checked)
            .setCutLocalUrls(this.ctx.getElementById("cut-urls").checked)
            .setFollowHTMLPresentationalHints(this.ctx.getElementById("presentational-hints").checked)
            .setNumberedListStyles(numberedListStyles)
            .setChapters(selectedChapters)
            .setMetadataFields(live_doc && this.ctx.getElementById('metadata-fields').checked ? selectedMetadataFields : null)
            .setLanguage(live_doc && this.ctx.getElementById('localization').checked ? this.ctx.getElementById("language").value : null)
            .setLinkedWorkitemRoles(selectedRoles)
            .setScaleFactor(this.ctx.getElementById("scale-factor-selector").value)
            .setFileName(fileName)
            .setUrlQueryParameters(Object.fromEntries([...urlSearchParams]))
            .build()
            .toJSON();
    }

    getSelectedChapters() {
        const chaptersValue = this.ctx.getElementById("chapters").value;
        let chapters = (chaptersValue?.replaceAll(" ", "") || "").split(",");
        if (chapters && chapters.length > 0) {
            for (const chapter of chapters) {
                const parsedValue = Number.parseInt(chapter);
                if (Number.isNaN(parsedValue) || parsedValue < 1 || String(parsedValue) !== chapter) {
                    // Stop processing if not valid numbers
                    return undefined;
                }
            }
        }
        return chapters;
    }

    getSelectedMetadataFields() {
        const metadataFieldsValue = this.ctx.getElementById("metadata-fields-input")?.value ?? "";
        return metadataFieldsValue
            .split(",")
            .map(f => f.trim())
            .filter(f => f.length > 0);
    }

    validateNumberedListStyles(numberedListStyles) {
        if (!numberedListStyles || numberedListStyles.trim().length === 0) {
            // Stop processing if empty
            return "Please, provide some value";
        } else if (numberedListStyles.match("[^1aAiI]+")) {
            // Stop processing if not valid styles
            return "Please, provide any combination of characters '1aAiI'";

        }
        return undefined;
    }

    loadPdf() {
        //clean previous errors
        this.ctx.getJQueryElement("#export-error").empty();
        this.ctx.getJQueryElement("#export-warning").empty();

        let fileName = this.ctx.getElementById("filename").value;
        if (!fileName) {
            fileName = this.ctx.getElementById("filename").dataset.default;
        }
        if (fileName && !fileName.endsWith(".pdf")) {
            fileName += ".pdf";
        }

        const projectId = this.ctx.getProjectId();
        const locationPath = this.ctx.getLocationPath();
        const baselineRevision = this.ctx.getBaselineRevision();
        const revision = this.ctx.getRevision();

        let request = this.prepareRequest(projectId, locationPath, baselineRevision, revision, fileName);
        if (request === undefined) {
            return;
        }

        this.actionInProgress(true);

        this.ctx.callAsync({
            method: "POST",
            url: "/polarion/pdf-exporter/rest/internal/checknestedlists",
            contentType: "application/json",
            responseType: "json",
            body: request,
            onOk: (responseText, request) => {
                if (request.response.containsNestedLists) {
                    this.ctx.getJQueryElement("#export-warning").append("Document contains nested numbered lists which structures were not valid. " +
                        "We did our best to fix it, but be aware of it.");
                }
            },
            onError: (status, errorMessage, request) => {
                this.ctx.getJQueryElement("#export-error").append("Error occurred validating nested lists" + (request.response.message ? ":<br>" + request.response.message : ""));
            }
        });

        this.ctx.asyncConvertPdf(request, result => {
            if (result.warning) {
                this.ctx.getJQueryElement("#export-warning").append(result.warning);
            }
            this.actionInProgress(false);

            this.ctx.downloadBlob(result.response, fileName);
        }, errorResponse => {
            this.actionInProgress(false);
            errorResponse.text().then(errorJson => {
                const error = errorJson && JSON.parse(errorJson);
                const errorMessage = error && (error.message ? error.message : error.errorMessage);
                this.ctx.getJQueryElement("#export-error").append("Error occurred during PDF generation" + (errorMessage ? ":<br>" + errorMessage : ""));
            });
        });
    }

    actionInProgress(inProgress) {
        if (inProgress) {
            //disable components
            this.ctx.getJQueryElement(":input").attr("disabled", true);
            //show loading icon
            this.ctx.getJQueryElement("#export-pdf-progress").show();
        } else {
            //enable components
            this.ctx.getJQueryElement(":input").attr("disabled", false);
            //hide loading icon
            this.ctx.getJQueryElement("#export-pdf-progress").hide();
        }
    }

    validatePdf() {
        //clean previous errors
        this.ctx.getJQueryElement("#validate-error").empty();
        this.ctx.getJQueryElement("#validate-ok").empty();

        const projectId = this.ctx.getProjectId();
        const locationPath = this.ctx.getLocationPath();
        const baselineRevision = this.ctx.getBaselineRevision();
        const revision = this.ctx.getRevision();

        const request = this.prepareRequest(projectId, locationPath, baselineRevision, revision);
        if (request === undefined) {
            return;
        }

        //disable components
        this.ctx.getJQueryElement(":input").attr("disabled", true);
        //show loading icon
        this.ctx.getJQueryElement("#validate-pdf-progress").show();

        const stopProgress = () => {
            //enable components
            this.ctx.getJQueryElement(":input").attr("disabled", false);
            //hide loading icon
            this.ctx.getJQueryElement("#validate-pdf-progress").hide();
            this.ctx.getElementById('validate-error').replaceChildren(); //remove div content
        };

        this.ctx.callAsync({
            method: "POST",
            url: "/polarion/pdf-exporter/rest/internal/validate?max-results=6",
            contentType: "application/json",
            responseType: "json",
            body: request,
            onOk: (responseText, request) => {
                stopProgress();
                const container = this.ctx.getElementById('validate-error');
                let result = request.response;
                let pages = result.invalidPages.length;
                if (pages === 0) {
                    this.ctx.getJQueryElement("#validate-ok").append("OK");
                    return;
                }
                let message = (pages > 5 ? 'More than 5' : pages) + ' invalid page' + (pages === 1 ? '' : 's') + ' found:';
                container.appendChild(document.createTextNode(message));
                container.appendChild(document.createElement("br"));
                for (const page of result.invalidPages) {
                    let img = document.createElement("img");
                    img.className = 'validate-result-img';
                    img.src = 'data:image/png;base64,' + page.content;
                    img.onclick = function () {
                        this.classList.toggle("img-zoomed");
                    };
                    container.appendChild(img);
                }
                let suspects = result.suspiciousWorkItems.length;
                if (suspects > 0) {
                    container.appendChild(document.createElement("br"));
                    container.appendChild(document.createTextNode("Suspicious work items:"));
                    let ul = document.createElement('ul');
                    ul.className = 'suspicious-list';
                    for (const suspect of result.suspiciousWorkItems) {
                        let li = document.createElement('li');
                        let link = document.createElement("a");
                        link.href = suspect.link;
                        link.text = suspect.id;
                        link.target = '_blank';
                        li.appendChild(link);
                        ul.appendChild(li);
                    }
                    container.appendChild(ul);
                }
            },
            onError: (status, errorMessage, request) => {
                stopProgress();
                this.ctx.getJQueryElement("#validate-error").append("Error occurred validating pages width" + (request.response.message ? ":<br>" + request.response.message : ""));
            }
        });
    }
}

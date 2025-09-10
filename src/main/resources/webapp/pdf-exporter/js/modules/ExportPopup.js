import ExportParams from "./ExportParams.js";
import ExportContext from "./ExportContext.js";

export default class ExportPopup {

    constructor({documentType = ExportParams.DocumentType.LIVE_DOC, bulkCallback} = {}) {
        this.ctx = new ExportContext({
            rootComponentSelector: "#pdf-export-popup",
            documentType: documentType,
            exportType: bulkCallback ? ExportParams.ExportType.BULK : ExportParams.ExportType.SINGLE,
        });
        this.bulkCallback = bulkCallback;
        this.initPopup();
    }

    initPopup() {
        document.getElementById(POPUP_ID)?.remove();
        const popup = document.createElement('div');
        popup.classList.add("modal");
        popup.classList.add("micromodal-slide");
        popup.id = POPUP_ID;
        popup.setAttribute("aria-hidden", "true");
        popup.innerHTML = POPUP_HTML;
        document.body.appendChild(popup);

        fetch('/polarion/pdf-exporter/html/popupForm.html')
            .then(response => response.text())
            .then(content => {
                this.renderPanel(content);
            });
    }

    renderPanel(content) {
        this.ctx.querySelector(".modal__content").innerHTML = content;
        this.ctx.querySelector(".modal__footer .action-button").style.display = "inline-block";

        this.ctx.onChange('popup-style-package-select', () => {
            this.onStylePackageChanged()
        });
        this.ctx.onClick(
            'popup-validate-pdf', () => {
                this.validatePdf()
            },
            'popup-export-pdf', () => {
                this.exportToPdf()
            });

        this.openPopup();
    }

    openPopup() {
        this.hideAlerts();
        this.loadFormData();

        const Action = {
            SHOW: "flex",
            HIDE: "none",
            getOpposite(value) {
                return value === this.HIDE ? this.SHOW : this.HIDE;
            }
        }

        function toggleAllOptionalPropertyBlocks(action, ctx) {
            const types = [
                ExportParams.DocumentType.LIVE_DOC,
                ExportParams.DocumentType.LIVE_REPORT,
                ExportParams.DocumentType.TEST_RUN,
                ExportParams.DocumentType.WIKI_PAGE,
                ExportParams.ExportType.SINGLE,
                ExportParams.ExportType.BULK,
            ];
            types.forEach(documentType => {
                toggleOptionalPropertyBlocks(documentType, action, ctx);
            });
        }

        function toggleOptionalPropertyBlocks(documentType, action, ctx) {
            ctx.querySelectorAll(`.property-wrapper.visible-for-${documentType}`)
                .forEach(propertyBlock => propertyBlock.style.display = action);
            ctx.querySelectorAll(`.property-wrapper.not-visible-for-${documentType}`)
                .forEach(propertyBlock => propertyBlock.style.display = Action.getOpposite(action));
        }

        toggleAllOptionalPropertyBlocks(Action.HIDE, this.ctx);
        toggleOptionalPropertyBlocks(this.ctx.getDocumentType(), Action.SHOW, this.ctx);
        toggleOptionalPropertyBlocks(this.ctx.getExportType(), Action.SHOW, this.ctx);

        MicroModal.show(POPUP_ID);
    }

    closePopup() {
        MicroModal.close(POPUP_ID);
    }

    loadFormData() {
        this.actionInProgress({inProgress: true, message: "Loading form data"});

        Promise.all([
            this.loadSettingNames({
                setting: "cover-page",
                scope: this.ctx.getScope(),
                selectElement: this.ctx.getElementById("popup-cover-page-selector")
            }),
            this.loadSettingNames({
                setting: "css",
                scope: this.ctx.getScope(),
                selectElement: this.ctx.getElementById("popup-css-selector")
            }),
            this.loadSettingNames({
                setting: "header-footer",
                scope: this.ctx.getScope(),
                selectElement: this.ctx.getElementById("popup-header-footer-selector")
            }),
            this.loadSettingNames({
                setting: "localization",
                scope: this.ctx.getScope(),
                selectElement: this.ctx.getElementById("popup-localization-selector")
            }),
            this.loadSettingNames({
                setting: "webhooks",
                scope: this.ctx.getScope(),
                selectElement: this.ctx.getElementById("popup-webhooks-selector")
            }),
            this.adjustWebhooksVisibility(),
            this.loadLinkRoles(),
            this.loadDocumentLanguage(),
            this.loadFileName(),
        ]).then(() => {
            return this.loadStylePackages();
        }).catch((error) => {
            this.showNotification({alertType: "error", message: "Error occurred loading form data" + (error.response.message ? ": " + error.response.message : "")});
            this.actionInProgress({inProgress: false});
        });
    }

    loadSettingNames({setting, scope, selectElement, customUrl, customMethod, customBody, customContentType}) {
        return new Promise((resolve, reject) => {
            this.callAsync({
                method: customMethod ? customMethod : "GET",
                url: customUrl ? customUrl : `/polarion/pdf-exporter/rest/internal/settings/${setting}/names?scope=${scope}`,
                body: customBody ? customBody : undefined,
                contentType: customContentType ? customContentType : undefined,
                responseType: "json",
            }).then(({response}) => {
                selectElement.innerHTML = ""; // Clear previously loaded content
                let namesCount = 0;
                for (let name of response) {
                    namesCount++;
                    const option = document.createElement('option');
                    option.value = name.name;
                    option.text = name.name;
                    selectElement.appendChild(option);
                }
                if (namesCount === 0) {
                    reject();
                } else {
                    resolve();
                }
            }).catch((error) => reject(error));
        });
    }

    loadLinkRoles() {
        if (this.ctx.getDocumentType() === ExportParams.DocumentType.LIVE_REPORT
            || this.ctx.getDocumentType() === ExportParams.DocumentType.TEST_RUN
            || this.ctx.getExportType() === ExportParams.ExportType.BULK) {
            return Promise.resolve(); // Skip loading link roles for reports, test runs and bulk export
        }

        return new Promise((resolve, reject) => {
            this.callAsync({
                method: "GET",
                url: `/polarion/pdf-exporter/rest/internal/link-role-names?scope=${this.ctx.getScope()}`,
                responseType: "json",
            }).then(({response}) => {
                const selectElement = this.ctx.getElementById("popup-roles-selector");
                selectElement.innerHTML = ""; // Clear previously loaded content
                for (let name of response) {
                    const option = document.createElement('option');
                    option.value = name;
                    option.text = name;
                    selectElement.appendChild(option);
                }
                resolve();
            }).catch((error) => reject(error));
        });
    }

    loadFileName() {
        if (this.ctx.getExportType() === ExportParams.ExportType.BULK) {
            return Promise.resolve(); // Skip loading file name for bulk export
        }

        const requestBody = this.ctx.toExportParams().toJSON();

        return new Promise((resolve, reject) => {
            this.callAsync({
                method: "POST",
                url: `/polarion/pdf-exporter/rest/internal/export-filename`,
                body: requestBody,
            }).then(({responseText}) => {
                this.ctx.getElementById("popup-filename").value = responseText;
                this.ctx.getElementById("popup-filename").dataset.default = responseText;
                resolve()
            }).catch((error) => reject(error));
        });
    }

    adjustWebhooksVisibility() {
        return new Promise((resolve, reject) => {
            this.callAsync({
                method: "GET",
                url: `/polarion/pdf-exporter/rest/internal/webhooks/status`,
                responseType: "json",
            }).then(({response}) => {
                this.ctx.getElementById("webhooks-container").style.display = response.enabled ? "flex" : "none";
                resolve()
            }).catch((error) => reject(error));
        });
    }

    loadDocumentLanguage() {
        if (this.ctx.documentType === ExportParams.DocumentType.LIVE_REPORT
            || this.ctx.documentType === ExportParams.DocumentType.TEST_RUN
            || this.ctx.getExportType() === ExportParams.ExportType.BULK) {
            return Promise.resolve(); // Skip loading language for reports, test runs and bulk export
        }

        let url = `/polarion/pdf-exporter/rest/internal/document-language?projectId=${this.ctx.getProjectId()}&spaceId=${this.ctx.getSpaceId()}&documentName=${this.ctx.getDocumentName()}`;
        if (this.ctx.revision) {
            url += `&revision=${this.ctx.revision}`;
        }
        return new Promise((resolve, reject) => {
            this.callAsync({
                method: "GET",
                url: url,
            }).then(({responseText}) => {
                this.documentLanguage = responseText;
                resolve();
            }).catch((error) => reject(error));
        });
    }

    loadStylePackages() {
        let stylePackagesUrl = `/polarion/pdf-exporter/rest/internal/settings/style-package/suitable-names`;
        const docIdentifiers = this.ctx.getExportType() === ExportParams.ExportType.BULK ? this.bulkCallback.getDocIdentifiers() :
            [{
                ...(this.ctx.getProjectId() !== null && {projectId: `${this.ctx.getProjectId()}`}),
                ...(this.ctx.getSpaceId() !== null && {spaceId: `${this.ctx.getSpaceId()}`}),
                documentName: `${this.ctx.getDocumentName()}`
            }];

        return this.loadSettingNames({
            customUrl: stylePackagesUrl,
            selectElement: this.ctx.getElementById("popup-style-package-select"),
            customMethod: 'POST',
            customBody: JSON.stringify(docIdentifiers),
            customContentType: 'application/json'
        }).then(() => {
            const stylePackageSelect = this.ctx.getElementById("popup-style-package-select");
            const valueToPreselect = this.ctx.getCookie(SELECTED_STYLE_PACKAGE_COOKIE);
            if (valueToPreselect && this.containsOption(stylePackageSelect, valueToPreselect)) {
                stylePackageSelect.value = valueToPreselect;
            }

            this.onStylePackageChanged();
            this.actionInProgress({inProgress: false});
        });
    }

    onStylePackageChanged() {
        const selectedStylePackageName = this.ctx.getElementById("popup-style-package-select").value;
        if (selectedStylePackageName) {
            this.ctx.setCookie(SELECTED_STYLE_PACKAGE_COOKIE, selectedStylePackageName);

            this.actionInProgress({inProgress: true, message: "Loading style package data"});

            this.callAsync({
                method: "GET",
                url: `/polarion/pdf-exporter/rest/internal/settings/style-package/names/${selectedStylePackageName}/content?scope=${this.ctx.getScope()}`,
                responseType: "json",
            }).then(({response}) => {
                this.stylePackageSelected(response);

                this.actionInProgress({inProgress: false});
            }).catch((error) => {
                this.showNotification({alertType: "error", message: "Error occurred loading style package data" + (error?.response.message ? ": " + error.response.message : "")});
                this.actionInProgress({inProgress: false});
            });
        }
    }

    stylePackageSelected(stylePackage) {
        if (!stylePackage) {
            return;
        }

        this.ctx.setCheckbox("popup-cover-page-checkbox", stylePackage.coverPage);

        this.ctx.setSelector("popup-cover-page-selector", stylePackage.coverPage);
        this.ctx.visibleIf("popup-cover-page-selector", stylePackage.coverPage)

        this.ctx.setSelector("popup-css-selector", stylePackage.css);
        this.ctx.setSelector("popup-header-footer-selector", stylePackage.headerFooter);
        this.ctx.setSelector("popup-localization-selector", stylePackage.localization);

        this.ctx.setCheckbox("popup-webhooks-checkbox", !!stylePackage.webhooks);
        this.ctx.setSelector("popup-webhooks-selector", stylePackage.webhooks);
        this.ctx.visibleIf("popup-webhooks-selector", !!stylePackage.webhooks)

        this.ctx.setValue("popup-headers-color", stylePackage.headersColor);
        this.ctx.setValue("popup-paper-size-selector", stylePackage.paperSize || ExportParams.PaperSize.A4);
        this.ctx.setValue("popup-orientation-selector", stylePackage.orientation || ExportParams.Orientation.PORTRAIT);
        this.ctx.setValue("popup-pdf-variant-selector", stylePackage.pdfVariant || ExportParams.PdfVariant.PDF_A_2B);
        this.ctx.setCheckbox("popup-fit-to-page", stylePackage.fitToPage);

        this.ctx.setCheckbox("popup-render-comments", !!stylePackage.renderComments);
        this.ctx.setValue("popup-render-comments-selector", stylePackage.renderComments || 'OPEN');
        this.ctx.visibleIf("popup-render-comments-selector", !!stylePackage.renderComments);

        this.ctx.setCheckbox("popup-watermark", stylePackage.watermark);
        this.ctx.setCheckbox("popup-mark-referenced-workitems", stylePackage.markReferencedWorkitems);
        this.ctx.setCheckbox("popup-cut-urls", stylePackage.cutLocalURLs);
        this.ctx.setCheckbox("popup-cut-empty-chapters", stylePackage.cutEmptyChapters);
        this.ctx.setCheckbox("popup-cut-empty-wi-attributes", stylePackage.cutEmptyWorkitemAttributes);
        this.ctx.setCheckbox("popup-presentational-hints", stylePackage.followHTMLPresentationalHints);

        this.ctx.setCheckbox("popup-custom-list-styles", stylePackage.customNumberedListStyles);
        this.ctx.setValue("popup-numbered-list-styles", stylePackage.customNumberedListStyles || "");
        this.ctx.visibleIf("popup-numbered-list-styles", stylePackage.customNumberedListStyles);

        this.ctx.setCheckbox("popup-specific-chapters", stylePackage.specificChapters);
        this.ctx.setValue("popup-chapters", stylePackage.specificChapters || "");
        this.ctx.visibleIf("popup-chapters", stylePackage.specificChapters);

        this.ctx.setCheckbox("popup-metadata-fields", stylePackage.metadataFields);
        this.ctx.setValue("popup-metadata-fields-input", stylePackage.metadataFields || "");
        this.ctx.visibleIf("popup-metadata-fields-input", stylePackage.metadataFields);

        this.ctx.setCheckbox("popup-localization", stylePackage.language);
        let languageValue;
        if (stylePackage.exposeSettings && stylePackage.language && this.documentLanguage) {
            languageValue = this.documentLanguage;
        } else if (stylePackage.language) {
            languageValue = stylePackage.language;
        } else {
            const firstOption = this.ctx.getElementById("popup-language").querySelector("option:first-child");
            languageValue = firstOption?.value;
        }
        this.ctx.setValue("popup-language", languageValue);
        this.ctx.visibleIf("popup-language", stylePackage.language);

        const rolesProvided = stylePackage.linkedWorkitemRoles && stylePackage.linkedWorkitemRoles.length && stylePackage.linkedWorkitemRoles.length > 0;
        this.ctx.setCheckbox("popup-selected-roles", rolesProvided);
        this.ctx.querySelectorAll(`#popup-roles-selector option`).forEach(roleOption => {
            roleOption.selected = false;
        });
        if (stylePackage.linkedWorkitemRoles) {
            for (const role of stylePackage.linkedWorkitemRoles) {
                this.ctx.querySelectorAll(`#popup-roles-selector option[value='${role}']`).forEach(roleOption => {
                    roleOption.selected = true;
                });
            }
        }
        this.ctx.displayIf("popup-roles-selector", this.ctx.getExportType() !== ExportParams.ExportType.BULK && rolesProvided, "inline-block");

        this.ctx.displayIf("popup-style-package-content", stylePackage.exposeSettings);
        this.ctx.displayIf("popup-page-width-validation", this.ctx.getExportType() !== ExportParams.ExportType.BULK && stylePackage.exposePageWidthValidation);

        let attachmentFieldsVisible = stylePackage.attachmentsFilter || stylePackage.testcaseFieldId;
        this.ctx.setCheckbox("popup-download-attachments", attachmentFieldsVisible);
        this.ctx.setValue("popup-attachments-filter", stylePackage.attachmentsFilter || "");
        this.ctx.setValue("popup-testcase-field-id", stylePackage.testcaseFieldId || "");
        this.ctx.displayIf("popup-attachments-filter-container", attachmentFieldsVisible, "flex");
        this.ctx.displayIf("popup-testcase-field-id-container", attachmentFieldsVisible, "flex");

        this.ctx.displayIf("popup-embed-attachments-label", attachmentFieldsVisible);
        this.ctx.setCheckbox("popup-embed-attachments", stylePackage.embedAttachments);
    }

    validatePdf() {
        this.hideAlerts();

        const exportParams = this.getExportParams();
        if (exportParams === undefined) {
            return;
        }
        this.actionInProgress({inProgress: true, message: "Performing PDF validation"})

        this.callAsync({
            method: "POST",
            url: "/polarion/pdf-exporter/rest/internal/validate?max-results=5",
            body: exportParams.toJSON(),
            responseType: "json"
        }).then(({response}) => {
            this.actionInProgress({inProgress: false});

            const pages = response.invalidPages?.length;
            if (pages && pages > 0) {
                const pagesWord = 'page' + (pages === 1 ? '' : 's');
                this.showValidationResult({
                    alertType: "error",
                    message: pages > MAX_PAGE_PREVIEWS
                        ? `Invalid pages found. First ${MAX_PAGE_PREVIEWS} of them:`
                        : `${pages} invalid ${pagesWord} found:`
                });
                this.createPreviews(response);
            } else {
                this.showValidationResult({alertType: "success", message: "All pages are valid"});
            }
        }).catch((error) => {
            this.showNotification({alertType: "error", message: "Error occurred validating pages width" + (error?.response.message ? ": " + error.response.message : "")});
            this.actionInProgress({inProgress: false});
        })
    }

    createPreviews(result) {
        const pagePreviews = this.ctx.getElementById('page-previews');
        const pagesQuantity = Math.min(MAX_PAGE_PREVIEWS, result.invalidPages.length);
        for (let i = 0; i < pagesQuantity; i++) {
            const page = result.invalidPages[i];
            const img = document.createElement("img");
            img.className = 'popup-validate-result-img';
            img.src = 'data:image/png;base64,' + page.content;
            img.onclick = function () {
                this.classList.toggle("popup-img-zoomed");
            };
            pagePreviews.appendChild(img);
        }

        const suspects = result.suspiciousWorkItems.length;
        if (suspects > 0) {
            this.addSuspiciousWiLinks(result.suspiciousWorkItems);
        }
    }

    addSuspiciousWiLinks(suspiciousWorkItems) {
        const suspiciousWiContainer = this.ctx.getElementById("suspicious-wi");
        suspiciousWiContainer.appendChild(document.createTextNode("Suspicious work items:"));
        const ul = document.createElement("ul");
        ul.classList.add("suspicious-list");
        for (const suspect of suspiciousWorkItems) {
            let li = document.createElement("li");
            let link = document.createElement("a");
            link.href = suspect.link;
            link.text = suspect.id;
            link.target = "_blank";
            li.appendChild(link);
            ul.appendChild(li);
        }
        suspiciousWiContainer.appendChild(ul);
    }

    exportToPdf() {
        this.hideAlerts();

        let fileName = this.ctx.getElementById("popup-filename").value;
        if (!fileName) {
            fileName = this.ctx.getElementById("popup-filename").dataset.default;
        }
        if (fileName && !fileName.endsWith(".pdf")) {
            fileName += ".pdf";
        }

        const exportParams = this.getExportParams(fileName);
        if (exportParams === undefined) {
            return;
        }

        if (this.bulkCallback && this.ctx.getExportType() === ExportParams.ExportType.BULK) {
            this.closePopup();
            this.bulkCallback.openPopup(exportParams);
            return;
        }

        if (this.ctx.getDocumentType() === ExportParams.DocumentType.TEST_RUN && exportParams.attachmentsFilter !== null && !exportParams.embedAttachments) {
            const testRunId = new URLSearchParams(this.ctx.getUrlQueryParameters()).get("id")
            this.ctx.downloadTestRunAttachments(exportParams.projectId, testRunId, exportParams.revision, exportParams.attachmentsFilter, exportParams.testcaseFieldId);
        }

        this.actionInProgress({inProgress: true, message: "Generating PDF"})

        const requestBody = exportParams.toJSON();
        if (this.ctx.getDocumentType() !== ExportParams.DocumentType.LIVE_REPORT && this.ctx.getDocumentType() !== ExportParams.DocumentType.TEST_RUN) {
            this.checkNestedListsAsync(requestBody);
        }

        this.ctx.asyncConvertPdf(requestBody, result => {
            if (result.warning) {
                this.showNotification({alertType: "warning", message: result.warning});
            }
            this.ctx.downloadBlob(result.response, fileName);

            this.showNotification({alertType: "success", message: "PDF was successfully generated"});
            this.actionInProgress({inProgress: false});
        }, errorResponse => {
            errorResponse.text().then(errorJson => {
                const error = errorJson && JSON.parse(errorJson);
                const errorMessage = error && (error.message ? error.message : error.errorMessage);
                this.showNotification({alertType: "error", message: "Error occurred during PDF generation" + (errorMessage ? ": " + errorMessage : "")});
            });
            this.actionInProgress({inProgress: false});
        });
    }

    checkNestedListsAsync(requestBody) {
        this.callAsync({
            method: "POST",
            url: "/polarion/pdf-exporter/rest/internal/checknestedlists",
            body: requestBody,
            responseType: "json"
        }).then(({response}) => {
            if (response?.containsNestedLists) {
                this.showNotification({alertType: "warning", message: "Document contains nested numbered lists which structures were not valid. We tried to fix this, but be aware of it."});
            }
        }).catch((error) => {
            this.showNotification({alertType: "error", message: "Error occurred validating nested lists" + (error?.response.message ? ": " + error.response.message : "")});
        })
    }

    getExportParams(fileName) {
        let selectedChapters = null;
        if (this.ctx.getElementById("popup-specific-chapters").checked) {
            selectedChapters = this.getSelectedChapters();
            if (!selectedChapters) {
                this.ctx.getElementById("popup-chapters").classList.add("error");
                this.showNotification({alertType: "error", message: "Please, provide comma separated list of integer values in 'Specific higher level chapters' field"});
                // Stop processing if not valid numbers
                return undefined;
            }
        }

        let selectedMetadataFields = null;
        if (this.ctx.getElementById("popup-metadata-fields").checked) {
            selectedMetadataFields = this.getSelectedMetadataFields();
            if (!selectedMetadataFields) {
                this.ctx.getElementById("popup-metadata-fields-input").classList.add("error");
                this.showNotification({alertType: "error", message: "Please, provide comma separated list of values in 'Metadata fields' field"});
                return undefined;
            }
        }

        let numberedListStyles = null;
        if (this.ctx.getElementById("popup-custom-list-styles").checked) {
            numberedListStyles = this.ctx.getElementById("popup-numbered-list-styles").value;
            const error = this.validateNumberedListStyles(numberedListStyles);
            if (error) {
                this.ctx.getElementById("popup-numbered-list-styles").classList.add("error");
                this.showNotification({alertType: "error", message: error});
                return undefined;
            }
        }

        const selectedRoles = [];
        if (this.ctx.getElementById("popup-selected-roles").checked) {
            const selectedOptions = Array.from(this.ctx.getElementById("popup-roles-selector").options).filter(opt => opt.selected);
            selectedRoles.push(...selectedOptions.map(opt => opt.value));
        }

        let attachmentsFilter = null;
        let testcaseFieldId = null;
        if (this.ctx.getElementById("popup-download-attachments").checked) {
            attachmentsFilter = this.ctx.getElementById("popup-attachments-filter").value;
            testcaseFieldId = this.ctx.getElementById("popup-testcase-field-id").value;
        }

        return this.buildExportParams(selectedChapters, selectedMetadataFields, numberedListStyles, selectedRoles, fileName, attachmentsFilter, testcaseFieldId);
    }

    buildExportParams(selectedChapters, selectedMetadataFields, numberedListStyles, selectedRoles, fileName, attachmentsFilter, testcaseFieldId) {
        const live_doc = this.ctx.getDocumentType() === ExportParams.DocumentType.LIVE_DOC;
        const test_run = this.ctx.getDocumentType() === ExportParams.DocumentType.TEST_RUN;
        return new ExportParams.Builder(this.ctx.getDocumentType())
            .setProjectId(this.ctx.getProjectId())
            .setLocationPath(this.ctx.getLocationPath())
            .setBaselineRevision(this.ctx.getBaselineRevision())
            .setRevision(this.ctx.getRevision())
            .setCoverPage(this.ctx.getElementById("popup-cover-page-checkbox").checked ? this.ctx.getElementById("popup-cover-page-selector").value : null)
            .setCss(this.ctx.getElementById("popup-css-selector").value)
            .setHeaderFooter(this.ctx.getElementById("popup-header-footer-selector").value)
            .setLocalization(this.ctx.getElementById("popup-localization-selector").value)
            .setWebhooks(this.ctx.getElementById("popup-webhooks-checkbox").checked ? this.ctx.getElementById("popup-webhooks-selector").value : null)
            .setHeadersColor(this.ctx.getElementById("popup-headers-color").value)
            .setPaperSize(this.ctx.getElementById("popup-paper-size-selector").value)
            .setOrientation(this.ctx.getElementById("popup-orientation-selector").value)
            .setPdfVariant(this.ctx.getElementById("popup-pdf-variant-selector").value)
            .setFitToPage((live_doc || test_run) && this.ctx.getElementById('popup-fit-to-page').checked)
            .setRenderComments(live_doc && this.ctx.getElementById('popup-render-comments').checked ? this.ctx.getElementById("popup-render-comments-selector").value : null)
            .setWatermark(this.ctx.getElementById("popup-watermark").checked)
            .setMarkReferencedWorkitems(live_doc && this.ctx.getElementById("popup-mark-referenced-workitems").checked)
            .setCutEmptyChapters(live_doc && this.ctx.getElementById("popup-cut-empty-chapters").checked)
            .setCutEmptyWIAttributes(live_doc && this.ctx.getElementById('popup-cut-empty-wi-attributes').checked)
            .setCutLocalUrls(this.ctx.getElementById("popup-cut-urls").checked)
            .setFollowHTMLPresentationalHints(this.ctx.getElementById("popup-presentational-hints").checked)
            .setNumberedListStyles(numberedListStyles)
            .setChapters(selectedChapters)
            .setMetadataFields(live_doc && this.ctx.getElementById('popup-metadata-fields').checked ? selectedMetadataFields : null)
            .setLanguage(live_doc && this.ctx.getElementById('popup-localization').checked ? this.ctx.getElementById("popup-language").value : null)
            .setLinkedWorkitemRoles(selectedRoles)
            .setFileName(fileName)
            .setUrlQueryParameters(this.ctx.getUrlQueryParameters())
            .setAttachmentsFilter(test_run && this.ctx.getElementById("popup-download-attachments").checked ? attachmentsFilter ?? '' : null)
            .setTestcaseFieldId(test_run && this.ctx.getElementById("popup-download-attachments").checked && testcaseFieldId ? testcaseFieldId : null)
            .setEmbedAttachments(test_run && this.ctx.getElementById("popup-download-attachments").checked && this.ctx.getElementById("popup-embed-attachments").checked)
            .setScaleFactor(this.ctx.getElementById("popup-scale-factor-selector").value)
            .build();
    }

    getSelectedChapters() {
        const chaptersValue = this.ctx.getElementById("popup-chapters").value;
        let selectedChapters = (chaptersValue?.replaceAll(" ", "") || "").split(",");
        if (selectedChapters && selectedChapters.length > 0) {
            for (const chapter of selectedChapters) {
                const parsedValue = Number.parseInt(chapter);
                if (Number.isNaN(parsedValue) || parsedValue < 1 || String(parsedValue) !== chapter) {
                    // Stop processing if not valid numbers
                    return undefined;
                }
            }
        }
        return selectedChapters;
    }

    getSelectedMetadataFields() {
        const metadataFieldsValue = this.ctx.getElementById("popup-metadata-fields-input")?.value ?? "";
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

    containsOption(selectElement, option) {
        return [...selectElement.options].map(o => o.value).includes(option);
    }

    actionInProgress({inProgress, message}) {
        if (inProgress) {
            this.hideAlerts();
        }
        this.ctx.querySelectorAll(".action-button").forEach(button => {
            button.disabled = inProgress;
        });
        this.ctx.getElementById("in-progress-message").innerHTML = message;
        if (inProgress) {
            this.ctx.querySelector(".in-progress-overlay").classList.add("show");
        } else {
            this.ctx.querySelector(".in-progress-overlay").classList.remove("show");
        }
    }

    showNotification({alertType, message}) {
        const alert = this.ctx.querySelector(`.modal__content .notifications .alert.alert-${alertType}`);
        if (alert) {
            alert.textContent = message; // to avoid XSS do not use innerHTML here because message may contain arbitrary error response data
            alert.style.display = "block";
        }
    }

    showValidationResult({alertType, message}) {
        const alert = this.ctx.querySelector(`.modal__content .validation-alerts .alert.alert-${alertType}`);
        if (alert) {
            alert.innerHTML = message;
            alert.style.display = "block";
        }
    }

    hideAlerts() {
        this.ctx.querySelectorAll(".modal__content .alert").forEach(alert => {
            alert.style.display = "none";
        });
        this.ctx.querySelectorAll(".modal__content input.error").forEach(input => {
            input.classList.remove("error");
        });
        if (this.ctx.getElementById('page-previews')) {
            this.ctx.getElementById('page-previews').innerHTML = "";
        }
        if (this.ctx.getElementById("suspicious-wi")) {
            this.ctx.getElementById("suspicious-wi").innerHTML = "";
        }
    }

    callAsync({method, url, contentType, responseType, body}) {
        return new Promise((resolve, reject) => {
            this.ctx.callAsync({
                method: method,
                url: url,
                contentType: contentType || 'application/json',
                responseType: responseType,
                body: body,
                onOk: (responseText, request) => {
                    resolve(responseType === "blob" || responseType === "json" ? {response: request.response} : {responseText: responseText});
                },
                onError: (status, errorMessage, request) => {
                    reject(request);
                }
            });
        });
    }
}

const SELECTED_STYLE_PACKAGE_COOKIE = 'selected-style-package';
const MAX_PAGE_PREVIEWS = 4;

const POPUP_ID = "pdf-export-modal-popup";
const POPUP_HTML = `
    <div class="modal__overlay" tabindex="-1" data-micromodal-close>
        <div id="pdf-export-popup" class="modal__container pdf-exporter" role="dialog" aria-modal="true" aria-labelledby="pdf-export-modal-popup-title">
            <header class="modal__header">
                <h2 class="modal__title" id="pdf-export-modal-popup-title" style="display: flex; justify-content: space-between; width: 100%">
                    <span>Export to PDF</span>
                    <i class="fa fa-times" aria-hidden="true" data-micromodal-close style="cursor: pointer"></i>
                </h2>
            </header>
            <main class="modal__content">
                <span style="color: red; font-style: italic;">PDF exporter extension wasn't fully initialized. Please, contact system administrator</span>
            </main>
            <footer class="modal__footer">
                <button class="polarion-JSWizardButton" data-micromodal-close aria-label="Close this dialog window">Close</button>
                <button id="popup-export-pdf" class="polarion-JSWizardButton-Primary action-button" style="display: none;">Export</button>
            </footer>
        </div>
    </div>
`;

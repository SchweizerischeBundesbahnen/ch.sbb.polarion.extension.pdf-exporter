const SELECTED_STYLE_PACKAGE_COOKIE = 'selected-style-package';
const MAX_PAGE_PREVIEWS = 4;

const POPUP_ID = "pdf-export-modal-popup";
const POPUP_HTML = `
    <div class="modal__overlay" tabindex="-1" data-micromodal-close>
        <div  id="pdf-export-popup" class="modal__container pdf-exporter" role="dialog" aria-modal="true" aria-labelledby="pdf-export-modal-popup-title">
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
                <button class="polarion-JSWizardButton-Primary action-button" onclick="PdfExporter.exportToPdf()" style="display: none;">Export</button>
            </footer>
        </div>
    </div>
`;


const PdfExporter = {
    exportContext: null,
    documentLanguage: null,

    init: function () {
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
                document.querySelector("#pdf-export-popup .modal__content").innerHTML = content;
                document.querySelector("#pdf-export-popup .modal__footer .action-button").style.display = "inline-block";
            });
    },

    openPopup: function (params) {
        this.exportContext = params?.exportContext ? params.exportContext : new ExportContext({});

        this.hideAlerts();
        this.loadFormData();
        const documentContext = this.exportContext.getDocumentType() === ExportParams.DocumentType.LIVE_DOC;
        document.querySelectorAll(".modal__container.pdf-exporter .property-wrapper.only-live-doc")
            .forEach(propertyBlock => propertyBlock.style.display = (documentContext ? "flex" : "none"));
        const mixedContext = this.exportContext.getDocumentType() === ExportParams.DocumentType.MIXED;
        document.querySelectorAll(".modal__container.pdf-exporter .property-wrapper.not-mixed")
            .forEach(propertyBlock => propertyBlock.style.display = (mixedContext ? "none" : "flex"));
        MicroModal.show(POPUP_ID);
    },

    openPopupForBulkExport: function (bulkExportWidget) {
        PdfExporter.openPopup({ exportContext: new ExportContext(
            {
                documentType: ExportParams.DocumentType.MIXED,
                bulkExportWidget: bulkExportWidget
            })
        });
    },

    closePopup: function () {
        MicroModal.close(POPUP_ID);
    },

    loadFormData: function () {
        this.actionInProgress({inProgress: true, message: "Loading form data"});

        Promise.all([
            this.loadSettingNames({
                setting: "cover-page",
                scope: this.exportContext.getScope(),
                selectElement: document.getElementById("popup-cover-page-selector")
            }),
            this.loadSettingNames({
                setting: "css",
                scope: this.exportContext.getScope(),
                selectElement: document.getElementById("popup-css-selector")
            }),
            this.loadSettingNames({
                setting: "header-footer",
                scope: this.exportContext.getScope(),
                selectElement: document.getElementById("popup-header-footer-selector")
            }),
            this.loadSettingNames({
                setting: "localization",
                scope: this.exportContext.getScope(),
                selectElement: document.getElementById("popup-localization-selector")
            }),
            this.loadSettingNames({
                setting: "webhooks",
                scope: this.exportContext.getScope(),
                selectElement: document.getElementById("popup-webhooks-selector")
            }),
            this.adjustWebhooksVisibility(),
            this.loadLinkRoles(this.exportContext),
            this.loadDocumentLanguage(this.exportContext),
            this.loadFileName(this.exportContext),
        ]).then(() => {
            return this.loadStylePackages(this.exportContext);
        }).catch((error) => {
            this.showNotification({alertType: "error", message: "Error occurred loading form data" + (error.response.message ? ": " + error.response.message : "")});
            this.actionInProgress({inProgress: false});
        });
    },

    loadSettingNames: function ({setting, scope, selectElement, customUrl}) {
        return new Promise((resolve, reject) => {
            this.callAsync({
                method: "GET",
                url: customUrl ? customUrl : `/polarion/pdf-exporter/rest/internal/settings/${setting}/names?scope=${scope}`,
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
    },

    loadLinkRoles: function (exportContext) {
        if (exportContext.getDocumentType() === ExportParams.DocumentType.LIVE_REPORT
            || exportContext.getDocumentType() === ExportParams.DocumentType.TEST_RUN
            || exportContext.getDocumentType() === ExportParams.DocumentType.MIXED) {
            return Promise.resolve(); // Skip loading link roles for reports, test runs and bulk export
        }

        return new Promise((resolve, reject) => {
            this.callAsync({
                method: "GET",
                url: `/polarion/pdf-exporter/rest/internal/link-role-names?scope=${exportContext.getScope()}`,
                responseType: "json",
            }).then(({response}) => {
                const selectElement = document.getElementById("popup-roles-selector");
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
    },

    loadFileName: function (exportContext) {
        if (exportContext.getDocumentType() === ExportParams.DocumentType.MIXED) {
            return Promise.resolve(); // Skip loading file name for bulk export
        }

        const requestBody = exportContext.toExportParams().toJSON();

        return new Promise((resolve, reject) => {
            this.callAsync({
                method: "POST",
                url: `/polarion/pdf-exporter/rest/internal/export-filename`,
                body: requestBody,
            }).then(({responseText}) => {
                document.getElementById("popup-filename").value = responseText;
                document.getElementById("popup-filename").dataset.default = responseText;
                resolve()
            }).catch((error) => reject(error));
        });
    },

    adjustWebhooksVisibility: function () {
        return new Promise((resolve, reject) => {
            this.callAsync({
                method: "GET",
                url: `/polarion/pdf-exporter/rest/internal/webhooks/status`,
                responseType: "json",
            }).then(({response}) => {
                document.getElementById("webhooks-container").style.display = response.enabled ? "flex" : "none";
                resolve()
            }).catch((error) => reject(error));
        });
    },

    loadDocumentLanguage: function (exportContext) {
        if (exportContext.documentType === ExportParams.DocumentType.LIVE_REPORT
            || exportContext.documentType === ExportParams.DocumentType.TEST_RUN
            || exportContext.getDocumentType() === ExportParams.DocumentType.MIXED) {
            return Promise.resolve(); // Skip loading language for reports, test runs and bulk export
        }

        let url = `/polarion/pdf-exporter/rest/internal/document-language?projectId=${exportContext.getProjectId()}&spaceId=${exportContext.getSpaceId()}&documentName=${exportContext.getDocumentName()}`;
        if (exportContext.revision) {
            url += `&revision=${exportContext.revision}`;
        }
        return new Promise((resolve, reject) => {
            this.callAsync({
                method: "GET",
                url: url,
            }).then(({responseText}) => {
                PdfExporter.documentLanguage = responseText;
                resolve();
            }).catch((error) => reject(error));
        });
    },

    loadStylePackages: function (exportContext) {
        let stylePackagesUrl;
        if (exportContext.getDocumentType() === ExportParams.DocumentType.MIXED) {
            stylePackagesUrl = `/polarion/pdf-exporter/rest/internal/settings/style-package/names?scope=${exportContext.getScope()}`;
        } else {
            stylePackagesUrl = `/polarion/pdf-exporter/rest/internal/settings/style-package/suitable-names`
                + `?spaceId=${exportContext.getSpaceId()}&documentName=${exportContext.getDocumentName()}`;
            if (exportContext.getProjectId()) {
                stylePackagesUrl += `&projectId=${exportContext.getProjectId()}`
            }
        }

        return this.loadSettingNames({
            customUrl: stylePackagesUrl,
            selectElement: document.getElementById("popup-style-package-select")
        }).then(() => {
            const stylePackageSelect = document.getElementById("popup-style-package-select");
            const valueToPreselect = SbbCommon.getCookie(SELECTED_STYLE_PACKAGE_COOKIE);
            if (valueToPreselect && this.containsOption(stylePackageSelect, valueToPreselect)) {
                stylePackageSelect.value = valueToPreselect;
            }

            this.onStylePackageChanged();
            this.actionInProgress({inProgress: false});
        });
    },

    onStylePackageChanged: function () {
        const selectedStylePackageName = document.getElementById("popup-style-package-select").value;
        if (selectedStylePackageName) {
            SbbCommon.setCookie(SELECTED_STYLE_PACKAGE_COOKIE, selectedStylePackageName);

            this.actionInProgress({inProgress: true, message: "Loading style package data"});

            this.callAsync({
                method: "GET",
                url: `/polarion/pdf-exporter/rest/internal/settings/style-package/names/${selectedStylePackageName}/content?scope=${this.exportContext.getScope()}`,
                responseType: "json",
            }).then(({response}) => {
                this.stylePackageSelected(response);

                this.actionInProgress({inProgress: false});
            }).catch((error) => {
                this.showNotification({alertType: "error", message: "Error occurred loading style package data" + (error?.response.message ? ": " + error.response.message : "")});
                this.actionInProgress({inProgress: false});
            });
        }
    },

    stylePackageSelected: function (stylePackage) {
        if (!stylePackage) {
            return;
        }

        ExportCommon.setCheckbox("popup-cover-page-checkbox", stylePackage.coverPage);

        ExportCommon.setSelector("popup-cover-page-selector", stylePackage.coverPage);
        ExportCommon.visibleIf("popup-cover-page-selector", stylePackage.coverPage)

        ExportCommon.setSelector("popup-css-selector", stylePackage.css);
        ExportCommon.setSelector("popup-header-footer-selector", stylePackage.headerFooter);
        ExportCommon.setSelector("popup-localization-selector", stylePackage.localization);

        ExportCommon.setCheckbox("popup-webhooks-checkbox", !!stylePackage.webhooks);
        ExportCommon.setSelector("popup-webhooks-selector", stylePackage.webhooks);
        ExportCommon.visibleIf("popup-webhooks-selector", !!stylePackage.webhooks)

        ExportCommon.setValue("popup-headers-color", stylePackage.headersColor);
        ExportCommon.setValue("popup-paper-size-selector", stylePackage.paperSize || ExportParams.PaperSize.A4);
        ExportCommon.setValue("popup-orientation-selector", stylePackage.orientation || ExportParams.Orientation.PORTRAIT);
        ExportCommon.setCheckbox("popup-fit-to-page", stylePackage.fitToPage);
        ExportCommon.setCheckbox("popup-enable-comments-rendering", stylePackage.renderComments);
        ExportCommon.setCheckbox("popup-watermark", stylePackage.watermark);
        ExportCommon.setCheckbox("popup-mark-referenced-workitems", stylePackage.markReferencedWorkitems);
        ExportCommon.setCheckbox("popup-cut-urls", stylePackage.cutLocalURLs);
        ExportCommon.setCheckbox("popup-cut-empty-chapters", stylePackage.cutEmptyChapters);
        ExportCommon.setCheckbox("popup-cut-empty-wi-attributes", stylePackage.cutEmptyWorkitemAttributes);
        ExportCommon.setCheckbox("popup-presentational-hints", stylePackage.followHTMLPresentationalHints);

        ExportCommon.setCheckbox("popup-custom-list-styles", stylePackage.customNumberedListStyles);
        ExportCommon.setValue("popup-numbered-list-styles", stylePackage.customNumberedListStyles || "");
        ExportCommon.visibleIf("popup-numbered-list-styles", stylePackage.customNumberedListStyles);

        ExportCommon.setCheckbox("popup-specific-chapters", stylePackage.specificChapters);
        ExportCommon.setValue("popup-chapters", stylePackage.specificChapters || "");
        ExportCommon.visibleIf("popup-chapters", stylePackage.specificChapters);

        ExportCommon.setCheckbox("popup-localization", stylePackage.language);
        let languageValue;
        if (stylePackage.exposeSettings && stylePackage.language && this.documentLanguage) {
            languageValue = this.documentLanguage;
        } else if (stylePackage.language) {
            languageValue = stylePackage.language;
        } else {
            const firstOption = document.getElementById("popup-language").querySelector("option:first-child");
            languageValue = firstOption?.value;
        }
        ExportCommon.setValue("popup-language", languageValue);
        ExportCommon.visibleIf("popup-language", stylePackage.language);

        ExportCommon.setCheckbox("popup-selected-roles", stylePackage.linkedWorkitemRoles);
        document.querySelectorAll(`#popup-roles-selector option`).forEach(roleOption => {
            roleOption.selected = false;
        });
        if (stylePackage.linkedWorkitemRoles) {
            for (const role of stylePackage.linkedWorkitemRoles) {
                document.querySelectorAll(`#popup-roles-selector option[value='${role}']`).forEach(roleOption => {
                    roleOption.selected = true;
                });
            }
        }
        ExportCommon.displayIf("popup-roles-selector", this.exportContext.getDocumentType() !== ExportParams.DocumentType.MIXED && stylePackage.linkedWorkitemRoles, "inline-block");

        ExportCommon.displayIf("popup-style-package-content", stylePackage.exposeSettings);
        ExportCommon.displayIf("popup-page-width-validation", this.exportContext.getDocumentType() !== ExportParams.DocumentType.MIXED && stylePackage.exposePageWidthValidation);
    },

    validatePdf: function () {
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
    },

    createPreviews: function (result) {
        const pagePreviews = document.getElementById('page-previews');
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
    },

    addSuspiciousWiLinks: function (suspiciousWorkItems) {
        const suspiciousWiContainer = document.getElementById("suspicious-wi");
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
    },

    exportToPdf: function () {
        this.hideAlerts();

        let fileName = document.getElementById("popup-filename").value;
        if (!fileName) {
            fileName = document.getElementById("popup-filename").dataset.default;
        }
        if (fileName && !fileName.endsWith(".pdf")) {
            fileName += ".pdf";
        }

        const exportParams = this.getExportParams(fileName);
        if (exportParams === undefined) {
            return;
        }

        if (this.exportContext.getBulkExportWidget() && this.exportContext.getDocumentType() === ExportParams.DocumentType.MIXED) {
            this.closePopup();
            BulkPdfExporter.openPopup(this.exportContext.getBulkExportWidget(), exportParams);
            return;
        }

        this.actionInProgress({inProgress: true, message: "Generating PDF"})

        const requestBody = exportParams.toJSON();
        if (this.exportContext.getDocumentType() !== ExportParams.DocumentType.LIVE_REPORT && this.exportContext.getDocumentType() !== ExportParams.DocumentType.TEST_RUN) {
            this.checkNestedListsAsync(requestBody);
        }

        ExportCommon.asyncConvertPdf(requestBody, responseBody => {
            const objectURL = (window.URL ? window.URL : window.webkitURL).createObjectURL(responseBody);
            const anchorElement = document.createElement("a");
            anchorElement.href = objectURL;
            anchorElement.download = fileName;
            anchorElement.target = "_blank";
            anchorElement.click();
            anchorElement.remove();
            setTimeout(() => URL.revokeObjectURL(objectURL), 100);

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
    },

    checkNestedListsAsync: function (requestBody) {
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
    },

    getExportParams: function (fileName) {
        let selectedChapters = null;
        if (document.getElementById("popup-specific-chapters").checked) {
            selectedChapters = this.getSelectedChapters();
            if (!selectedChapters) {
                document.getElementById("popup-chapters").classList.add("error");
                this.showNotification({alertType: "error", message: "Please, provide comma separated list of integer values in 'Specific higher level chapters' field"});
                // Stop processing if not valid numbers
                return undefined;
            }
        }

        let numberedListStyles = null;
        if (document.getElementById("popup-custom-list-styles").checked) {
            numberedListStyles = document.getElementById("popup-numbered-list-styles").value;
            const error = this.validateNumberedListStyles(numberedListStyles);
            if (error) {
                document.getElementById("popup-numbered-list-styles").classList.add("error");
                this.showNotification({alertType: "error", message: error});
                return undefined;
            }
        }

        const selectedRoles = [];
        if (document.getElementById("popup-selected-roles").checked) {
            const selectedOptions = Array.from(document.getElementById("popup-roles-selector").options).filter(opt => opt.selected);
            selectedRoles.push(...selectedOptions.map(opt => opt.value));
        }

        return this.buildExportParams(selectedChapters, numberedListStyles, selectedRoles, fileName);
    },

    buildExportParams: function (selectedChapters, numberedListStyles, selectedRoles, fileName) {
        const live_doc = this.exportContext.getDocumentType() === ExportParams.DocumentType.LIVE_DOC;
        const test_run = this.exportContext.getDocumentType() === ExportParams.DocumentType.TEST_RUN;
        return new ExportParams.Builder(this.exportContext.getDocumentType())
            .setProjectId(this.exportContext.getProjectId())
            .setLocationPath(this.exportContext.getLocationPath())
            .setRevision(this.exportContext.getRevision())
            .setCoverPage(document.getElementById("popup-cover-page-checkbox").checked ? document.getElementById("popup-cover-page-selector").value : null)
            .setCss(document.getElementById("popup-css-selector").value)
            .setHeaderFooter(document.getElementById("popup-header-footer-selector").value)
            .setLocalization(document.getElementById("popup-localization-selector").value)
            .setWebhooks(document.getElementById("popup-webhooks-checkbox").checked ? document.getElementById("popup-webhooks-selector").value : null)
            .setHeadersColor(document.getElementById("popup-headers-color").value)
            .setPaperSize(document.getElementById("popup-paper-size-selector").value)
            .setOrientation(document.getElementById("popup-orientation-selector").value)
            .setFitToPage((live_doc || test_run) && document.getElementById('popup-fit-to-page').checked)
            .setEnableCommentsRendering(live_doc && document.getElementById('popup-enable-comments-rendering').checked)
            .setWatermark(document.getElementById("popup-watermark").checked)
            .setMarkReferencedWorkitems(live_doc && document.getElementById("popup-mark-referenced-workitems").checked)
            .setCutEmptyChapters(live_doc && document.getElementById("popup-cut-empty-chapters").checked)
            .setCutEmptyWIAttributes(live_doc && document.getElementById('popup-cut-empty-wi-attributes').checked)
            .setCutLocalUrls(document.getElementById("popup-cut-urls").checked)
            .setFollowHTMLPresentationalHints(document.getElementById("popup-presentational-hints").checked)
            .setNumberedListStyles(numberedListStyles)
            .setChapters(selectedChapters)
            .setLanguage(live_doc && document.getElementById('popup-localization').checked ? document.getElementById("popup-language").value : null)
            .setLinkedWorkitemRoles(selectedRoles)
            .setFileName(fileName)
            .setUrlQueryParameters(this.exportContext.getUrlQueryParameters())
            .build();
    },

    getSelectedChapters: function () {
        const chaptersValue = document.getElementById("popup-chapters").value;
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
    },

    validateNumberedListStyles: function (numberedListStyles) {
        if (!numberedListStyles || numberedListStyles.trim().length === 0) {
            // Stop processing if empty
            return "Please, provide some value";
        } else if (numberedListStyles.match("[^1aAiI]+")) {
            // Stop processing if not valid styles
            return "Please, provide any combination of characters '1aAiI'";

        }
        return undefined;
    },

    containsOption: function (selectElement, option) {
        return [...selectElement.options].map(o => o.value).includes(option);
    },

    actionInProgress: function ({inProgress, message}) {
        if (inProgress) {
            this.hideAlerts();
        }
        document.querySelectorAll(".modal__container.pdf-exporter .action-button").forEach(button => {
            button.disabled = inProgress;
        });
        document.getElementById("in-progress-message").innerHTML = message;
        if (inProgress) {
            document.querySelector(".modal__container.pdf-exporter .in-progress-overlay").classList.add("show");
        } else {
            document.querySelector(".modal__container.pdf-exporter .in-progress-overlay").classList.remove("show");
        }
    },

    showNotification: function ({alertType, message}) {
        const alert = document.querySelector(`.modal__container.pdf-exporter .notifications .alert.alert-${alertType}`);
        if (alert) {
            alert.textContent = message; // to avoid XSS do not use innerHTML here because message may contain arbitrary error response data
            alert.style.display = "block";
        }
    },

    showValidationResult: function ({alertType, message}) {
        const alert = document.querySelector(`.modal__container.pdf-exporter .validation-alerts .alert.alert-${alertType}`);
        if (alert) {
            alert.innerHTML = message;
            alert.style.display = "block";
        }
    },

    hideAlerts: function () {
        document.querySelectorAll(".modal__container.pdf-exporter .alert").forEach(alert => {
            alert.style.display = "none";
        });
        document.querySelectorAll(".modal__container.pdf-exporter input.error").forEach(input => {
            input.classList.remove("error");
        });
        if (document.getElementById('page-previews')) {
            document.getElementById('page-previews').innerHTML = "";
        }
        if (document.getElementById("suspicious-wi")) {
            document.getElementById("suspicious-wi").innerHTML = "";
        }
    },

    callAsync: function ({method, url, contentType, responseType, body}) {
        return new Promise((resolve, reject) => {
            SbbCommon.callAsync({
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
    },
}

PdfExporter.init();

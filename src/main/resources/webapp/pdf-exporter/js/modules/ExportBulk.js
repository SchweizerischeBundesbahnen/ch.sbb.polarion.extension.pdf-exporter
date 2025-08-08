import ExportParams from "./ExportParams.js";
import ExportContext from "./ExportContext.js";
import ExportPopup from "./ExportPopup.js";
import('./../micromodal.min.js');

export default class ExportBulk {
    ctx = null;
    widgetDocumentType = null;
    popupCtx = null;
    exportParams = null;
    itemsCount = 0;
    finishedCount = 0;
    state = null;
    errors = false;

    constructor(rootComponentSelector) {
        this.ctx = new ExportContext({rootComponentSelector: rootComponentSelector});
        this.widgetDocumentType = this.ctx.querySelector(".header")?.getAttribute("document-type");
        if (!this.widgetDocumentType) {
            throw new Error("unable to get documentType from the header");
        }
        this.ctx.onClick(
            'export-all', () => {
                this.selectAllItems()
            },
            'bulk-export-pdf', () => {
                if (this.getExportButton().classList.contains(DISABLED_BUTTON_CLASS)) {
                    return;
                }
                new ExportPopup({
                    documentType: this.widgetDocumentType,
                    bulkCallback: this
                });
            });
        this.getAllDocumentCheckboxes().forEach((checkbox) => {
            checkbox.addEventListener('click', () => {
                this.validateActiveComponentsState();
            })
        })
    }

    validateActiveComponentsState() {
        let documentCheckboxes = this.getAllDocumentCheckboxes();
        const button = this.getExportButton();
        if ([...documentCheckboxes].some(c => c.checked)) {
            button.classList.remove(DISABLED_BUTTON_CLASS);
        } else if (!button.classList.contains(DISABLED_BUTTON_CLASS)) {
            button.classList.add(DISABLED_BUTTON_CLASS);
        }
        this.ctx.setCheckboxValueById('export-all', documentCheckboxes.length > 0 && [...documentCheckboxes].every(c => c.checked));
    }

    selectAllItems() {
        const checked = this.ctx.getCheckboxValueById('export-all');
        this.getAllDocumentCheckboxes().forEach(checkbox => {
            checkbox.checked = checked;
        });
        this.validateActiveComponentsState();
    }

    getExportButton() {
        return this.ctx.querySelector(".polarion-TestsExecutionButton-buttons");
    }

    getAllDocumentCheckboxes() {
        return this.ctx.querySelectorAll('input[type="checkbox"]:not(#export-all)');
    }

    getDocIdentifiers() {
        const docIdentifiers = [];
        this.ctx.querySelectorAll('input[type="checkbox"]:not(#export-all):checked').forEach((selectedCheckbox) => {
            const docIdentifier = {
                ...(selectedCheckbox.dataset["project"] ? { projectId: selectedCheckbox.dataset["project"] } : {}),
                ...(selectedCheckbox.dataset["space"] ? { spaceId: selectedCheckbox.dataset["space"] } : {}),
                documentName: selectedCheckbox.dataset["id"]
            };
            docIdentifiers.push(docIdentifier);
        });
        return docIdentifiers;
    }

    openPopup(exportParams) {
        this.removePopupIfExists();
        const popup = document.createElement('div');
        popup.classList.add("modal");
        popup.classList.add("micromodal-slide");
        popup.id = BULK_POPUP_ID;
        popup.setAttribute("aria-hidden", "true");
        popup.innerHTML = BULK_POPUP_HTML;
        document.body.appendChild(popup);

        this.popupCtx = new ExportContext({rootComponentSelector: "#" + BULK_POPUP_ID});
        this.exportParams = exportParams;
        this.itemsCount = 0;
        this.finishedCount = 0;
        this.errors = false;
        this.updateState(BULK_EXPORT_IN_PROGRESS);
        this.renderBulkExportItems();

        MicroModal.show(BULK_POPUP_ID, {
            onClose: () => {
                // remove popup after usage otherwise it leads to extra UI artifacts creation after page edit
                this.removePopupIfExists();
            }
        });

        this.popupCtx.onClick('bulk-stop-export-pdf', () => {
            this.stopBulkExport();
        });

        this.startNextItemExport();
    }

    removePopupIfExists() {
        document.getElementById(BULK_POPUP_ID)?.remove();
    }

    renderBulkExportItems() {
        const modalContent = this.popupCtx.querySelector(".modal__content");
        modalContent.innerHTML = "";
        this.ctx.querySelectorAll('input[type="checkbox"]:not(#export-all):checked').forEach((selectedCheckbox) => {
            this.itemsCount += 1;

            const div = document.createElement("div");
            div.className = "export-item paused";
            div.dataset["type"] = selectedCheckbox.dataset["type"];
            if (selectedCheckbox.dataset["project"]) {
                div.dataset["project"] = selectedCheckbox.dataset["project"];
            }
            if (selectedCheckbox.dataset["space"]) {
                div.dataset["space"] = selectedCheckbox.dataset["space"];
            }
            div.dataset["id"] = selectedCheckbox.dataset["id"];

            const iconSpan = document.createElement("span");
            iconSpan.className = "icon";
            const fontAwesomeIcon = document.createElement("i");
            fontAwesomeIcon.className = "fa";
            iconSpan.appendChild(fontAwesomeIcon);
            const inProgressIcon = document.createElement("img");
            inProgressIcon.src = '/polarion/ria/images/progress.gif';
            iconSpan.appendChild(inProgressIcon);
            div.appendChild(iconSpan);

            const titleSpan = document.createElement("span");
            titleSpan.className = "title";

            const typeSpan = document.createElement("span");
            typeSpan.className = "type";
            typeSpan.innerText = this.getItemType(selectedCheckbox);
            titleSpan.appendChild(typeSpan);

            const nameSpan = document.createElement("span");
            nameSpan.className = "name";
            nameSpan.innerText = this.getDocumentType(selectedCheckbox.dataset["type"]) === ExportParams.DocumentType.BASELINE_COLLECTION ? selectedCheckbox.dataset["name"] : this.getSpace(selectedCheckbox) + selectedCheckbox.dataset["id"];
            titleSpan.appendChild(nameSpan);

            div.appendChild(titleSpan);

            modalContent.appendChild(div);
        });
        this.updateState(BULK_EXPORT_IN_PROGRESS);
    }

    getItemType(selectedCheckbox) {
        switch (selectedCheckbox.dataset["type"]) {
            case "Module": return "Document: ";
            case "RichPage": return "Report: ";
            case "TestRun": return "Test Run: ";
            case "BaselineCollection": return "Collection: ";
            default: return "";
        }
    }

    getSpace(selectedCheckbox) {
        if (!selectedCheckbox.dataset["space"] || selectedCheckbox.dataset["space"] === "_default") {
            return "";
        } else {
            return selectedCheckbox.dataset["space"] + " / ";
        }
    }

    stopBulkExport() {
        this.updateState(BULK_EXPORT_INTERRUPTED);
    }

    updateState(state) {
        this.state = state;

        const resultSpan = this.popupCtx.querySelector(".modal__footer .result");
        const progressBar = this.popupCtx.querySelector(".modal__footer .progress-bar");
        if (this.state === BULK_EXPORT_IN_PROGRESS) {
            this.popupCtx.querySelector(".polarion-JSWizardButton-Primary").style.display = "block";
            this.popupCtx.querySelector(".polarion-JSWizardButton").style.display = "none";
            resultSpan.style.display = "none";
            resultSpan.classList.remove("interrupted");
            resultSpan.classList.remove("finished");
            this.updateProgress(progressBar);
        } else {
            this.popupCtx.querySelector(".polarion-JSWizardButton-Primary").style.display = "none";
            this.popupCtx.querySelector(".polarion-JSWizardButton").style.display = "block";
            progressBar.style.display = "none";
            resultSpan.style.display = "block";
            if (this.state === BULK_EXPORT_INTERRUPTED) {
                this.popupCtx.querySelectorAll(".export-item.paused").forEach(item => {
                    item.classList.remove("paused");
                    item.classList.add("interrupted");
                });

                resultSpan.classList.add("interrupted");
                resultSpan.innerText = "Export interrupted by user";
            } else if (this.state === BULK_EXPORT_FINISHED) {
                resultSpan.classList.add("finished");
                if (this.errors) {
                    resultSpan.classList.add("with-errors");
                } else {
                    resultSpan.classList.remove("with-errors");
                }
                resultSpan.innerText = this.errors ? "Export finished with errors" : "Export successfully finished";
            }
        }
    }

    updateProgress(progressBar) {
        progressBar.style.display = this.itemsCount > 1 ? "block" : "none";
        const progressBarSpan = progressBar.querySelector("span");
        const progress = Math.round(this.finishedCount / this.itemsCount * 100);
        if (progress > 25) {
            progressBarSpan.innerText = `${this.finishedCount} out of ${this.itemsCount} finished`;
        } else {
            progressBarSpan.innerText = "";
        }
        progressBarSpan.style.width = progress + "%";
    }

    startNextItemExport() {
        const pausedItems = this.popupCtx.querySelectorAll(".export-item.paused");
        if (this.exportParams && pausedItems && pausedItems.length > 0) {
            const currentItem = pausedItems[0];
            currentItem.classList.remove("paused");
            currentItem.classList.add("in-progress");

            const documentType = this.getDocumentType(currentItem.dataset["type"]);
            this.exportParams["projectId"] = `${currentItem.dataset["project"]}`;
            this.exportParams["documentType"] = documentType;
            const documentId = currentItem.dataset["id"];
            if (documentType === ExportParams.DocumentType.TEST_RUN) {
                this.exportParams["urlQueryParameters"] = {id: documentId};
                if (this.exportParams.attachmentsFilter !== null) {
                    this.ctx.downloadTestRunAttachments(this.exportParams.projectId, documentId, this.exportParams.revision, this.exportParams.attachmentsFilter);
                }
            } else if (documentType === ExportParams.DocumentType.BASELINE_COLLECTION) {
                this.ctx.convertCollectionDocuments(this.exportParams, documentId, () => {
                        currentItem.classList.remove("in-progress");
                        currentItem.classList.add("finished");
                        this.finishedCount += 1;
                        if (this.state !== BULK_EXPORT_INTERRUPTED) {
                            this.updateState(BULK_EXPORT_IN_PROGRESS);
                        }
                        this.startNextItemExport();
                    },
                    (error) => {
                        this.errors = true;
                        currentItem.classList.remove("in-progress");
                        currentItem.classList.add("error");
                        error.text().then(errorJson => {
                            const error = errorJson && JSON.parse(errorJson);
                            const errorMessage = error && (error.message ? error.message : error.errorMessage);
                            const errorDiv = document.createElement("div");
                            errorDiv.className = "error-message";
                            errorDiv.innerText = errorMessage;
                            currentItem.appendChild(errorDiv);
                        });
                        this.startNextItemExport();
                    });
            } else {
                this.exportParams["locationPath"] = `${currentItem.dataset["space"]}/${documentId}`;
            }

            if (documentType !== ExportParams.DocumentType.BASELINE_COLLECTION) {
                this.ctx.asyncConvertPdf(this.exportParams.toJSON(), (result, fileName) => {
                    currentItem.classList.remove("in-progress");
                    currentItem.classList.add("finished");

                    this.finishedCount += 1;
                    this.updateState(BULK_EXPORT_IN_PROGRESS);
                    const downloadFileName = fileName || `${currentItem.dataset["space"] ? currentItem.dataset["space"] + "_" : ""}${documentId}.pdf`; // Fallback if file name wasn't received in response
                    this.ctx.downloadBlob(result.response, downloadFileName);
                    this.startNextItemExport();
                }, errorResponse => {
                    this.errors = true;
                    currentItem.classList.remove("in-progress");
                    currentItem.classList.add("error");

                    errorResponse.text().then(errorJson => {
                        const error = errorJson && JSON.parse(errorJson);
                        const errorMessage = error && (error.message ? error.message : error.errorMessage);
                        const errorDiv = document.createElement("div");
                        errorDiv.className = "error-message";
                        errorDiv.innerText = errorMessage;
                        currentItem.appendChild(errorDiv);
                    });
                    this.startNextItemExport();
                });
            }
        } else if (this.state !== BULK_EXPORT_INTERRUPTED) {
            this.updateState(BULK_EXPORT_FINISHED);
        }
    }

    getDocumentType(itemType) {
        switch (itemType) {
            case "Module": return ExportParams.DocumentType.LIVE_DOC;
            case "RichPage": return ExportParams.DocumentType.LIVE_REPORT;
            case "TestRun": return ExportParams.DocumentType.TEST_RUN;
            case "BaselineCollection": return ExportParams.DocumentType.BASELINE_COLLECTION;
            default: return "";
        }
    }
}

const BULK_POPUP_ID = 'bulk-pdf-export-modal-popup';
const BULK_POPUP_HTML = `
    <div class="modal__overlay" tabindex="-1">
        <div id="bulk-pdf-export-popup" class="modal__container pdf-exporter" role="dialog" aria-modal="true" aria-labelledby="bulk-pdf-export-modal-popup-title">
            <header class="modal__header">
                <h2 class="modal__title" id="bulk-pdf-export-modal-popup-title" style="display: flex; justify-content: space-between; width: 100%">
                    <span>Bulk export to PDF</span>
                </h2>
            </header>
            <main class="modal__content">

            </main>
            <footer class="modal__footer">
                <div class="progress-bar" style="display: none">
                    <span></span>
                </div>
                <span class="result" style="display: none"></span>
                <button id="bulk-stop-export-pdf" class="polarion-JSWizardButton-Primary action-button">Stop</button>
                <button class="polarion-JSWizardButton" data-micromodal-close aria-label="Close this dialog window" style="display: none">Close</button>
            </footer>
        </div>
    </div>
`;

const DISABLED_BUTTON_CLASS = "polarion-TestsExecutionButton-buttons-defaultCursor";
const BULK_EXPORT_IN_PROGRESS = "IN_PROGRESS";
const BULK_EXPORT_INTERRUPTED = "INTERRUPTED";
const BULK_EXPORT_FINISHED = "FINISHED";

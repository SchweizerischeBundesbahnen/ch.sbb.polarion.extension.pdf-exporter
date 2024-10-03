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
                <button class="polarion-JSWizardButton-Primary action-button" onclick="BulkPdfExporter.stopBulkExport();">Stop</button>
                <button class="polarion-JSWizardButton" data-micromodal-close aria-label="Close this dialog window" style="display: none">Close</button>
            </footer>
        </div>
    </div>
`;
const DISABLED_BUTTON_CLASS= "polarion-TestsExecutionButton-buttons-defaultCursor";
const BULK_EXPORT_IN_PROGRESS = "IN_PROGRESS";
const BULK_EXPORT_INTERRUPTED = "INTERRUPTED";
const BULK_EXPORT_FINISHED = "FINISHED";

const BulkPdfExporter = {
    exportParams: null,
    itemsCount: 0,
    finishedCount: 0,
    state: null,
    registeredButtonClickListener: null,
    errors: false,

    init: function () {
        const popup = document.createElement('div');
        popup.classList.add("modal");
        popup.classList.add("micromodal-slide");
        popup.id = BULK_POPUP_ID;
        popup.setAttribute("aria-hidden", "true");
        popup.innerHTML = BULK_POPUP_HTML;
        document.body.appendChild(popup);
    },

    openPopup: function (bulkExportWidget, exportParams) {
        this.exportParams = exportParams;
        this.itemsCount = 0;
        this.finishedCount = 0;
        this.updateState(BULK_EXPORT_IN_PROGRESS);
        this.renderBulkExportItems(bulkExportWidget);
        MicroModal.show(BULK_POPUP_ID);
        this.startNextItemExport();
    },

    renderBulkExportItems: function(bulkExportWidget) {
        if (bulkExportWidget) {
            const modalContent = document.querySelector("#bulk-pdf-export-popup .modal__content");
            modalContent.innerHTML = "";
            bulkExportWidget.querySelectorAll('input[type="checkbox"]:not(.export-all):checked').forEach((selectedCheckbox) => {
                BulkPdfExporter.itemsCount += 1;

                const div = document.createElement("div");
                div.className = "export-item paused";
                div.dataset["type"] = selectedCheckbox.dataset["type"];
                div.dataset["space"] = selectedCheckbox.dataset["space"];
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
                nameSpan.innerText = this.getSpace(selectedCheckbox) + selectedCheckbox.dataset["id"];
                titleSpan.appendChild(nameSpan);

                div.appendChild(titleSpan);

                modalContent.appendChild(div);
            });
            this.updateState(BULK_EXPORT_IN_PROGRESS);
        }
    },

    getItemType: function (selectedCheckbox) {
        switch (selectedCheckbox.dataset["type"]) {
            case "Module": return "Document: ";
            case "RichPage": return "Report: ";
            case "TestRun": return "Test Run: ";
            default: return "";
        }
    },

    getSpace: function (selectedCheckbox) {
        if (!selectedCheckbox.dataset["space"] || selectedCheckbox.dataset["space"] === "_default") {
            return "";
        } else {
            return selectedCheckbox.dataset["space"] + " / ";
        }
    },

    updateBulkExportButton: function (clickedElement) {
        const bulkExportWidget = clickedElement && clickedElement.closest("div.polarion-PdfExporter-BulkExportWidget");
        const button = bulkExportWidget && bulkExportWidget.querySelector(".polarion-TestsExecutionButton-buttons");
        if (button) {
            if (button.classList.contains(DISABLED_BUTTON_CLASS) && bulkExportWidget.querySelectorAll('input[type="checkbox"]:checked').length > 0) {
                button.classList.remove(DISABLED_BUTTON_CLASS);
                // We need to remember click listener to remove it from button later
                this.registeredButtonClickListener = () => PdfExporter.openPopupForBulkExport(bulkExportWidget);
                button.addEventListener("click", this.registeredButtonClickListener);
            }
            if (!button.classList.contains(DISABLED_BUTTON_CLASS) && bulkExportWidget.querySelectorAll('input[type="checkbox"]:checked').length === 0) {
                button.classList.add(DISABLED_BUTTON_CLASS);
                button.removeEventListener("click", this.registeredButtonClickListener);
            }
            if (bulkExportWidget.querySelectorAll('input[type="checkbox"]:not(.export-all):not(:checked)').length > 0) {
                const exportAllCheckbox = bulkExportWidget.querySelector('input[type="checkbox"].export-all');
                if (exportAllCheckbox) {
                    exportAllCheckbox.checked = false;
                }
            }
        }
    },

    selectAllItems: function (clickedElement) {
        const bulkExportWidget = clickedElement && clickedElement.closest("div.polarion-PdfExporter-BulkExportWidget");
        if (bulkExportWidget) {
            bulkExportWidget.querySelectorAll('input[type="checkbox"]:not(.export-all)').forEach(checkbox => {
                checkbox.checked = clickedElement.checked;
            });
        }
    },

    stopBulkExport: function () {
        this.updateState(BULK_EXPORT_INTERRUPTED);
    },

    updateState: function (state) {
        this.state = state;

        const popup = document.getElementById("bulk-pdf-export-popup");
        const resultSpan = popup.querySelector(".modal__footer .result");
        const progressBar = popup.querySelector(".modal__footer .progress-bar");
        if (this.state === BULK_EXPORT_IN_PROGRESS) {
            popup.querySelector(".polarion-JSWizardButton-Primary").style.display = "block";
            popup.querySelector(".polarion-JSWizardButton").style.display = "none";
            resultSpan.style.display = "none";
            progressBar.style.display = this.itemsCount > 1 ? "block" : "none";
            const progressBarSpan = progressBar.querySelector("span");
            const progress = Math.round(BulkPdfExporter.finishedCount / BulkPdfExporter.itemsCount * 100);
            if (progress > 25) {
                progressBarSpan.innerText = `${this.finishedCount} out of ${this.itemsCount} finished`;
            } else {
                progressBarSpan.innerText = "";
            }
            progressBarSpan.style.width = progress + "%";
        } else {
            popup.querySelector(".polarion-JSWizardButton-Primary").style.display = "none";
            popup.querySelector(".polarion-JSWizardButton").style.display = "block";
            progressBar.style.display = "none";
            resultSpan.style.display = "block";
            if (this.state === BULK_EXPORT_INTERRUPTED) {
                document.querySelectorAll("#bulk-pdf-export-popup .export-item.paused").forEach(item => {
                    item.classList.remove("paused");
                    item.classList.add("interrupted");
                });

                resultSpan.classList.add("interrupted");
                resultSpan.innerText = "Export interrupted by user";
            } else if (this.state === BULK_EXPORT_FINISHED) {
                resultSpan.classList.add("finished");
                if (this.errors) {
                    resultSpan.classList.add("with-errors");
                }
                resultSpan.innerText = this.errors ? "Export finished with errors" : "Export successfully finished";
            }
        }
    },

    startNextItemExport: function () {
        const pausedItems = document.querySelectorAll("#bulk-pdf-export-popup .export-item.paused");
        if (this.exportParams && pausedItems && pausedItems.length > 0) {
            const nextItem = pausedItems[0];
            nextItem.classList.remove("paused");
            nextItem.classList.add("in-progress");

            const documentType = this.getDocumentType(nextItem.dataset["type"]);
            this.exportParams["documentType"] = documentType;
            if (documentType === ExportParams.DocumentType.TEST_RUN) {
                this.exportParams["urlQueryParameters"] = { id: nextItem.dataset["id"] };
            } else {
                this.exportParams["locationPath"] = `${nextItem.dataset["space"]}/${nextItem.dataset["id"]}`;
            }

            ExportCommon.asyncConvertPdf(this.exportParams.toJSON(), (responseBody, fileName) => {
                nextItem.classList.remove("in-progress");
                nextItem.classList.add("finished");

                BulkPdfExporter.finishedCount += 1;
                BulkPdfExporter.updateState(BULK_EXPORT_IN_PROGRESS);

                const objectURL = (window.URL ? window.URL : window.webkitURL).createObjectURL(responseBody);
                const anchorElement = document.createElement("a");
                anchorElement.href = objectURL;
                anchorElement.download = fileName || `${nextItem.dataset["space"] ? nextItem.dataset["space"] + "_" : ""}${nextItem.dataset["id"]}.pdf`; // Fallback if file name wasn't received in response
                anchorElement.target = "_blank";
                anchorElement.click();
                anchorElement.remove();
                setTimeout(() => URL.revokeObjectURL(objectURL), 100);
                this.startNextItemExport();
            }, errorResponse => {
                this.errors = true;
                nextItem.classList.remove("in-progress");
                nextItem.classList.add("error");

                errorResponse.text().then(errorJson => {
                    const error = errorJson && JSON.parse(errorJson);
                    const errorMessage = error && (error.message ? error.message : error.errorMessage);
                    const errorDiv = document.createElement("div");
                    errorDiv.className = "error-message";
                    errorDiv.innerText = errorMessage;
                    nextItem.appendChild(errorDiv);
                });
                this.startNextItemExport();
            });
        } else {
            if (this.state !== BULK_EXPORT_INTERRUPTED) {
                this.updateState(BULK_EXPORT_FINISHED);
            }
        }
    },

    getDocumentType: function (itemType) {
        switch (itemType) {
            case "Module": return ExportParams.DocumentType.LIVE_DOC;
            case "RichPage": return ExportParams.DocumentType.LIVE_REPORT;
            case "TestRun": return ExportParams.DocumentType.TEST_RUN;
            default: return "";
        }
    }
}

BulkPdfExporter.init();
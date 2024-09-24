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
                <button class="polarion-JSWizardButton-Primary action-button" onclick="BulkPdfExporter.stopBulkExport();">Stop</button>
                <button class="polarion-JSWizardButton" data-micromodal-close aria-label="Close this dialog window" style="display: none">Close</button>
            </footer>
        </div>
    </div>
`;
const BULK_EXPORT_WIDGET_ID = "polarion-PdfExporter-BulkExportWidget";
const DISABLED_BUTTON_CLASS= "polarion-TestsExecutionButton-buttons-defaultCursor";
const IN_PROGRESS = "IN_PROGRESS";
const INTERRUPTED = "INTERRUPTED";
const FINISHED = "FINISHED";

const BulkPdfExporter = {
    exportParams: null,

    init: function () {
        document.body.appendChild(ExportCommon.buildMicromodal(BULK_POPUP_ID, BULK_POPUP_HTML));
    },

    openPopup: function (exportParams) {
        this.exportParams = exportParams;
        this.updateState(IN_PROGRESS);
        this.renderBulkExportItems();
        MicroModal.show(BULK_POPUP_ID);
        this.startNextItemExport();
    },

    renderBulkExportItems: function() {
        const bulkExportWidget = document.getElementById(BULK_EXPORT_WIDGET_ID);
        if (bulkExportWidget) {
            const modalContent = document.querySelector("#bulk-pdf-export-popup .modal__content");
            modalContent.innerHTML = "";
            bulkExportWidget.querySelectorAll('input[type="checkbox"]:checked').forEach((selectedCheckbox) => {
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

    updateBulkExportButton: function () {
        const bulkExportWidget = document.getElementById(BULK_EXPORT_WIDGET_ID);
        const button = bulkExportWidget && bulkExportWidget.querySelector(".polarion-TestsExecutionButton-buttons");
        if (button) {
            if (bulkExportWidget.querySelectorAll('input[type="checkbox"]:checked').length > 0) {
                button.classList.remove(DISABLED_BUTTON_CLASS);
                button.addEventListener("click", PdfExporter.openPopupForBulkExport);
            } else {
                button.classList.add(DISABLED_BUTTON_CLASS);
                button.removeEventListener("click", PdfExporter.openPopupForBulkExport);
            }
        }
    },

    stopBulkExport: function () {
        this.updateState(INTERRUPTED);
    },

    updateState: function (state) {
        if (state === IN_PROGRESS) {
            document.querySelector("#bulk-pdf-export-popup .polarion-JSWizardButton-Primary").style.display = "block";
            document.querySelector("#bulk-pdf-export-popup .polarion-JSWizardButton").style.display = "none";
        } else {
            document.querySelector("#bulk-pdf-export-popup .polarion-JSWizardButton-Primary").style.display = "none";
            document.querySelector("#bulk-pdf-export-popup .polarion-JSWizardButton").style.display = "block";
        }
    },

    startNextItemExport: function () {
        const pausedItems = document.querySelectorAll("#bulk-pdf-export-popup .export-item.paused");
        if (this.exportParams && pausedItems && pausedItems.length > 0) {
            const nextItem = pausedItems[0];
            nextItem.classList.remove("paused");
            nextItem.classList.add("in-progress");

            this.exportParams["documentType"] = this.getDocumentType(nextItem.dataset["type"]);
            this.exportParams["locationPath"] = `${nextItem.dataset["space"]}/${nextItem.dataset["id"]}`;

            ExportCommon.asyncConvertPdf(this.exportParams.toJSON(), (responseBody, fileName) => {
                console.log(fileName);
                nextItem.classList.remove("in-progress");
                nextItem.classList.add("finished");

                const objectURL = (window.URL ? window.URL : window.webkitURL).createObjectURL(responseBody);
                const anchorElement = document.createElement("a");
                anchorElement.href = objectURL;
                anchorElement.download = fileName || `${nextItem.dataset["space"]}_${nextItem.dataset["id"]}.pdf`; // Fallback if file name wasn't received in response
                anchorElement.target = "_blank";
                anchorElement.click();
                anchorElement.remove();
                setTimeout(() => URL.revokeObjectURL(objectURL), 100);
                this.startNextItemExport();
                // this.showNotification({alertType: "success", message: "PDF was successfully generated"});
            }, errorResponse => {
                // errorResponse.text().then(errorJson => {
                //     const error = errorJson && JSON.parse(errorJson);
                //     const errorMessage = error && (error.message ? error.message : error.errorMessage);
                //     this.showNotification({alertType: "error", message: "Error occurred during PDF generation" + (errorMessage ? ": " + errorMessage : "")});
                // });
            });
        } else {
            this.updateState(FINISHED);
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
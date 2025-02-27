import ExportContext from "./ExportContext.js";

export default class ExportBulk {

    constructor(rootComponentSelector) {
        this.ctx = new ExportContext({rootComponentSelector: rootComponentSelector});
        console.log('INIT!!!!');
        this.ctx.querySelector('.export-all').addEventListener("onclick", () => {
            this.selectAllItems()
        });

        this.popup = document.createElement('div');
        this.popup.classList.add("modal");
        this.popup.classList.add("micromodal-slide");
        this.popup.id = BULK_POPUP_ID;
        this.popup.setAttribute("aria-hidden", "true");
        this.popup.innerHTML = BULK_POPUP_HTML;
        document.body.appendChild(this.popup);

        this.ctx.querySelector('.polarion-rpw-table-main').addEventListener("onclick", () => {
            this.updateBulkExportButton();
        })
    }

    updateBulkExportButton(clickedElement) {
        const bulkExportWidget = clickedElement?.closest("div.polarion-PdfExporter-BulkExportWidget");
        const button = bulkExportWidget?.querySelector(".polarion-TestsExecutionButton-buttons");
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
    }

    selectAllItems() {
        const checked = this.ctx.getCheckboxValueById('export-all');
        this.ctx.querySelectorAll('input[type="checkbox"]:not(.export-all)').forEach(checkbox => {
            checkbox.checked = checked;
        });
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
                <button class="polarion-JSWizardButton-Primary action-button" onclick="BulkPdfExporter.stopBulkExport();">Stop</button>
                <button class="polarion-JSWizardButton" data-micromodal-close aria-label="Close this dialog window" style="display: none">Close</button>
            </footer>
        </div>
    </div>
`;

const DISABLED_BUTTON_CLASS = "polarion-TestsExecutionButton-buttons-defaultCursor";
const BULK_EXPORT_IN_PROGRESS = "IN_PROGRESS";
const BULK_EXPORT_INTERRUPTED = "INTERRUPTED";
const BULK_EXPORT_FINISHED = "FINISHED";

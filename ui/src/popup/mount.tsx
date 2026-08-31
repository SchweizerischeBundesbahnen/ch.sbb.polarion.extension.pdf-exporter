import { createRoot } from 'react-dom/client';
import type { Root } from 'react-dom/client';
import type { DocumentType, ExportType } from '../export/documentType';
import formStyle from '../export/export-form.css?inline';
import type { DocIdentifier } from '../export/exportData';
import type { DocumentIdentity } from '../services/exportContext';
import { currentDocumentLocation, toDocumentIdentity } from '../services/exportContext';
import { mountInShadow } from '../services/shadowMount';
import ExportPopupModal from './ExportPopupModal';
import type { BulkExportStarter, ExportPopupDependencies } from './ExportPopupModal';
import popupStyle from './export-popup.css?inline';

/**
 * Entry point for the "Export to PDF" dialog, built by Vite into a fixed-name module
 * (`assets/export-popup.js`; the Vite input key `export-popup` sets the output name).
 *
 * Three server-side surfaces import this module and call {@link openExportPopup} on click:
 * `webapp/pdf-exporter/js/starter.js` (the document editor toolbar button),
 * `webapp/pdf-exporter/js/live-reports.js` (the Live Report toolbar button) and
 * `ExportToPdfButtonRenderer` (the "Export to PDF Button" report widget). The fourth caller, the Bulk PDF
 * Export widget, is part of this app and renders `ExportPopupModal` directly instead - it has a React tree
 * of its own to render the dialog into, and a progress dialog to hand the parameters to.
 *
 * The dialog is mounted inside a **shadow root** on a throwaway host appended to the page body, so its
 * styles are fully encapsulated on whatever page opened it: RSP's stylesheet, the shared
 * `export/export-form.css` and the dialog's own `export-popup.css` are injected into that root, and the SearchableDropdown popup portals into the same root. Closing unmounts
 * React and removes the host. That is what replaced the page-level micromodal library and the six generic
 * control stylesheets the legacy popup needed injected before it could be opened.
 */
export interface OpenExportPopupOptions {
  /** What the surface that opened the dialog assumes it is exporting. A test run hash overrides it. */
  documentType?: DocumentType;
  /** `BULK` collects the parameters for a selection instead of exporting; defaults to one item. */
  exportType?: ExportType;
  /** The items a bulk export was started for. Only read for `exportType: 'BULK'`. */
  identifiers?: DocIdentifier[];
  /** Where a bulk export goes once the parameters are in. Only read for `exportType: 'BULK'`. */
  onBulkExport?: BulkExportStarter;
  /** Where the item is. Read from the page URL when not given, which is what happens in Polarion. */
  location?: DocumentIdentity;
  /**
   * What the dialog reaches outside itself for. Nothing in Polarion passes this - the toolbar buttons want
   * the real endpoints - but the visual references need a dialog that reads no network, the way
   * `mountSidePanel` takes the panel's dependencies for the same reason.
   */
  deps?: ExportPopupDependencies;
}

/**
 * Closes whatever dialog a previous {@link openExportPopup} call left open, if any. Without this, a second
 * click on a toolbar button (or any other trigger) while the first dialog is still open would mount a
 * second, independently submittable dialog on top of it - the toolbar buttons carry no disabled state of
 * their own while a dialog is open.
 */
let closeOpenPopup: (() => void) | null = null;

/** Opens the dialog. Returns the React root so the dev harness and the tests can unmount it. */
export function openExportPopup(options: OpenExportPopupOptions = {}): Root {
  closeOpenPopup?.();

  const host = document.createElement('div');
  document.body.appendChild(host);
  const container = mountInShadow(host, {
    // `pdf-exporter form-wrapper` so the form's own rules match, `sbb-ui` for the design tokens - the same
    // three classes the side panel's container carries.
    containerClassName: 'pdf-exporter form-wrapper sbb-ui',
    styleTexts: [formStyle, popupStyle],
  });

  // `exportType` is passed on so that a bulk run resolves no location path at all: its items are picked in
  // the widget, not addressed by the page URL, and a path left over from the page would travel in the
  // request the widget then reuses for every item.
  const location =
    options.location ??
    toDocumentIdentity(currentDocumentLocation({ documentType: options.documentType, exportType: options.exportType }));

  const root = createRoot(container);
  const close = () => {
    closeOpenPopup = null;
    root.unmount();
    host.remove();
  };
  closeOpenPopup = close;
  root.render(
    <ExportPopupModal
      document={location}
      exportType={options.exportType}
      identifiers={options.identifiers}
      onBulkExport={options.onBulkExport}
      onClose={close}
      deps={options.deps}
    />,
  );
  return root;
}

export default openExportPopup;

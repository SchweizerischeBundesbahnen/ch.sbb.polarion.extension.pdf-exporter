/**
 * Loads the product's own export JS, which the widget drives but does not own.
 * <p>
 * `ExportPopup` is the export parameters dialog shared with the document export and the DLE toolbar, and
 * `ExportContext` carries the conversion protocol (start a job, poll it, download the result). Both are
 * served from the extension's other webapp, so they are imported at runtime rather than bundled, and
 * both are behind this module so that tests can replace them.
 */
const MODULES_BASE = '/polarion/pdf-exporter/ui/js/modules';
const MICROMODAL_URL = '/polarion/pdf-exporter/ui/generic/js/micromodal.min.js';

/** The subset of the product's ExportParams the bulk export sets per item. */
export interface ExportParamsLike {
  projectId?: string | null;
  documentType?: string;
  locationPath?: string;
  revision?: string | null;
  urlQueryParameters?: Record<string, string>;
  attachmentsFilter?: string | null;
  embedAttachments?: boolean;
  testcaseFieldId?: string;
  toJSON: () => unknown;
}

export interface ConversionResult {
  response: Blob;
  fileName?: string;
  warning?: string;
}

/** The subset of the product's ExportContext the bulk export calls. */
export interface ExportContextLike {
  asyncConvertPdf: (
    request: unknown,
    onSuccess: (result: ConversionResult) => void,
    onError: (response: Response) => void,
  ) => void;
  convertCollectionDocuments: (
    exportParams: ExportParamsLike,
    collectionId: string,
    onComplete: () => void,
    onError: (response: Response) => void,
  ) => void;
  downloadTestRunAttachments: (
    projectId: string,
    testRunId: string,
    revision: string | null,
    filter: string | null,
    testCaseFieldId?: string,
  ) => void;
  downloadBlob: (blob: Blob, fileName: string) => void;
}

/** What ExportPopup calls back into while the user fills in the export parameters. */
export interface BulkCallback {
  getDocIdentifiers: () => { projectId?: string; spaceId?: string; documentName: string }[];
  openPopup: (exportParams: ExportParamsLike) => void;
}

export interface ExportContextOptions {
  documentType?: string;
  exportType?: string;
  exportPages?: boolean;
  rootComponentSelector: string;
}

/**
 * Puts micromodal on the page, which `ExportPopup` needs and does not load itself.
 *
 * `MicroModal` is a global, and every entry point that opens the export dialog is expected to provide
 * it: `live-reports.js` and `starter.js` inject the script, and the widget's vanilla predecessor
 * imported it on its first line. Without it the dialog throws `MicroModal is not defined` and nothing
 * opens. Loading it twice is harmless - the script only reassigns the global - but the page usually has
 * it already when the widget sits on a report page next to the toolbar.
 */
export async function ensureMicroModal(): Promise<void> {
  if ((window as { MicroModal?: unknown }).MicroModal) {
    return;
  }
  await import(/* @vite-ignore */ MICROMODAL_URL);
}

export async function openExportPopup(documentType: string, bulkCallback: BulkCallback): Promise<void> {
  await ensureMicroModal();
  const module = await import(/* @vite-ignore */ `${MODULES_BASE}/ExportPopup.js`);
  new module.default({ documentType, bulkCallback });
}

export async function createExportContext(options: ExportContextOptions): Promise<ExportContextLike> {
  const module = await import(/* @vite-ignore */ `${MODULES_BASE}/ExportContext.js`);
  return new module.default({ exportType: 'BULK', ...options }) as ExportContextLike;
}

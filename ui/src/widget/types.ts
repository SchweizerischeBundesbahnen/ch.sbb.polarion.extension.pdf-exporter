import type { BulkExportWidgetShim } from '@sbb-polarion/react-sbb-polarion';
// The document type is the export model's, not the widget's: what a widget lists decides what its export
// dialog offers, so both sides read the same definition. Imported rather than re-exported straight through,
// because `DocumentType` is also a DOM global (the `<!DOCTYPE>` node) - a bare `export type { … } from` would
// leave the references below silently bound to that one instead.
import type { DocumentType } from '../export/documentType';

export type { DocumentType };

export type { BulkExportColumn, BulkExportItem, BulkExportItems } from '@sbb-polarion/react-sbb-polarion';

/**
 * What the widget renderer puts on the shim element: react-sbb-polarion's shim, plus what the export
 * dialog of this extension needs. Presentation only, so that the widget's frame renders before the rows
 * arrive; the endpoint acts on the signed descriptor alone.
 */
export interface WidgetShim extends BulkExportWidgetShim {
  documentType: DocumentType;
  exportPages: boolean;
}

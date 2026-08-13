// The document type is the export model's, not the widget's: what a widget lists decides what its export
// dialog offers, so both sides read the same definition. Imported rather than re-exported straight through,
// because `DocumentType` is also a DOM global (the `<!DOCTYPE>` node) - a bare `export type { … } from` would
// leave the references below silently bound to that one instead.
import type { DocumentType } from '../export/documentType';

export type { DocumentType };

/** A column of the widget's table, as the widget was configured on the page. */
export interface BulkExportColumn {
  id: string;
  label: string;
}

/**
 * One row. An item the current user may not read carries only `message`: the widget shows that instead
 * of the cells, exactly as Polarion's own tables do.
 */
export interface BulkExportItem {
  readable: boolean;
  message?: string | null;
  type?: string | null;
  projectId?: string | null;
  spaceId?: string | null;
  id?: string | null;
  name?: string | null;
  cells?: string[] | null;
}

/** What `/widgets/bulk-export/items` answers. */
export interface BulkExportItems {
  columns: BulkExportColumn[];
  items: BulkExportItem[];
  totalCount: number;
  countMessage: string;
  openInTableUrl?: string | null;
  query?: string | null;
}

/**
 * What the widget renderer puts on the shim element. `descriptor` and `signature` are opaque here: they
 * are passed back to the endpoint unchanged, which is what lets the server trust the query they carry.
 * The rest is presentation only, so that the widget's frame renders before the rows arrive.
 */
export interface WidgetShim {
  descriptor: string;
  signature: string;
  title: string;
  documentType: DocumentType;
  exportPages: boolean;
}

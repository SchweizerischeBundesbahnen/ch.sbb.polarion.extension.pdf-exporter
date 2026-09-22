/**
 * The Bulk PDF Export widgets on the page, as the report's own "Export to PDF" buttons see them.
 *
 * A report toolbar button that finds rows selected in a widget asks whether to export the report or the
 * selection (see `popup/ExportTargetChooser.tsx`). The widget and the dialog are two Vite entries imported
 * by different URLs (`bulk-widget.js?v=` and `export-popup.js?timestamp=`), so a module both of them import
 * is not guaranteed to be one instance. The list lives on `window` instead, which both share: the widget
 * mounts into the report page, and the buttons open the dialog in that same page.
 */

/** A widget as the choice offers it. */
export interface BulkExportTarget {
  /** Unique on the page. A report may carry two widgets of the same title. */
  id: string;
  /** The widget's title, which is what tells two widgets on one page apart. */
  title: string;
  /** How many rows are selected right now. */
  selectedCount: () => number;
  /** Opens the widget's own export dialog for the selection, as its own button does. */
  startExport: () => void;
}

const KEY = '__pdfExporterBulkExportTargets';

type TargetWindow = Window & { [KEY]?: Set<BulkExportTarget> };

function targets(): Set<BulkExportTarget> {
  const holder = window as TargetWindow;
  holder[KEY] ??= new Set();
  return holder[KEY];
}

/** Adds a widget to the list. Returns what removes it again, for the widget's unmount. */
export function registerBulkExportTarget(target: BulkExportTarget): () => void {
  targets().add(target);
  return () => {
    targets().delete(target);
  };
}

/** The widgets which have rows selected, in the order they were mounted. */
export function selectedBulkExportTargets(): BulkExportTarget[] {
  return [...targets()].filter((target) => target.selectedCount() > 0);
}

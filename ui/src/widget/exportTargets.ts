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
  /**
   * What the widget lists: "Documents", "Test Runs" and the like, from the renderer. It is the type of the
   * items, not a name of the widget, so two widgets over the same type share it.
   */
  title: string;
  /**
   * The widget's element on the page - its shadow host in Polarion - or null before it rendered. Says whether
   * the widget is still on the page, and where.
   */
  anchor: () => Element | null;
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

const isOnPage = (target: BulkExportTarget): boolean => target.anchor()?.isConnected ?? false;

/** Earlier on the page first. */
function byPagePosition(first: BulkExportTarget, second: BulkExportTarget): number {
  const position = first.anchor()!.compareDocumentPosition(second.anchor()!);
  if (position & Node.DOCUMENT_POSITION_FOLLOWING) {
    return -1;
  }
  return position & Node.DOCUMENT_POSITION_PRECEDING ? 1 : 0;
}

/**
 * The widgets on the page which have rows selected, in the order the page shows them.
 *
 * A widget whose element has left the page is dropped from the list for good. Nothing unmounts a widget in
 * Polarion - its root is simply discarded with the report when the user moves on to another one, which does
 * not reload the page - so this is where a stale entry goes.
 */
export function selectedBulkExportTargets(): BulkExportTarget[] {
  const all = targets();
  for (const target of [...all]) {
    if (!isOnPage(target)) {
      all.delete(target);
    }
  }
  return [...all].filter((target) => target.selectedCount() > 0).sort(byPagePosition);
}

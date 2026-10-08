/**
 * The widgets of other extensions which a report's own "Export to PDF" button can export alone (#1183).
 *
 * A widget offers itself by adding an entry to a set on the top window, which the button reads when it is clicked.
 * The set is a plain `window` property rather than a module both import, for the reason the Bulk PDF Export targets
 * are (see `widget/exportTargets.ts`): a widget of another extension shares no module with this one at all.
 *
 * ```js
 * const top = window.top ?? window;
 * (top.__pdfExporterExportTargets ??= new Set()).add({
 *   title: 'Timesheet report',
 *   // An element of the widget on the report page. A widget drawn in an iframe gives the iframe: window.frameElement
 *   anchor: () => element,
 * });
 * ```
 *
 * The widget needs no ID of its own. Polarion puts every widget of a report in an element of the class
 * `polarion-rp-widget-part`, whose ID is kept with the page (`polarion_client1`), and the server exports the widget of
 * that ID: what the anchor stands in is what the user picks.
 */

/** A widget as it offers itself. */
export interface WidgetExportTarget {
  /** What the choice calls the widget. */
  title: string;
  /** An element of the widget on the report page, or null before it rendered. */
  anchor: () => Element | null;
}

/** A widget the choice offers: one on the page, inside a widget of the report. */
export interface OfferedWidget {
  title: string;
  /** The ID of the report's widget the anchor stands in, which the export is asked for. */
  widgetId: string;
  /** The report's widget the anchor stands in. */
  part: Element;
}

export const WIDGET_EXPORT_TARGETS_KEY = '__pdfExporterExportTargets';

const WIDGET_PART_CLASS = 'polarion-rp-widget-part';

type TargetWindow = Window & { [WIDGET_EXPORT_TARGETS_KEY]?: Set<WidgetExportTarget> };

function targets(): Set<WidgetExportTarget> {
  const holder = window as TargetWindow;
  holder[WIDGET_EXPORT_TARGETS_KEY] ??= new Set();
  return holder[WIDGET_EXPORT_TARGETS_KEY];
}

/** Adds a widget to the set, as a widget of another extension does by hand. Returns what removes it again. */
export function registerWidgetExportTarget(target: WidgetExportTarget): () => void {
  targets().add(target);
  return () => {
    targets().delete(target);
  };
}

/** The report's widget an element stands in, through any shadow root it is mounted in, or null. */
function widgetPartOf(element: Element): Element | null {
  let node: Node | null = element;
  while (node) {
    if (node instanceof Element && node.classList.contains(WIDGET_PART_CLASS) && node.id) {
      return node;
    }
    node = node instanceof ShadowRoot ? node.host : node.parentNode;
  }
  return null;
}

/** The ID of the report's widget an element stands in, or null. */
export function widgetIdOf(element: Element): string | null {
  return widgetPartOf(element)?.id ?? null;
}

/**
 * Earlier on the page first. The widgets of the report are compared, not the anchors: two anchors in shadow
 * roots of their own have no position relative to each other.
 */
function byPagePosition(first: OfferedWidget, second: OfferedWidget): number {
  const position = first.part.compareDocumentPosition(second.part);
  if (position & Node.DOCUMENT_POSITION_FOLLOWING) {
    return -1;
  }
  return position & Node.DOCUMENT_POSITION_PRECEDING ? 1 : 0;
}

/**
 * The widgets on the page which offered themselves, in the order the page shows them, one per widget of the report.
 *
 * A widget whose element has left the page is dropped for good: nothing unmounts a widget in Polarion, its element is
 * discarded with the report when the user moves on. A widget outside any widget of the report has nothing to export.
 */
export function offeredWidgets(): OfferedWidget[] {
  const all = targets();
  const offered = new Map<string, OfferedWidget>();
  for (const target of [...all]) {
    const anchor = target.anchor();
    if (!anchor?.isConnected) {
      all.delete(target);
      continue;
    }
    const part = widgetPartOf(anchor);
    if (part && !offered.has(part.id)) {
      offered.set(part.id, { title: target.title, widgetId: part.id, part });
    }
  }
  return [...offered.values()].sort(byPagePosition);
}

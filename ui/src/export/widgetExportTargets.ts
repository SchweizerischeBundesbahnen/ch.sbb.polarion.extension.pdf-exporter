import { PDF_EXPORT_TARGETS_KEY } from '@sbb-polarion/react-sbb-polarion';
import type { PdfExportTarget } from '@sbb-polarion/react-sbb-polarion';

/**
 * The widgets of other extensions which a report's own "Export to PDF" button can export alone (#1183).
 *
 * A widget offers itself with `useOfferForPdfExport` or `offerForPdfExport` of react-sbb-polarion, which add an
 * entry to a set on the top window; the key and the type of the entry come from there too. The set is a plain
 * `window` property rather than module state, for the reason the Bulk PDF Export targets are (see
 * `widget/exportTargets.ts`): a widget of another extension shares no module with this one at all.
 *
 * The widget needs no ID of its own. Polarion puts every widget of a report in an element of the class
 * `polarion-rp-widget-part`, whose ID is kept with the page (`polarion_client1`), and the server exports the widget of
 * that ID: what the anchor stands in is what the user picks.
 */

/** A widget the choice offers: one on the page, inside a widget of the report. */
export interface OfferedWidget {
  title: string;
  /** The ID of the report's widget the anchor stands in, which the export is asked for. */
  widgetId: string;
  /** The report's widget the anchor stands in. */
  part: Element;
}

const WIDGET_PART_CLASS = 'polarion-rp-widget-part';

type TargetWindow = Window & { [PDF_EXPORT_TARGETS_KEY]?: Set<PdfExportTarget> };

/**
 * The offers, read where the widgets put them. The dialog runs in the top window in Polarion, so it is the same
 * window there; a test runs in an iframe of its runner, as a widget app does in a report.
 */
function targets(): Set<PdfExportTarget> {
  try {
    const holder = (window.top ?? window) as TargetWindow;
    return holder[PDF_EXPORT_TARGETS_KEY] ?? new Set();
  } catch {
    // A page of another origin embeds Polarion, whose top window cannot be read: no widget can have offered itself
    return new Set();
  }
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

/** The element a widget gives, or null where it gives none or fails: a widget of another extension cannot stop the button. */
function anchorOf(target: PdfExportTarget): Element | null {
  try {
    return target.anchor();
  } catch (error) {
    console.error(`The widget '${target.title}' could not say where it is`, error);
    return null;
  }
}

/**
 * The widgets on the page which offered themselves, in the order the page shows them, one per widget of the report.
 *
 * A widget which has not rendered yet gives no element and is asked again on the next click. A widget whose element
 * has left the page is dropped for good: nothing unmounts a widget in Polarion, its element is discarded with the report
 * when the user moves on. A widget outside any widget of the report has nothing to export.
 */
export function offeredWidgets(): OfferedWidget[] {
  const all = targets();
  const offered = new Map<string, OfferedWidget>();
  for (const target of [...all]) {
    const anchor = anchorOf(target);
    if (!anchor) {
      continue;
    }
    if (!anchor.isConnected) {
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

/** What the report shows as it is, rather than as content a widget export would drop. */
const NOT_CONTENT = 'h1, h2, h3, h4, h5, h6, script, style';

/**
 * Characters which show nothing. Polarion keeps two zero width spaces in every empty line of a report, which `trim()`
 * leaves where they are.
 */
const INVISIBLE = /[\s\u200b-\u200d\u2060\ufeff]/g;

/** Elements which show something without a word of text. */
const MEDIA = 'img, svg, canvas, video, audio, iframe, object, embed, table, .polarion-rp-inline-widget';

/**
 * Whether the widget is all the report shows, so that exporting the report means exporting the widget.
 *
 * Strict on purpose: the button then exports the widget without asking, and a report reduced to one widget by mistake
 * loses the rest without a word. Anything else on the page keeps the choice - another widget of the report, an inline
 * widget, a picture or a table, any text outside the headings. The headings are allowed: a report names itself in one,
 * and the export of a widget carries the title of the report instead.
 */
export function isAloneOnReport(widget: OfferedWidget): boolean {
  const content = widget.part.closest('.polarion-rpe-content');
  if (!content || content.querySelectorAll(`.${WIDGET_PART_CLASS}`).length !== 1) {
    return false;
  }
  const outside = (node: Node) => !widget.part.contains(node);
  if ([...content.querySelectorAll(MEDIA)].some(outside)) {
    return false;
  }
  const texts = document.createTreeWalker(content, NodeFilter.SHOW_TEXT);
  for (let text = texts.nextNode(); text; text = texts.nextNode()) {
    if (outside(text) && text.textContent?.replace(INVISIBLE, '') && !text.parentElement?.closest(NOT_CONTENT)) {
      return false;
    }
  }
  return true;
}

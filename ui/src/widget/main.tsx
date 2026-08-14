import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import rspStyles from '@grigoriev/react-sbb-polarion/style.css?inline';
import popupStyles from '../popup/export-popup.css?inline';
import BulkExportWidget from './BulkExportWidget';
import type { WidgetDependencies } from './BulkExportWidget';
import type { DocumentType, WidgetShim } from './types';
import widgetStyles from './widget.css?inline';

/**
 * Reads what the widget renderer put on the shim element. The descriptor and its signature are passed
 * back to the server untouched; the rest only decides what the widget's frame says.
 */
export function readShim(host: HTMLElement): WidgetShim {
  return {
    descriptor: host.dataset.descriptor ?? '',
    signature: host.dataset.signature ?? '',
    title: host.dataset.title ?? '',
    documentType: (host.dataset.documentType ?? 'LIVE_DOC') as DocumentType,
    exportPages: host.dataset.exportPages === 'true',
  };
}

/**
 * Brings the page's own stylesheets into the shadow root.
 *
 * The widget renders Polarion's table and button markup and has to look like the rest of the report
 * page, but a shadow root sees none of the page's rules. Cloning the links rather than copying the
 * rules keeps every relative url() in those stylesheets resolving against the stylesheet, and the
 * browser serves the second request from its cache.
 */
export function adoptPageStyles(root: ShadowRoot): void {
  document.head.querySelectorAll<HTMLLinkElement>('link[rel="stylesheet"]').forEach((link) => {
    const clone = document.createElement('link');
    clone.rel = 'stylesheet';
    clone.href = link.href;
    root.appendChild(clone);
  });
}

function addStyle(root: ShadowRoot, css: string): void {
  const style = document.createElement('style');
  style.textContent = css;
  root.appendChild(style);
}

/**
 * Gives a host element its shadow root with the styles the widget needs, and renders the widget into
 * it. Separate from {@link mount} so that the dev harness can mount the same widget with its own data.
 */
export function mountInto(host: HTMLElement, shim: WidgetShim, deps?: WidgetDependencies): void {
  if (host.shadowRoot) {
    return;
  }
  const root = host.attachShadow({ mode: 'open' });
  adoptPageStyles(root);
  // After the page's stylesheets, so that the widget's own rules win where both have something to say
  addStyle(root, rspStyles);
  // The widget renders two dialogs of its own: the export parameters dialog, whose stylesheet this is, and
  // the progress dialog, which widget.css styles. Both used to be micromodal markup in the report page's
  // body, styled by stylesheets the widget renderer put on the page; both are inside this root now.
  addStyle(root, popupStyles);
  addStyle(root, widgetStyles);

  const container = document.createElement('div');
  // `sbb-ui` is the scope the --sbb-* tokens are declared on. They would also inherit from the host, which
  // carries the same class in Polarion, but declaring them inside keeps the widget self-contained - which is
  // what the dev harness and the tests mount it as. `pdf-exporter` is what the export dialog's own rules are
  // scoped to; the widget's markup has nothing those rules match.
  container.className = 'sbb-ui pdf-exporter';
  root.appendChild(container);

  createRoot(container).render(
    <StrictMode>
      <BulkExportWidget shim={shim} deps={deps} />
    </StrictMode>,
  );
}

/**
 * Mounts the widget into the shim the renderer emitted.
 *
 * A report page may carry several of these widgets, so each is found by the shim's own id and gets a shadow
 * root of its own: the page around it is Polarion's, not this app's.
 */
export default function mount(selector: string): void {
  const host = document.querySelector<HTMLElement>(selector);
  if (!host) {
    console.error(`Bulk PDF Export widget: no element matches ${selector}`);
    return;
  }
  mountInto(host, readShim(host));
}

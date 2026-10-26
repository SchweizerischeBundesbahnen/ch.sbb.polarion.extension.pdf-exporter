import {
  adoptPageStyles,
  bulkExportWidgetStyles,
  mountInShadowRoot,
  readBulkExportWidgetShim,
} from '@sbb-polarion/react-sbb-polarion';
import rspStyles from '@sbb-polarion/react-sbb-polarion/style.css?inline';
import formStyles from '../export/export-form.css?inline';
import popupStyles from '../popup/export-popup.css?inline';
import BulkExportWidget from './BulkExportWidget';
import type { WidgetDependencies } from './BulkExportWidget';
import type { DocumentType, WidgetShim } from './types';
import progressStyles from './widget.css?inline';

export { adoptPageStyles };

/**
 * Reads what the widget renderer put on the shim element. The descriptor and its signature are passed
 * back to the server untouched; the rest only decides what the widget's frame and its dialog say.
 */
export function readShim(host: HTMLElement): WidgetShim {
  return {
    ...readBulkExportWidgetShim(host),
    documentType: (host.dataset.documentType ?? 'LIVE_DOC') as DocumentType,
    exportPages: host.dataset.exportPages === 'true',
  };
}

/**
 * Gives a host element its shadow root with the styles the widget needs, and renders the widget into
 * it. Separate from {@link mount} so that the dev harness can mount the same widget with its own data.
 */
export function mountInto(host: HTMLElement, shim: WidgetShim, deps?: WidgetDependencies): void {
  mountInShadowRoot(
    host,
    {
      // After the page's stylesheets, so that the widget's own rules win where both have something to say.
      // The export dialog's form and chrome are the second and the third; the widget's frame, which is
      // react-sbb-polarion's, and the progress dialog are the last two.
      styles: [rspStyles, formStyles, popupStyles, bulkExportWidgetStyles, progressStyles],
      // `pdf-exporter` is what the export dialog's own rules are scoped to; the widget's markup has nothing
      // those rules match.
      className: 'sbb-ui pdf-exporter',
    },
    <BulkExportWidget shim={shim} deps={deps} />,
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

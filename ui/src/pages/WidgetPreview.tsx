import { useEffect, useRef, useState } from 'react';
import { PageLayout } from '@sbb-polarion/react-sbb-polarion';
import {
  SAMPLE_ITEMS,
  SAMPLE_ITEMS_EMPTY,
  SAMPLE_ITEMS_TRUNCATED,
  SAMPLE_ITEMS_WITH_UNREADABLE,
  SAMPLE_SHIM,
} from '../widget/sampleData';
import type { BulkExportItems } from '../widget/types';

/**
 * Development harness for the Bulk PDF Export widget.
 *
 * The widget itself renders on a Polarion report page, which `vite dev` has no way to reproduce, so
 * this page mounts the very same widget - shadow root, styles and all - against sample data. Nothing in
 * Polarion points here: the widget in production is mounted by BulkPdfExportWidgetRenderer.
 *
 * The two dialogs the widget owns are reached through it rather than rendered standalone here, because both
 * live in its shadow root now and are styled by the stylesheets that root carries. Selecting rows and
 * pressing "Export to PDF" opens the real export dialog, which needs a Polarion (VITE_BASE_URL); the
 * progress dialog's own states are covered offline and pixel-locked by test/BulkExportWidget.visual.test.tsx.
 */
const WIDGET_SCENARIOS: Record<string, BulkExportItems | 'error'> = {
  Loaded: SAMPLE_ITEMS,
  'With an unreadable row': SAMPLE_ITEMS_WITH_UNREADABLE,
  'More than the top value': SAMPLE_ITEMS_TRUNCATED,
  Empty: SAMPLE_ITEMS_EMPTY,
  Failing: 'error',
};

export default function WidgetPreview() {
  const [scenario, setScenario] = useState<keyof typeof WIDGET_SCENARIOS>('Loaded');
  const host = useRef<HTMLDivElement>(null);

  // A mounted widget owns its shadow root, so switching scenario replaces the host element entirely.
  // The widget is imported here rather than at the top of the file on purpose: a static import would put
  // the whole widget bundle into the admin app's own entry, which every administration page then loads
  // for the sake of this development page.
  useEffect(() => {
    const element = host.current;
    if (!element) {
      return;
    }
    let cancelled = false;
    const data = WIDGET_SCENARIOS[scenario];
    void import('../widget/main').then(({ mountInto }) => {
      if (cancelled) {
        return;
      }
      mountInto(element, SAMPLE_SHIM, {
        loadItems: () =>
          data === 'error'
            ? Promise.reject(new Error('The widget descriptor is no longer valid. Reload the page.'))
            : Promise.resolve(data),
      });
    });
    return () => {
      cancelled = true;
    };
  }, [scenario]);

  return (
    <PageLayout title="PDF Exporter: Bulk PDF Export widget (dev harness)">
      <p>
        The widget as a report page shows it, mounted with sample data. Pick a state to work on; the export dialog needs
        a running Polarion (VITE_BASE_URL).
      </p>
      <div className="preview-controls">
        {Object.keys(WIDGET_SCENARIOS).map((name) => (
          <button key={name} disabled={name === scenario} onClick={() => setScenario(name)}>
            {name}
          </button>
        ))}
      </div>
      <div className="preview-surface">
        <div
          key={scenario}
          id="widget-preview-host"
          className="polarion-PdfExporter-BulkExportWidget sbb-ui"
          ref={host}
        />
      </div>
    </PageLayout>
  );
}

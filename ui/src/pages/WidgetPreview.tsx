import { useEffect, useRef, useState } from 'react';
import { PageLayout } from '@grigoriev/react-sbb-polarion';
import BulkExportProgressModal from '../widget/BulkExportProgressModal';
import {
  SAMPLE_ITEMS,
  SAMPLE_ITEMS_EMPTY,
  SAMPLE_ITEMS_TRUNCATED,
  SAMPLE_ITEMS_WITH_UNREADABLE,
  SAMPLE_SHIM,
} from '../widget/sampleData';
import type { BulkExportItems } from '../widget/types';
import type { BulkExportState } from '../widget/useBulkExport';

/**
 * Development harness for the Bulk PDF Export widget.
 *
 * The widget itself renders on a Polarion report page, which `vite dev` has no way to reproduce, so
 * this page mounts the very same widget - shadow root, styles and all - against sample data, and lets
 * every state be reached without a running export. Nothing in Polarion points here: the widget in
 * production is mounted by BulkPdfExportWidgetRenderer.
 */
const WIDGET_SCENARIOS: Record<string, BulkExportItems | 'error'> = {
  Loaded: SAMPLE_ITEMS,
  'With an unreadable row': SAMPLE_ITEMS_WITH_UNREADABLE,
  'More than the top value': SAMPLE_ITEMS_TRUNCATED,
  Empty: SAMPLE_ITEMS_EMPTY,
  Failing: 'error',
};

const progressState = (state: BulkExportState['status'], errors = false): BulkExportState => ({
  status: state,
  rows: [
    { item: SAMPLE_ITEMS.items[0], state: 'finished' },
    {
      item: SAMPLE_ITEMS.items[1],
      state: state === 'in-progress' ? 'in-progress' : 'error',
      error: errors ? 'Conversion failed: the document has no content' : undefined,
    },
    { item: SAMPLE_ITEMS.items[2], state: state === 'interrupted' ? 'interrupted' : 'finished' },
  ],
  processed: 2,
  errors,
  merge: false,
});

const MODAL_SCENARIOS: Record<string, BulkExportState | null> = {
  Closed: null,
  'In progress': progressState('in-progress'),
  Interrupted: progressState('interrupted'),
  Finished: {
    ...progressState('finished'),
    rows: progressState('finished').rows.map((row) => ({ ...row, state: 'finished' as const })),
    processed: 3,
  },
  'Finished with errors': progressState('finished', true),
};

/**
 * What the widget renderer puts on a report page for the export dialogs, which render in the page body
 * rather than in the widget's shadow root. Served by the extension's own webapp, so they only resolve
 * when `vite dev` proxies to a Polarion (VITE_BASE_URL); without one the dialog previews unstyled.
 */
const PAGE_STYLESHEETS = [
  '/polarion/pdf-exporter/ui/generic/css/micromodal.css',
  '/polarion/pdf-exporter/ui/generic/css/control-tokens.css',
  '/polarion/pdf-exporter/ui/css/pdf-exporter.css',
];

export default function WidgetPreview() {
  const [scenario, setScenario] = useState<keyof typeof WIDGET_SCENARIOS>('Loaded');
  const [modal, setModal] = useState<keyof typeof MODAL_SCENARIOS>('Closed');
  const host = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const links = PAGE_STYLESHEETS.map((href) => {
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = href;
      document.head.appendChild(link);
      return link;
    });
    return () => links.forEach((link) => link.remove());
  }, []);

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
      mountInto(element, SAMPLE_SHIM, '#widget-preview-host', {
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
    <PageLayout title="PDF Exporter: Bulk PDF Export widget">
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
      <div className="preview-controls">
        {Object.keys(MODAL_SCENARIOS).map((name) => (
          <button key={name} disabled={name === modal} onClick={() => setModal(name)}>
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

      {MODAL_SCENARIOS[modal] && (
        <BulkExportProgressModal
          state={MODAL_SCENARIOS[modal]}
          onStop={() => setModal('Interrupted')}
          onClose={() => setModal('Closed')}
        />
      )}
    </PageLayout>
  );
}

import { useCallback, useEffect, useRef, useState } from 'react';
import { PageLayout } from '@grigoriev/react-sbb-polarion';
import DocumentPicker from '../components/DocumentPicker';
import { documentEditorHash } from '../services/documents';
import type { ProjectDocument } from '../services/documents';

const HOST_ID = 'side-panel-preview-host';

/** The width of Polarion's Document Properties pane, so the rows wrap where they really wrap. */
const PANE_WIDTH = 360;

/**
 * Development harness for the Document Properties side panel.
 *
 * It runs the **real** panel against a **real** document: pick one of the project's documents and the
 * harness writes the Polarion editor hash that document is opened at, then mounts the panel with no
 * stubbed dependencies at all. So the panel reads the document out of the hash the way it does in the
 * editor, and every REST call it makes goes to the Polarion behind `VITE_BASE_URL`. What is exercised here
 * is what runs in production, which is the point: the one thing a Vitest suite cannot cover is a real
 * editor URL and the real endpoints behind it.
 *
 * That also means this page needs a Polarion: `VITE_BASE_URL` for the proxy and `VITE_BEARER_TOKEN` for the
 * platform API the document list comes from. The panel's own states, offline and pixel-locked, are covered
 * by `test/SidePanel.visual.test.tsx` instead.
 */
export default function SidePanelPreview() {
  const [picked, setPicked] = useState<{ projectId: string; document: ProjectDocument } | undefined>();
  const host = useRef<HTMLDivElement>(null);

  const onPick = useCallback((next: { projectId: string; document: ProjectDocument } | undefined) => {
    setPicked(next);
  }, []);

  // The panel is mounted only once a document is picked, and re-mounted when another one is: it reads the
  // hash when it is created, so a new document means a new panel.
  //
  // The panel bundle is imported here rather than at the top of the file on purpose: a static import would
  // put it into the admin app's own entry, which every administration page then loads for the sake of this
  // development page.
  useEffect(() => {
    const element = host.current;
    if (!element || !picked) {
      return undefined;
    }
    // The hash a real editor would have. `history.replaceState` keeps `?feature=` and `?scope=` intact -
    // the app routes on the search parameters, not on the hash.
    const hash = documentEditorHash(picked.projectId, picked.document);
    window.history.replaceState({}, '', `${window.location.pathname}${window.location.search}${hash}`);

    let root: { unmount: () => void } | undefined;
    let cancelled = false;
    void import('../formext/mount').then(({ mountSidePanel }) => {
      if (cancelled) {
        return;
      }
      // No dependencies: the real REST calls, the real document.
      root = mountSidePanel(`#${HOST_ID}`);
    });
    return () => {
      cancelled = true;
      // Deferred, not called here: the panel is a root of its own, and unmounting one synchronously from a
      // cleanup that runs while this page is still rendering makes React warn about exactly that. By the
      // time the microtask runs, a re-mount has already replaced the shadow root's children, so
      // unmounting the old root is the no-op it should be.
      const previous = root;
      queueMicrotask(() => previous?.unmount());
    };
  }, [picked]);

  return (
    <PageLayout title="PDF Exporter: Document Properties side panel (dev harness)">
      <p className="landing-intro">
        The panel as the document editor shows it, mounted through the real <code>mountSidePanel</code> against a real
        document: the harness writes the editor hash the document is opened at, and the panel reads it. Needs a Polarion
        behind <code>VITE_BASE_URL</code>.
      </p>

      <DocumentPicker onChange={onPick} />

      {picked && (
        <div className="preview-surface" style={{ maxWidth: PANE_WIDTH }}>
          <div id={HOST_ID} ref={host} className="pdf-exporter form-wrapper" />
        </div>
      )}
    </PageLayout>
  );
}

import { useEffect, useMemo, useRef, useState } from 'react';
import { PageLayout, SearchableSelect } from '@grigoriev/react-sbb-polarion';
import { getCookie, setCookie } from '../services/cookies';
import { MAX_PAGES, documentEditorHash, documentPath, fetchDocuments } from '../services/documents';
import type { ProjectDocument } from '../services/documents';
import { getProjectIdFromScope, getScope } from '../services/scope';

const HOST_ID = 'side-panel-preview-host';

/** The last document picked, remembered across visits. Scoped by project, since ids are per project. */
const DOCUMENT_COOKIE = 'pdf-exporter-dev-document';

/** The width of Polarion's Document Properties pane, so the rows wrap where they really wrap. */
const PANE_WIDTH = 360;

/**
 * Reads the remembered document, but only for the project it was picked in - a `<space>/<name>` from
 * another project would address a document that does not exist.
 */
function rememberedDocument(projectId: string): string {
  const stored = getCookie(DOCUMENT_COOKIE) ?? '';
  const separator = stored.indexOf('|');
  return separator > 0 && stored.slice(0, separator) === projectId ? stored.slice(separator + 1) : '';
}

/**
 * Development harness for the Document Properties side panel.
 *
 * It runs the **real** panel against a **real** document: pick one of the project's documents and the
 * harness writes the Polarion editor hash that document is opened at, then mounts the panel with no
 * stubbed dependencies at all. So the panel loads the product's export JS, that JS reads the document out
 * of the hash the way it does in the editor, and every REST call the panel makes goes to the Polarion
 * behind `VITE_BASE_URL`. What is exercised here is what runs in production, which is the point: the one
 * thing a Vitest suite cannot cover is a real editor URL and the real endpoints behind it.
 *
 * That also means this page needs a Polarion: `VITE_BASE_URL` for the proxy and `VITE_BEARER_TOKEN` for the
 * platform API the document list comes from. The panel's own states, offline and pixel-locked, are covered
 * by `test/SidePanel.visual.test.tsx` instead.
 *
 * The project comes from the scope the Overview page carries, the same way every feature page gets it.
 */
export default function SidePanelPreview() {
  const projectId = getProjectIdFromScope(getScope());

  const [documents, setDocuments] = useState<ProjectDocument[]>([]);
  const [truncated, setTruncated] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState('');

  const host = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!projectId) {
      return undefined;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetchDocuments(projectId)
      .then((list) => {
        if (cancelled) return;
        setDocuments(list.documents);
        setTruncated(list.truncated);
        // Restore the last pick, but only if the project still offers it.
        const remembered = rememberedDocument(projectId);
        setSelected(list.documents.some((document) => documentPath(document) === remembered) ? remembered : '');
        setLoading(false);
      })
      .catch(() => {
        if (cancelled) return;
        setError(
          'Could not load the documents of this project. Set VITE_BASE_URL and VITE_BEARER_TOKEN in ' +
            'ui/.env.local and restart the dev server.',
        );
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [projectId]);

  const picked = useMemo(
    () => documents.find((candidate) => documentPath(candidate) === selected),
    [documents, selected],
  );

  useEffect(() => {
    if (projectId && selected) {
      setCookie(DOCUMENT_COOKIE, `${projectId}|${selected}`);
    }
  }, [projectId, selected]);

  // The panel is mounted only once a document is picked, and re-mounted when another one is: the export
  // context reads the hash when it is constructed, so a new document means a new panel.
  //
  // The panel bundle is imported here rather than at the top of the file on purpose: a static import would
  // put it into the admin app's own entry, which every administration page then loads for the sake of this
  // development page.
  useEffect(() => {
    const element = host.current;
    if (!element || !picked || !projectId) {
      return undefined;
    }
    // The hash a real editor would have. `history.replaceState` keeps `?feature=` and `?scope=` intact -
    // the app routes on the search parameters, not on the hash.
    const hash = documentEditorHash(projectId, picked);
    window.history.replaceState({}, '', `${window.location.pathname}${window.location.search}${hash}`);

    let root: { unmount: () => void } | undefined;
    let cancelled = false;
    void import('../formext/mount').then(({ mountSidePanel }) => {
      if (cancelled) {
        return;
      }
      // No dependencies: the real product JS, the real REST calls, the real document.
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
  }, [picked, projectId]);

  const options = documents.map((candidate) => ({
    id: documentPath(candidate),
    name: `${candidate.spaceId} / ${candidate.moduleName}`,
  }));

  return (
    <PageLayout title="PDF Exporter: Document Properties side panel (dev harness)">
      <p className="landing-intro">
        The panel as the document editor shows it, mounted through the real <code>mountSidePanel</code> against a real
        document: the harness writes the editor hash the document is opened at, and the panel reads it through the
        product&apos;s own export JS. Needs a Polarion behind <code>VITE_BASE_URL</code>.
      </p>

      {!projectId && (
        <div className="alert alert-error">
          Pick a project on the <a href="?">Overview</a> page first - documents are listed per project.
        </div>
      )}

      {projectId && (
        <>
          <div className="landing-scope">
            <label htmlFor="dev-document-select">Document:</label>
            <SearchableSelect
              id="dev-document-select"
              value={selected}
              onChange={setSelected}
              options={options}
              loading={loading}
              placeholder={loading ? 'Loading documents…' : 'Select a document…'}
              allowEmpty
            />
          </div>
          {truncated && (
            <div className="alert alert-warning">
              Showing the first {MAX_PAGES * 100} documents of this project; the list was cut off there.
            </div>
          )}
          {error && <div className="alert alert-error">{error}</div>}
          {!loading && !error && documents.length === 0 && (
            <div className="alert alert-warning">This project has no documents to export.</div>
          )}
        </>
      )}

      {picked && (
        <div className="preview-surface" style={{ maxWidth: PANE_WIDTH }}>
          <div id={HOST_ID} ref={host} className="pdf-exporter form-wrapper" />
        </div>
      )}
    </PageLayout>
  );
}

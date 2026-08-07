import { useEffect, useMemo, useState } from 'react';
import { SearchableSelect } from '@grigoriev/react-sbb-polarion';
import { getCookie, setCookie } from '../services/cookies';
import { MAX_PAGES, documentPath, fetchDocuments } from '../services/documents';
import type { ProjectDocument } from '../services/documents';
import { getProjectIdFromScope, getScope } from '../services/scope';

/**
 * Picks one of a project's real documents, for the development harnesses.
 *
 * Both harness pages - the Document Properties side panel and the "Export to PDF" dialog - run the **real**
 * surface against a **real** document: the page writes the Polarion editor hash that document is opened at,
 * and the surface reads it exactly as it does in Polarion. Choosing that document is this component.
 *
 * It needs a Polarion: `VITE_BASE_URL` for the proxy and `VITE_BEARER_TOKEN` for the platform API the
 * document list comes from. The project is the scope the Overview page carries, the same way every feature
 * page gets it.
 */

/** The last document picked, remembered across visits. Scoped by project, since ids are per project. */
const DOCUMENT_COOKIE = 'pdf-exporter-dev-document';

const LOAD_ERROR =
  'Could not load the documents of this project. Set VITE_BASE_URL and VITE_BEARER_TOKEN in ' +
  'ui/.env.local and restart the dev server.';

/**
 * Reads the remembered document, but only for the project it was picked in - a `<space>/<name>` from another
 * project would address a document that does not exist.
 */
function rememberedDocument(projectId: string): string {
  const stored = getCookie(DOCUMENT_COOKIE) ?? '';
  const separator = stored.indexOf('|');
  return separator > 0 && stored.slice(0, separator) === projectId ? stored.slice(separator + 1) : '';
}

export interface DocumentPickerProps {
  /** The picked document, or undefined while none is picked. */
  onChange: (picked: { projectId: string; document: ProjectDocument } | undefined) => void;
}

export default function DocumentPicker({ onChange }: Readonly<DocumentPickerProps>) {
  const projectId = getProjectIdFromScope(getScope());

  const [documents, setDocuments] = useState<ProjectDocument[]>([]);
  const [truncated, setTruncated] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState('');

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
        setError(LOAD_ERROR);
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

  useEffect(() => {
    onChange(picked && projectId ? { projectId, document: picked } : undefined);
    // `onChange` is the caller's own callback and not necessarily stable; depending on it would report the
    // same pick again on every render of the page around this one.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [picked, projectId]);

  if (!projectId) {
    return (
      <div className="alert alert-error">
        Pick a project on the <a href="?">Overview</a> page first - documents are listed per project.
      </div>
    );
  }

  const options = documents.map((candidate) => ({
    id: documentPath(candidate),
    name: `${candidate.spaceId} / ${candidate.moduleName}`,
  }));

  return (
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
  );
}

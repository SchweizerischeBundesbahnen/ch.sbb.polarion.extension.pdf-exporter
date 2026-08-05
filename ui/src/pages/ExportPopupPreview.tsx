import { useCallback, useState } from 'react';
import { PageLayout, SearchableSelect } from '@grigoriev/react-sbb-polarion';
import DocumentPicker from '../components/DocumentPicker';
import type { DocumentType, ExportType } from '../export/documentType';
import { documentEditorHash } from '../services/documents';
import type { ProjectDocument } from '../services/documents';

/** What the dialog can be opened for. `WIKI_PAGE` is offered too, although no toolbar opens it that way. */
const DOCUMENT_TYPES: { id: DocumentType; name: string }[] = [
  { id: 'LIVE_DOC', name: 'LIVE_DOC (a document, as the editor toolbar opens it)' },
  { id: 'LIVE_REPORT', name: 'LIVE_REPORT (a report, as the report toolbar opens it)' },
  { id: 'TEST_RUN', name: 'TEST_RUN (offers the attachment fields)' },
  { id: 'BASELINE_COLLECTION', name: 'BASELINE_COLLECTION' },
  { id: 'WIKI_PAGE', name: 'WIKI_PAGE' },
];

const EXPORT_TYPES: { id: ExportType; name: string }[] = [
  { id: 'SINGLE', name: 'SINGLE (one item, from a toolbar)' },
  { id: 'BULK', name: 'BULK (a selection, from the widget)' },
];

/**
 * The two types Polarion addresses by an id in the URL rather than by a space and a name. Their hash carries
 * no `/wiki/` part at all, which is why the harness cannot build it out of a picked document.
 */
const ID_ADDRESSED: Partial<Record<DocumentType, string>> = {
  TEST_RUN: 'testrun',
  BASELINE_COLLECTION: 'collection',
};

/**
 * The page hash Polarion would be on for this item, which is what the dialog reads its location out of.
 *
 * Getting this right per type is the point of the harness: a test run whose hash still named a document
 * would send an export request no endpoint accepts, which is not a state the dialog can ever be in.
 */
function hashFor(documentType: DocumentType, projectId: string, document: ProjectDocument, itemId: string): string {
  const segment = ID_ADDRESSED[documentType];
  if (segment) {
    return `#/project/${encodeURI(projectId)}/${segment}?id=${encodeURIComponent(itemId)}`;
  }
  return documentEditorHash(projectId, document);
}

/**
 * Development harness for the "Export to PDF" dialog.
 *
 * It opens the dialog through the **real** `openExportPopup` path - shadow-root mounted, the same way the
 * document editor toolbar, the Live Report toolbar and the report page's export button open it. The harness
 * writes the Polarion hash the item is opened at, so the dialog reads its location out of the URL exactly as
 * it does in Polarion and every REST call goes to the Polarion behind `VITE_BASE_URL`.
 *
 * The document type and the export type are picked here rather than derived, because that is the part a
 * running Polarion cannot easily be talked into: reaching the test run form means opening a test run, and
 * reaching the bulk form means a report page with a configured widget. Both change which rows the form
 * offers and what the request carries, which is the most intricate part of this dialog.
 *
 * That is the one thing the Vitest suites cannot cover - a real page URL and the real endpoints behind it.
 * The dialog's own states are covered offline and pixel-locked by `test/ExportPopup.visual.test.tsx`.
 */
export default function ExportPopupPreview() {
  const [picked, setPicked] = useState<{ projectId: string; document: ProjectDocument } | undefined>();
  const [documentType, setDocumentType] = useState<DocumentType>('LIVE_DOC');
  const [exportType, setExportType] = useState<ExportType>('SINGLE');
  const [itemId, setItemId] = useState('');
  const [bulkParams, setBulkParams] = useState<string | null>(null);

  const onPick = useCallback((next: { projectId: string; document: ProjectDocument } | undefined) => {
    setPicked(next);
  }, []);

  const idSegment = ID_ADDRESSED[documentType];
  const ready = !!picked && (!idSegment || itemId.trim().length > 0);

  const open = () => {
    if (!picked || !ready) {
      return;
    }
    setBulkParams(null);
    // `history.replaceState` keeps `?feature=` and `?scope=` intact - the app routes on the search
    // parameters, not on the hash.
    const hash = hashFor(documentType, picked.projectId, picked.document, itemId.trim());
    window.history.replaceState({}, '', `${window.location.pathname}${window.location.search}${hash}`);

    // Imported here rather than at the top of the file on purpose: a static import would put the whole popup
    // bundle into the admin app's own entry, which every administration page then loads for the sake of this
    // development page.
    void import('../popup/mount').then(({ openExportPopup }) => {
      openExportPopup({
        documentType,
        exportType,
        // A bulk run belongs to the widget, so the harness stands in for it: one identifier for the picked
        // item, and the parameters the dialog hands over are shown instead of exported.
        identifiers:
          exportType === 'BULK'
            ? [
                {
                  projectId: picked.projectId,
                  ...(idSegment ? {} : { spaceId: picked.document.spaceId }),
                  documentName: idSegment ? itemId.trim() : picked.document.moduleName,
                },
              ]
            : undefined,
        onBulkExport: (params) => setBulkParams(JSON.stringify(params, null, 2)),
      });
    });
  };

  return (
    <PageLayout title="PDF Exporter: Export to PDF dialog (dev harness)">
      <p className="landing-intro">
        The export dialog as a toolbar button opens it, through the real <code>openExportPopup</code> path (shadow-root
        mounted) against a real item: the harness writes the page hash the item is opened at, and the dialog reads it.
        Needs a Polarion behind <code>VITE_BASE_URL</code>.
      </p>

      <DocumentPicker onChange={onPick} />

      <div className="landing-scope">
        <label htmlFor="dev-document-type">Document type:</label>
        <SearchableSelect
          id="dev-document-type"
          value={documentType}
          onChange={(value) => setDocumentType(value as DocumentType)}
          options={DOCUMENT_TYPES}
        />
      </div>

      {idSegment && (
        <>
          <div className="landing-scope">
            <label htmlFor="dev-item-id">{documentType === 'TEST_RUN' ? 'Test run id:' : 'Collection id:'}</label>
            <input id="dev-item-id" type="text" value={itemId} onChange={(e) => setItemId(e.target.value)} />
          </div>
          <p>
            A {documentType === 'TEST_RUN' ? 'test run' : 'baseline collection'} is addressed by its id, not by a space
            and a name, so the picked document only decides which project is in scope.
          </p>
        </>
      )}

      <div className="landing-scope">
        <label htmlFor="dev-export-type">Export type:</label>
        <SearchableSelect
          id="dev-export-type"
          value={exportType}
          onChange={(value) => setExportType(value as ExportType)}
          options={EXPORT_TYPES}
        />
      </div>

      <div className="preview-controls">
        <button type="button" disabled={!ready} onClick={open}>
          Open Export to PDF
        </button>
      </div>

      {bulkParams && (
        <>
          <p>
            A bulk export hands its parameters back to the widget that opened the dialog, which then runs one conversion
            per selected item. This is what it handed over:
          </p>
          <pre className="dev-bulk-params">{bulkParams}</pre>
        </>
      )}
    </PageLayout>
  );
}

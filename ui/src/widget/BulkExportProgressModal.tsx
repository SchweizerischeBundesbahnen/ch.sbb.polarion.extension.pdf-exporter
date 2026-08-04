import { createPortal } from 'react-dom';
import type { BulkExportState } from './useBulkExport';
import { itemName, itemTypeLabel } from './useBulkExport';

const POPUP_ID = 'bulk-pdf-export-modal-popup';

interface Props {
  state: BulkExportState;
  onStop: () => void;
  onClose: () => void;
}

function resultMessage(state: BulkExportState): string {
  if (state.status === 'interrupted') {
    return 'Export interrupted by user';
  }
  return state.errors ? 'Export finished with errors' : 'Export successfully finished';
}

/**
 * The progress dialog of a bulk export.
 *
 * It is rendered into the document body rather than into the widget's shadow root: the dialog wears the
 * same micromodal styling as the export parameters dialog that opens it, and that stylesheet is put on
 * the page by the widget renderer, where the product's own dialogs need it too.
 */
export default function BulkExportProgressModal({ state, onStop, onClose }: Props) {
  if (state.status === 'closed') {
    return null;
  }

  const running = state.status === 'in-progress';
  const progress = state.rows.length === 0 ? 0 : Math.round((state.processed / state.rows.length) * 100);

  return createPortal(
    <div className="modal micromodal-slide is-open" id={POPUP_ID} aria-hidden="false">
      <div className="modal__overlay" tabIndex={-1}>
        <div
          id="bulk-pdf-export-popup"
          className="modal__container pdf-exporter"
          role="dialog"
          aria-modal="true"
          aria-labelledby={`${POPUP_ID}-title`}
        >
          <header className="modal__header">
            <h2
              className="modal__title"
              id={`${POPUP_ID}-title`}
              style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}
            >
              <span>{state.merge ? 'Bulk export to PDF (merging into single file)' : 'Bulk export to PDF'}</span>
            </h2>
          </header>
          <main className="modal__content">
            {state.rows.map((row, index) => (
              <div className={`export-item ${row.state}`} key={`${row.item.projectId}/${row.item.id}/${index}`}>
                <span className="icon">
                  <i className="fa" />
                  <span className="sbb-spinner" role="img" aria-label="In progress" />
                </span>
                <span className="title">
                  <span className="type">{itemTypeLabel(row.item)}</span>
                  <span className="name">{itemName(row.item)}</span>
                </span>
                {row.error && <div className="error-message">{row.error}</div>}
              </div>
            ))}
          </main>
          <footer className="modal__footer">
            {/* A single item needs no progress bar: its own row already shows the state */}
            {running && state.rows.length > 1 && (
              <div className="progress-bar">
                <span style={{ width: `${progress}%` }}>
                  {progress > 25 ? `${state.processed} out of ${state.rows.length} finished` : ''}
                </span>
              </div>
            )}
            {!running && (
              <span
                className={`result ${state.status === 'interrupted' ? 'interrupted' : 'finished'}${
                  state.status === 'finished' && state.errors ? ' with-errors' : ''
                }`}
              >
                {resultMessage(state)}
              </span>
            )}
            {running ? (
              <button
                id="bulk-stop-export-pdf"
                className="polarion-JSWizardButton-Primary action-button"
                onClick={onStop}
              >
                Stop
              </button>
            ) : (
              <button className="polarion-JSWizardButton" aria-label="Close this dialog window" onClick={onClose}>
                Close
              </button>
            )}
          </footer>
        </div>
      </div>
    </div>,
    document.body,
  );
}

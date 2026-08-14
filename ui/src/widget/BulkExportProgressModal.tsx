import { Modal } from '@grigoriev/react-sbb-polarion';
import type { BulkExportState } from './useBulkExport';
import { itemName, itemTypeLabel } from './useBulkExport';

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
 * The progress dialog of a bulk export: one row per selected item, its state as the run reaches it.
 *
 * It is RSP's shared `Modal`, rendered inside the widget's own shadow root - so it is styled by the
 * stylesheets that root carries and needs nothing on the report page around it. It used to be micromodal
 * markup portalled into the page body, because the export dialog it follows was the product's own and put
 * its stylesheet there; both dialogs are this app's now.
 *
 * The shared Modal owns its footer, so the progress bar and the outcome are the last thing in the body
 * rather than sitting next to the buttons. That footer always renders both of its buttons, while this dialog
 * offers exactly one at a time - Stop while the run is going, Close once it is over - so `widget.css` hides
 * the other one, keyed off the `running` / `done` class below. A `Modal` that could be told to show one
 * button would make that unnecessary; until then this is the one place that needs it.
 */
export default function BulkExportProgressModal({ state, onStop, onClose }: Readonly<Props>) {
  if (state.status === 'closed') {
    return null;
  }

  const running = state.status === 'in-progress';
  const progress = state.rows.length === 0 ? 0 : Math.round((state.processed / state.rows.length) * 100);

  return (
    <Modal
      open
      // A merge run says so in the title, the way the legacy dialog did: the whole selection becomes one file.
      title={state.merge ? 'Bulk export to PDF (merging into single file)' : 'Bulk export to PDF'}
      okText="Stop"
      cancelText="Close"
      onOk={running ? onStop : onClose}
      // The header's close button and Escape. Closing stops the run as well - a conversion already handed to
      // the server cannot be recalled, but nothing further is started.
      onCancel={onClose}
    >
      <div className={`bulk-export-progress ${running ? 'running' : 'done'}`} id="bulk-pdf-export-popup">
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

        <div className="bulk-export-outcome">
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
        </div>
      </div>
    </Modal>
  );
}

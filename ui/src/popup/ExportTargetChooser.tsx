import { useState } from 'react';
import type { ReactNode } from 'react';
import { Modal } from '@sbb-polarion/react-sbb-polarion';
import type { BulkExportTarget } from '../widget/exportTargets';

export interface ExportTargetChooserProps {
  /** The Bulk PDF Export widgets with rows selected. The chooser is only shown when there is one at least. */
  targets: BulkExportTarget[];
  /** The export dialog for the report itself, shown in place of the chooser when the report is picked. */
  report: ReactNode;
  onClose: () => void;
}

/** What a widget is called in the choice. A widget configured without a title still needs a name. */
const nameOf = (target: BulkExportTarget): string => target.title || 'the Bulk PDF Export widget';

/** "3 selected items from Test Runs", with the count read when the choice opens. */
export function describeTarget(target: BulkExportTarget): string {
  const count = target.selectedCount();
  return `${count} selected ${count === 1 ? 'item' : 'items'} from ${nameOf(target)}`;
}

/**
 * Asks what a report's own "Export to PDF" button exports: the report, or the rows selected in a Bulk PDF
 * Export widget on it.
 *
 * Shown only where a widget has a selection, since the selection is what says the user may mean it; a report
 * without one opens its export dialog straight away, as before. The selection is preselected for the same
 * reason. A widget picked here opens that widget's own export dialog, so a selection is exported the same
 * way whichever button started it - with the widget's progress dialog, its stop and its merge option.
 */
export default function ExportTargetChooser({ targets, report, onClose }: Readonly<ExportTargetChooserProps>) {
  /** Which option is picked: the index of a widget in `targets`, or -1 for the report. */
  const [picked, setPicked] = useState(0);
  const [reportChosen, setReportChosen] = useState(false);

  if (reportChosen) {
    return report;
  }

  const proceed = () => {
    if (picked < 0) {
      setReportChosen(true);
      return;
    }
    // Closed first: the widget's dialog opens in the widget's own root, not in this one.
    onClose();
    targets[picked].startExport();
  };

  return (
    <Modal open title="Export to PDF" okText="Continue" cancelText="Close" onOk={proceed} onCancel={onClose}>
      <div className="export-target-chooser">
        <p id="export-target-question">What do you want to export?</p>
        <div className="export-target-options" role="radiogroup" aria-labelledby="export-target-question">
          <label>
            <input type="radio" name="export-target" checked={picked < 0} onChange={() => setPicked(-1)} />
            This report
          </label>
          {targets.map((target, index) => (
            <label key={target.id}>
              <input type="radio" name="export-target" checked={picked === index} onChange={() => setPicked(index)} />
              {describeTarget(target)}
            </label>
          ))}
        </div>
      </div>
    </Modal>
  );
}

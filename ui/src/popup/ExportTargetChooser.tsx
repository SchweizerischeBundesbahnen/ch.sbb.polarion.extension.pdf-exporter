import { useState } from 'react';
import type { ReactNode } from 'react';
import { Modal } from '@sbb-polarion/react-sbb-polarion';
import type { BulkExportTarget } from '../widget/exportTargets';

export interface ExportTargetChooserProps {
  /** The Bulk PDF Export widgets with rows selected. The chooser is only shown when there is one at least. */
  targets: BulkExportTarget[];
  /** What the page itself is called in the choice: a report, or the test run a test run page shows. */
  pageLabel: string;
  /** The export dialog for the report itself, shown in place of the chooser when the report is picked. */
  report: ReactNode;
  onClose: () => void;
}

/** What a widget is called in the choice. A widget configured without a title still needs a name. */
const nameOf = (target: BulkExportTarget): string => target.title || 'the Bulk PDF Export widget';

/**
 * "3 selected items from Test Runs" per widget, with the counts read when the choice opens.
 *
 * The title is the type of what a widget lists, so two widgets over the same type share it. Those are told
 * apart by their order on the page, which is the order `targets` comes in: "(widget 1 of 2)".
 */
export function describeTargets(targets: BulkExportTarget[]): string[] {
  const sameName = (target: BulkExportTarget) => targets.filter((other) => nameOf(other) === nameOf(target));
  return targets.map((target) => {
    const count = target.selectedCount();
    const label = `${count} selected ${count === 1 ? 'item' : 'items'} from ${nameOf(target)}`;
    const namesakes = sameName(target);
    return namesakes.length > 1 ? `${label} (widget ${namesakes.indexOf(target) + 1} of ${namesakes.length})` : label;
  });
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
export default function ExportTargetChooser({
  targets,
  pageLabel,
  report,
  onClose,
}: Readonly<ExportTargetChooserProps>) {
  /** Which option is picked: the index of a widget in `targets`, or -1 for the report. */
  const [picked, setPicked] = useState(0);
  const [reportChosen, setReportChosen] = useState(false);

  if (reportChosen) {
    return report;
  }

  const labels = describeTargets(targets);

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
            {pageLabel}
          </label>
          {targets.map((target, index) => (
            <label key={target.id}>
              <input type="radio" name="export-target" checked={picked === index} onChange={() => setPicked(index)} />
              {labels[index]}
            </label>
          ))}
        </div>
      </div>
    </Modal>
  );
}

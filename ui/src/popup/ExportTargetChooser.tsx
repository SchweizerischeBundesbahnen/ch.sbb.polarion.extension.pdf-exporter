import { useState } from 'react';
import type { ReactNode } from 'react';
import { Modal } from '@sbb-polarion/react-sbb-polarion';
import type { OfferedWidget } from '../export/widgetExportTargets';
import type { BulkExportTarget } from '../widget/exportTargets';

export interface ExportTargetChooserProps {
  /** The Bulk PDF Export widgets with rows selected. */
  targets: BulkExportTarget[];
  /** The widgets of other extensions which offered to be exported alone. */
  widgets?: OfferedWidget[];
  /** What the page itself is called in the choice: a report, or the test run a test run page shows. */
  pageLabel: string;
  /** The export dialog shown in place of the chooser: for the report, or for the one widget of it given. */
  dialog: (widgetId?: string) => ReactNode;
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
 * "Only Timesheet" per widget offered to be exported alone. Widgets of the same title are told apart as the
 * Bulk PDF Export widgets are.
 */
export function describeWidgets(widgets: OfferedWidget[]): string[] {
  const name = (widget: OfferedWidget) => widget.title || 'the widget';
  return widgets.map((widget) => {
    const label = `Only ${name(widget)}`;
    const namesakes = widgets.filter((other) => name(other) === name(widget));
    return namesakes.length > 1 ? `${label} (widget ${namesakes.indexOf(widget) + 1} of ${namesakes.length})` : label;
  });
}

/** Which option is picked: the page, a Bulk PDF Export selection or a widget, by its index. */
type Pick = { kind: 'page' } | { kind: 'bulk'; index: number } | { kind: 'widget'; index: number };

const samePick = (first: Pick, second: Pick): boolean =>
  first.kind === second.kind && (first.kind === 'page' || first.index === (second as { index: number }).index);

/**
 * Asks what a report's own "Export to PDF" button exports: the report, or the rows selected in a Bulk PDF
 * Export widget on it, or one widget of another extension which offered itself (#1183).
 *
 * Shown only where there is something to choose; a report without either opens its export dialog straight
 * away, as before. A selection is preselected, since the selection is what says the user may mean it; the
 * report is preselected otherwise. A selection picked here opens its Bulk PDF Export widget's own export dialog,
 * so it is exported the same way whichever button started it - with the widget's progress dialog, its stop and
 * its merge option. A widget of another extension picked here opens the export dialog of the report, for that
 * widget only.
 */
export default function ExportTargetChooser({
  targets,
  widgets = [],
  pageLabel,
  dialog,
  onClose,
}: Readonly<ExportTargetChooserProps>) {
  const [picked, setPicked] = useState<Pick>(targets.length > 0 ? { kind: 'bulk', index: 0 } : { kind: 'page' });
  /** Where the dialog was chosen: undefined while choosing, null for the whole report, or a widget's ID. */
  const [chosen, setChosen] = useState<string | null>();

  if (chosen !== undefined) {
    return dialog(chosen ?? undefined);
  }

  const labels = describeTargets(targets);
  const widgetLabels = describeWidgets(widgets);

  const proceed = () => {
    if (picked.kind === 'page') {
      setChosen(null);
    } else if (picked.kind === 'widget') {
      setChosen(widgets[picked.index].widgetId);
    } else {
      // Closed first: the widget's dialog opens in the widget's own root, not in this one.
      onClose();
      targets[picked.index].startExport();
    }
  };

  const option = (pick: Pick, key: string, label: string) => (
    <label key={key}>
      <input type="radio" name="export-target" checked={samePick(picked, pick)} onChange={() => setPicked(pick)} />
      {label}
    </label>
  );

  return (
    <Modal open title="Export to PDF" okText="Continue" cancelText="Close" onOk={proceed} onCancel={onClose}>
      <div className="export-target-chooser">
        <p id="export-target-question">What do you want to export?</p>
        <div className="export-target-options" role="radiogroup" aria-labelledby="export-target-question">
          {option({ kind: 'page' }, 'page', pageLabel)}
          {targets.map((target, index) => option({ kind: 'bulk', index }, `bulk:${target.id}`, labels[index]))}
          {widgets.map((widget, index) =>
            option({ kind: 'widget', index }, `widget:${widget.widgetId}`, widgetLabels[index]),
          )}
        </div>
      </div>
    </Modal>
  );
}

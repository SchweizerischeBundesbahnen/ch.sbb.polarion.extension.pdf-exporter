import { Modal } from '@sbb-polarion/react-sbb-polarion';
import { type DiffCell, lineDiff, toRows } from '../services/lineDiff';

interface CompareWithDefaultProps {
  open: boolean;
  fields: ReadonlyArray<{ key: string; label: string }>;
  custom: Record<string, string>;
  builtIn: Record<string, string>;
  /** Heads the column of the built-in values, e.g. with the predefined template they are. "Default" without it. */
  defaultLabel?: string;
  onClose: () => void;
}

/** The number and the text of one side of a row; an empty side where the other one has a line this side has not. */
function Side({ cell }: Readonly<{ cell?: DiffCell }>) {
  return (
    <>
      <td className="line-number">{cell?.number ?? ''}</td>
      <td className={cell ? `diff-${cell.kind}` : 'diff-empty'}>{cell?.text ?? ''}</td>
    </>
  );
}

/**
 * The built-in values and the custom values side by side, field by field: the built-in ones on the left, the custom
 * ones on the right, with the lines only one side has highlighted. What the administrator changed and what a newer
 * built-in version brought both show as highlighted lines, and they read the way two files are compared.
 */
export default function CompareWithDefault({
  open,
  fields,
  custom,
  builtIn,
  defaultLabel = 'Default',
  onClose,
}: Readonly<CompareWithDefaultProps>) {
  return (
    <Modal open={open} title="Compare with default" okText="Close" onOk={onClose} onCancel={onClose}>
      <div className="compare-with-default">
        {open &&
          fields.map((field) => {
            const rows = toRows(lineDiff(builtIn[field.key] ?? '', custom[field.key] ?? ''));
            return (
              <section key={field.key}>
                <h3>{field.label}</h3>
                {rows.every((row) => row.left?.kind === 'same') ? (
                  <p className="no-differences">No differences.</p>
                ) : (
                  <div className="side-by-side">
                    <table>
                      <colgroup>
                        <col className="line-number" />
                        <col />
                        <col className="line-number" />
                        <col />
                      </colgroup>
                      <thead>
                        <tr>
                          <th colSpan={2}>{defaultLabel}</th>
                          <th colSpan={2}>Custom</th>
                        </tr>
                      </thead>
                      <tbody>
                        {rows.map((row, index) => (
                          <tr key={`${index}-${row.left?.number ?? ''}-${row.right?.number ?? ''}`}>
                            <Side cell={row.left} />
                            <Side cell={row.right} />
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            );
          })}
      </div>
    </Modal>
  );
}

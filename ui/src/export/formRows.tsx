import type { CSSProperties, ReactNode } from 'react';

/**
 * The rows an export form is built from.
 *
 * One row is a three-track grid - the checkbox gutter, the label, the control - so every checkbox, every
 * label text and every control of the whole form lands on the same three x positions. See
 * `export-form.css`; these components are what guarantees the markup those rules expect, which is why the
 * form does not write `<div className="property-wrapper">` by hand anywhere.
 *
 * The checkbox is a **sibling** of its label rather than a child of it, which is what gives it a gutter of
 * its own. `htmlFor` still makes the text toggle the switch.
 */

/**
 * Reserves a control's space while hiding it, which is how every optional value field of this form behaves:
 * `visibility` rather than `display`, so ticking a checkbox does not reflow the rows around it.
 */
const reserved = (shown: boolean): CSSProperties | undefined => (shown ? undefined : { visibility: 'hidden' });

const classes = (...names: (string | false | undefined)[]): string => names.filter(Boolean).join(' ');

export interface FieldCellProps {
  children: ReactNode;
  /** The cell takes whatever the label leaves, rather than its natural width. */
  grows?: boolean;
  /** The cell replaces the label rather than following it, starting where the label texts start. */
  wide?: boolean;
  /** `false` keeps the cell's space but hides it - see {@link reserved}. */
  shown?: boolean;
}

/**
 * The cell a control sits in. Always an element of its own: a `SearchableSelect` renders the native
 * `<select>` it upgrades *and* the visible dropdown next to it, and two grid items in one track would put
 * the second one on a row of its own.
 */
export function FieldCell({ children, grows, wide, shown = true }: Readonly<FieldCellProps>) {
  return (
    <div className={classes('field', grows && 'grows', wide && 'field-wide')} style={reserved(shown)}>
      {children}
    </div>
  );
}

interface RowBase {
  /** Row modifiers: `full-row` for a row that takes a line of its own, `sub-row` for one that belongs above. */
  className?: string;
  /** The id of the row element itself, where something needs to address the row rather than the control. */
  rowId?: string;
  children?: ReactNode;
}

export interface FieldRowProps extends RowBase {
  label: ReactNode;
  /** The control the label names. */
  labelFor: string;
  title?: string;
}

/** A row whose label names the control beside it. The gutter stays empty, so the label still lines up. */
export function FieldRow({ label, labelFor, title, className, rowId, children }: Readonly<FieldRowProps>) {
  return (
    <div className={classes('property-wrapper', className)} id={rowId}>
      <label htmlFor={labelFor} title={title}>
        {label}
      </label>
      {children}
    </div>
  );
}

export interface SwitchRowProps extends RowBase {
  /** The id of the checkbox, which is also what the label points at. */
  id: string;
  label: ReactNode;
  title?: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}

/** A row the user switches on, with whatever value it carries beside it. */
export function SwitchRow({
  id,
  label,
  title,
  checked,
  onChange,
  className,
  rowId,
  children,
}: Readonly<SwitchRowProps>) {
  return (
    <div className={classes('property-wrapper', className)} id={rowId}>
      <input id={id} type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      <label htmlFor={id} title={title}>
        {label}
      </label>
      {children}
    </div>
  );
}

export interface TextFieldRowProps {
  /** The id of the checkbox, which is also what the label points at. */
  id: string;
  label: ReactNode;
  checked: boolean;
  onChange: (checked: boolean) => void;
  /** The text field the switch reveals. Its space is kept while the switch is off. */
  children: ReactNode;
}

/**
 * One of the switches whose text is too long for the label column, so that all of them share a label track
 * of their own - see `.pdf-fields` in `export-form.css`. The row keeps no box: `display: contents` puts the
 * checkbox, the label and the field straight into that grid.
 */
export function TextFieldRow({ id, label, checked, onChange, children }: Readonly<TextFieldRowProps>) {
  return (
    <div className="pdf-field">
      <input id={id} type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      <label htmlFor={id}>{label}</label>
      <div className="field" style={reserved(checked)}>
        {children}
      </div>
    </div>
  );
}

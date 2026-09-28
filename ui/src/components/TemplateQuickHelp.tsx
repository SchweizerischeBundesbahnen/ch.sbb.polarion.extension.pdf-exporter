import type { ReactNode } from 'react';
import Placeholders from './Placeholders';

export interface TemplateQuickHelpProps {
  /** What the help explains, e.g. "How-to configure PDF cover page". */
  title: string;
  /** How the templates of this page are written. The variables table follows it. */
  children: ReactNode;
}

/**
 * The Quick Help of a page which edits templates: how they are written, then the variables they understand.
 *
 * The markup is the one `Localization` and `Webhooks` render, class names included, so the help reads the
 * same wherever it appears.
 */
export default function TemplateQuickHelp({ title, children }: Readonly<TemplateQuickHelpProps>) {
  return (
    <div className="quick-help">
      <h2 className="align-left">Quick Help</h2>
      <div className="quick-help-text">
        <h3>{title}</h3>
        {children}
        <Placeholders />
      </div>
    </div>
  );
}

/**
 * The two sentences every template of this extension shares: what is substituted, and that Velocity is
 * evaluated. `subject` names what carries the expressions, e.g. "Cover page".
 */
export function SubstitutionRules({ subject }: Readonly<{ subject: string }>) {
  return (
    <>
      <p>
        During PDF generation provided special variables and/or custom fields of a document will be replaced with actual
        values.
      </p>
      <p>
        {subject} can contain velocity expressions which will be evaluated during PDF generation. For example:{' '}
        <span className="monospace">$document.getId()</span>
      </p>
    </>
  );
}

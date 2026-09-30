import CustomTemplatesPage from '../components/CustomTemplatesPage';
import TemplateQuickHelp, { SubstitutionRules } from '../components/TemplateQuickHelp';

/** The six parts of the first page, which replace the other ones there once the first page is different. */
const FIRST_PAGE = {
  key: 'differentFirstPage',
  label: 'Different first page',
  tabLabel: 'First Page Templates',
  intro:
    'Here you can define the header and footer of the first page. They replace the custom ones there, and an empty part prints nothing.',
  fields: (['header', 'footer'] as const).flatMap((row) =>
    (['Left', 'Center', 'Right'] as const).map((column) => ({
      key: `firstPage${row === 'header' ? 'Header' : 'Footer'}${column}`,
      label: `First page ${row}'s ${column.toLowerCase()} part:`,
      language: 'velocity' as const,
      placeholder: `Enter template of first page ${row}'s ${column.toLowerCase()} part here`,
    })),
  ),
};

/**
 * PDF Exporter: Header and footer - the six cells printed on every page of the exported PDF, and six more for the
 * first page when it is different.
 */
export default function HeaderFooter() {
  return (
    <CustomTemplatesPage
      title="PDF Exporter: Header and Footer"
      feature="header-footer"
      defaultLabel="Use default header and footer"
      customLabel="Use custom header and footer"
      customIntro="Here you can define your custom header and footer. Choose them above to use them instead of the default ones."
      defaultIntro="Here are displayed default header and footer, which are used unless the custom ones are chosen above. They are displayed here only for informational purposes and can't be modified."
      emptyWarning="All parts of the custom header and footer are empty, so the exported PDF gets neither. Save anyway?"
      editorsClassName="three-across"
      optionalTemplates={FIRST_PAGE}
      fields={[
        {
          key: 'headerLeft',
          label: "Header's left part:",
          language: 'velocity',
          placeholder: "Enter template of header's left part here",
        },
        {
          key: 'headerCenter',
          label: "Header's center part:",
          language: 'velocity',
          placeholder: "Enter template of header's center part here",
        },
        {
          key: 'headerRight',
          label: "Header's right part:",
          language: 'velocity',
          placeholder: "Enter template of header's right part here",
        },
        {
          key: 'footerLeft',
          label: "Footer's left part:",
          language: 'velocity',
          placeholder: "Enter template of footer's left part here",
        },
        {
          key: 'footerCenter',
          label: "Footer's center part:",
          language: 'velocity',
          placeholder: "Enter template of footer's center part here",
        },
        {
          key: 'footerRight',
          label: "Footer's right part:",
          language: 'velocity',
          placeholder: "Enter template of footer's right part here",
        },
      ]}
      footer={
        <TemplateQuickHelp title="How-to configure PDF header and footer">
          <p>
            Header and footer divided into 3 parts: left, center and right sections. Each section can be configured
            using HTML, where you can insert special variables (upper case, exactly like in table below) and
            document&apos;s custom fields (case-sensitive custom field ID, exactly how it&apos;s configured in
            administration pane), both enclosed in double curly brackets, eg.:{' '}
            <span className="monospace">{'{{ DOCUMENT_TITLE }}'}</span> for special variables or{' '}
            <span className="monospace">{'{{ docRevision }}'}</span> for document&apos;s custom fields.
          </p>
          <p>
            Check <b>Different first page</b> under the custom header and footer to give the first page a header and
            footer of its own, on the tab <b>First Page Templates</b>. Its six parts replace the other ones on the first
            page, which gets no header or footer where they are empty. A cover page is a page of its own, so the first
            page is then the one after it.
          </p>
          <SubstitutionRules subject="Header and footer parts" />
        </TemplateQuickHelp>
      }
    />
  );
}

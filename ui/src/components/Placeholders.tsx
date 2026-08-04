/**
 * The special variables the templates understand. A reference table, identical on every page that
 * edits a template - which is why the JSP pages included one file and these pages render one
 * component.
 */
const PLACEHOLDERS: [string, React.ReactNode][] = [
  ['PROJECT_NAME', 'Project name'],
  ['DOCUMENT_ID', 'Document ID'],
  ['DOCUMENT_TITLE', 'Document title'],
  [
    'DOCUMENT_REVISION',
    <>
      If document is displayed in certain revision&nbsp;&ndash; this revision value. Otherwise, if a document has custom
      field <span className="monospace">docRevision</span>&nbsp;&ndash; values of this custom field.
      Otherwise&nbsp;&ndash; value of <span className="monospace">HEAD</span> revision.
    </>,
  ],
  [
    'DOCUMENT_FILTER',
    <>
      Filter (query) applied to the document during export. This is the value of the{' '}
      <span className="monospace">query</span> URL parameter. Empty string if no filter is applied.
    </>,
  ],
  ['REVISION', 'Document revision'],
  ['BASELINE_NAME', 'Baseline name'],
  ['REVISION_AND_BASELINE_NAME', 'Revision and baseline name'],
  ['PAGE_NUMBER', 'Page counter'],
  ['PAGES_TOTAL_COUNT', 'Total count of pages in the document'],
  ['PRODUCT_NAME', 'Product name, in common case it will be "Polarion"'],
  ['PRODUCT_VERSION', 'Polarion version'],
  ['TIMESTAMP', 'Date and time of PDF generation'],
];

export default function Placeholders() {
  return (
    <div className="placeholders">
      <h3>Supported special variables</h3>
      <table>
        <thead>
          <tr>
            <th>Variable</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          {PLACEHOLDERS.map(([name, description]) => (
            <tr key={name}>
              <td>
                <span className="monospace">{`{{ ${name} }}`}</span>
              </td>
              <td>{description}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

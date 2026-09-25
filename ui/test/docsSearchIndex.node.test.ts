import { describe, expect, it } from 'vitest';
import config from '../src/docs/docs.config.json';
import searchIndex from '../src/docs/search-index.json';

// Guards the build-generated documentation search index (scripts/build-docs-index.mjs). Runs in the node
// project since it is plain data, not a component.
//
// The index is generated, not committed: in the Maven build it is built from the rendered articles before the
// JS suite runs, so these assertions are real there. A plain `npm test` without the rendered articles gets the
// empty dev-mode index, and there is nothing to check - the suite is skipped rather than failing.

interface SearchRecord {
  doc: string;
  docTitle: string;
  anchor: string;
  title: string;
  text: string;
}

const RECORDS = searchIndex as SearchRecord[];
const DOC_IDS = new Set(config.items.map((i) => i.id));

describe.skipIf(RECORDS.length === 0)('documentation search index', () => {
  it('is non-empty and covers every manifest article', () => {
    expect(RECORDS.length).toBeGreaterThan(0);
    const docsWithRecords = new Set(RECORDS.map((r) => r.doc));
    for (const id of DOC_IDS) {
      expect(docsWithRecords.has(id)).toBe(true);
    }
  });

  it('references only manifest docs and carries a title per record', () => {
    for (const record of RECORDS) {
      expect(DOC_IDS.has(record.doc)).toBe(true);
      expect(record.title.length).toBeGreaterThan(0);
    }
  });

  it('uses slug-form anchors that match the rendered heading ids', () => {
    // Same shape HtmlProcessor.generateId produces: lowercase word chars and hyphens, no leading/trailing -.
    for (const record of RECORDS) {
      expect(record.anchor).toMatch(/^[a-z0-9]+(?:-[a-z0-9]+)*$/);
    }
  });

  it('carries the text of code blocks', () => {
    // A properties line exists only inside a <pre> of the articles. A parser setting that treated <pre> as
    // block text would drop every code block, and with them the property keys a reader searches for.
    const configuration = RECORDS.filter((r) => r.doc === 'configuration').map((r) => r.text);
    expect(configuration.some((text) => text.includes('pdf-exporter.weasyprint.service=http'))).toBe(true);
  });

  it('keeps the whole text of long sections', () => {
    // No section is cut short: a term further down a long section must stay searchable. Several sections of
    // the configuration reference run to thousands of characters; the former 400-character cap stayed below this.
    expect(Math.max(...RECORDS.map((r) => r.text.length))).toBeGreaterThan(1000);
  });
});

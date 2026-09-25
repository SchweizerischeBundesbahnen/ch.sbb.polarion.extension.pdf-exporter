// Builds the documentation-site search index by PARSING the HTML the markdown2html-maven-plugin renders for
// each article, rather than parsing the markdown here. The plugin owns the canonical heading-slug algorithm
// (ICU transliteration + the -2/-3 disambiguation) and stamps it onto the headings (generateHeadingIds); this
// reads those ids straight out of the HTML, so search anchors are identical to the ids the rendered headings
// carry, with no second slug implementation to drift.
//
// The plugin renders each article to the shipped `webapp/pdf-exporter-app/html/<id>.html` in generate-sources
// (before this frontend build, with the Table of contents excluded); this reads those same shipped files -
// there is no separate render - parsing each into one record per h2/h3 heading (its anchor id, title and the
// plain text beneath it up to the next heading) and concatenates them in the docs.config.json reading order
// into src/docs/search-index.json.
//
// Two modes:
//   --require (used by `prebuild`, i.e. the real Maven/production build): the rendered article MUST be present;
//      a missing article fails the build (exit 1) rather than silently shipping a stale index.
//   default  (used by `predev`/`pretest`/`pretypecheck`, i.e. runs without Maven): a missing article is a
//      warning and an EMPTY index is written, so a fresh clone still typechecks, runs and tests - the
//      documentation search is then simply hidden.
//
// src/docs/search-index.json is a pure build artifact and is not committed (.gitignore): App.tsx imports it
// statically, so every entry point that needs it (build, dev, test, typecheck) generates it first.
//
// The article directory defaults to the shipped webapp html (src/main/resources/webapp/pdf-exporter-app/html,
// where markdown2html renders the articles in generate-sources); override it with DOCS_SECTION_INDEX_DIR.
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse } from 'node-html-parser';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const uiDir = resolve(scriptDir, '..');
const repoRoot = resolve(uiDir, '..');
const config = JSON.parse(readFileSync(resolve(uiDir, 'src/docs/docs.config.json'), 'utf8'));
const strict = process.argv.includes('--require');
const indexDir = process.env.DOCS_SECTION_INDEX_DIR
  ? resolve(process.env.DOCS_SECTION_INDEX_DIR)
  : resolve(repoRoot, 'src/main/resources/webapp/pdf-exporter-app/html');
const outFile = resolve(uiDir, 'src/docs/search-index.json');

const MAX_TEXT = 400; // enough to search on; keeps the bundled index small

/** Collapse runs of whitespace and trim. */
const clean = (text) => text.replace(/\s+/g, ' ').trim();
/** Slice by code point, so a cap never splits a surrogate pair. */
const capText = (text) => [...text].slice(0, MAX_TEXT).join('');

/** One record per h2/h3 heading: its anchor id, title, and the plain text beneath it up to the next heading. */
function sectionsOf(html) {
  // Parse <pre>/<code> into elements (default treats <pre> as raw text, which would leak the syntax-highlight
  // <span> markup into `.text`); keep the real raw-text elements raw.
  const root = parse(html, { blockTextElements: { script: true, style: true, noscript: true, pre: false } });
  const elements = root.childNodes.filter((node) => node.nodeType === 1);
  const sections = [];
  let current = null;
  for (const element of elements) {
    const tag = element.tagName?.toLowerCase() ?? '';
    if (tag === 'h2' || tag === 'h3') {
      current = { anchor: element.getAttribute('id') ?? '', title: clean(element.text), body: [] };
      sections.push(current);
    } else if (/^h[1-6]$/.test(tag)) {
      current = null; // h1 / h4-h6 end the current section but are not indexed themselves
    } else if (current) {
      current.body.push(element.text);
    }
  }
  return sections.map((section) => ({
    anchor: section.anchor,
    title: section.title,
    text: capText(clean(section.body.join(' '))),
  }));
}

const missing = config.items.filter((item) => !existsSync(resolve(indexDir, `${item.id}.html`)));
if (missing.length > 0) {
  const message =
    `build-docs-index: no rendered article in ${indexDir} for ${missing.map((i) => i.id).join(', ')}. ` +
    'Run the Maven build (it renders them in generate-sources) before the frontend build.';
  if (strict) {
    console.error(message);
    process.exit(1);
  }
  console.warn(`${message} Writing an empty search-index.json (dev mode): the documentation search is hidden.`);
  writeFileSync(outFile, '[]\n', 'utf8');
  process.exit(0);
}

const records = [];
for (const item of config.items) {
  const html = readFileSync(resolve(indexDir, `${item.id}.html`), 'utf8');
  for (const section of sectionsOf(html)) {
    records.push({ doc: item.id, docTitle: item.title, anchor: section.anchor, title: section.title, text: section.text });
  }
}

writeFileSync(outFile, `${JSON.stringify(records, null, 2)}\n`, 'utf8');
console.log(`build-docs-index: built ${records.length} sections from ${config.items.length} docs into ${outFile}`);

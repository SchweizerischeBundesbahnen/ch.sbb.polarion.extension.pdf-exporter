// Checks the built bundles that Polarion imports by URL, which nothing else can check.
//
// Two of this app's three entries are not the SPA: they are ES modules loaded at runtime by
// server-rendered markup, by a fixed name, for a named export.
//
//   BulkPdfExportWidgetRenderer:
//     import('/polarion/pdf-exporter-app/ui/app/assets/bulk-widget.js?v=<version>')
//         .then(module => module.default('#<shim id>'))
//   webapp/pdf-exporter/html/sidePanelContent.html:
//     import('/polarion/pdf-exporter-app/ui/app/assets/side-panel.js')
//         .then(module => module.mountSidePanel('#pdf-exporter-panel'))
//
// So each emitted file must keep both its name and that export. The Vitest suites import the source
// modules instead of the build output, so neither would notice: a Vite app build drops entry signatures
// unless preserveEntrySignatures says otherwise, and that is exactly how the widget once shipped as
// "module.default is not a function" on a report page.
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const assets = resolve(dirname(fileURLToPath(import.meta.url)), '../dist/app/assets');

const ENTRIES = [
  {
    file: 'bulk-widget.js',
    exported: 'default',
    // Either form rollup may emit: `export default x` or `export{x as default}` in a list.
    present: (bundle) => /export\s+default\s/.test(bundle) || /export\s*\{[^}]*\bas default\b/.test(bundle),
    importer: 'BulkPdfExportWidgetRenderer calls module.default(selector)',
  },
  {
    file: 'side-panel.js',
    exported: 'mountSidePanel',
    // A named export survives either as a declaration or, after minification, as an alias in a list.
    present: (bundle) =>
      /export\s+(function|const|let|var)\s+mountSidePanel\b/.test(bundle) ||
      /export\s*\{[^}]*\bmountSidePanel\b/.test(bundle),
    importer: 'the side panel fragment calls module.mountSidePanel(selector)',
  },
];

const fail = (message) => {
  console.error(`[check-runtime-entries] ${message}`);
  process.exit(1);
};

for (const entry of ENTRIES) {
  const path = resolve(assets, entry.file);
  let bundle;
  try {
    bundle = readFileSync(path, 'utf8');
  } catch {
    fail(`${path} was not emitted. Its importer names it exactly, so it may not be hashed.`);
  }
  if (!entry.present(bundle)) {
    fail(
      `${entry.file} has no "${entry.exported}" export. ${entry.importer}, which would fail on the page ` +
        'with "is not a function". Check rollupOptions.preserveEntrySignatures.',
    );
  }
  console.log(`[check-runtime-entries] ${entry.file} is present and exports ${entry.exported}`);
}

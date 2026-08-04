// Checks the built bulk export widget bundle, which nothing else can check.
//
// BulkPdfExportWidgetRenderer loads the widget with
//     import('/polarion/pdf-exporter-app/ui/app/assets/bulk-widget.js?v=<version>')
//         .then(module => module.default('#<shim id>'))
// so the emitted file must keep both its name and its default export. The Vitest suites import the
// source module instead of the build output, so neither would notice: a Vite app build drops entry
// signatures unless preserveEntrySignatures says otherwise, and that is exactly how the widget once
// shipped as "module.default is not a function" on a report page.
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const entry = resolve(dirname(fileURLToPath(import.meta.url)), '../dist/app/assets/bulk-widget.js');

const fail = (message) => {
  console.error(`[check-widget-entry] ${message}`);
  process.exit(1);
};

let bundle;
try {
  bundle = readFileSync(entry, 'utf8');
} catch {
  fail(`${entry} was not emitted. The widget renderer imports it by that exact name, so it may not be hashed.`);
}

// Either form rollup may emit: `export default x` or `export{x as default}` in a list.
const exportsDefault = /export\s+default\s/.test(bundle) || /export\s*\{[^}]*\bas default\b/.test(bundle);
if (!exportsDefault) {
  fail(
    'the widget bundle has no default export. The renderer calls module.default(selector), which would ' +
      'fail on the page with "module.default is not a function". Check rollupOptions.preserveEntrySignatures.',
  );
}

console.log('[check-widget-entry] bulk-widget.js is present and exports a default');

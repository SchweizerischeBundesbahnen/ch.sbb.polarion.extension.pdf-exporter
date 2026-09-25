// Vitest globalSetup (vitest.config.ts). src/docs/search-index.json is a build artifact, not committed, and
// App.tsx imports it statically - so it must exist before any test file is transformed. Generating it here
// covers every way the suite is started (npm test, watch, coverage, the Docker runs, an IDE runner) without a
// pre-script per npm script. Without the Maven-rendered articles the script writes an empty index (the
// documentation search is then hidden). Kept under scripts/ with the other node scripts, outside the
// browser-typed tsconfig.
import { execFileSync } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

export default function setup() {
  const script = resolve(dirname(fileURLToPath(import.meta.url)), 'build-docs-index.mjs');
  execFileSync(process.execPath, [script], { stdio: 'inherit' });
}

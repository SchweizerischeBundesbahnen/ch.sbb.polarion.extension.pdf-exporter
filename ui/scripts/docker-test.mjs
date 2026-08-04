// Runs the test suite inside the pinned Playwright Docker image (Linux) so screenshots match the
// committed references - wrapping the long `docker run ...` command behind an npm script. Used by
// test:docker / test:update:docker / test:coverage:docker.
import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const uiDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');

// Which inner npm script to run in the container (default: test).
const script = process.argv[2] || 'test';

// Pin the image to the installed Playwright version so the container's browser + system deps match.
let playwrightVersion;
try {
  const pkg = JSON.parse(readFileSync(resolve(uiDir, 'node_modules/playwright/package.json'), 'utf8'));
  playwrightVersion = pkg.version;
} catch {
  console.error('Cannot read node_modules/playwright - run `npm install` first.');
  process.exit(1);
}
const image = `mcr.microsoft.com/playwright:v${playwrightVersion}-noble`;

const args = [
  'run',
  '--rm',
  // Marks this run as THE reference environment for the pixel comparisons. The committed screenshots
  // are locked to this image, so the visual tests assert only when this is set (see vitest.config.ts);
  // anywhere else they skip instead of failing on the host's font metrics.
  '-e',
  'PIXEL_REFERENCES=1',
  // Keeps corepack from announcing the npm download on stderr, which the Maven frontend plugin would
  // then print as an [ERROR] line in an otherwise clean build.
  '-e',
  'COREPACK_ENABLE_DOWNLOAD_PROMPT=0',
  '-v',
  `${uiDir}:/work`,
  // Shadow node_modules so the container's Linux install does not overwrite host binaries.
  '-v',
  '/work/node_modules',
  '-w',
  '/work',
  image,
  'bash',
  '-c',
  // The image's bundled npm is older than this project's `packageManager` pin, so `npm ci` used to warn
  // EBADENGINE on every run. corepack installs the pinned npm instead, which both silences the warning
  // and makes the container resolve the lock file with the very npm the developers use - about 4s.
  // Tolerated rather than required: a future image without corepack falls back to the bundled npm, and
  // says so, instead of failing the whole suite over the toolchain.
  `corepack enable npm || echo "corepack unavailable - using the image's bundled npm"; npm ci && npm run ${script}`,
];

console.log(`> docker ${args.join(' ')}`);
const result = spawnSync('docker', args, { stdio: 'inherit' });
if (result.error) {
  console.error(`Failed to launch docker: ${result.error.message}. Is Docker installed and running?`);
  process.exit(1);
}
process.exit(result.status ?? 1);

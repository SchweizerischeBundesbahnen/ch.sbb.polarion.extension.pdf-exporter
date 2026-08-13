import react from '@vitejs/plugin-react';
import { playwright } from '@vitest/browser-playwright';
import { defaultExclude, defineConfig } from 'vitest/config';

// Two projects, because this repository has two kinds of JavaScript to test.
//
//   browser - the React app in src/. Vitest browser mode (real Chromium via Playwright), the same
//             setup as react-sbb-polarion: behavior assertions see real CSS/layout and the visual layer
//             (toMatchScreenshot) captures the real look. The extension's REST calls are mocked at the
//             global fetch level (see test/mockFetch.ts), so no Polarion is needed. Reference
//             screenshots are committed and MUST be generated in the pinned Playwright Docker image
//             (npm run test:update:docker) so Windows-dev and Linux-CI produce identical pixels.
//
//   node    - the product injector scripts under src/main/resources/webapp/pdf-exporter/js/, which are
//             plain page scripts, not part of the app bundle. They used to have their own mocha suite,
//             their own package.json and their own node/ + node_modules/ at the repository root; that
//             whole second toolchain is gone and the tests live here.
//
//             jsdom, NOT browser mode, and the reason is specific: those scripts are written against the
//             TOP frame (top.document, top.__genericDleToolbarEnginePromise, top.__genericDleToolbarSeq).
//             Vitest browser mode runs each test file in an iframe and keeps `top` for its own runner
//             page, which is never reloaded between files - so the engine promise would leak across the
//             whole run and the injections would land in the runner's DOM instead of the test's. In
//             production these scripts run via scriptInjection.mainHead, where top === self, which is
//             what jsdom reproduces. The assertions are element ids, markup and a promise race: no
//             layout, no CSS, no paint, so browser mode would add the frame problem and no signal.

// Per-component subfolder derived from the test file name (e.g. "Panel.visual.test.tsx" -> "Panel").
const componentDir = (testFileName: string): string => testFileName.split(/[\\/]/).pop()!.split('.')[0];

// The committed reference screenshots are pixel-locked to the pinned Playwright image, so the visual
// assertions are meaningful only there. scripts/docker-test.mjs sets PIXEL_REFERENCES=1 inside the
// container; everywhere else (a developer's macOS/Windows box, a plain CI runner) the visual suites
// skip themselves rather than failing on the host's font metrics - which shift both the antialiasing
// and the rendered element height, i.e. a red run that says nothing about the code.
const pixelReferences = process.env.PIXEL_REFERENCES === '1';

// The node project's files. Named so the browser project can exclude exactly them and nothing else.
const NODE_TESTS = 'test/**/*.node.test.ts';

export default defineConfig({
  define: { __PIXEL_REFERENCES__: JSON.stringify(pixelReferences) },
  plugins: [react()],
  // Resolve React to this app's single instance, mirroring vite.config.js. Redundant while RSP is
  // consumed as a published tarball (React is a peer dependency there), but required the moment the
  // dependency is temporarily pointed at a local RSP checkout to iterate - otherwise the app and the
  // linked library get two Reacts and every hook throws "invalid hook call".
  resolve: { dedupe: ['react', 'react-dom', 'sonner'] },
  // Pre-bundle these so Vite does not discover a new dependency mid-run and reload the browser page
  // (which intermittently fails a test file with "Vitest failed to find the runner"). Matters most on
  // a fresh `npm ci` in Docker where there is no warm dep-optimize cache.
  optimizeDeps: {
    include: [
      'react',
      'react-dom',
      'react-dom/client',
      'react/jsx-runtime',
      'react/jsx-dev-runtime',
      'vitest-browser-react',
      'sonner',
      '@grigoriev/react-sbb-polarion',
    ],
  },
  test: {
    // `extends: true` on both projects: they inherit the Vite config above (plugins, resolve,
    // optimizeDeps, define) and override only what differs.
    projects: [
      {
        extends: true,
        test: {
          name: 'browser',
          include: ['test/**/*.{test,spec}.{ts,tsx}'],
          // defaultExclude first, or naming an exclude here would drop node_modules/dist from it.
          exclude: [...defaultExclude, NODE_TESTS],
          setupFiles: ['./test/setup.ts'],
          // Insulate the suite from a developer's `ui/.env.local`. Vite loads that file in test mode too,
          // so a personal VITE_BEARER_TOKEN would otherwise switch useRemote to the /api base and add an
          // Authorization header, which is not the session-auth default the tests are written against.
          // Empty, not removed, so `vi.stubEnv('VITE_BEARER_TOKEN', ...)` still works where a test wants
          // a token. (vite.config.js guards the shipped bundle the same way, with `define`.)
          env: { VITE_BEARER_TOKEN: '' },
          // Run test files one at a time. Under high parallelism the Playwright browser provider
          // intermittently fails a worker with "Vitest failed to find the runner"; serializing the files
          // avoids that race. The suite is small and each file is fast, so the cost is minor.
          fileParallelism: false,
          browser: {
            enabled: true,
            // deviceScaleFactor: 2 -> all visual-regression references are captured at 2x (sharper, and
            // finer diffs). Set on the provider's contextOptions (not the instance - the provider reads
            // it there).
            // `ignoreDefaultArgs: ['--hide-scrollbars']` because Playwright passes that flag to headless
            // Chromium by default, and a hidden scrollbar takes no width. The export dialog once shipped
            // with its two columns wrapping into one on a real Polarion, for want of the ~15px a scrollbar
            // takes, and every reference screenshot stayed green. Scrollbars are real here now, so the
            // three references that scroll show one - and the layout is held to it.
            provider: playwright({
              contextOptions: { deviceScaleFactor: 2 },
              launchOptions: { ignoreDefaultArgs: ['--hide-scrollbars'] },
            }),
            headless: true,
            instances: [{ browser: 'chromium', viewport: { width: 1280, height: 720 } }],
            expect: {
              toMatchScreenshot: {
                resolveScreenshotPath: ({ root, arg, ext, testFileName }) =>
                  `${root}/test/expected/${componentDir(testFileName)}/${arg}${ext}`,
                resolveDiffPath: ({ root, arg, ext, testFileName }) =>
                  `${root}/test/__diff__/${componentDir(testFileName)}/${arg}${ext}`,
              },
            },
          },
        },
      },
      {
        extends: true,
        test: {
          name: 'node',
          include: [NODE_TESTS],
          environment: 'jsdom',
          // No setupFiles: test/setup.ts loads stylesheets and jest-dom matchers for the React suite,
          // and the injectors need neither.
        },
      },
    ],
    coverage: {
      // istanbul (source instrumented at transform time), NOT v8: in browser mode v8 intermittently
      // reports 0% depending on the dep-optimization cache.
      provider: 'istanbul',
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: './coverage',
      // `include` also pulls untested files into the report (Vitest 4 runs the uncovered-files pass
      // whenever it is set), so an unimported source file cannot silently pass the threshold gate. It
      // replaced `coverage.all`, which Vitest 4 dropped from the option type: the `all: false` that used to
      // sit here did nothing at runtime - the istanbul provider never reads it - and only showed up as a
      // type error in the IDE. `npm run typecheck` does not see it either way: tsconfig covers `src` and
      // `test`, so this config file is not part of the program.
      // Scope is this app's src/ only, so the injector scripts the node project covers (which live
      // outside ui/) do not enter the report or move the gate - exactly as under the old mocha suite,
      // which measured no coverage at all.
      include: ['src/**'],
      // Excluded: the app bootstrap (main.tsx), declaration files and CSS, plus the dev-only Landing
      // page (`vite dev` scaffolding never opened in Polarion; the router test covers its selection
      // logic). Do NOT exclude real product code to hit the gate.
      exclude: ['src/**/*.d.ts', 'src/**/*.css', 'src/main.tsx', 'src/pages/Landing.tsx'],
      // Uniform 80% gate on all four metrics.
      thresholds: {
        statements: 80,
        functions: 80,
        lines: 80,
        branches: 80,
      },
    },
  },
});

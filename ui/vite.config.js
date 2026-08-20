import { copyFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// react-sbb-polarion emits the DLE toolbar engine as its own classic script: it runs in Polarion's
// document-editor iframe, driving the shell window, so it is not part of this app's bundle. The
// extension serves it - copied into the built app's assets/ for a build (the one path this app's
// web.xml serves without authentication), and answered from the package under
// `vite dev`, where nothing is built. See "Shell scripts" in the library's README.
const RSP_SHELL_SCRIPTS = ['dle-toolbar-starter.js'];

function copyRspShellScripts() {
  return {
    name: 'copy-rsp-shell-scripts',
    writeBundle(options) {
      const require = createRequire(import.meta.url);
      for (const name of RSP_SHELL_SCRIPTS) {
        copyFileSync(require.resolve(`@sbb-polarion/react-sbb-polarion/${name}`), `${options.dir}/assets/${name}`);
      }
    },
  };
}

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const polarionUrl = env.VITE_BASE_URL || 'http://localhost';

  // Dedupe so the app and @sbb-polarion/react-sbb-polarion resolve to this app's single instance of
  // each: React (two copies mean "invalid hook call") and sonner (the RSP `Toaster` host and the
  // toasts RSP components fire must share one instance, or the toasts never reach the host).
  const resolve = { dedupe: ['react', 'react-dom', 'sonner'] };

  if (command === 'serve') {
    return {
      plugins: [react(), copyRspShellScripts()],
      resolve,
      server: {
        proxy: {
          // The extension's own webapp context: its REST API, which the About page reads.
          '/polarion/pdf-exporter/rest': {
            target: polarionUrl,
            changeOrigin: true,
          },
          // The product JS the bulk export widget drives: the export parameters dialog and the
          // conversion protocol, which are served from the extension's own webapp, not from this app.
          '/polarion/pdf-exporter/ui': {
            target: polarionUrl,
            changeOrigin: true,
          },
          '/polarion/rest': {
            target: polarionUrl,
            changeOrigin: true,
          },
          '/polarion/ria': {
            target: polarionUrl,
            changeOrigin: true,
          },
          '/polarion/icons': {
            target: polarionUrl,
            changeOrigin: true,
          },
        },
      },
    };
  }

  return {
    plugins: [react(), copyRspShellScripts()],
    resolve,
    // Never let a developer's personal access token reach a shipped bundle. VITE_BEARER_TOKEN is a
    // `vite dev` convenience (it switches useRemote to the token-authenticated /api endpoints); Vite
    // inlines import.meta.env.VITE_* at build time, so a local .env.local would otherwise be baked
    // into the bundle that `mvn -P install-to-local-polarion` deploys, readable by everyone the SPA is
    // served to. Forcing it undefined here keeps production on the session-authenticated /internal
    // endpoints, which is what Polarion provides anyway.
    define: { 'import.meta.env.VITE_BEARER_TOKEN': 'undefined' },
    base: '/polarion/pdf-exporter-app/ui/app/',
    build: {
      outDir: './dist/app',
      emptyOutDir: true,
      rollupOptions: {
        // Keep what an entry exports. A Vite app build assumes its entries are only ever executed, so it
        // drops their exports - which leaves the widget bundle without the `default` the renderer's
        // `import(...).then(module => module.default(...))` calls, the side panel bundle without its
        // `mountSidePanel` and the popup bundle without its `openExportPopup`.
        // scripts/check-runtime-entries.mjs guards all three after every build, since no test sees the
        // built files.
        preserveEntrySignatures: 'strict',
        // Four entries: the admin SPA (index.html), the bulk export widget imported at runtime by the
        // widget renderer on a Polarion report page, the Document Properties side panel imported by the
        // form-extension fragment in the document editor, and the "Export to PDF" dialog imported by the
        // two toolbar injectors and the report page's export button.
        input: {
          index: fileURLToPath(new URL('index.html', import.meta.url)),
          'bulk-widget': fileURLToPath(new URL('src/widget/main.tsx', import.meta.url)),
          'side-panel': fileURLToPath(new URL('src/sidepanel/mount.tsx', import.meta.url)),
          'export-popup': fileURLToPath(new URL('src/popup/mount.tsx', import.meta.url)),
        },
        output: {
          // These three file names must stay predictable: their importers name them by URL and cannot know
          // the hash Vite would append. They append the extension version instead, which is what busts
          // the browser cache on an update.
          entryFileNames: (chunk) =>
            ['bulk-widget', 'side-panel', 'export-popup'].includes(chunk.name)
              ? `assets/${chunk.name}.js`
              : 'assets/[name]-[hash].js',
          // What the entries share (React above all) lands in one chunk. Rollup would name it after
          // whichever module it happened to pick, which reads as nonsense next to bulk-widget.js on a
          // report page.
          chunkFileNames: 'assets/shared-[hash].js',
        },
      },
    },
  };
});

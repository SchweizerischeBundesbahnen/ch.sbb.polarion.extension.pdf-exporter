import { fileURLToPath } from 'node:url';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const polarionUrl = env.VITE_BASE_URL || 'http://localhost';

  // Dedupe so the app and @grigoriev/react-sbb-polarion resolve to this app's single instance of
  // each: React (two copies mean "invalid hook call") and sonner (the RSP `Toaster` host and the
  // toasts RSP components fire must share one instance, or the toasts never reach the host).
  const resolve = { dedupe: ['react', 'react-dom', 'sonner'] };

  if (command === 'serve') {
    return {
      plugins: [react()],
      resolve,
      server: {
        proxy: {
          // Generic UI toolkit (SearchableDropdown JS + its CSS) served by GenericUiServlet. Served
          // unauthenticated in Polarion (see the pdf-exporter-app web.xml), so the dev proxy can fetch
          // it without a session.
          '/polarion/pdf-exporter-app/ui/generic': {
            target: polarionUrl,
            changeOrigin: true,
          },
          // The build-generated help articles served straight from the app webapp; the Usage
          // Disclaimer page reads disclaimer.html from here (there is no REST endpoint for it).
          '/polarion/pdf-exporter-app/ui/html': {
            target: polarionUrl,
            changeOrigin: true,
          },
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
    plugins: [react()],
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
        // `import(...).then(module => module.default(...))` calls, and the side panel bundle without its
        // `mountSidePanel`. scripts/check-runtime-entries.mjs guards both after every build, since no test
        // sees the built files.
        preserveEntrySignatures: 'strict',
        // Three entries: the admin SPA (index.html), the bulk export widget imported at runtime by the
        // widget renderer on a Polarion report page, and the Document Properties side panel imported by
        // the form-extension fragment in the document editor.
        input: {
          index: fileURLToPath(new URL('index.html', import.meta.url)),
          'bulk-widget': fileURLToPath(new URL('src/widget/main.tsx', import.meta.url)),
          'side-panel': fileURLToPath(new URL('src/formext/mount.tsx', import.meta.url)),
        },
        output: {
          // These two file names must stay predictable: their importers name them by URL and cannot know
          // the hash Vite would append. They append the extension version instead, which is what busts
          // the browser cache on an update.
          entryFileNames: (chunk) =>
            chunk.name === 'bulk-widget' || chunk.name === 'side-panel'
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

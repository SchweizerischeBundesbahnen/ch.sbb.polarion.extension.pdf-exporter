import { copyFileSync, existsSync, readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// react-sbb-polarion emits the DLE toolbar engine as its own classic script: it runs in Polarion's
// document-editor iframe, driving the shell window, so it is not part of this app's bundle. The
// extension serves it, copied into the built app's assets/ - the one path this app's web.xml serves
// without authentication. There is no `vite dev` counterpart, and none is needed: the scripts that
// request the engine are injected into Polarion's own pages through scriptInjection and ask for an
// absolute /polarion/... URL, which the dev server is never in the path of. See "Shell scripts" in the
// library's README.
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

// Where the Maven build renders the help articles (markdown2html), next to the app bundle in the webapp.
const RENDERED_ARTICLES = fileURLToPath(
  new URL('../src/main/resources/webapp/pdf-exporter-app/html/', import.meta.url),
);

// `vite dev` counterpart of the webapp's html/: a documentation page fetches `../../html/<id>.html` relative to
// the app, which under the dev server's root is `/html/<id>.html`. Without this Vite's SPA fallback answers that
// with index.html and a 200, so the page shows an empty article instead of the article or its "not generated"
// message. A file the Maven build has not rendered yet answers 404, which is what the page reports as not
// generated. Only a flat `<name>.html` is served, so a request cannot reach outside the directory.
function serveRenderedArticles() {
  return {
    name: 'serve-rendered-articles',
    configureServer(server) {
      server.middlewares.use('/html', (req, res) => {
        let name = '';
        try {
          name = decodeURIComponent((req.url ?? '').split('?')[0]).replace(/^\//, '');
        } catch {
          // a malformed escape: no such article
        }
        const file = join(RENDERED_ARTICLES, name);
        if (!/^[\w-]+\.html$/.test(name) || !existsSync(file)) {
          res.statusCode = 404;
          res.end();
          return;
        }
        res.setHeader('Content-Type', 'text/html; charset=utf-8');
        res.end(readFileSync(file));
      });
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
      plugins: [react(), serveRenderedArticles()],
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
    // into the bundle that `mvn -P local-install-into-polarion` deploys, readable by everyone the SPA is
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
        // Polarion serves /polarion/... at runtime, e.g. the fonts and images RSP's CSS references.
        // Marking them external tells Vite to leave them as they are without a warning per URL.
        external: [/^\/polarion\//],
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
          // the hash Vite would append. They append a cache key instead: the Java renderers the extension
          // version and a hash of the module (BundleCacheKey), the toolbar injectors a per-page-load timestamp.
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

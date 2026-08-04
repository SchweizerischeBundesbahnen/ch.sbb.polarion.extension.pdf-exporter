# PDF Exporter UI

A React + Vite single-page app on [react-sbb-polarion](https://github.com/grigoriev/react-sbb-polarion)
(RSP), served from the `pdf-exporter-app` webapp.

It replaced the JSP administration UI **page by page**, and every administration entry of the extension
is now served from here. The `pdf-exporter-admin` webapp is gone; its menu icons moved into this
webapp's `images/`, which is where `hivemodule.xml` and the report widgets point.

## Feature routing

There is one `index.html` / bundle. The page to render is chosen from the `feature` query parameter:

- `/` (no param) renders a development landing page listing every feature.
- `/?feature=about` - About (RSP's shared `About`).
- `/?feature=disclaimer` - Usage Disclaimer. The only page whose article is **not** a REST call:
  generic serves `/readme` and `/user-guide` but has no disclaimer endpoint, so `disclaimer.html` is
  read as a static file from this app's own webapp, where markdown2html writes it during the build.
- `/?feature=user-guide` - User Guide (RSP's shared `UserGuide`).
- `/?feature=css` - CSS, `/?feature=cover-page` - Cover Page, `/?feature=header-footer` - Header and
  Footer, `/?feature=filename` - Filename template. The four editor pages: RSP's `CodeEditor` over the
  named settings, with the built-in values shown read-only on a second tab. The last three share
  `components/CustomTemplatesPage.tsx`, since they differ only in their fields and their copy.
- `/?feature=style-package` - Style Packages. The widest settings page of the extension: the ~30 export
  switches of one named package, plus the four child settings it points at (cover page, CSS,
  header/footer, localization) and the optional webhooks. It lists their names from
  `settings/<child>/names` and marks the ones of a parent scope `(inherited)`, the way
  `ConfigurationsPane` marks its own. The "Use webhooks" row is there only while `/webhooks/status`
  reports the feature on, and the role picker needs `link-role-names`; both failures are reported
  rather than shown as an empty dropdown.
- `/?feature=style-package-weights` - Style Package Weights (RSP's shared `StylePackageWeights` over
  this extension's `settings/style-package/weights` endpoint; the list is shared with docx-exporter).
- `/?feature=authorization` - Authorization (RSP's shared `AuthorizationSettings` over the
  `authorization` named setting). It reads generic's `/roles` endpoint, which is opt-in: the two roles
  controllers are registered in `PdfExporterRestApplication` for this page.
- `/?feature=bulk-widget` - the development harness of the Bulk PDF Export widget (see below).
- `/?feature=side-panel` - the development harness of the Document Properties side panel (see below).
  Together with `bulk-widget` these are the only two features no administration page points at.

Features are declared in [`src/features.tsx`](src/features.tsx). Add a page component under
`src/pages/`, register it there, and it appears on the landing page automatically. The ids must stay
in sync with the `pageUrl`s in `src/main/resources/META-INF/hivemodule.xml` — a mismatch shows up as a
blank page in Polarion and no test catches it.

## The three entries

`index.html` is the administration SPA. The other two are ES modules that server-rendered markup imports
at runtime, each built to a **fixed** name because its importer names it by URL and cannot know the hash
Vite would emit:

| Entry                   | Emitted as              | Imported by                                      | Export it is called through |
| ----------------------- | ----------------------- | ------------------------------------------------ | --------------------------- |
| `src/widget/main.tsx`   | `assets/bulk-widget.js` | `BulkPdfExportWidgetRenderer`                    | `default(selector)`         |
| `src/formext/mount.tsx` | `assets/side-panel.js`  | `webapp/pdf-exporter/html/sidePanelContent.html` | `mountSidePanel(selector)`  |

Both need `rollupOptions.preserveEntrySignatures: 'strict'` to keep that export, which a Vite app build
otherwise drops. Nothing in the Vitest suites sees the built files, so
[`scripts/check-runtime-entries.mjs`](scripts/check-runtime-entries.mjs) checks both after every build -
the widget once shipped as `module.default is not a function` on a report page for exactly this reason.

## The Bulk PDF Export widget

The widget a report page can carry is `src/widget/main.tsx`, built to the
fixed name `assets/bulk-widget.js`. `BulkPdfExportWidgetRenderer` renders a shim element carrying the
widget's resolved data set (signed, see `WidgetDescriptorSigner`) and imports that file, appending the
extension version to bust the browser cache — which is why the file name may not be hashed.

The widget mounts into a **shadow root** of the shim: the report page around it is Polarion's, so the
widget's own stylesheet (`src/widget/widget.css`) and RSP's tokens are injected into that root, and the
page's own stylesheets are cloned into it so that the table and the button keep the native look. The
rows come from `POST /widgets/bulk-export/items`, cells included: those are the HTML Polarion rendered
for each field.

Two things stay outside the shadow root, both because they are shared with the product's other export
surfaces: the export parameters dialog (`ExportPopup.js`, imported at runtime from the `pdf-exporter`
webapp) and the bulk progress dialog, which renders into the page body where the micromodal styling
the widget renderer puts on the page reaches it.

`/?feature=bulk-widget` mounts the very same widget against sample data, one button per state. The
export dialog itself needs a Polarion behind `VITE_BASE_URL`.

## The Document Properties side panel

The "PDF Exporter" pane of the document editor's Document Properties sidebar is `src/formext/`, built to
`assets/side-panel.js`. `PdfExporterFormExtension` contributes nothing but the fragment that imports it:
an empty `#pdf-exporter-panel` div plus a `<link>` to an empty `css/starter.css` whose `onload` fires the
import (an inline module `<script>` does not run inside a GWT-injected fragment).

It mounts into a **shadow root** of that div. The properties pane is one page shared by every extension's
panel, each possibly built against a different RSP version, so the isolation goes both ways:
`shadowMount.ts` injects RSP's stylesheet, a base-font rule (nothing inside a shadow root inherits the
page's font) and the panel's own `side-panel.css` into the root, and none of it can leak out. The
SearchableDropdown popup is shadow-aware and portals into the same root.

Everything the panel offers is read over REST from the endpoints the DLE toolbar's export popup has always
used: the suitable style packages, the child setting names, the link roles, the default file name, the
document language, the webhooks switch and the export permission. The server side used to substitute all
of that into the fragment's markup, which is why there is now one description of this form instead of two.
The trade is a short loading state, where the server-rendered panel arrived populated.

What the popup will reuse when it is migrated is kept out of the component: `exportForm.ts` (a style
package read into form state), `exportParams.ts` (form state into an export request), `validation.ts` and
`../services/stylePackage.ts` (the model plus the fixed option lists, shared with the Style Packages
administration page).

`/?feature=side-panel` is a **real** scenario rather than a set of stubs. It takes the project from the
scope the Overview page carries, lists that project's documents (`services/documents.ts`, following the
JSON:API pages up to a cap), and once one is picked it writes the Polarion editor hash that document is
opened at and mounts the panel with **no dependencies injected**. So the panel loads the product's export
JS, that JS reads the document out of the hash exactly as it does in the editor, and every REST call goes
to the real server. The pick is remembered in a cookie, keyed by project, and preselected next time.

That is the one thing the Vitest suites cannot cover — a real editor URL and the real endpoints behind it —
so the page needs `VITE_BASE_URL` for the proxy and `VITE_BEARER_TOKEN` for the platform API the document
list comes from. The panel's own states are covered offline and pixel-locked by
`test/SidePanel.visual.test.tsx`. Its fixture is `test/sidePanelSamples.ts`, which lives with the tests
because nothing in `src/` stubs the panel any more.

The legacy `js/modules/ExportPanel.js` is now unused; it is kept for one release as the revert path.

## Local development

No Polarion restart is needed to develop the UI:

```bash
cd ui
cp .env.local.template .env.local   # optional: VITE_BASE_URL / VITE_BEARER_TOKEN for real REST calls
npm install
npm run dev                          # http://localhost:5173/
```

REST calls are proxied to the Polarion instance in `VITE_BASE_URL`; a personal access token in
`VITE_BEARER_TOKEN` switches `useRemote` from the session `/internal` endpoints to the token `/api`
ones.

> **Stop the dev server before running a Maven build.** The build runs `npm ci`, which starts by
> deleting `node_modules`, and on Windows that fails with `EPERM (-4048)` while `vite` holds files
> there — leaving `node_modules` half-deleted, so the dev server and the next build both break until
> `npm ci` is run again. `npm ci` is not covered by `-DskipJsTests`, so this applies to every Maven
> goal from `compile` upwards.

## Running the tests

**One command, locally and in CI: `npm run test:coverage:docker`.** It runs the full suite (behavior +
visual regression) plus the 80% istanbul coverage gate inside the pinned Playwright Docker image,
which is what the Maven `test` phase and the pre-commit hook execute. Docker must be running.

The image's bundled npm is older than the `packageManager` pin, so the container installs the pinned
npm through corepack before `npm ci`. That costs a few seconds and one download per run, and it is why
`npm ci` no longer warns `EBADENGINE`.

```bash
npm run test:coverage:docker   # the canonical run: full suite + coverage gate, in the pinned image
npm run test:coverage          # fast local loop: behavior only + the gate, no Docker, no pixels
npm run test:update:docker     # regenerate the committed reference PNGs after an intentional UI change
```

> Do **not** run `npm run test:coverage:full` directly outside a container. It is the inner command the
> Docker wrapper invokes; the visual suites detect that they are not in the reference environment and
> skip themselves, so a run there proves nothing about the screenshots.

## Formatting & linting

```bash
npm run format          # Prettier: format every file in place
npm run format:check    # Prettier: check only (what pre-commit / CI runs)
npm run lint            # ESLint: report problems
npm run lint:fix        # ESLint: auto-fix what it can
```

The repo's pre-commit hooks run `format:check`, `lint` and the dockerized coverage suite on any change
under `ui/`. They are check-only and never modify your files.

## Production build

`npm run build` emits all three entries to `ui/dist/app` with base path
`/polarion/pdf-exporter-app/ui/app/`. The Maven build (frontend-maven-plugin +
maven-resources-plugin) runs this automatically and copies the bundle into
`src/main/resources/webapp/pdf-exporter-app/app`, where `PdfExporterAppServlet` serves it at
`/polarion/pdf-exporter-app/ui/app/index.html`.

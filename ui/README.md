# PDF Exporter UI

A React + Vite single-page app on [react-sbb-polarion](https://github.com/SchweizerischeBundesbahnen/react-sbb-polarion)
(RSP), served from the `pdf-exporter-app` webapp.

It replaced the JSP administration UI **page by page**, and every administration entry of the extension
is now served from here. The `pdf-exporter-admin` webapp is gone; its menu icons moved into this
webapp's `images/`, which is where `hivemodule.xml` and the report widgets point.

## Feature routing

There is one `index.html` / bundle. The page to render is chosen from the `feature` query parameter:

- `/` (no param) renders a development landing page listing every feature.
- `/?feature=about` - About (RSP's shared `About`).
- `/?feature=disclaimer` - Usage Disclaimer. Reads the build-generated DISCLAIMER article from
  generic's `/disclaimer` endpoint, the same way About and User Guide read theirs. An empty response
  means the extension ships no disclaimer; the page then links to the online source.
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
- `/?feature=export-popup` - the development harness of the "Export to PDF" dialog (see below). Together
  with the two above, these are the only three features no administration page points at.

Features are declared in [`src/features.tsx`](src/features.tsx). Add a page component under
`src/pages/`, register it there, and it appears on the landing page automatically. The ids must stay
in sync with the `pageUrl`s in `src/main/resources/META-INF/hivemodule.xml` — a mismatch shows up as a
blank page in Polarion and no test catches it.

## The four entries

`index.html` is the administration SPA. The other three are ES modules that server-rendered markup imports
at runtime, each built to a **fixed** name because its importer names it by URL and cannot know the hash
Vite would emit:

| Entry                     | Emitted as               | Imported by                                                                            | Export it is called through |
| ------------------------- | ------------------------ | -------------------------------------------------------------------------------------- | --------------------------- |
| `src/widget/main.tsx`     | `assets/bulk-widget.js`  | `BulkPdfExportWidgetRenderer`                                                          | `default(selector)`         |
| `src/sidepanel/mount.tsx` | `assets/side-panel.js`   | `webapp/pdf-exporter/html/sidePanelContent.html`                                        | `mountSidePanel(selector)`  |
| `src/popup/mount.tsx`     | `assets/export-popup.js` | `webapp/pdf-exporter/js/starter.js`, `js/live-reports.js`, `ExportToPdfButtonRenderer` | `openExportPopup(options)`  |

All three need `rollupOptions.preserveEntrySignatures: 'strict'` to keep that export, which a Vite app
build otherwise drops. Nothing in the Vitest suites sees the built files, so
[`scripts/check-runtime-entries.mjs`](scripts/check-runtime-entries.mjs) checks all three after every
build - the widget once shipped as `module.default is not a function` on a report page for exactly this
reason.

## The shared export model

Three surfaces export: the toolbar dialog, the Document Properties side panel and the bulk export widget.
What they share is [`src/export/`](src/export/) plus two services:

| Module                       | What it holds                                                                          |
| ---------------------------- | -------------------------------------------------------------------------------------- |
| `export/documentType.ts`     | Which rows a document type shows, and which of them the request carries.                |
| `export/exportForm.ts`       | A style package read into form state.                                                   |
| `export/exportParams.ts`     | Form state turned into an export request.                                               |
| `export/exportData.ts`       | The REST reads each dialog needs before it can be shown.                                |
| `export/validation.ts`       | The three fields a user can get wrong.                                                  |
| `services/exportContext.ts`  | Where the item is, read out of the Polarion location hash.                              |
| `services/conversion.ts`     | Submit a conversion job, poll it, download the result; test run and collection extras.  |

The last two were `webapp/pdf-exporter/js/modules/ExportContext.js` and `ExportParams.js`, loaded at
runtime from the other webapp by whichever surface needed them. Nothing loads them any more and
`js/modules/` is gone; what remains in that webapp is the three injector scripts, the empty
`css/starter.css` trigger, and the three HTML templates the Java renderer reads server-side.
`css/pdf-exporter.css` is gone too: the injectors put no stylesheet on the page any more.

`export/documentType.ts` deliberately answers two questions rather than one. The legacy popup showed a
handful of rows for a baseline collection - fit to page, mark referenced work items, the two "cut empty"
switches, localize enums - whose export request then left every one of them out. That divergence is
transcribed as data (`VISIBLE_FOR` vs `SENT_FOR`) and asserted in `test/documentType.test.ts`, so it stays a
decision instead of becoming a regression.

## The "Export to PDF" dialog

`src/popup/` is the dialog four surfaces open: the document editor toolbar button, the Live Report toolbar
button, the "Export to PDF Button" report widget, and the Bulk PDF Export widget. The first three import
`assets/export-popup.js` and call `openExportPopup({documentType})`; the widget is part of this app and
renders `ExportPopupModal` directly, because it has a React tree to render it into and a progress dialog to
hand the chosen parameters to.

The chrome is RSP's shared `Modal` - a native `<dialog>`, so the top layer, the backdrop and Escape come for
free. That replaced micromodal: `openExportPopup` appends a host to the page body, mounts into a **shadow
root** of it with RSP's stylesheet and `src/popup/export-popup.css` injected, and removes the host on close.
Nothing is put on the page for it any more, which is why `starter.js` and `live-reports.js` no longer inject
the micromodal library and the six generic control stylesheets, and why `BulkPdfExportWidgetRenderer` no
longer inlines four stylesheets next to its shim.

Two details of `export-popup.css` are worth knowing. It raises the shared modal's `max-width` past its 640px
cap, because this form is two 340px columns - keyed on `.pdf-export-form` so that the same stylesheet,
injected next to the progress dialog in the widget's shadow root, does not resize that one. And the optional
value fields are hidden with `visibility` rather than removed, which is what the legacy popup did: ticking a
checkbox must not reflow the column around it.

`/?feature=export-popup` opens the dialog through the **real** `openExportPopup` against a **real** item:
pick a document, a document type and an export type, and the harness writes the Polarion hash that item is
opened at. A test run and a baseline collection are addressed by an id rather than by a space and a name, so
the harness asks for that id and writes the hash shape they really have - feeding a test run type a
document's path is a combination no endpoint accepts, and not a state the dialog can be in. A bulk export
shows the parameters it hands back instead of running one.

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

Both dialogs the widget owns are inside that shadow root too: the export parameters dialog
(`src/popup/ExportPopupModal.tsx`, rendered directly rather than imported at runtime) and the progress
dialog, which is RSP's shared `Modal` styled by `widget.css`. Both used to be micromodal markup in the
report page's body, styled by stylesheets the renderer inlined next to the shim; nothing is inlined there
now.

The progress dialog offers exactly one action at a time - Stop while the run is going, Close once it is
over - while the shared `Modal` always renders both of its footer buttons, so `widget.css` hides the one
that does not apply. A `Modal` that could be told to render a single button would replace that; it is the
only place in the extension that needs it.

`/?feature=bulk-widget` mounts the very same widget against sample data, one button per table state. Its two
dialogs are reached through it rather than rendered standalone, since both are styled by that shadow root;
the export dialog needs a Polarion behind `VITE_BASE_URL`, and the progress dialog's own states are
pixel-locked in `test/BulkExportWidget.visual.test.tsx`.

## The Document Properties side panel

The "PDF Exporter" pane of the document editor's Document Properties sidebar is `src/sidepanel/`, built to
`assets/side-panel.js`. `PdfExporterFormExtension` contributes nothing but the fragment that imports it:
an empty `#pdf-exporter-panel` div plus a `<link>` to an empty `css/starter.css` whose `onload` fires the
import (an inline module `<script>` does not run inside a GWT-injected fragment).

It mounts into a **shadow root** of that div. The properties pane is one page shared by every extension's
panel, each possibly built against a different RSP version, so the isolation goes both ways:
`services/shadowMount.ts` (shared with the export dialog) injects RSP's stylesheet, a base-font rule
(nothing inside a shadow root inherits the page's font) and the panel's own `side-panel.css` into the
root, and none of it can leak out. The
SearchableDropdown popup is shadow-aware and portals into the same root.

Everything the panel offers is read over REST from the endpoints the DLE toolbar's export popup has always
used: the suitable style packages, the child setting names, the link roles, the default file name, the
document language, the webhooks switch and the export permission. The server side used to substitute all
of that into the fragment's markup, which is why there is now one description of this form instead of two.
The trade is a short loading state, where the server-rendered panel arrived populated.

`/?feature=side-panel` is a **real** scenario rather than a set of stubs. It takes the project from the
scope the Overview page carries, lists that project's documents (`services/documents.ts`, following the
JSON:API pages up to a cap, behind the shared `components/DocumentPicker.tsx`), and once one is picked it
writes the Polarion editor hash that document is opened at and mounts the panel with **no dependencies
injected**. So the panel reads the document out of the hash exactly as it does in the editor, and every REST
call goes to the real server. The pick is remembered in a cookie, keyed by project, and preselected next
time.

That is the one thing the Vitest suites cannot cover — a real editor URL and the real endpoints behind it —
so the page needs `VITE_BASE_URL` for the proxy and `VITE_BEARER_TOKEN` for the platform API the document
list comes from. The panel's own states are covered offline and pixel-locked by
`test/SidePanel.visual.test.tsx`. Its fixture is `test/sidePanelSamples.ts`, which lives with the tests
because nothing in `src/` stubs the panel any more.

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
which is what the Maven `test` phase and CI execute. Docker must be running. It is also a pre-commit
hook; see [Formatting, linting & typechecking](#formatting-linting--typechecking).

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

### Two projects

`vitest.config.ts` declares two projects, and every command above runs both:

| Project | Environment | Files | Tests |
| --- | --- | --- | --- |
| `browser` | real Chromium via Playwright | `test/**/*.{test,spec}.{ts,tsx}` | this app, in `src/` |
| `node` | jsdom | `test/**/*.node.test.ts` | the product injector scripts in `src/main/resources/webapp/pdf-exporter/js/` |

Target one with `--project`, e.g. `npx vitest run --project node`.

The injectors are page scripts served from the `pdf-exporter` webapp, not part of this bundle. They had
their own mocha suite, `package.json` and `node_modules/` at the repository root; that second toolchain
is gone. They stay out of browser mode on purpose: the scripts drive the **top** frame, and Vitest
browser mode runs each test file in an iframe while keeping `top` for its own runner page, which is
never reloaded between files. Their import specifier must be written out in full at each call site - the
files sit outside the Vite root, and a specifier held in a variable resolves against the root instead.

Coverage is unaffected by the `node` project: the gate's `include` is `src/**`, relative to `ui/`.

## Formatting, linting & typechecking

```bash
npm run format          # Prettier: format every file in place
npm run format:check    # Prettier: check only (what pre-commit / CI runs)
npm run lint            # ESLint: report problems
npm run lint:fix        # ESLint: auto-fix what it can
npm run typecheck       # tsc --noEmit over src/ and test/
```

`typecheck` runs first in `npm run build`, so the Maven build fails on a type error rather than only the
IDE showing one. `tsconfig.json` covers `src` **and** `test`: a test is code, and while it was left out
three tests kept passing a `hostSelector` prop that `BulkExportWidget` had stopped declaring. The config
files themselves (`vite.config.js`, `vitest.config.ts`, `scripts/*.mjs`) are still outside the program.

The repo's pre-commit hooks run `format:check`, `lint` and the dockerized coverage suite on any change
under `ui/`. They are check-only and never modify your files. All use `language: system`, so run
`npm ci` in `ui/` before `pre-commit run -a`. Without it they fail with an npm error rather than a
lint finding.

The dockerized suite is the slow one: it needs Docker running and adds 30-60s+ to a UI commit. That is
the price of catching a broken suite or a coverage drop before the push rather than in CI, which runs
the same command.

## Production build

`npm run build` emits all four entries to `ui/dist/app` with base path
`/polarion/pdf-exporter-app/ui/app/`. The Maven build (the parent's `vite-ui` profile:
frontend-maven-plugin + maven-resources-plugin) runs this automatically and copies the bundle into
`src/main/resources/webapp/pdf-exporter-app/app`, where `PdfExporterAppServlet` serves it at
`/polarion/pdf-exporter-app/ui/app/index.html`.

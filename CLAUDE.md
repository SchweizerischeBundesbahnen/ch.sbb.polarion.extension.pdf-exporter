# CLAUDE.md

## Gotchas

- **A dependency can block Polarion startup through its manifest alone.** Polarion 2606 scans every
  nested jar and rejects both a class that references a forbidden package and a `META-INF/MANIFEST.MF`
  whose attribute value *contains* one, so an OSGi `uses:="javax.annotation.processing,..."` counts as
  `javax.annotation` and the whole extension is reported as "not Jakarta compatible".
  `polarion-compatibility-maven-plugin` fails the build on this at `verify`, so a green build means a
  server which boots. That is why `tika.version` stays on 3.x (`renovate.json` holds it there);
  tika-core 4.0.0 trips it. Keep the pin: the check turns a startup refusal into a red build, it does
  not make tika 4 usable.
- **`ch.sbb.polarion.extension.generic`** is the parent project providing reusable infrastructure for all Polarion plugins in this org (settings framework, REST base classes, OSGi helpers, etc.). Before implementing anything cross-cutting, check if it already exists there.
- **All administration pages are React now.** They were converted to
  [react-sbb-polarion](https://github.com/SchweizerischeBundesbahnen/react-sbb-polarion) one at a time, and
  `pdf-exporter-app` (the Vite bundle in `ui/`, see [`ui/README.md`](ui/README.md)) serves every one of
  them. `hivemodule.xml` carries a `pageUrl` per menu entry; the ids there must match
  `ui/src/features.tsx` - a mismatch is a blank page and no test catches it. The legacy
  `pdf-exporter-admin` webapp is gone: its menu icons moved to `webapp/pdf-exporter-app/images/`, so
  two webapps remain - `pdf-exporter` (REST + the toolbar injectors) and `pdf-exporter-app`.
- **`pdf-exporter-app` also serves three surfaces that are not administration pages**, each a Vite entry of
  its own with a **fixed** file name (their importers name them by URL) kept exporting by
  `preserveEntrySignatures: 'strict'` and guarded by `ui/scripts/check-runtime-entries.mjs`:
  - **Bulk PDF Export widget** - `BulkPdfExportWidgetRenderer` emits a shim on the report page and imports
    `assets/bulk-widget.js`, which mounts React into a shadow root of that shim. The rows come from
    `POST /widgets/bulk-export/items`, carrying the signed descriptor the renderer resolved - see
    `WidgetDescriptorSigner` for why it is signed. Its CSS is `ui/src/widget/widget.css`, which also styles
    the bulk progress dialog. The renderer puts **nothing** else on the page. Its container carries no
    `form-wrapper` on purpose - RSP scopes its control system to that class and the widget's markup is
    Polarion's own table - so the export dialog it renders is wrapped in one of its own, without which the
    same dialog looks different here than from a toolbar button.
  - **Document Properties side panel** - `PdfExporterFormExtension` emits only a fragment (an empty
    `#pdf-exporter-panel` div plus a `<link>` to `css/starter.css` whose `onload` fires the import) and
    `assets/side-panel.js` mounts React into a shadow root of it. It reads its data from the same internal
    REST endpoints the export dialog uses; the Java side substitutes nothing but the bundle version.
    Its CSS is the shared `ui/src/export/export-form.css` plus `ui/src/sidepanel/side-panel.css` for the
    pane's own chrome.
  - **"Export to PDF" dialog** - `assets/export-popup.js` exporting `openExportPopup({documentType})`,
    imported on click by `js/starter.js` (document editor toolbar), `js/live-reports.js` (report toolbar),
    `js/classic-wiki.js` (Classic Wiki toolbar) and `ExportToPdfButtonRenderer` (the report widget button).
    It appends its own host to the page body and mounts into a shadow root of it. The Bulk PDF Export
    widget is the fifth caller and renders `ExportPopupModal` directly instead, being part of the same
    app. Its CSS is the shared
    `ui/src/export/export-form.css` plus `ui/src/popup/export-popup.css` for the dialog's own chrome.

  Each shadow root carries its own CSS, so the extension now puts **no stylesheet on a Polarion page at
  all**. `css/pdf-exporter.css` is deleted and the injectors call no `injectStyle`; the toolbar buttons use
  Polarion's own classes plus generic's `css/dle-toolbar.css`. See [`ui/README.md`](ui/README.md) for the
  layering.
- **The report's own "Export to PDF" buttons see the Bulk PDF Export widgets through `window`.** Each widget
  registers its selection in `ui/src/widget/exportTargets.ts`, and `openExportPopup` for a `LIVE_REPORT`
  shows `ExportTargetChooser` first when one has rows selected. The list is a `window` property, not module
  state: `bulk-widget.js?v=` and `export-popup.js?timestamp=` are imported by different URLs, so a module they
  share is not guaranteed to be one instance. Picking a widget calls its own `startExport`, so a selection is
  always exported through the widget's own dialog and progress run.
- **A toast inside a shadow root needs its stylesheet brought in, and one host.** `sonner` (through RSP's
  `Toaster`) injects its CSS into `document.head` when its module loads, which none of the three
  shadow-mounted surfaces can see - so `ui/src/export/export-form.css` imports `sonner/dist/styles.css` and
  Vite inlines it into every root that carries the form. And `toast()` broadcasts to **every** mounted
  `Toaster`, while the side panel and the export dialog are both on the page whenever a document is open in
  the editor: `ui/src/components/ToastHost.tsx` is what makes the newest host the only one that renders. The
  dialog's host must be **inside** the `<dialog>`, the top layer painting above everything outside it, and
  the panel's outside its `<fieldset>`, which would otherwise disable the toast's own close button.
- **`webapp/pdf-exporter/js/modules/` is gone.** `ExportPopup.js`, `ExportPanel.js`, `ExportContext.js` and
  `ExportParams.js` were ported into the app: `ui/src/export/` (the shared export model **and the form
  itself** - which rows a document type shows, a style package read into a form, a form turned into a
  request, and `ExportFormView.tsx`, which the dialog and the side panel both render: they differ only in
  the chrome around it, and its layout follows the width it is given through a container query),
  `ui/src/services/exportContext.ts` (the location hash) and `ui/src/services/conversion.ts` (the convert-job
  protocol). Nothing is loaded across webapps at runtime any more. What is left in `webapp/pdf-exporter` is
  the four injector scripts, the empty `css/starter.css` trigger and the three HTML templates the Java
  renderer reads server-side (`sidePanelContent.html`, `pdfTemplate.html`, `headerAndFooter.html`).
- **A Classic Wiki page lives in an iframe no scriptInjection reaches.** Polarion renders it server-side
  into a same-origin `/polarion/wiki/bin/view/...` iframe, and the toolbar is inside it. So `js/classic-wiki.js`
  runs from `mainHead` and injects into that iframe from outside: a `load` listener per iframe (Polarion reloads
  it between wiki pages) and an observer for new ones (it is replaced when the user leaves the wiki). The click
  handler stays a function of the main page, so the dialog reads the main page's hash, which is what addresses
  the page.
- **The UI build comes from the generic parent**, activated by the presence of `ui/package.json` (its
  `ui-build-react-app` profile): `npm ci` + `npm run build`, the bundle copied into `webapp/pdf-exporter-app/`, and
  the JS suite in the Maven `test` phase. This pom adds nothing for it beyond pinning
  `frontend-maven-plugin.version`, which the parent's profile reads. Note it also redirects
  markdown2html's output (`about.html`, `user-guide.html`, `disclaimer.html`) into
  `webapp/pdf-exporter-app/html/`.
- **There is one JS toolchain, and it lives in `ui/`.** The root `package.json`, `package-lock.json`,
  `node/`, `node_modules/`, `src/test/js/` and this pom's own `frontend-maven-plugin` block are gone. The
  mocha suite that tested the toolbar injectors is now `ui/test/liveReportsInjector.node.test.ts`, run by
  the **`node`** project of `ui/vitest.config.ts` (jsdom) next to the **`browser`** project that tests the
  app. Injector tests must stay in the `node` project: those scripts drive the top frame, and Vitest
  browser mode runs each file in an iframe and keeps `top` for its own runner page. Name them
  `*.node.test.ts` - that suffix is what routes a file between the two projects.
- **A custom setting value remembers what it was copied from.** CSS, cover page, header and footer, and filename template
  store the hash of the built-in values a custom value was copied from (`defaultHash`) and compare it with the current built-in
  values on reading. A cover page also stores the predefined template it was copied from (`defaultSource`), so it is compared with that
  template only. That is how an unedited copy and a newer built-in version are recognized, so a change of
  `dle-pdf-export.css`, a cover page template or a Java default needs nothing else. Settings stored before carry no hash:
  `src/main/resources/default/legacy-built-in-values.json` lists the built-in values shipped until then. It is frozen, never
  add a new version to it.
- **Package naming**: Use `ch.sbb.polarion.extension.pdf_exporter` (underscore). Pre-v7.0.0 code used `pdf.exporter` (dot) — don't follow old patterns still present in the codebase.
- **Maven Settings**: Builds require `.mvn/settings.xml` (JFrog, GitHub Packages, Sonatype credentials via env vars). CI passes it with `-s .mvn/settings.xml`.
- **Polarion Dependencies**: You must extract dependencies from the Polarion installer using [polarion-artifacts-deployer](https://github.com/SchweizerischeBundesbahnen/polarion-artifacts-deployer) before the Maven build will work.
- **Local Polarion Installation**: Requires `POLARION_HOME` environment variable. Use the `local-install-into-polarion` Maven profile: `mvn clean install -P local-install-into-polarion`
- **After any code change**: Delete `<POLARION_HOME>/data/workspace/.config` before restarting Polarion or changes won't be picked up.
- **Remote Debugging**: Add to Polarion's `config.sh`: `JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"`
- **Logging**: Polarion logs: `<POLARION_HOME>/polarion/logs/main/*.log`
- **Branch conventions**: Conventional commits enforced by commitizen (pre-commit hook). Feature branches: `feature/<name>`, bug fixes: `fix/<name>`, LTS branches: `release-v*` (e.g., `release-v6`).
- **Pre-commit hooks block internal patterns**: some org-specific identifiers are treated as secrets. Run `pre-commit run -a` after implementation.

# CLAUDE.md

## Gotchas

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
    the bulk progress dialog. The renderer puts **nothing** else on the page.
  - **Document Properties side panel** - `PdfExporterFormExtension` emits only a fragment (an empty
    `#pdf-exporter-panel` div plus a `<link>` to `css/starter.css` whose `onload` fires the import) and
    `assets/side-panel.js` mounts React into a shadow root of it. It reads its data from the same internal
    REST endpoints the export dialog uses; the Java side substitutes nothing but the bundle version.
    Its CSS is `ui/src/sidepanel/side-panel.css`.
  - **"Export to PDF" dialog** - `assets/export-popup.js` exporting `openExportPopup({documentType})`,
    imported on click by `js/starter.js` (document editor toolbar), `js/live-reports.js` (report toolbar)
    and `ExportToPdfButtonRenderer` (the report widget button). It appends its own host to the page body and
    mounts into a shadow root of it. The Bulk PDF Export widget is the fourth caller and renders
    `ExportPopupModal` directly instead, being part of the same app. Its CSS is
    `ui/src/popup/export-popup.css`.

  Each shadow root carries its own CSS, so the extension now puts **no stylesheet on a Polarion page at
  all**. `css/pdf-exporter.css` is deleted and the injectors call no `injectStyle`; the toolbar buttons use
  Polarion's own classes plus generic's `css/dle-toolbar.css`. See [`ui/README.md`](ui/README.md) for the
  layering.
- **`webapp/pdf-exporter/js/modules/` is gone.** `ExportPopup.js`, `ExportPanel.js`, `ExportContext.js` and
  `ExportParams.js` were ported into the app: `ui/src/export/` (the shared export model - which rows a
  document type shows, a style package read into a form, a form turned into a request),
  `ui/src/services/exportContext.ts` (the location hash) and `ui/src/services/conversion.ts` (the convert-job
  protocol). Nothing is loaded across webapps at runtime any more. What is left in `webapp/pdf-exporter` is
  the three injector scripts, the empty `css/starter.css` trigger and the three HTML templates the Java
  renderer reads server-side (`sidePanelContent.html`, `pdfTemplate.html`, `headerAndFooter.html`).
- **The UI build comes from the generic parent**, activated by the presence of `ui/package.json` (its
  `vite-ui` profile): `npm ci` + `npm run build`, the bundle copied into `webapp/pdf-exporter-app/`, and
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
- **Package naming**: Use `ch.sbb.polarion.extension.pdf_exporter` (underscore). Pre-v7.0.0 code used `pdf.exporter` (dot) — don't follow old patterns still present in the codebase.
- **Maven Settings**: Builds require `.mvn/settings.xml` (JFrog, GitHub Packages, Sonatype credentials via env vars). CI passes it with `-s .mvn/settings.xml`.
- **Polarion Dependencies**: You must extract dependencies from the Polarion installer using [polarion-artifacts-deployer](https://github.com/SchweizerischeBundesbahnen/polarion-artifacts-deployer) before the Maven build will work.
- **Local Polarion Installation**: Requires `POLARION_HOME` environment variable. Use the `install-to-local-polarion` Maven profile: `mvn clean install -P install-to-local-polarion`
- **After any code change**: Delete `<POLARION_HOME>/data/workspace/.config` before restarting Polarion or changes won't be picked up.
- **Remote Debugging**: Add to Polarion's `config.sh`: `JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"`
- **Logging**: Polarion logs: `<POLARION_HOME>/polarion/logs/main/*.log`
- **Branch conventions**: Conventional commits enforced by commitizen (pre-commit hook). Feature branches: `feature/<name>`, bug fixes: `fix/<name>`, LTS branches: `release-v*` (e.g., `release-v6`).
- **Pre-commit hooks block internal patterns**: some org-specific identifiers are treated as secrets. Run `pre-commit run -a` after implementation.

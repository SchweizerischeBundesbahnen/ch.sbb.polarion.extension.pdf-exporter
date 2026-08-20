/*
 * One-tag DLE toolbar injector — adds the PDF Exporter button to Polarion's native document editor
 * toolbar. Configure a single script tag:
 *
 *   scriptInjection.dleEditorHead=<script src="/polarion/pdf-exporter/js/dle-toolbar.js"></script>
 *
 * Everything below the button's own identity - the toolbar selectors, the stable left-to-right order
 * across extensions, the permission check, and re-injection after GWT re-renders the toolbar - lives in
 * the shared engine, which installs itself from the data-* attributes on the script tag that loads it.
 *
 * The engine comes from react-sbb-polarion now, not from generic's webapp: it is emitted as a classic
 * script and copied next to this extension's built app, which is where the URL below points.
 */
(function () {
    // One timestamp per page load: a click reuses what the previous click loaded, while an updated
    // extension is still picked up on the next page open.
    const timestampParam = `?timestamp=${Date.now()}`;
    const APP_BASE = '/polarion/pdf-exporter-app/ui/app/';

    // The export dialog is a React module of the pdf-exporter-app webapp, imported on click. It mounts
    // into a shadow root of its own, so nothing has to be injected into the page for it.
    const openPopup = `import('${APP_BASE}assets/export-popup.js${timestampParam}')
        .then(module => module.openExportPopup({documentType: 'LIVE_DOC'}))
        .catch(console.error);`;

    const engine = document.createElement('script');
    engine.src = `${APP_BASE}assets/dle-toolbar-starter.js${timestampParam}`;
    engine.dataset.marker = 'pdf-exporter';
    engine.dataset.title = 'Export to PDF';
    engine.dataset.icon = `/polarion/ria/images/dle/operations/actionPdfExport16.svg${timestampParam}`;
    engine.dataset.onclick = openPopup;
    // GET → { permitted: boolean }. The engine appends the current project and injects the button
    // disabled until the answer arrives, fail-closed. Server-side authorization is enforced regardless.
    engine.dataset.permissionUrl = '/polarion/pdf-exporter/rest/internal/permissions/export';
    engine.onerror = function () {
        console.error('pdf-exporter: failed to load the DLE toolbar engine — toolbar injection skipped.');
    };
    document.head.appendChild(engine);
})();

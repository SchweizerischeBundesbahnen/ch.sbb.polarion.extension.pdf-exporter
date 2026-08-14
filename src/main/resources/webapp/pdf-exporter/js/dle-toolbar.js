/*
 * The document-editor toolbar button, configured by an administrator with a single tag:
 *
 *   scriptInjection.dleEditorHead=<script src="/polarion/pdf-exporter/js/dle-toolbar.js"></script>
 *
 * Everything about placing, styling, ordering, permission-gating and re-injecting the button lives
 * in react-sbb-polarion's shared engine (dle-toolbar-starter.js), built into this extension's own
 * app bundle. This file is only the configuration: append the engine with the button's details on
 * it and the engine installs itself from `document.currentScript`.
 *
 * The engine re-injects the button whenever Polarion (GWT) re-renders the toolbar, e.g. on save.
 */
(function () {
    // Extension web-context base, derived from this script's own URL (…/polarion/<ext>/js/dle-toolbar.js)
    // so nothing below hardcodes the /polarion/<ext>/ segment.
    const EXT_BASE = (document.currentScript && document.currentScript.src || '').replace(/js\/dle-toolbar\.js.*$/, '') || '/polarion/pdf-exporter/';
    // The React app's webapp, which serves both the engine and the export dialog.
    const APP_BASE = EXT_BASE.replace(/\/$/, '-app/ui/app/');
    // Captured once per page load, so a click reuses the module the previous click loaded while an
    // updated extension is still picked up on the next page open.
    const timestampParam = `?timestamp=${Date.now()}`;

    const engine = document.createElement('script');
    engine.src = `${APP_BASE}dle-toolbar-starter.js${timestampParam}`;
    engine.dataset.marker = 'pdf-exporter';
    engine.dataset.title = 'Export to PDF';
    engine.dataset.icon = `/polarion/ria/images/dle/operations/actionPdfExport16.svg`;
    // Runs as the button's onclick. The export dialog is a React module of the <ext>-app webapp,
    // imported on click; it mounts into a shadow root of its own, so nothing is injected for it.
    engine.dataset.onclick = `import('${APP_BASE}assets/export-popup.js${timestampParam}')
                                .then(module => module.openExportPopup({documentType: 'LIVE_DOC'}))
                                .catch(console.error);`;
    // The engine GETs this (with the current project appended), shows the button disabled until it
    // answers and keeps it disabled if it cannot. Server-side authorization is enforced regardless.
    engine.dataset.permissionUrl = `${EXT_BASE}rest/internal/permissions/export`;
    document.head.appendChild(engine);
})();

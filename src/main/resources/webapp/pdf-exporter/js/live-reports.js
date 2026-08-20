/*
 * Live Reports injector — the recommended way to enable PDF export in Polarion Live Reports.
 * Configure a single script tag (injected into every page):
 *
 *   scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/live-reports.js"></script>
 *
 * It injects an "Export to PDF" button into the native Live Report toolbar (the one behind
 * "Expand Tools") via the shared self-healing engine — no report modification needed. The button
 * appears only in view mode, once the toolbar is expanded. To keep that toolbar always expanded
 * (Polarion itself forgets the state on every page open), opt in with:
 *
 *   scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/live-reports.js" data-expand-tools="true"></script>
 *
 * Nothing else is put on the page. The button uses Polarion's own toolbar classes, and the export
 * dialog is a React module that mounts into a shadow root of its own, styles included — so this
 * script injects no stylesheet.
 *
 * This script replaces loading starter.js via mainHead (deprecated). The element ids match the
 * ones starter.js uses, so nothing is injected twice if both run.
 */
(function () {
    const timestampParam = `?timestamp=${Date.now()}`;

    // Extension web-context base, derived from this script's own URL (…/polarion/<ext>/js/live-reports.js)
    // so nothing below hardcodes the /polarion/<ext>/ segment.
    const EXT_BASE = (document.currentScript && document.currentScript.src || '').replace(/js\/live-reports\.js.*$/, '') || '/polarion/pdf-exporter/';
    // The toolbar engine is a react-sbb-polarion artifact, copied next to this extension's built app.
    const APP_BASE = '/polarion/pdf-exporter-app/ui/app/';

    // Read the opt-in for keeping the report toolbar always expanded from the script tag itself.
    const expandTools = !!(document.currentScript && document.currentScript.dataset.expandTools === 'true');

    // Capture config-execution order synchronously (like starter.js does) so several extensions'
    // report-toolbar buttons keep a stable left-to-right order on re-render.
    const seq = top.__genericDleToolbarSeq || (top.__genericDleToolbarSeq = { n: 0 });
    const myOrder = seq.n++;

    // Native separator (padding + splitter + padding, as Polarion renders between its own groups)
    // followed by an exact replica of Polarion's labeled toolbar buttons (e.g. Add Comment) so the
    // injected button inherits the native look, sizing and hover behavior
    // (polarion-Button-HighlightOnHover dims the icon and brightens it on hover; the label color
    // never changes).
    // The export dialog is a React module of the pdf-exporter-app webapp, imported on click. It mounts
    // itself into a shadow root of its own, so nothing has to be injected into the page for it. The
    // timestamp is captured once per page load, so a click reuses the module the previous click loaded
    // while an updated extension is still picked up on the next page open.
    const POPUP_MODULE = `/polarion/pdf-exporter-app/ui/app/assets/export-popup.js${timestampParam}`;

    const TOOLBAR_HTML = `
        <table class="dleToolBarTable">
            <tr class="dleToolBarRow">
                <td><div class="gwt-Label polarion-dle-toolbar-Padding"></div></td>
                <td><img src="/polarion/ria/images/toolbar_splitter_gray.gif" class="gwt-Image polarion-dle-ToolbarPanel-separator"></td>
                <td><div class="gwt-Label polarion-dle-toolbar-Padding"></div></td>
                <td style="vertical-align: middle;">
                    <table class="polarion-dle-toolbar-ButtonWithLabel polarion-Button-shared polarion-Button-HighlightOnHover"
                           role="button" cellpadding="0" cellspacing="0" title="Export to PDF" tabindex="0"
                           onclick="import('${POPUP_MODULE}')
                                      .then(module => module.openExportPopup({documentType: 'LIVE_REPORT'}))
                                      .catch(console.error);">
                        <colgroup><col><col></colgroup>
                        <tbody><tr>
                            <td class="polarion-Button-GridImpl-ImageCell"><img src="/polarion/ria/images/dle/operations/actionPdfExport16.svg" class="gwt-Image" alt="Export to PDF"></td>
                            <td class="polarion-Button-GridImpl-TextCell"><div class="gwt-Label">Export to PDF</div></td>
                        </tr></tbody>
                    </table>
                </td>
            </tr>
        </table>`;

    // Load the shared engine once across all extensions and resolve when it is ready. Using a single
    // shared promise (kept on `top`) is race-free for the multi-extension case: whichever extension's
    // live-reports.js runs first creates the <script> and the promise; the others reuse the same
    // promise, and `.then()` fires for every extension whether the engine is still loading or already
    // loaded (unlike a load-event listener, which is missed if the load already happened).
    // Shared with any extension still on generic's older engine: same id, same promise key, so one
    // engine is loaded per page and the buttons keep ordering against each other.
    const ENGINE_ID = 'generic-dle-toolbar-engine';
    function loadEngine(src) {
        if (!top.__genericDleToolbarEnginePromise) {
            top.__genericDleToolbarEnginePromise = new Promise((resolve) => {
                if (top.CommonDleToolbarStarter || window.CommonDleToolbarStarter) {
                    resolve();
                    return;
                }
                const existing = top.document.getElementById(ENGINE_ID);
                if (existing) {
                    existing.addEventListener('load', resolve);
                    return;
                }
                const script = top.document.createElement('script');
                script.id = ENGINE_ID;
                script.setAttribute('src', src);
                script.setAttribute('type', 'text/javascript');
                script.onload = resolve;
                top.document.head.appendChild(script);
            });
        }
        return top.__genericDleToolbarEnginePromise;
    }

    // Load the shared self-healing engine and inject the report-toolbar button through it.
    loadEngine(`${APP_BASE}dle-toolbar-starter.js${timestampParam}`).then(function () {
        const engine = top.CommonDleToolbarStarter || window.CommonDleToolbarStarter;
        if (!engine) {
            console.error("pdf-exporter: CommonDleToolbarStarter is not available after the engine loaded — Live Report toolbar button injection skipped.");
            return;
        }
        if (expandTools) {
            engine.autoExpandRichPageTools();
        }
        engine.create({
            markerId: 'pdf-exporter-rp-toolbar-injected',
            html: TOOLBAR_HTML,
            target: 'richPagePreview',
            order: myOrder
        }).injectToolbar();
    });
})();

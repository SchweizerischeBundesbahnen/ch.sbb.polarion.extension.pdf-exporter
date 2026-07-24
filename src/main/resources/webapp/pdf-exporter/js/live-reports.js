/*
 * Live Reports injector — the recommended way to enable PDF export in Polarion Live Reports.
 * Configure a single script tag (injected into every page):
 *
 *   scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/live-reports.js"></script>
 *
 * It provides two things:
 *
 * 1. An "Export to PDF" button in the native Live Report toolbar (the one behind "Expand Tools"),
 *    injected via the shared self-healing engine — no report modification needed. The button
 *    appears only in view mode, once the toolbar is expanded. To keep that toolbar always
 *    expanded (Polarion itself forgets the state on every page open), opt in with:
 *
 *   scriptInjection.mainHead=<script src="/polarion/pdf-exporter/js/live-reports.js" data-expand-tools="true"></script>
 *
 * 2. The stylesheets and micromodal library used by ExportPopup.js — both for the toolbar button
 *    above and for the classic "Export to PDF Button" report widget (which renders server-side
 *    and opens the same popup on click).
 *
 * This script replaces loading starter.js via mainHead (deprecated). The element ids match the
 * ones starter.js uses, so nothing is injected twice if both run.
 */
(function () {
    const timestampParam = `?timestamp=${Date.now()}`;

    // Extension web-context base, derived from this script's own URL (…/polarion/<ext>/js/live-reports.js)
    // so nothing below hardcodes the /polarion/<ext>/ segment.
    const EXT_BASE = (document.currentScript && document.currentScript.src || '').replace(/js\/live-reports\.js.*$/, '') || '/polarion/pdf-exporter/';

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
    const TOOLBAR_HTML = `
        <table class="dleToolBarTable">
            <tr class="dleToolBarRow">
                <td><div class="gwt-Label polarion-dle-toolbar-Padding"></div></td>
                <td><img src="/polarion/ria/images/toolbar_splitter_gray.gif" class="gwt-Image polarion-dle-ToolbarPanel-separator"></td>
                <td><div class="gwt-Label polarion-dle-toolbar-Padding"></div></td>
                <td style="vertical-align: middle;">
                    <table class="polarion-dle-toolbar-ButtonWithLabel polarion-Button-shared polarion-Button-HighlightOnHover pdf-rp-toolbar-button"
                           role="button" cellpadding="0" cellspacing="0" title="Export to PDF" tabindex="0"
                           onclick="import('${EXT_BASE}ui/js/modules/ExportPopup.js')
                                      .then(module => new module.default({documentType: 'LIVE_REPORT'}))
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

    function injectStyle(id, href) {
        if (!top.document.getElementById(id)) {
            const link = top.document.createElement('link');
            link.id = id;
            link.rel = 'stylesheet';
            link.type = 'text/css';
            link.href = href;
            top.document.head.appendChild(link);
        }
    }

    function injectScript(id, src, onload) {
        const existing = top.document.getElementById(id);
        if (!existing) {
            const script = top.document.createElement('script');
            script.id = id;
            script.setAttribute('src', src);
            script.setAttribute('type', 'text/javascript');
            if (onload) {
                script.onload = onload;
            }
            top.document.head.appendChild(script);
        } else if (onload) {
            // Another extension already injected this script (same id). If it has finished loading
            // (its global is available), run onload now; otherwise it is still in flight — wait for
            // its load event. Calling onload synchronously here would run before the engine is
            // defined, silently dropping this extension's button (multi-extension setup).
            if (top.GenericDleToolbarStarter || window.GenericDleToolbarStarter) {
                onload();
            } else {
                existing.addEventListener('load', onload);
            }
        }
    }

    injectStyle('pdf-exporter-styles', `${EXT_BASE}css/pdf-exporter.css${timestampParam}`);
    injectStyle('pdf-micromodal-styles', `${EXT_BASE}ui/generic/css/micromodal.css${timestampParam}`);
    injectStyle('generic-control-tokens', `${EXT_BASE}ui/generic/css/control-tokens.css${timestampParam}`);
    injectStyle('generic-checkbox-styles', `${EXT_BASE}ui/generic/css/checkboxes.css${timestampParam}`);
    injectStyle('generic-searchable-dropdown-styles', `${EXT_BASE}ui/generic/css/searchable-dropdown.css${timestampParam}`);
    injectStyle('generic-inputs-styles', `${EXT_BASE}ui/generic/css/inputs.css${timestampParam}`);
    injectStyle('generic-alerts-styles', `${EXT_BASE}ui/generic/css/alerts.css${timestampParam}`);
    injectScript('pdf-micromodal-script', `${EXT_BASE}ui/generic/js/micromodal.min.js${timestampParam}`);

    // Load the shared self-healing engine and inject the report-toolbar button through it.
    injectScript('generic-dle-toolbar-engine', `${EXT_BASE}ui/generic/js/dle-toolbar-starter.js${timestampParam}`, function () {
        const generic = top.GenericDleToolbarStarter || window.GenericDleToolbarStarter;
        if (!generic) {
            console.error("pdf-exporter: GenericDleToolbarStarter is not available after the engine loaded — Live Report toolbar button injection skipped.");
            return;
        }
        if (expandTools) {
            generic.autoExpandRichPageTools();
        }
        generic.create({
            markerId: 'pdf-exporter-rp-toolbar-injected',
            alternateHtml: TOOLBAR_HTML,
            target: 'richPagePreview',
            order: myOrder
        }).injectToolbar();
    });
})();
